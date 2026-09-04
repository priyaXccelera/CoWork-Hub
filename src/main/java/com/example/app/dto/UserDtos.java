package com.example.app.dto;

import com.example.app.entity.Role;
import com.example.app.entity.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class UserDtos {

  public static class UserRequest {

    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "email is required")
    @Email(message = "email must be a valid email address")
    private String email;

    @NotNull(message = "role is required")
    private Role role;

    private Long membershipPlanId;

    private Double creditHoursRemaining;

    private UserStatus status;

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

    public Long getMembershipPlanId() {
      return membershipPlanId;
    }

    public void setMembershipPlanId(Long membershipPlanId) {
      this.membershipPlanId = membershipPlanId;
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
  }

  public static class UserResponse {

    private Long id;
    private String name;
    private String email;
    private Role role;
    private String apiKey;
    private Long membershipPlanId;
    private String membershipPlanName;
    private Double creditHoursRemaining;
    private UserStatus status;
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

    public Long getMembershipPlanId() {
      return membershipPlanId;
    }

    public void setMembershipPlanId(Long membershipPlanId) {
      this.membershipPlanId = membershipPlanId;
    }

    public String getMembershipPlanName() {
      return membershipPlanName;
    }

    public void setMembershipPlanName(String membershipPlanName) {
      this.membershipPlanName = membershipPlanName;
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
