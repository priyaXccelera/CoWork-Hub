package com.example.app.repository;

import com.example.app.entity.Review;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

  Optional<Review> findByIdAndDeletedFalse(Long id);

  Page<Review> findBySpaceIdAndDeletedFalse(Long spaceId, Pageable pageable);

  boolean existsByBookingIdAndDeletedFalse(Long bookingId);

  /** Used by the space-utilization report and the space read endpoints to show average rating. */
  @Query("select avg(r.rating) from Review r where r.spaceId = :spaceId and r.deleted = false")
  Double findAverageRatingBySpaceId(@Param("spaceId") Long spaceId);

  @Query("select count(r) from Review r where r.spaceId = :spaceId and r.deleted = false")
  long countBySpaceIdAndDeletedFalse(@Param("spaceId") Long spaceId);

  /** Bulk variant used when rendering a paginated list of spaces, to avoid N+1 queries. */
  @Query(
      "select r.spaceId as spaceId, avg(r.rating) as avgRating, count(r) as cnt from Review r "
          + "where r.deleted = false and r.spaceId in :spaceIds group by r.spaceId")
  List<SpaceRatingProjection> findRatingStatsBySpaceIds(@Param("spaceIds") List<Long> spaceIds);

  interface SpaceRatingProjection {
    Long getSpaceId();

    Double getAvgRating();

    Long getCnt();
  }
}
