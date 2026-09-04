package com.example.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "membership_plans")
public class MembershipPlan {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(name = "monthly_price", nullable = false)
  private BigDecimal monthlyPrice;

  @Column(name = "included_credit_hours", nullable = false)
  private Double includedCreditHours;

  @Column(name = "overage_rate_per_hour", nullable = false)
  private BigDecimal overageRatePerHour;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    LocalDateTime now = LocalDateTime.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = LocalDateTime.now();
  }

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
