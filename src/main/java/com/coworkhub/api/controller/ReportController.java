package com.coworkhub.api.controller;

import com.coworkhub.api.dto.ReportDtos.MonthlyRevenueResponse;
import com.coworkhub.api.dto.ReportDtos.SpaceUtilizationResponse;
import com.coworkhub.api.dto.ReportDtos.TopMemberResponse;
import com.coworkhub.api.exception.BusinessRuleException;
import com.coworkhub.api.security.AccessGuard;
import com.coworkhub.api.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports", description = "Admin-only reporting endpoints")
public class ReportController {

  private final ReportService reportService;

  public ReportController(ReportService reportService) {
    this.reportService = reportService;
  }

  @GetMapping("/space-utilization")
  @Operation(summary = "Space utilization percentage per week (Admin only)")
  public ResponseEntity<List<SpaceUtilizationResponse>> spaceUtilization(
      @RequestParam(required = false) LocalDate weekStart) {
    AccessGuard.requireAdmin();
    return ResponseEntity.ok(reportService.spaceUtilization(weekStart));
  }

  @GetMapping("/revenue")
  @Operation(summary = "Monthly revenue report (Admin only), month format YYYY-MM")
  public ResponseEntity<MonthlyRevenueResponse> monthlyRevenue(@RequestParam String month) {
    AccessGuard.requireAdmin();
    return ResponseEntity.ok(reportService.monthlyRevenue(month));
  }

  @GetMapping("/top-members")
  @Operation(summary = "Top 5 active members by booking count (Admin only)")
  public ResponseEntity<List<TopMemberResponse>> topMembers(
      @RequestParam(defaultValue = "5") int limit) {
    AccessGuard.requireAdmin();
    if (limit < 1 || limit > 100) {
      throw new BusinessRuleException("limit must be between 1 and 100");
    }
    return ResponseEntity.ok(reportService.topMembers(limit));
  }
}
