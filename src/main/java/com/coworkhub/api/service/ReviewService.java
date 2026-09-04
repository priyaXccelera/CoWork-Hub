package com.coworkhub.api.service;

import com.coworkhub.api.dto.ReviewDtos.ReviewRequest;
import com.coworkhub.api.dto.ReviewDtos.ReviewResponse;
import com.coworkhub.api.dto.ReviewDtos.ReviewUpdateRequest;
import com.coworkhub.api.entity.Booking;
import com.coworkhub.api.entity.BookingStatus;
import com.coworkhub.api.entity.Review;
import com.coworkhub.api.exception.BusinessRuleException;
import com.coworkhub.api.exception.ConflictException;
import com.coworkhub.api.exception.ForbiddenException;
import com.coworkhub.api.exception.ResourceNotFoundException;
import com.coworkhub.api.repository.BookingRepository;
import com.coworkhub.api.repository.ReviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReviewService {

  private final ReviewRepository reviewRepository;
  private final BookingRepository bookingRepository;

  public ReviewService(ReviewRepository reviewRepository, BookingRepository bookingRepository) {
    this.reviewRepository = reviewRepository;
    this.bookingRepository = bookingRepository;
  }

  /**
   * Creates a review for the booking supplied in the request. The space is always derived from the
   * booking itself (never trusted from the caller) so a review can never be attached to a space the
   * booking doesn't actually belong to.
   */
  public ReviewResponse create(ReviewRequest request, Long actorUserId) {
    Booking booking =
        bookingRepository
            .findById(request.getBookingId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Booking not found with id: " + request.getBookingId()));

    if (!booking.getUserId().equals(actorUserId)) {
      throw new ForbiddenException("You can only review your own bookings");
    }

    if (booking.getStatus() != BookingStatus.COMPLETED) {
      throw new BusinessRuleException(
          "A review can only be submitted for a booking with status COMPLETED");
    }

    if (reviewRepository.existsByBookingIdAndDeletedFalse(booking.getId())) {
      throw new ConflictException("A review already exists for booking id: " + booking.getId());
    }

    Review review = new Review();
    review.setUserId(actorUserId);
    review.setSpaceId(booking.getSpaceId());
    review.setBookingId(booking.getId());
    review.setRating(request.getRating());
    review.setComment(request.getComment());
    Review saved = reviewRepository.save(review);
    return toResponse(saved);
  }

  @Transactional(readOnly = true)
  public Page<ReviewResponse> listBySpace(Long spaceId, Pageable pageable) {
    return reviewRepository.findBySpaceIdAndDeletedFalse(spaceId, pageable).map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public ReviewResponse get(Long id) {
    return toResponse(findEntity(id));
  }

  public ReviewResponse update(Long id, ReviewUpdateRequest request, Long actorUserId) {
    Review review = findEntity(id);
    if (!review.getUserId().equals(actorUserId)) {
      throw new ForbiddenException("You can only edit your own review");
    }
    review.setRating(request.getRating());
    review.setComment(request.getComment());
    Review saved = reviewRepository.save(review);
    return toResponse(saved);
  }

  public void delete(Long id, Long actorUserId, boolean isAdmin) {
    Review review = findEntity(id);
    if (!isAdmin && !review.getUserId().equals(actorUserId)) {
      throw new ForbiddenException("You can only delete your own review");
    }
    review.setDeleted(true);
    reviewRepository.save(review);
  }

  private Review findEntity(Long id) {
    return reviewRepository
        .findByIdAndDeletedFalse(id)
        .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + id));
  }

  private ReviewResponse toResponse(Review review) {
    ReviewResponse response = new ReviewResponse();
    response.setId(review.getId());
    response.setUserId(review.getUserId());
    response.setSpaceId(review.getSpaceId());
    response.setBookingId(review.getBookingId());
    response.setRating(review.getRating());
    response.setComment(review.getComment());
    response.setCreatedAt(review.getCreatedAt());
    response.setUpdatedAt(review.getUpdatedAt());
    return response;
  }
}
