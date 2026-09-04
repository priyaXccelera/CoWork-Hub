package com.example.app.repository;

import com.example.app.entity.Booking;
import com.example.app.entity.BookingStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class BookingSpecifications {

  private BookingSpecifications() {}

  public static Specification<Booking> build(
      Long userId, LocalDate date, BookingStatus status, List<Long> spaceIds) {
    return build(userId, date, null, null, status, spaceIds);
  }

  /**
   * @param date optional single-day filter (matches bookings starting on that calendar day)
   * @param from optional inclusive lower bound on startTime (date range filtering)
   * @param to optional exclusive upper bound on startTime (date range filtering)
   */
  public static Specification<Booking> build(
      Long userId,
      LocalDate date,
      LocalDateTime from,
      LocalDateTime to,
      BookingStatus status,
      List<Long> spaceIds) {
    return (root, query, cb) -> {
      List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

      if (userId != null) {
        predicates.add(cb.equal(root.get("userId"), userId));
      }
      if (status != null) {
        predicates.add(cb.equal(root.get("status"), status));
      }
      if (date != null) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        predicates.add(
            cb.and(
                cb.lessThan(root.get("startTime"), end),
                cb.greaterThanOrEqualTo(root.get("startTime"), start)));
      }
      if (from != null) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("startTime"), from));
      }
      if (to != null) {
        predicates.add(cb.lessThan(root.get("startTime"), to));
      }
      if (spaceIds != null) {
        if (spaceIds.isEmpty()) {
          predicates.add(cb.disjunction());
        } else {
          predicates.add(root.get("spaceId").in(spaceIds));
        }
      }

      return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
    };
  }
}
