package com.coworkhub.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
public class Booking {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "space_id", nullable = false)
  private Long spaceId;

  @Column(name = "start_time", nullable = false)
  private LocalDateTime startTime;

  @Column(name = "end_time", nullable = false)
  private LocalDateTime endTime;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private BookingStatus status;

  @Column(name = "cost_charged", nullable = false)
  private BigDecimal costCharged = BigDecimal.ZERO;

  /**
   * The full monetary value of the booking at the time it was made (space hourly rate x hours),
   * regardless of how much of it was actually paid in cash vs. covered by membership credit hours.
   * Preserved permanently so that cancellation fees and historical records always have a reference
   * to what the booking was originally worth, even after {@link #costCharged} is overwritten with a
   * cancellation fee.
   */
  @Column(name = "original_cost", nullable = false)
  private BigDecimal originalCost = BigDecimal.ZERO;

  @Column(name = "credit_hours_used", nullable = false)
  private Double creditHoursUsed = 0.0;

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

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public Long getSpaceId() {
    return spaceId;
  }

  public void setSpaceId(Long spaceId) {
    this.spaceId = spaceId;
  }

  public LocalDateTime getStartTime() {
    return startTime;
  }

  public void setStartTime(LocalDateTime startTime) {
    this.startTime = startTime;
  }

  public LocalDateTime getEndTime() {
    return endTime;
  }

  public void setEndTime(LocalDateTime endTime) {
    this.endTime = endTime;
  }

  public BookingStatus getStatus() {
    return status;
  }

  public void setStatus(BookingStatus status) {
    this.status = status;
  }

  public BigDecimal getCostCharged() {
    return costCharged;
  }

  public void setCostCharged(BigDecimal costCharged) {
    this.costCharged = costCharged;
  }

  public BigDecimal getOriginalCost() {
    return originalCost;
  }

  public void setOriginalCost(BigDecimal originalCost) {
    this.originalCost = originalCost;
  }

  public Double getCreditHoursUsed() {
    return creditHoursUsed;
  }

  public void setCreditHoursUsed(Double creditHoursUsed) {
    this.creditHoursUsed = creditHoursUsed;
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
