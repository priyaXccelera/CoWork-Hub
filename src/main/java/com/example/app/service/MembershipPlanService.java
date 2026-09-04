package com.example.app.service;

import com.example.app.dto.MembershipPlanDtos.MembershipPlanRequest;
import com.example.app.dto.MembershipPlanDtos.MembershipPlanResponse;
import com.example.app.entity.MembershipPlan;
import com.example.app.exception.ResourceNotFoundException;
import com.example.app.repository.MembershipPlanRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class MembershipPlanService {

  private final MembershipPlanRepository membershipPlanRepository;

  public MembershipPlanService(MembershipPlanRepository membershipPlanRepository) {
    this.membershipPlanRepository = membershipPlanRepository;
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
