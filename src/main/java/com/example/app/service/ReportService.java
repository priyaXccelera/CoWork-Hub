package com.example.app.service;

import com.example.app.dto.ReportDtos.LocalWeek;
import com.example.app.dto.ReportDtos.MonthlyRevenueResponse;
import com.example.app.dto.ReportDtos.SpaceUtilizationResponse;
import com.example.app.dto.ReportDtos.TopMemberResponse;
import com.example.app.entity.Booking;
import com.example.app.entity.Space;
import com.example.app.entity.User;
import com.example.app.repository.BookingRepository;
import com.example.app.repository.SpaceRepository;
import com.example.app.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ReportService {

  private final BookingRepository bookingRepository;
  private final SpaceRepository spaceRepository;
  private final UserRepository userRepository;

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

  public ReportService(
      BookingRepository bookingRepository,
      SpaceRepository spaceRepository,
      UserRepository userRepository) {
    this.bookingRepository = bookingRepository;
    this.spaceRepository = spaceRepository;
    this.userRepository = userRepository;
  }

  public List<SpaceUtilizationResponse> spaceUtilization(LocalDate weekStartDate) {
    LocalDate weekStart =
        weekStartDate != null
            ? weekStartDate
            : LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1);
    LocalDateTime start = weekStart.atStartOfDay();
    LocalDateTime end = start.plusDays(7);
    double availableHours = 7 * 24.0;

    List<Space> spaces =
        spaceRepository.findAll().stream().filter(s -> !s.isDeleted()).collect(Collectors.toList());

    return spaces.stream()
        .map(
            space -> {
              List<Booking> bookings =
                  bookingRepository.findForUtilization(space.getId(), start, end);
              double bookedHours =
                  bookings.stream()
                      .mapToDouble(b -> overlapHours(b.getStartTime(), b.getEndTime(), start, end))
                      .sum();

              SpaceUtilizationResponse response = new SpaceUtilizationResponse();
              response.setSpaceId(space.getId());
              response.setSpaceName(space.getName());
              response.setWeek(
                  new LocalWeek(
                      start.toLocalDate().format(DATE_FORMATTER),
                      end.toLocalDate().minusDays(1).format(DATE_FORMATTER)));
              response.setBookedHours(round(bookedHours));
              response.setAvailableHours(availableHours);
              response.setUtilizationPercentage(round((bookedHours / availableHours) * 100));
              return response;
            })
        .collect(Collectors.toList());
  }

  public MonthlyRevenueResponse monthlyRevenue(String month) {
    YearMonth yearMonth = YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyy-MM"));
    LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
    LocalDateTime end = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

    BigDecimal revenue = bookingRepository.sumRevenueBetween(start, end);
    long count = bookingRepository.countBookingsBetween(start, end);

    MonthlyRevenueResponse response = new MonthlyRevenueResponse();
    response.setMonth(month);
    response.setTotalRevenue(
        revenue == null ? BigDecimal.ZERO : revenue.setScale(2, RoundingMode.HALF_UP));
    response.setBookingCount(count);
    return response;
  }

  public List<TopMemberResponse> topMembers(int limit) {
    return bookingRepository.findTopMembers().stream()
        .limit(limit)
        .map(
            projection -> {
              User user = userRepository.findById(projection.getUserId()).orElse(null);
              List<Booking> userBookings = bookingRepository.findByUserId(projection.getUserId());
              double totalHours =
                  userBookings.stream()
                      .filter(
                          b ->
                              b.getStatus().name().equals("CONFIRMED")
                                  || b.getStatus().name().equals("COMPLETED"))
                      .mapToDouble(
                          b ->
                              Duration.between(b.getStartTime(), b.getEndTime()).toMinutes() / 60.0)
                      .sum();

              TopMemberResponse response = new TopMemberResponse();
              response.setUserId(projection.getUserId());
              response.setUserName(user != null ? user.getName() : "Unknown");
              response.setBookingCount(projection.getCnt());
              response.setTotalHoursBooked(round(totalHours));
              return response;
            })
        .sorted(Comparator.comparingLong(TopMemberResponse::getBookingCount).reversed())
        .collect(Collectors.toList());
  }

  private double overlapHours(
      LocalDateTime bStart,
      LocalDateTime bEnd,
      LocalDateTime windowStart,
      LocalDateTime windowEnd) {
    LocalDateTime overlapStart = bStart.isAfter(windowStart) ? bStart : windowStart;
    LocalDateTime overlapEnd = bEnd.isBefore(windowEnd) ? bEnd : windowEnd;
    if (overlapEnd.isBefore(overlapStart)) {
      return 0.0;
    }
    return Duration.between(overlapStart, overlapEnd).toMinutes() / 60.0;
  }

  private double round(double value) {
    return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
  }
}
