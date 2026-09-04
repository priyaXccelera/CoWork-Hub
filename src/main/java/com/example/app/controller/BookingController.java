package com.example.app.controller;

import com.example.app.dto.BookingDtos.BookingRequest;
import com.example.app.dto.BookingDtos.BookingResponse;
import com.example.app.entity.BookingStatus;
import com.example.app.entity.SpaceType;
import com.example.app.security.AccessGuard;
import com.example.app.security.CurrentActor;
import com.example.app.service.BookingService;
import com.example.app.util.OffsetPageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings")
@Tag(name = "Bookings", description = "Booking creation, cancellation and listing")
public class BookingController {

  private final BookingService bookingService;

  public BookingController(BookingService bookingService) {
    this.bookingService = bookingService;
  }

  @PostMapping
  @Operation(
      summary = "Create a booking (Members book for themselves, Admins can book for any user)")
  public ResponseEntity<BookingResponse> create(@Valid @RequestBody BookingRequest request) {
    boolean isAdmin = CurrentActor.isAdmin();
    Long actorUserId =
        isAdmin ? CurrentActor.currentUserId() : AccessGuard.requireAuthenticatedUserId();
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(bookingService.createBooking(request, actorUserId, isAdmin));
  }

  @GetMapping
  @Operation(
      summary = "List bookings with pagination, sorting and filtering by date/status/spaceType")
  public ResponseEntity<Page<BookingResponse>> list(
      @RequestParam(required = false) LocalDate date,
      @RequestParam(required = false) BookingStatus status,
      @RequestParam(required = false) SpaceType spaceType,
      @RequestParam(defaultValue = "0") int offset,
      @RequestParam(defaultValue = "20") int limit) {
    boolean isAdmin = CurrentActor.isAdmin();
    Long actorUserId =
        isAdmin ? CurrentActor.currentUserId() : AccessGuard.requireAuthenticatedUserId();
    Page<BookingResponse> page =
        bookingService.list(
            actorUserId,
            isAdmin,
            date,
            status,
            spaceType,
            OffsetPageRequest.of(offset, limit, Sort.by(Sort.Direction.DESC, "startTime")));
    return ResponseEntity.ok(page);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a booking by id (own booking for Members, any for Admin)")
  public ResponseEntity<BookingResponse> get(@PathVariable Long id) {
    boolean isAdmin = CurrentActor.isAdmin();
    Long actorUserId =
        isAdmin ? CurrentActor.currentUserId() : AccessGuard.requireAuthenticatedUserId();
    return ResponseEntity.ok(bookingService.get(id, actorUserId, isAdmin));
  }

  @PostMapping("/{id}/cancel")
  @Operation(
      summary =
          "Cancel a booking, applying the cancellation policy and promoting the next waitlisted"
              + " user")
  public ResponseEntity<BookingResponse> cancel(@PathVariable Long id) {
    boolean isAdmin = CurrentActor.isAdmin();
    Long actorUserId =
        isAdmin ? CurrentActor.currentUserId() : AccessGuard.requireAuthenticatedUserId();
    return ResponseEntity.ok(bookingService.cancelBooking(id, actorUserId, isAdmin));
  }
}
