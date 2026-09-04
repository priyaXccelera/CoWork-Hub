package com.example.app.service;

import com.example.app.dto.BookingDtos.BookingRequest;
import com.example.app.dto.BookingDtos.BookingResponse;
import com.example.app.entity.Booking;
import com.example.app.entity.BookingStatus;
import com.example.app.entity.MembershipPlan;
import com.example.app.entity.Space;
import com.example.app.entity.SpaceType;
import com.example.app.entity.User;
import com.example.app.entity.Waitlist;
import com.example.app.entity.WaitlistStatus;
import com.example.app.exception.BusinessRuleException;
import com.example.app.exception.ForbiddenException;
import com.example.app.exception.ResourceNotFoundException;
import com.example.app.repository.BookingRepository;
import com.example.app.repository.BookingSpecifications;
import com.example.app.repository.SpaceRepository;
import com.example.app.repository.UserRepository;
import com.example.app.repository.WaitlistRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

  private static final int FREE_CANCELLATION_WINDOW_HOURS = 2;
  private static final BigDecimal LATE_CANCELLATION_FEE_RATE = new BigDecimal("0.25");

  private final BookingRepository bookingRepository;
  private final SpaceRepository spaceRepository;
  private final UserRepository userRepository;
  private final WaitlistRepository waitlistRepository;

  public BookingService(
      BookingRepository bookingRepository,
      SpaceRepository spaceRepository,
      UserRepository userRepository,
      WaitlistRepository waitlistRepository) {
    this.bookingRepository = bookingRepository;
    this.spaceRepository = spaceRepository;
    this.userRepository = userRepository;
    this.waitlistRepository = waitlistRepository;
  }

  @Transactional
  public BookingResponse createBooking(BookingRequest request, Long actorUserId, boolean isAdmin) {
    Long effectiveUserId = isAdmin ? request.getUserId() : actorUserId;
    if (effectiveUserId == null) {
      throw new BusinessRuleException("userId is required when creating a booking as an admin");
    }
    if (!request.getEndTime().isAfter(request.getStartTime())) {
      throw new BusinessRuleException("endTime must be after startTime");
    }

    User user =
        userRepository
            .findByIdAndDeletedFalse(effectiveUserId)
            .orElseThrow(
                () -> new ResourceNotFoundException("User not found with id: " + effectiveUserId));

    Space space =
        spaceRepository
            .findByIdForUpdate(request.getSpaceId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Space not found with id: " + request.getSpaceId()));

    if (space.isDeleted() || !space.isActive()) {
      throw new BusinessRuleException("Space is not available for booking: " + space.getId());
    }

    List<Booking> overlapping =
        bookingRepository.findOverlappingConfirmed(
            space.getId(), request.getStartTime(), request.getEndTime());

    Booking booking = new Booking();
    booking.setUserId(effectiveUserId);
    booking.setSpaceId(space.getId());
    booking.setStartTime(request.getStartTime());
    booking.setEndTime(request.getEndTime());

    if (!overlapping.isEmpty()) {
      booking.setStatus(BookingStatus.WAITLISTED);
      booking.setCostCharged(BigDecimal.ZERO);
      booking.setCreditHoursUsed(0.0);
      Booking saved = bookingRepository.save(booking);

      Waitlist waitlist = new Waitlist();
      waitlist.setUserId(effectiveUserId);
      waitlist.setSpaceId(space.getId());
      waitlist.setRequestedStart(request.getStartTime());
      waitlist.setRequestedEnd(request.getEndTime());
      waitlist.setStatus(WaitlistStatus.WAITING);
      waitlist.setBookingId(saved.getId());
      waitlistRepository.save(waitlist);

      return toResponse(saved);
    }

    double hours = hoursBetween(request.getStartTime(), request.getEndTime());
    CostResult costResult = computeCost(user, space, hours);

    user.setCreditHoursRemaining(user.getCreditHoursRemaining() - costResult.creditHoursUsed);
    userRepository.save(user);

    booking.setStatus(BookingStatus.CONFIRMED);
    booking.setCostCharged(costResult.cost);
    booking.setCreditHoursUsed(costResult.creditHoursUsed);

    Booking saved = bookingRepository.save(booking);
    return toResponse(saved);
  }

  public Page<BookingResponse> list(
      Long actorUserId,
      boolean isAdmin,
      LocalDate date,
      BookingStatus status,
      SpaceType spaceType,
      Pageable pageable) {
    Long filterUserId = isAdmin ? null : actorUserId;

    List<Long> spaceIds = null;
    if (spaceType != null) {
      spaceIds =
          spaceRepository.findByDeletedFalseAndType(spaceType).stream()
              .map(Space::getId)
              .collect(Collectors.toList());
    }

    var spec = BookingSpecifications.build(filterUserId, date, status, spaceIds);
    return bookingRepository.findAll(spec, pageable).map(this::toResponse);
  }

  public BookingResponse get(Long id, Long actorUserId, boolean isAdmin) {
    Booking booking = findEntity(id);
    if (!isAdmin && !booking.getUserId().equals(actorUserId)) {
      throw new ForbiddenException("You are not allowed to access this booking");
    }
    return toResponse(booking);
  }

  @Transactional
  public BookingResponse cancelBooking(Long id, Long actorUserId, boolean isAdmin) {
    Booking booking = findEntity(id);

    if (!isAdmin && !booking.getUserId().equals(actorUserId)) {
      throw new ForbiddenException("You are not allowed to cancel this booking");
    }

    if (booking.getStatus() != BookingStatus.CONFIRMED
        && booking.getStatus() != BookingStatus.WAITLISTED) {
      throw new BusinessRuleException(
          "Booking cannot be cancelled from status: " + booking.getStatus());
    }

    if (booking.getStatus() == BookingStatus.WAITLISTED) {
      booking.setStatus(BookingStatus.CANCELLED);
      bookingRepository.save(booking);

      waitlistRepository
          .findBySpaceIdAndStatusOrderByCreatedAtAsc(booking.getSpaceId(), WaitlistStatus.WAITING)
          .stream()
          .filter(w -> w.getBookingId().equals(booking.getId()))
          .findFirst()
          .ifPresent(
              w -> {
                w.setStatus(WaitlistStatus.CANCELLED);
                waitlistRepository.save(w);
              });

      return toResponse(booking);
    }

    LocalDateTime now = LocalDateTime.now();
    boolean freeCancellation =
        now.isBefore(booking.getStartTime().minusHours(FREE_CANCELLATION_WINDOW_HOURS));

    User user = userRepository.findByIdAndDeletedFalse(booking.getUserId()).orElse(null);

    if (freeCancellation) {
      if (user != null
          && booking.getCreditHoursUsed() != null
          && booking.getCreditHoursUsed() > 0) {
        user.setCreditHoursRemaining(user.getCreditHoursRemaining() + booking.getCreditHoursUsed());
        userRepository.save(user);
      }
      booking.setCostCharged(BigDecimal.ZERO);
      booking.setCreditHoursUsed(0.0);
    } else {
      BigDecimal fee =
          booking
              .getCostCharged()
              .multiply(LATE_CANCELLATION_FEE_RATE)
              .setScale(2, RoundingMode.HALF_UP);
      booking.setCostCharged(fee);
    }

    booking.setStatus(BookingStatus.CANCELLED);
    Booking saved = bookingRepository.save(booking);

    promoteWaitlistForSpace(booking.getSpaceId());

    return toResponse(saved);
  }

  private void promoteWaitlistForSpace(Long spaceId) {
    Optional<Waitlist> earliest =
        waitlistRepository.findFirstBySpaceIdAndStatusOrderByCreatedAtAsc(
            spaceId, WaitlistStatus.WAITING);
    if (earliest.isEmpty()) {
      return;
    }
    Waitlist waitlist = earliest.get();

    List<Booking> overlapping =
        bookingRepository.findOverlappingConfirmed(
            spaceId, waitlist.getRequestedStart(), waitlist.getRequestedEnd());
    if (!overlapping.isEmpty()) {
      return;
    }

    Booking waitlistedBooking = bookingRepository.findById(waitlist.getBookingId()).orElse(null);
    if (waitlistedBooking == null || waitlistedBooking.getStatus() != BookingStatus.WAITLISTED) {
      return;
    }

    User user = userRepository.findByIdAndDeletedFalse(waitlist.getUserId()).orElse(null);
    Space space = spaceRepository.findById(spaceId).orElse(null);
    if (user == null || space == null) {
      return;
    }

    double hours = hoursBetween(waitlistedBooking.getStartTime(), waitlistedBooking.getEndTime());
    CostResult costResult = computeCost(user, space, hours);

    user.setCreditHoursRemaining(user.getCreditHoursRemaining() - costResult.creditHoursUsed);
    userRepository.save(user);

    waitlistedBooking.setStatus(BookingStatus.CONFIRMED);
    waitlistedBooking.setCostCharged(costResult.cost);
    waitlistedBooking.setCreditHoursUsed(costResult.creditHoursUsed);
    bookingRepository.save(waitlistedBooking);

    waitlist.setStatus(WaitlistStatus.PROMOTED);
    waitlistRepository.save(waitlist);
  }

  private CostResult computeCost(User user, Space space, double hours) {
    MembershipPlan plan = user.getMembershipPlan();

    if (plan != null) {
      double creditAvailable =
          user.getCreditHoursRemaining() == null ? 0.0 : user.getCreditHoursRemaining();
      double creditUsed = Math.min(hours, Math.max(creditAvailable, 0.0));
      double overageHours = hours - creditUsed;
      BigDecimal overageCost =
          plan.getOverageRatePerHour()
              .multiply(BigDecimal.valueOf(overageHours))
              .setScale(2, RoundingMode.HALF_UP);
      return new CostResult(overageCost, creditUsed);
    }

    BigDecimal cost =
        space.getHourlyRate().multiply(BigDecimal.valueOf(hours)).setScale(2, RoundingMode.HALF_UP);
    return new CostResult(cost, 0.0);
  }

  private double hoursBetween(LocalDateTime start, LocalDateTime end) {
    return Duration.between(start, end).toMinutes() / 60.0;
  }

  private Booking findEntity(Long id) {
    return bookingRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
  }

  private BookingResponse toResponse(Booking booking) {
    BookingResponse response = new BookingResponse();
    response.setId(booking.getId());
    response.setUserId(booking.getUserId());
    response.setSpaceId(booking.getSpaceId());
    response.setStartTime(booking.getStartTime());
    response.setEndTime(booking.getEndTime());
    response.setStatus(booking.getStatus());
    response.setCostCharged(booking.getCostCharged());
    response.setCreditHoursUsed(booking.getCreditHoursUsed());
    response.setCreatedAt(booking.getCreatedAt());
    response.setUpdatedAt(booking.getUpdatedAt());
    return response;
  }

  private static final class CostResult {
    private final BigDecimal cost;
    private final double creditHoursUsed;

    private CostResult(BigDecimal cost, double creditHoursUsed) {
      this.cost = cost;
      this.creditHoursUsed = creditHoursUsed;
    }
  }
}
