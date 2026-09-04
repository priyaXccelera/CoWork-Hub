package com.example.app.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public class ReviewDtos {

  public static class ReviewRequest {

    @NotNull(message = "bookingId is required")
    private Long bookingId;

    @NotNull(message = "rating is required")
    @Min(value = 1, message = "rating must be between 1 and 5")
    @Max(value = 5, message = "rating must be between 1 and 5")
    private Integer rating;

    @Size(max = 500, message = "comment must not exceed 500 characters")
    private String comment;

    public Long getBookingId() {
      return bookingId;
    }

    public void setBookingId(Long bookingId) {
      this.bookingId = bookingId;
    }

    public Integer getRating() {
      return rating;
    }

    public void setRating(Integer rating) {
      this.rating = rating;
    }

    public String getComment() {
      return comment;
    }

    public void setComment(String comment) {
      this.comment = comment;
    }
  }

  public static class ReviewUpdateRequest {

    @NotNull(message = "rating is required")
    @Min(value = 1, message = "rating must be between 1 and 5")
    @Max(value = 5, message = "rating must be between 1 and 5")
    private Integer rating;

    @Size(max = 500, message = "comment must not exceed 500 characters")
    private String comment;

    public Integer getRating() {
      return rating;
    }

    public void setRating(Integer rating) {
      this.rating = rating;
    }

    public String getComment() {
      return comment;
    }

    public void setComment(String comment) {
      this.comment = comment;
    }
  }

  public static class ReviewResponse {

    private Long id;
    private Long userId;
    private Long spaceId;
    private Long bookingId;
    private Integer rating;
    private String comment;
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

    public Long getBookingId() {
      return bookingId;
    }

    public void setBookingId(Long bookingId) {
      this.bookingId = bookingId;
    }

    public Integer getRating() {
      return rating;
    }

    public void setRating(Integer rating) {
      this.rating = rating;
    }

    public String getComment() {
      return comment;
    }

    public void setComment(String comment) {
      this.comment = comment;
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
