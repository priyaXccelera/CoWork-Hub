package com.example.app.repository;

import com.example.app.entity.Booking;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
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

  @Query(
      "select coalesce(sum(b.costCharged), 0) from Booking b where b.startTime >= :from and"
          + " b.startTime < :to and b.status in ('CONFIRMED', 'COMPLETED', 'CANCELLED')")
  BigDecimal sumRevenueBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

  @Query(
      "select count(b) from Booking b where b.startTime >= :from and b.startTime < :to "
          + "and b.status in ('CONFIRMED', 'COMPLETED')")
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
