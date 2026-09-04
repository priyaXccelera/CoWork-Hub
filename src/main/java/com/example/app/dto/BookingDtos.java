package com.example.app.dto;

import com.example.app.entity.BookingStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BookingDtos {

  public static class BookingRequest {

    private Long userId;

    @NotNull(message = "spaceId is required")
    private Long spaceId;

    @NotNull(message = "startTime is required")
    @Future(message = "startTime must be in the future")
    private LocalDateTime startTime;

    @NotNull(message = "endTime is required")
    @Future(message = "endTime must be in the future")
    private LocalDateTime endTime;

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
  }

  public static class BookingResponse {

    private Long id;
    private Long userId;
    private Long spaceId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BookingStatus status;
    private BigDecimal costCharged;
    private Double creditHoursUsed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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
}
