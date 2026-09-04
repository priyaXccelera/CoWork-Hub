package com.coworkhub.api.service;

import com.coworkhub.api.dto.ReportDtos.LocalWeek;
import com.coworkhub.api.dto.ReportDtos.MonthlyRevenueResponse;
import com.coworkhub.api.dto.ReportDtos.SpaceUtilizationResponse;
import com.coworkhub.api.dto.ReportDtos.TopMemberResponse;
import com.coworkhub.api.entity.Booking;
import com.coworkhub.api.entity.Space;
import com.coworkhub.api.entity.User;
import com.coworkhub.api.exception.BusinessRuleException;
import com.coworkhub.api.repository.BookingRepository;
import com.coworkhub.api.repository.ReviewRepository;
import com.coworkhub.api.repository.SpaceRepository;
import com.coworkhub.api.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReportService {

  private static final int MIN_YEAR = 2000;
  private static final int MAX_YEAR = 2100;

  private final BookingRepository bookingRepository;
  private final SpaceRepository spaceRepository;
  private final UserRepository userRepository;
  private final ReviewRepository reviewRepository;

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

  public ReportService(
      BookingRepository bookingRepository,
      SpaceRepository spaceRepository,
      UserRepository userRepository,
      ReviewRepository reviewRepository) {
    this.bookingRepository = bookingRepository;
    this.spaceRepository = spaceRepository;
    this.userRepository = userRepository;
    this.reviewRepository = reviewRepository;
  }

  public List<SpaceUtilizationResponse> spaceUtilization(LocalDate weekStartDate) {
    LocalDate weekStart =
        weekStartDate != null
            ? weekStartDate
            : LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1);
    LocalDateTime start = weekStart.atStartOfDay();
    LocalDateTime end = start.plusDays(7);
    double hoursInWeek = 7 * 24.0;

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

              // Total available capacity for the week is capacity (concurrent slots) x hours in
              // the week, since a space with capacity > 1 can host that many bookings at once.
              int capacity = space.getCapacity() == null ? 1 : space.getCapacity();
              double availableHours = hoursInWeek * capacity;

              SpaceUtilizationResponse response = new SpaceUtilizationResponse();
              response.setSpaceId(space.getId());
              response.setSpaceName(space.getName());
              response.setWeek(
                  new LocalWeek(
                      start.toLocalDate().format(DATE_FORMATTER),
                      end.toLocalDate().minusDays(1).format(DATE_FORMATTER)));
              response.setBookedHours(round(bookedHours));
              response.setAvailableHours(availableHours);
              response.setUtilizationPercentage(
                  availableHours > 0 ? round((bookedHours / availableHours) * 100) : 0.0);
              Double avgRating = reviewRepository.findAverageRatingBySpaceId(space.getId());
              response.setAverageRating(avgRating != null ? roundToOneDecimal(avgRating) : null);
              return response;
            })
        .collect(Collectors.toList());
  }

  public MonthlyRevenueResponse monthlyRevenue(String month) {
    YearMonth yearMonth = parseMonth(month);
    LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
    LocalDateTime end = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

    // Revenue and booking count use the same underlying status set (see
    // BookingRepository#sumRevenueBetween / #countBookingsBetween) so the two numbers reported
    // together are always consistent with one another.
    BigDecimal revenue = bookingRepository.sumRevenueBetween(start, end);
    long count = bookingRepository.countBookingsBetween(start, end);

    MonthlyRevenueResponse response = new MonthlyRevenueResponse();
    response.setMonth(month);
    response.setTotalRevenue(
        revenue == null ? BigDecimal.ZERO : revenue.setScale(2, RoundingMode.HALF_UP));
    response.setBookingCount(count);
    return response;
  }

  private YearMonth parseMonth(String month) {
    if (month == null || !month.matches("\\d{4}-\\d{2}")) {
      throw new BusinessRuleException("month must be in YYYY-MM format, e.g. 2024-05");
    }
    YearMonth yearMonth;
    try {
      yearMonth = YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyy-MM"));
    } catch (DateTimeParseException e) {
      throw new BusinessRuleException("Invalid month value: " + month);
    }
    if (yearMonth.getYear() < MIN_YEAR || yearMonth.getYear() > MAX_YEAR) {
      throw new BusinessRuleException(
          "month year must be between " + MIN_YEAR + " and " + MAX_YEAR);
    }
    return yearMonth;
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

  private double roundToOneDecimal(double value) {
    return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
  }
}
