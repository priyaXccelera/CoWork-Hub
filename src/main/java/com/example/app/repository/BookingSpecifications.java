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
