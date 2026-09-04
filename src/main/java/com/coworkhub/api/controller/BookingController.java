package com.coworkhub.api.controller;

import com.coworkhub.api.dto.BookingDtos.BookingRequest;
import com.coworkhub.api.dto.BookingDtos.BookingResponse;
import com.coworkhub.api.entity.BookingStatus;
import com.coworkhub.api.entity.SpaceType;
import com.coworkhub.api.exception.BusinessRuleException;
import com.coworkhub.api.security.AccessGuard;
import com.coworkhub.api.security.CurrentActor;
import com.coworkhub.api.service.BookingService;
import com.coworkhub.api.util.OffsetPageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
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

  private static final Set<String> SORTABLE_FIELDS =
      Set.of("startTime", "endTime", "createdAt", "updatedAt", "costCharged", "status", "id");

  @GetMapping
  @Operation(
      summary =
          "List bookings with pagination, sorting and filtering by date/date-range/status/"
              + "spaceType. Date range accepts from/to or startDate/endDate (inclusive, "
              + "yyyy-MM-dd). Sort accepts '<field>,<asc|desc>', e.g. 'startTime,asc'.")
  public ResponseEntity<Page<BookingResponse>> list(
      @RequestParam(required = false) LocalDate date,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      @RequestParam(required = false) LocalDate startDate,
      @RequestParam(required = false) LocalDate endDate,
      @RequestParam(required = false) BookingStatus status,
      @RequestParam(required = false) SpaceType spaceType,
      @RequestParam(required = false) String sort,
      @RequestParam(defaultValue = "0") int offset,
      @RequestParam(defaultValue = "20") int limit) {
    boolean isAdmin = CurrentActor.isAdmin();
    Long actorUserId =
        isAdmin ? CurrentActor.currentUserId() : AccessGuard.requireAuthenticatedUserId();

    LocalDate effectiveFrom = from != null ? from : startDate;
    LocalDate effectiveTo = to != null ? to : endDate;
    LocalDateTime fromDateTime = effectiveFrom != null ? effectiveFrom.atStartOfDay() : null;
    // Upper bound is exclusive, so add a day to make the "to"/"endDate" date inclusive.
    LocalDateTime toDateTime = effectiveTo != null ? effectiveTo.plusDays(1).atStartOfDay() : null;
    if (fromDateTime != null && toDateTime != null && !toDateTime.isAfter(fromDateTime)) {
      throw new BusinessRuleException("'to'/'endDate' must not be before 'from'/'startDate'");
    }

    Page<BookingResponse> page =
        bookingService.list(
            actorUserId,
            isAdmin,
            date,
            fromDateTime,
            toDateTime,
            status,
            spaceType,
            OffsetPageRequest.of(offset, limit, parseSort(sort)));
    return ResponseEntity.ok(page);
  }

  private Sort parseSort(String sort) {
    String field = "startTime";
    Sort.Direction direction = Sort.Direction.DESC;
    if (sort != null && !sort.isBlank()) {
      String[] parts = sort.split(",");
      field = parts[0].trim();
      if (!SORTABLE_FIELDS.contains(field)) {
        throw new BusinessRuleException(
            "Invalid sort field '" + field + "'. Allowed values: " + SORTABLE_FIELDS);
      }
      if (parts.length > 1) {
        String dir = parts[1].trim();
        if ("asc".equalsIgnoreCase(dir)) {
          direction = Sort.Direction.ASC;
        } else if ("desc".equalsIgnoreCase(dir)) {
          direction = Sort.Direction.DESC;
        } else {
          throw new BusinessRuleException(
              "Invalid sort direction '" + dir + "'. Allowed values: asc, desc");
        }
      }
    }
    // Always break ties on id so repeated identical queries return a stable, deterministic order
    // even when multiple bookings share the same value for the sorted field (e.g. startTime).
    if ("id".equals(field)) {
      return Sort.by(direction, "id");
    }
    return Sort.by(direction, field).and(Sort.by(Sort.Direction.ASC, "id"));
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
