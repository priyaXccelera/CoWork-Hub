package com.example.app.service;

import com.example.app.dto.UserDtos.UserRequest;
import com.example.app.dto.UserDtos.UserResponse;
import com.example.app.entity.BookingStatus;
import com.example.app.entity.MembershipPlan;
import com.example.app.entity.User;
import com.example.app.entity.UserStatus;
import com.example.app.exception.ConflictException;
import com.example.app.exception.ResourceNotFoundException;
import com.example.app.repository.BookingRepository;
import com.example.app.repository.MembershipPlanRepository;
import com.example.app.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

  private static final List<BookingStatus> ACTIVE_STATUSES =
      List.of(BookingStatus.CONFIRMED, BookingStatus.WAITLISTED);

  private final UserRepository userRepository;
  private final MembershipPlanRepository membershipPlanRepository;
  private final ApiKeyService apiKeyService;
  private final BookingRepository bookingRepository;

  public UserService(
      UserRepository userRepository,
      MembershipPlanRepository membershipPlanRepository,
      ApiKeyService apiKeyService,
      BookingRepository bookingRepository) {
    this.userRepository = userRepository;
    this.membershipPlanRepository = membershipPlanRepository;
    this.apiKeyService = apiKeyService;
    this.bookingRepository = bookingRepository;
  }

  public UserResponse create(UserRequest request) {
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new ConflictException("A user with this email already exists: " + request.getEmail());
    }

    User user = new User();
    applyRequest(user, request, true);

    User saved = userRepository.save(user);

    // The raw API key is only ever available here, right after creation. It is not persisted in
    // plaintext on the user record and will never be returned again by any other endpoint.
    String rawKey =
        apiKeyService.generateAndStoreRawKeyForUser(
            saved.getId(), saved.getRole(), saved.getName());

    UserResponse response = toResponse(saved);
    response.setApiKey(rawKey);
    return response;
  }

  @Transactional(readOnly = true)
  public Page<UserResponse> list(Pageable pageable) {
    return userRepository.findByDeletedFalse(pageable).map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public UserResponse get(Long id) {
    return toResponse(findEntity(id));
  }

  public UserResponse update(Long id, UserRequest request) {
    User user = findEntity(id);
    applyRequest(user, request, false);
    User saved = userRepository.save(user);
    return toResponse(saved);
  }

  public void delete(Long id) {
    User user = findEntity(id);

    boolean hasActiveOrFutureBookings =
        bookingRepository.existsByUserIdAndStatusInAndEndTimeAfter(
            id, ACTIVE_STATUSES, LocalDateTime.now());
    if (hasActiveOrFutureBookings) {
      throw new ConflictException(
          "Cannot delete user with id "
              + id
              + ": they have active or future bookings. Cancel those bookings first.");
    }

    user.setDeleted(true);
    user.setStatus(UserStatus.INACTIVE);
    userRepository.save(user);

    // Immediately revoke any API keys tied to this user so they can no longer authenticate.
    apiKeyService.revokeAllForUser(id);
  }

  public User findEntity(Long id) {
    return userRepository
        .findByIdAndDeletedFalse(id)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
  }

  private void applyRequest(User user, UserRequest request, boolean isCreate) {
    user.setName(request.getName());
    user.setEmail(request.getEmail());
    user.setRole(request.getRole());

    if (request.getMembershipPlanId() != null) {
      MembershipPlan plan =
          membershipPlanRepository
              .findById(request.getMembershipPlanId())
              .orElseThrow(
                  () ->
                      new ResourceNotFoundException(
                          "Membership plan not found with id: " + request.getMembershipPlanId()));
      user.setMembershipPlan(plan);
      if (isCreate) {
        user.setCreditHoursRemaining(
            request.getCreditHoursRemaining() != null
                ? request.getCreditHoursRemaining()
                : plan.getIncludedCreditHours());
      }
    } else if (isCreate) {
      user.setCreditHoursRemaining(
          request.getCreditHoursRemaining() != null ? request.getCreditHoursRemaining() : 0.0);
    }

    if (!isCreate && request.getCreditHoursRemaining() != null) {
      user.setCreditHoursRemaining(request.getCreditHoursRemaining());
    }

    if (request.getStatus() != null) {
      user.setStatus(request.getStatus());
    } else if (isCreate) {
      user.setStatus(UserStatus.ACTIVE);
    }
  }

  private UserResponse toResponse(User user) {
    UserResponse response = new UserResponse();
    response.setId(user.getId());
    response.setName(user.getName());
    response.setEmail(user.getEmail());
    response.setRole(user.getRole());
    // apiKey is intentionally left null here: raw keys are only ever exposed once, at creation
    // time, via UserService#create. They are never stored in plaintext nor returned again.
    if (user.getMembershipPlan() != null) {
      response.setMembershipPlanId(user.getMembershipPlan().getId());
      response.setMembershipPlanName(user.getMembershipPlan().getName());
    }
    response.setCreditHoursRemaining(user.getCreditHoursRemaining());
    response.setStatus(user.getStatus());
    response.setCreatedAt(user.getCreatedAt());
    response.setUpdatedAt(user.getUpdatedAt());
    return response;
  }
}
