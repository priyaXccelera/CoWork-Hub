package com.coworkhub.api.service;

import com.coworkhub.api.dto.BookingDtos.BookingRequest;
import com.coworkhub.api.dto.BookingDtos.BookingResponse;
import com.coworkhub.api.entity.Booking;
import com.coworkhub.api.entity.BookingStatus;
import com.coworkhub.api.entity.MembershipPlan;
import com.coworkhub.api.entity.NotificationType;
import com.coworkhub.api.entity.Space;
import com.coworkhub.api.entity.SpaceType;
import com.coworkhub.api.entity.User;
import com.coworkhub.api.entity.Waitlist;
import com.coworkhub.api.entity.WaitlistStatus;
import com.coworkhub.api.exception.BusinessRuleException;
import com.coworkhub.api.exception.ConflictException;
import com.coworkhub.api.exception.ForbiddenException;
import com.coworkhub.api.exception.ResourceNotFoundException;
import com.coworkhub.api.repository.BookingRepository;
import com.coworkhub.api.repository.BookingSpecifications;
import com.coworkhub.api.repository.SpaceRepository;
import com.coworkhub.api.repository.UserRepository;
import com.coworkhub.api.repository.WaitlistRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
  private final NotificationService notificationService;

  public BookingService(
      BookingRepository bookingRepository,
      SpaceRepository spaceRepository,
      UserRepository userRepository,
      WaitlistRepository waitlistRepository,
      NotificationService notificationService) {
    this.bookingRepository = bookingRepository;
    this.spaceRepository = spaceRepository;
    this.userRepository = userRepository;
    this.waitlistRepository = waitlistRepository;
    this.notificationService = notificationService;
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
      throw new ConflictException("Space is not available for booking: " + space.getId());
    }

    // Capacity-aware conflict detection: a space can host up to `capacity` concurrent CONFIRMED
    // bookings. Only once that capacity is exhausted for the requested slot do we reject the
    // request with 409 Conflict (the caller can then explicitly join the waitlist).
    List<Booking> overlapping =
        bookingRepository.findOverlappingConfirmed(
            space.getId(), request.getStartTime(), request.getEndTime());
    int capacity = space.getCapacity() == null ? 1 : space.getCapacity();
    if (overlapping.size() >= capacity) {
      throw new ConflictException(
          "Space '"
              + space.getName()
              + "' is fully booked for the requested time slot (capacity: "
              + capacity
              + "). Join the waitlist instead via POST /api/v1/waitlist.");
    }

    double hours = hoursBetween(request.getStartTime(), request.getEndTime());
    BigDecimal originalCost = valuationCost(space, hours);
    CostResult costResult = computeCost(user, space, hours);

    user.setCreditHoursRemaining(user.getCreditHoursRemaining() - costResult.creditHoursUsed);
    userRepository.save(user);

    Booking booking = new Booking();
    booking.setUserId(effectiveUserId);
    booking.setSpaceId(space.getId());
    booking.setStartTime(request.getStartTime());
    booking.setEndTime(request.getEndTime());
    booking.setStatus(BookingStatus.CONFIRMED);
    booking.setCostCharged(costResult.cost);
    booking.setOriginalCost(originalCost);
    booking.setCreditHoursUsed(costResult.creditHoursUsed);

    Booking saved = bookingRepository.save(booking);
    notificationService.createSafely(
        saved.getUserId(),
        NotificationType.BOOKING_CONFIRMED,
        "Booking confirmed",
        "Your booking for " + space.getName() + " has been confirmed.",
        "BOOKING",
        saved.getId());
    return toResponse(saved);
  }

  @Transactional(readOnly = true)
  public Page<BookingResponse> list(
      Long actorUserId,
      boolean isAdmin,
      LocalDate date,
      LocalDateTime from,
      LocalDateTime to,
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

    var spec = BookingSpecifications.build(filterUserId, date, from, to, status, spaceIds);
    return bookingRepository.findAll(spec, pageable).map(this::toResponse);
  }

  @Transactional(readOnly = true)
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
      throw new ConflictException(
          "Booking cannot be cancelled from status: " + booking.getStatus());
    }

    if (booking.getStatus() == BookingStatus.WAITLISTED) {
      booking.setStatus(BookingStatus.CANCELLED);
      bookingRepository.save(booking);
      notificationService.createSafely(
          booking.getUserId(),
          NotificationType.BOOKING_CANCELLED,
          "Booking cancelled",
          "Your waitlisted booking has been cancelled.",
          "BOOKING",
          booking.getId());

      waitlistRepository
          .findBySpaceIdAndStatusOrderByCreatedAtAsc(booking.getSpaceId(), WaitlistStatus.WAITING)
          .stream()
          .filter(w -> booking.getId().equals(w.getBookingId()))
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

    // Cancelling always releases the space and refunds any membership credit hours that had been
    // reserved for the booking - the user never actually used them.
    if (user != null && booking.getCreditHoursUsed() != null && booking.getCreditHoursUsed() > 0) {
      user.setCreditHoursRemaining(user.getCreditHoursRemaining() + booking.getCreditHoursUsed());
      userRepository.save(user);
    }
    booking.setCreditHoursUsed(0.0);

    if (freeCancellation) {
      booking.setCostCharged(BigDecimal.ZERO);
    } else {
      // The late-cancellation fee is 25% of what the booking was actually worth (originalCost),
      // not just the cash portion that was charged - otherwise bookings paid entirely out of
      // membership credit hours would incur a $0 fee.
      BigDecimal baseValue =
          booking.getOriginalCost() != null ? booking.getOriginalCost() : booking.getCostCharged();
      BigDecimal fee =
          baseValue.multiply(LATE_CANCELLATION_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
      booking.setCostCharged(fee);
    }

    booking.setStatus(BookingStatus.CANCELLED);
    Booking saved = bookingRepository.save(booking);

    notificationService.createSafely(
        saved.getUserId(),
        NotificationType.BOOKING_CANCELLED,
        "Booking cancelled",
        "Your booking has been cancelled.",
        "BOOKING",
        saved.getId());
    promoteWaitlistForSpace(booking.getSpaceId());

    return toResponse(saved);
  }

  /**
   * Attempts to promote every WAITING waitlist entry for the given space, in first-come-first
   * served (createdAt) order. Each candidate is evaluated independently against the space's current
   * capacity: a candidate whose requested slot still conflicts is skipped (left WAITING) without
   * blocking promotion of later candidates whose slot may already be free.
   */
  private void promoteWaitlistForSpace(Long spaceId) {
    List<Waitlist> waitingEntries =
        waitlistRepository.findBySpaceIdAndStatusOrderByCreatedAtAsc(
            spaceId, WaitlistStatus.WAITING);
    if (waitingEntries.isEmpty()) {
      return;
    }

    Space space = spaceRepository.findById(spaceId).orElse(null);
    if (space == null) {
      return;
    }
    int capacity = space.getCapacity() == null ? 1 : space.getCapacity();

    for (Waitlist waitlist : waitingEntries) {
      List<Booking> overlapping =
          bookingRepository.findOverlappingConfirmed(
              spaceId, waitlist.getRequestedStart(), waitlist.getRequestedEnd());
      if (overlapping.size() >= capacity) {
        // Still full for this candidate's slot; try the next waiting candidate instead of
        // stopping entirely.
        continue;
      }

      User user = userRepository.findByIdAndDeletedFalse(waitlist.getUserId()).orElse(null);
      if (user == null) {
        continue;
      }

      Booking targetBooking = null;
      if (waitlist.getBookingId() != null) {
        targetBooking = bookingRepository.findById(waitlist.getBookingId()).orElse(null);
        if (targetBooking != null && targetBooking.getStatus() != BookingStatus.WAITLISTED) {
          continue;
        }
      }
      if (targetBooking == null) {
        targetBooking = new Booking();
        targetBooking.setUserId(waitlist.getUserId());
        targetBooking.setSpaceId(spaceId);
        targetBooking.setStartTime(waitlist.getRequestedStart());
        targetBooking.setEndTime(waitlist.getRequestedEnd());
      }

      double hours = hoursBetween(waitlist.getRequestedStart(), waitlist.getRequestedEnd());
      BigDecimal originalCost = valuationCost(space, hours);
      CostResult costResult = computeCost(user, space, hours);

      user.setCreditHoursRemaining(user.getCreditHoursRemaining() - costResult.creditHoursUsed);
      userRepository.save(user);

      targetBooking.setStatus(BookingStatus.CONFIRMED);
      targetBooking.setCostCharged(costResult.cost);
      targetBooking.setOriginalCost(originalCost);
      targetBooking.setCreditHoursUsed(costResult.creditHoursUsed);
      Booking promotedBooking = bookingRepository.save(targetBooking);
      notificationService.createSafely(
          promotedBooking.getUserId(),
          NotificationType.WAITLIST_PROMOTED,
          "Waitlist booking confirmed",
          "A space is available and your waitlisted booking has been confirmed.",
          "BOOKING",
          promotedBooking.getId());

      waitlist.setStatus(WaitlistStatus.PROMOTED);
      waitlist.setBookingId(targetBooking.getId());
      waitlistRepository.save(waitlist);
    }
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

  /** The full market value of a booking, independent of payment method (cash vs credit). */
  private BigDecimal valuationCost(Space space, double hours) {
    return space
        .getHourlyRate()
        .multiply(BigDecimal.valueOf(hours))
        .setScale(2, RoundingMode.HALF_UP);
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
    response.setOriginalCost(booking.getOriginalCost());
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
