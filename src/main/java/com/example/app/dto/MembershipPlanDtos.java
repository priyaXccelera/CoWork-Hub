package com.example.app.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MembershipPlanDtos {

  public static class MembershipPlanRequest {

    @NotBlank(message = "name is required")
    private String name;

    @NotNull(message = "monthlyPrice is required")
    @DecimalMin(value = "0.0", message = "monthlyPrice must not be negative")
    private BigDecimal monthlyPrice;

    @NotNull(message = "includedCreditHours is required")
    @DecimalMin(value = "0.0", message = "includedCreditHours must not be negative")
    private Double includedCreditHours;

    @NotNull(message = "overageRatePerHour is required")
    @DecimalMin(value = "0.0", message = "overageRatePerHour must not be negative")
    private BigDecimal overageRatePerHour;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public BigDecimal getMonthlyPrice() {
      return monthlyPrice;
    }

    public void setMonthlyPrice(BigDecimal monthlyPrice) {
      this.monthlyPrice = monthlyPrice;
    }

    public Double getIncludedCreditHours() {
      return includedCreditHours;
    }

    public void setIncludedCreditHours(Double includedCreditHours) {
      this.includedCreditHours = includedCreditHours;
    }

    public BigDecimal getOverageRatePerHour() {
      return overageRatePerHour;
    }

    public void setOverageRatePerHour(BigDecimal overageRatePerHour) {
      this.overageRatePerHour = overageRatePerHour;
    }
  }

  public static class MembershipPlanResponse {

    private Long id;
    private String name;
    private BigDecimal monthlyPrice;
    private Double includedCreditHours;
    private BigDecimal overageRatePerHour;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public BigDecimal getMonthlyPrice() {
      return monthlyPrice;
    }

    public void setMonthlyPrice(BigDecimal monthlyPrice) {
      this.monthlyPrice = monthlyPrice;
    }

    public Double getIncludedCreditHours() {
      return includedCreditHours;
    }

    public void setIncludedCreditHours(Double includedCreditHours) {
      this.includedCreditHours = includedCreditHours;
    }

    public BigDecimal getOverageRatePerHour() {
      return overageRatePerHour;
    }

    public void setOverageRatePerHour(BigDecimal overageRatePerHour) {
      this.overageRatePerHour = overageRatePerHour;
    }

    public LocalDateTime getCreatedAt() {
      return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
      return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
      this.updatedAt = updatedAt;
    }
  }
}
