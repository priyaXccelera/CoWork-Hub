package com.example.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false, unique = true)
  private String email;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role;

  @Column(name = "api_key")
  private String apiKey;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "membership_plan_id")
  private MembershipPlan membershipPlan;

  @Column(name = "credit_hours_remaining", nullable = false)
  private Double creditHoursRemaining = 0.0;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private UserStatus status = UserStatus.ACTIVE;

  @Column(nullable = false)
  private boolean deleted = false;

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

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public Role getRole() {
    return role;
  }

  public void setRole(Role role) {
    this.role = role;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public MembershipPlan getMembershipPlan() {
    return membershipPlan;
  }

  public void setMembershipPlan(MembershipPlan membershipPlan) {
    this.membershipPlan = membershipPlan;
  }

  public Double getCreditHoursRemaining() {
    return creditHoursRemaining;
  }

  public void setCreditHoursRemaining(Double creditHoursRemaining) {
    this.creditHoursRemaining = creditHoursRemaining;
  }

  public UserStatus getStatus() {
    return status;
  }

  public void setStatus(UserStatus status) {
    this.status = status;
  }

  public boolean isDeleted() {
    return deleted;
  }

  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
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
