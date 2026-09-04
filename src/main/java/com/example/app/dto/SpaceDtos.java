package com.example.app.dto;

import com.example.app.entity.SpaceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SpaceDtos {

  public static class SpaceRequest {

    @NotBlank(message = "name is required")
    private String name;

    @NotNull(message = "type is required")
    private SpaceType type;

    @NotNull(message = "capacity is required")
    @Min(value = 1, message = "capacity must be at least 1")
    private Integer capacity;

    @NotNull(message = "hourlyRate is required")
    @DecimalMin(value = "0.0", message = "hourlyRate must not be negative")
    private BigDecimal hourlyRate;

    private Boolean isActive;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public SpaceType getType() {
      return type;
    }

    public void setType(SpaceType type) {
      this.type = type;
    }

    public Integer getCapacity() {
      return capacity;
    }

    public void setCapacity(Integer capacity) {
      this.capacity = capacity;
    }

    public BigDecimal getHourlyRate() {
      return hourlyRate;
    }

    public void setHourlyRate(BigDecimal hourlyRate) {
      this.hourlyRate = hourlyRate;
    }

    public Boolean getIsActive() {
      return isActive;
    }

    public void setIsActive(Boolean isActive) {
      this.isActive = isActive;
    }
  }

  public static class SpaceResponse {

    private Long id;
    private String name;
    private SpaceType type;
    private Integer capacity;
    private BigDecimal hourlyRate;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Double averageRating;
    private long totalReviews;

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

    public SpaceType getType() {
      return type;
    }

    public void setType(SpaceType type) {
      this.type = type;
    }

    public Integer getCapacity() {
      return capacity;
    }

    public void setCapacity(Integer capacity) {
      this.capacity = capacity;
    }

    public BigDecimal getHourlyRate() {
      return hourlyRate;
    }

    public void setHourlyRate(BigDecimal hourlyRate) {
      this.hourlyRate = hourlyRate;
    }

    public boolean isActive() {
      return isActive;
    }

    public void setActive(boolean active) {
      isActive = active;
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

    public Double getAverageRating() {
      return averageRating;
    }

    public void setAverageRating(Double averageRating) {
      this.averageRating = averageRating;
    }

    public long getTotalReviews() {
      return totalReviews;
    }

    public void setTotalReviews(long totalReviews) {
      this.totalReviews = totalReviews;
    }
  }
}
