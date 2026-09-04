package com.example.app.service;

import com.example.app.dto.UserDtos.UserRequest;
import com.example.app.dto.UserDtos.UserResponse;
import com.example.app.entity.MembershipPlan;
import com.example.app.entity.User;
import com.example.app.entity.UserStatus;
import com.example.app.exception.BusinessRuleException;
import com.example.app.exception.ResourceNotFoundException;
import com.example.app.repository.MembershipPlanRepository;
import com.example.app.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class UserService {

  private final UserRepository userRepository;
  private final MembershipPlanRepository membershipPlanRepository;
  private final ApiKeyService apiKeyService;

  public UserService(
      UserRepository userRepository,
      MembershipPlanRepository membershipPlanRepository,
      ApiKeyService apiKeyService) {
    this.userRepository = userRepository;
    this.membershipPlanRepository = membershipPlanRepository;
    this.apiKeyService = apiKeyService;
  }

  public UserResponse create(UserRequest request) {
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new BusinessRuleException(
          "A user with this email already exists: " + request.getEmail());
    }

    User user = new User();
    applyRequest(user, request, true);

    User saved = userRepository.save(user);

    String rawKey =
        apiKeyService.generateAndStoreRawKeyForUser(
            saved.getId(), saved.getRole(), saved.getName());
    saved.setApiKey(rawKey);
    saved = userRepository.save(saved);

    return toResponse(saved);
  }

  public Page<UserResponse> list(Pageable pageable) {
    return userRepository.findByDeletedFalse(pageable).map(this::toResponse);
  }

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
    user.setDeleted(true);
    user.setStatus(UserStatus.INACTIVE);
    userRepository.save(user);
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
    response.setApiKey(user.getApiKey());
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
