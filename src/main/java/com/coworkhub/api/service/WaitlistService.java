package com.coworkhub.api.service;

import com.coworkhub.api.dto.WaitlistDtos.WaitlistJoinRequest;
import com.coworkhub.api.dto.WaitlistDtos.WaitlistResponse;
import com.coworkhub.api.entity.Booking;
import com.coworkhub.api.entity.BookingStatus;
import com.coworkhub.api.entity.Space;
import com.coworkhub.api.entity.User;
import com.coworkhub.api.entity.Waitlist;
import com.coworkhub.api.entity.WaitlistStatus;
import com.coworkhub.api.exception.BusinessRuleException;
import com.coworkhub.api.exception.ConflictException;
import com.coworkhub.api.exception.ResourceNotFoundException;
import com.coworkhub.api.repository.BookingRepository;
import com.coworkhub.api.repository.SpaceRepository;
import com.coworkhub.api.repository.UserRepository;
import com.coworkhub.api.repository.WaitlistRepository;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WaitlistService {

  private final WaitlistRepository waitlistRepository;
  private final BookingRepository bookingRepository;
  private final SpaceRepository spaceRepository;
  private final UserRepository userRepository;

  public WaitlistService(
      WaitlistRepository waitlistRepository,
      BookingRepository bookingRepository,
      SpaceRepository spaceRepository,
      UserRepository userRepository) {
    this.waitlistRepository = waitlistRepository;
    this.bookingRepository = bookingRepository;
    this.spaceRepository = spaceRepository;
    this.userRepository = userRepository;
  }

  /**
   * Explicitly joins the waitlist for a space/time-slot (typically called by a Member after
   * receiving a 409 Conflict from POST /bookings because the space is fully booked). Creates a
   * placeholder WAITLISTED booking plus a linked waitlist entry; the placeholder is later promoted
   * to CONFIRMED automatically when capacity frees up.
   */
  public WaitlistResponse join(WaitlistJoinRequest request, Long actorUserId, boolean isAdmin) {
    Long effectiveUserId =
        isAdmin && request.getUserId() != null ? request.getUserId() : actorUserId;
    if (effectiveUserId == null) {
      throw new BusinessRuleException("userId is required when joining a waitlist as an admin");
    }
    if (!request.getRequestedEnd().isAfter(request.getRequestedStart())) {
      throw new BusinessRuleException("requestedEnd must be after requestedStart");
    }

    User user =
        userRepository
            .findByIdAndDeletedFalse(effectiveUserId)
            .orElseThrow(
                () -> new ResourceNotFoundException("User not found with id: " + effectiveUserId));

    Space space =
        spaceRepository
            .findByIdAndDeletedFalse(request.getSpaceId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Space not found with id: " + request.getSpaceId()));

    if (!space.isActive()) {
      throw new ConflictException("Space is not available for booking: " + space.getId());
    }

    Booking booking = new Booking();
    booking.setUserId(user.getId());
    booking.setSpaceId(space.getId());
    booking.setStartTime(request.getRequestedStart());
    booking.setEndTime(request.getRequestedEnd());
    booking.setStatus(BookingStatus.WAITLISTED);
    booking.setCostCharged(BigDecimal.ZERO);
    booking.setOriginalCost(BigDecimal.ZERO);
    booking.setCreditHoursUsed(0.0);
    Booking savedBooking = bookingRepository.save(booking);

    Waitlist waitlist = new Waitlist();
    waitlist.setUserId(user.getId());
    waitlist.setSpaceId(space.getId());
    waitlist.setRequestedStart(request.getRequestedStart());
    waitlist.setRequestedEnd(request.getRequestedEnd());
    waitlist.setStatus(WaitlistStatus.WAITING);
    waitlist.setBookingId(savedBooking.getId());
    Waitlist saved = waitlistRepository.save(waitlist);

    return toResponse(saved);
  }

  @Transactional(readOnly = true)
  public Page<WaitlistResponse> list(Long actorUserId, boolean isAdmin, Pageable pageable) {
    if (isAdmin) {
      return waitlistRepository.findAll(pageable).map(this::toResponse);
    }
    return waitlistRepository.findByUserId(actorUserId, pageable).map(this::toResponse);
  }

  private WaitlistResponse toResponse(Waitlist waitlist) {
    WaitlistResponse response = new WaitlistResponse();
    response.setId(waitlist.getId());
    response.setUserId(waitlist.getUserId());
    response.setSpaceId(waitlist.getSpaceId());
    response.setRequestedStart(waitlist.getRequestedStart());
    response.setRequestedEnd(waitlist.getRequestedEnd());
    response.setStatus(waitlist.getStatus());
    response.setBookingId(waitlist.getBookingId());
    response.setCreatedAt(waitlist.getCreatedAt());
    response.setUpdatedAt(waitlist.getUpdatedAt());
    return response;
  }
}
