package com.example.app.entity;

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
import java.time.LocalDateTime;

@Entity
@Table(name = "waitlists")
public class Waitlist {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "space_id", nullable = false)
  private Long spaceId;

  @Column(name = "requested_start", nullable = false)
  private LocalDateTime requestedStart;

  @Column(name = "requested_end", nullable = false)
  private LocalDateTime requestedEnd;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private WaitlistStatus status = WaitlistStatus.WAITING;

  @Column(name = "booking_id")
  private Long bookingId;

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

  public LocalDateTime getRequestedStart() {
    return requestedStart;
  }

  public void setRequestedStart(LocalDateTime requestedStart) {
    this.requestedStart = requestedStart;
  }

  public LocalDateTime getRequestedEnd() {
    return requestedEnd;
  }

  public void setRequestedEnd(LocalDateTime requestedEnd) {
    this.requestedEnd = requestedEnd;
  }

  public WaitlistStatus getStatus() {
    return status;
  }

  public void setStatus(WaitlistStatus status) {
    this.status = status;
  }

  public Long getBookingId() {
    return bookingId;
  }

  public void setBookingId(Long bookingId) {
    this.bookingId = bookingId;
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
