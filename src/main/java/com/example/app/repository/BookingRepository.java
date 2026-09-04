package com.example.app.repository;

import com.example.app.entity.Booking;
import com.example.app.entity.BookingStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository
    extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

  @Query(
      "select b from Booking b where b.spaceId = :spaceId and b.status = 'CONFIRMED' "
          + "and b.startTime < :end and b.endTime > :start")
  List<Booking> findOverlappingConfirmed(
      @Param("spaceId") Long spaceId,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end);

  List<Booking> findByUserId(Long userId);

  /** Used to block deletion of a space/user that still has active or future bookings. */
  boolean existsBySpaceIdAndStatusInAndEndTimeAfter(
      Long spaceId, List<BookingStatus> statuses, LocalDateTime after);

  boolean existsByUserIdAndStatusInAndEndTimeAfter(
      Long userId, List<BookingStatus> statuses, LocalDateTime after);

  @Modifying
  @Query(
      "update Booking b set b.status = 'COMPLETED', b.updatedAt = CURRENT_TIMESTAMP "
          + "where b.status = 'CONFIRMED' and b.endTime <= :now")
  int completePastBookings(@Param("now") LocalDateTime now);

  // Revenue and booking-count use the same status set (CONFIRMED, COMPLETED, CANCELLED) so the
  // two figures reported together stay consistent: cancelled bookings still contribute their
  // cancellation fee to revenue and are counted as a booking transaction that occurred in the
  // period.
  @Query(
      "select coalesce(sum(b.costCharged), 0) from Booking b where b.startTime >= :from and"
          + " b.startTime < :to and b.status in ('CONFIRMED', 'COMPLETED', 'CANCELLED')")
  BigDecimal sumRevenueBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

  @Query(
      "select count(b) from Booking b where b.startTime >= :from and b.startTime < :to "
          + "and b.status in ('CONFIRMED', 'COMPLETED', 'CANCELLED')")
  long countBookingsBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

  @Query(
      "select b from Booking b where b.spaceId = :spaceId and b.status in ('CONFIRMED',"
          + " 'COMPLETED') and b.startTime < :weekEnd and b.endTime > :weekStart")
  List<Booking> findForUtilization(
      @Param("spaceId") Long spaceId,
      @Param("weekStart") LocalDateTime weekStart,
      @Param("weekEnd") LocalDateTime weekEnd);

  @Query(
      "select b.userId as userId, count(b) as cnt from Booking b where b.status in ('CONFIRMED',"
          + " 'COMPLETED') group by b.userId order by cnt desc")
  List<TopMemberProjection> findTopMembers();

  interface TopMemberProjection {
    Long getUserId();

    Long getCnt();
  }

  boolean existsBySpaceId(Long spaceId);

  boolean existsByUserId(Long userId);
}
