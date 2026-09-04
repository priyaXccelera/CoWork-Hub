package com.example.app.dto;

import com.example.app.entity.Role;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public class ApiKeyDtos {

  public static class CreateApiKeyRequest {

    @NotBlank(message = "name is required")
    private String name;

    private Role role;

    private Long userId;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public Role getRole() {
      return role;
    }

    public void setRole(Role role) {
      this.role = role;
    }

    public Long getUserId() {
      return userId;
    }

    public void setUserId(Long userId) {
      this.userId = userId;
    }
  }

  public static class ApiKeyResponse {

    private Long id;
    private String name;
    private Role role;
    private Long userId;
    private boolean active;
    private String apiKey;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;

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

    public Role getRole() {
      return role;
    }

    public void setRole(Role role) {
      this.role = role;
    }

    public Long getUserId() {
      return userId;
    }

    public void setUserId(Long userId) {
      this.userId = userId;
    }

    public boolean isActive() {
      return active;
    }

    public void setActive(boolean active) {
      this.active = active;
    }

    public String getApiKey() {
      return apiKey;
    }

    public void setApiKey(String apiKey) {
      this.apiKey = apiKey;
    }

    public LocalDateTime getCreatedAt() {
      return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
    }

    public LocalDateTime getLastUsedAt() {
      return lastUsedAt;
    }

    public void setLastUsedAt(LocalDateTime lastUsedAt) {
      this.lastUsedAt = lastUsedAt;
    }
  }
}
