package com.example.app.dto;

import com.example.app.entity.WaitlistStatus;
import java.time.LocalDateTime;

public class WaitlistDtos {

  public static class WaitlistResponse {

    private Long id;
    private Long userId;
    private Long spaceId;
    private LocalDateTime requestedStart;
    private LocalDateTime requestedEnd;
    private WaitlistStatus status;
    private Long bookingId;
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
}
