package com.coworkhub.api.service;

import com.coworkhub.api.dto.MembershipPlanDtos.MembershipPlanRequest;
import com.coworkhub.api.dto.MembershipPlanDtos.MembershipPlanResponse;
import com.coworkhub.api.entity.MembershipPlan;
import com.coworkhub.api.exception.ConflictException;
import com.coworkhub.api.exception.ResourceNotFoundException;
import com.coworkhub.api.repository.MembershipPlanRepository;
import com.coworkhub.api.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MembershipPlanService {

  private final MembershipPlanRepository membershipPlanRepository;
  private final UserRepository userRepository;

  public MembershipPlanService(
      MembershipPlanRepository membershipPlanRepository, UserRepository userRepository) {
    this.membershipPlanRepository = membershipPlanRepository;
    this.userRepository = userRepository;
  }

  public MembershipPlanResponse create(MembershipPlanRequest request) {
    MembershipPlan plan = new MembershipPlan();
    applyRequest(plan, request);
    MembershipPlan saved = membershipPlanRepository.save(plan);
    return toResponse(saved);
  }

  public Page<MembershipPlanResponse> list(Pageable pageable) {
    return membershipPlanRepository.findAll(pageable).map(this::toResponse);
  }

  public MembershipPlanResponse get(Long id) {
    return toResponse(findEntity(id));
  }

  public MembershipPlanResponse update(Long id, MembershipPlanRequest request) {
    MembershipPlan plan = findEntity(id);
    applyRequest(plan, request);
    MembershipPlan saved = membershipPlanRepository.save(plan);
    return toResponse(saved);
  }

  public void delete(Long id) {
    MembershipPlan plan = findEntity(id);
    if (userRepository.existsByMembershipPlan_IdAndDeletedFalse(id)) {
      throw new ConflictException(
          "Cannot delete membership plan with id "
              + id
              + ": it is still assigned to one or more active users");
    }
    membershipPlanRepository.delete(plan);
  }

  private MembershipPlan findEntity(Long id) {
    return membershipPlanRepository
        .findById(id)
        .orElseThrow(
            () -> new ResourceNotFoundException("Membership plan not found with id: " + id));
  }

  private void applyRequest(MembershipPlan plan, MembershipPlanRequest request) {
    plan.setName(request.getName());
    plan.setMonthlyPrice(request.getMonthlyPrice());
    plan.setIncludedCreditHours(request.getIncludedCreditHours());
    plan.setOverageRatePerHour(request.getOverageRatePerHour());
  }

  private MembershipPlanResponse toResponse(MembershipPlan plan) {
    MembershipPlanResponse response = new MembershipPlanResponse();
    response.setId(plan.getId());
    response.setName(plan.getName());
    response.setMonthlyPrice(plan.getMonthlyPrice());
    response.setIncludedCreditHours(plan.getIncludedCreditHours());
    response.setOverageRatePerHour(plan.getOverageRatePerHour());
    response.setCreatedAt(plan.getCreatedAt());
    response.setUpdatedAt(plan.getUpdatedAt());
    return response;
  }
}
