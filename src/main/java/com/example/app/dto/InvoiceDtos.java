package com.example.app.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InvoiceDtos {

  public static class InvoiceGenerateRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotNull(message = "month is required")
    @Pattern(regexp = "\\d{4}-\\d{2}", message = "month must be in YYYY-MM format")
    private String month;

    public Long getUserId() {
      return userId;
    }

    public void setUserId(Long userId) {
      this.userId = userId;
    }

    public String getMonth() {
      return month;
    }

    public void setMonth(String month) {
      this.month = month;
    }
  }

  public static class InvoiceResponse {

    private Long id;
    private Long userId;
    private String month;
    private BigDecimal totalAmount;
    private BigDecimal totalCreditOverageCharged;
    private LocalDateTime createdAt;

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

    public String getMonth() {
      return month;
    }

    public void setMonth(String month) {
      this.month = month;
    }

    public BigDecimal getTotalAmount() {
      return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
      this.totalAmount = totalAmount;
    }

    public BigDecimal getTotalCreditOverageCharged() {
      return totalCreditOverageCharged;
    }

    public void setTotalCreditOverageCharged(BigDecimal totalCreditOverageCharged) {
      this.totalCreditOverageCharged = totalCreditOverageCharged;
    }

    public LocalDateTime getCreatedAt() {
      return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
    }
  }
}
