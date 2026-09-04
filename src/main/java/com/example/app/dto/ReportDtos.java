package com.example.app.dto;

import java.math.BigDecimal;

public class ReportDtos {

  public static class SpaceUtilizationResponse {

    private Long spaceId;
    private String spaceName;
    private LocalWeek week;
    private double bookedHours;
    private double availableHours;
    private double utilizationPercentage;

    public Long getSpaceId() {
      return spaceId;
    }

    public void setSpaceId(Long spaceId) {
      this.spaceId = spaceId;
    }

    public String getSpaceName() {
      return spaceName;
    }

    public void setSpaceName(String spaceName) {
      this.spaceName = spaceName;
    }

    public LocalWeek getWeek() {
      return week;
    }

    public void setWeek(LocalWeek week) {
      this.week = week;
    }

    public double getBookedHours() {
      return bookedHours;
    }

    public void setBookedHours(double bookedHours) {
      this.bookedHours = bookedHours;
    }

    public double getAvailableHours() {
      return availableHours;
    }

    public void setAvailableHours(double availableHours) {
      this.availableHours = availableHours;
    }

    public double getUtilizationPercentage() {
      return utilizationPercentage;
    }

    public void setUtilizationPercentage(double utilizationPercentage) {
      this.utilizationPercentage = utilizationPercentage;
    }
  }

  public static class LocalWeek {

    private String start;
    private String end;

    public LocalWeek(String start, String end) {
      this.start = start;
      this.end = end;
    }

    public String getStart() {
      return start;
    }

    public void setStart(String start) {
      this.start = start;
    }

    public String getEnd() {
      return end;
    }

    public void setEnd(String end) {
      this.end = end;
    }
  }

  public static class MonthlyRevenueResponse {

    private String month;
    private BigDecimal totalRevenue;
    private long bookingCount;

    public String getMonth() {
      return month;
    }

    public void setMonth(String month) {
      this.month = month;
    }

    public BigDecimal getTotalRevenue() {
      return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
      this.totalRevenue = totalRevenue;
    }

    public long getBookingCount() {
      return bookingCount;
    }

    public void setBookingCount(long bookingCount) {
      this.bookingCount = bookingCount;
    }
  }

  public static class TopMemberResponse {

    private Long userId;
    private String userName;
    private long bookingCount;
    private double totalHoursBooked;

    public Long getUserId() {
      return userId;
    }

    public void setUserId(Long userId) {
      this.userId = userId;
    }

    public String getUserName() {
      return userName;
    }

    public void setUserName(String userName) {
      this.userName = userName;
    }

    public long getBookingCount() {
      return bookingCount;
    }

    public void setBookingCount(long bookingCount) {
      this.bookingCount = bookingCount;
    }

    public double getTotalHoursBooked() {
      return totalHoursBooked;
    }

    public void setTotalHoursBooked(double totalHoursBooked) {
      this.totalHoursBooked = totalHoursBooked;
    }
  }
}
