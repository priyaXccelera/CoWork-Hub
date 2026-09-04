package com.example.app.controller;

import com.example.app.dto.ReviewDtos.ReviewRequest;
import com.example.app.dto.ReviewDtos.ReviewResponse;
import com.example.app.dto.ReviewDtos.ReviewUpdateRequest;
import com.example.app.security.AccessGuard;
import com.example.app.security.CurrentActor;
import com.example.app.service.ReviewService;
import com.example.app.util.OffsetPageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reviews")
@Tag(
    name = "Reviews",
    description =
        "Member reviews of spaces, tied to a COMPLETED booking; soft-deleted, never hard-deleted")
public class ReviewController {

  private final ReviewService reviewService;

  public ReviewController(ReviewService reviewService) {
    this.reviewService = reviewService;
  }

  @PostMapping
  @Operation(
      summary =
          "Create a review for one of your own COMPLETED bookings (Member). One review per"
              + " booking; multiple reviews of the same space are allowed via separate completed"
              + " bookings.")
  public ResponseEntity<ReviewResponse> create(@Valid @RequestBody ReviewRequest request) {
    Long actorUserId = AccessGuard.requireAuthenticatedUserId();
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(reviewService.create(request, actorUserId));
  }

  @GetMapping
  @Operation(
      summary =
          "List reviews for a space, paginated (offset/limit), sorted by createdAt desc by"
              + " default")
  public ResponseEntity<Page<ReviewResponse>> list(
      @RequestParam Long spaceId,
      @RequestParam(defaultValue = "0") int offset,
      @RequestParam(defaultValue = "20") int limit) {
    return ResponseEntity.ok(
        reviewService.listBySpace(
            spaceId, OffsetPageRequest.of(offset, limit, Sort.by(Sort.Direction.DESC, "createdAt"))));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a single review by id")
  public ResponseEntity<ReviewResponse> get(@PathVariable Long id) {
    return ResponseEntity.ok(reviewService.get(id));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Edit your own review (Member)")
  public ResponseEntity<ReviewResponse> update(
      @PathVariable Long id, @Valid @RequestBody ReviewUpdateRequest request) {
    Long actorUserId = AccessGuard.requireAuthenticatedUserId();
    return ResponseEntity.ok(reviewService.update(id, request, actorUserId));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Soft delete your own review (Member), or any review (Admin)")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    boolean isAdmin = CurrentActor.isAdmin();
    Long actorUserId = AccessGuard.requireAuthenticatedUserId();
    reviewService.delete(id, actorUserId, isAdmin);
    return ResponseEntity.noContent().build();
  }
}
