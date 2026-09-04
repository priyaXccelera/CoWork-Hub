package com.coworkhub.api.controller;

import com.coworkhub.api.dto.MembershipPlanDtos.MembershipPlanRequest;
import com.coworkhub.api.dto.MembershipPlanDtos.MembershipPlanResponse;
import com.coworkhub.api.security.AccessGuard;
import com.coworkhub.api.service.MembershipPlanService;
import com.coworkhub.api.util.OffsetPageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/membership-plans")
@Tag(name = "Membership Plans", description = "Admin-only membership plan management")
public class MembershipPlanController {

  private final MembershipPlanService membershipPlanService;

  public MembershipPlanController(MembershipPlanService membershipPlanService) {
    this.membershipPlanService = membershipPlanService;
  }

  @PostMapping
  @Operation(summary = "Create a membership plan (Admin only)")
  public ResponseEntity<MembershipPlanResponse> create(
      @Valid @RequestBody MembershipPlanRequest request) {
    AccessGuard.requireAdmin();
    return ResponseEntity.status(HttpStatus.CREATED).body(membershipPlanService.create(request));
  }

  @GetMapping
  @Operation(summary = "List membership plans with pagination (Admin only)")
  public ResponseEntity<Page<MembershipPlanResponse>> list(
      @RequestParam(defaultValue = "0") int offset, @RequestParam(defaultValue = "20") int limit) {
    AccessGuard.requireAdmin();
    return ResponseEntity.ok(
        membershipPlanService.list(
            OffsetPageRequest.of(offset, limit, Sort.by(Sort.Direction.ASC, "id"))));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a membership plan by id (Admin only)")
  public ResponseEntity<MembershipPlanResponse> get(@PathVariable Long id) {
    AccessGuard.requireAdmin();
    return ResponseEntity.ok(membershipPlanService.get(id));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update a membership plan (Admin only)")
  public ResponseEntity<MembershipPlanResponse> update(
      @PathVariable Long id, @Valid @RequestBody MembershipPlanRequest request) {
    AccessGuard.requireAdmin();
    return ResponseEntity.ok(membershipPlanService.update(id, request));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete a membership plan (Admin only)")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    AccessGuard.requireAdmin();
    membershipPlanService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
