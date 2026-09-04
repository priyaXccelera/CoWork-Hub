package com.coworkhub.api.controller;

import com.coworkhub.api.dto.WaitlistDtos.WaitlistJoinRequest;
import com.coworkhub.api.dto.WaitlistDtos.WaitlistResponse;
import com.coworkhub.api.security.AccessGuard;
import com.coworkhub.api.security.CurrentActor;
import com.coworkhub.api.service.WaitlistService;
import com.coworkhub.api.util.OffsetPageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/waitlist")
@Tag(
    name = "Waitlist",
    description =
        "Join the waitlist for a fully booked space/slot; entries are auto-promoted to CONFIRMED"
            + " as capacity frees up")
public class WaitlistController {

  private final WaitlistService waitlistService;

  public WaitlistController(WaitlistService waitlistService) {
    this.waitlistService = waitlistService;
  }

  @PostMapping
  @Operation(
      summary =
          "Join the waitlist for a space/time-slot (Members join for themselves, Admins can join"
              + " on behalf of any user)")
  public ResponseEntity<WaitlistResponse> join(@Valid @RequestBody WaitlistJoinRequest request) {
    boolean isAdmin = CurrentActor.isAdmin();
    Long actorUserId =
        isAdmin ? CurrentActor.currentUserId() : AccessGuard.requireAuthenticatedUserId();
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(waitlistService.join(request, actorUserId, isAdmin));
  }

  @GetMapping
  @Operation(summary = "List waitlist entries (own for Members, all for Admin)")
  public ResponseEntity<Page<WaitlistResponse>> list(
      @RequestParam(defaultValue = "0") int offset, @RequestParam(defaultValue = "20") int limit) {
    boolean isAdmin = CurrentActor.isAdmin();
    Long actorUserId =
        isAdmin ? CurrentActor.currentUserId() : AccessGuard.requireAuthenticatedUserId();
    return ResponseEntity.ok(
        waitlistService.list(
            actorUserId,
            isAdmin,
            OffsetPageRequest.of(offset, limit, Sort.by(Sort.Direction.ASC, "createdAt"))));
  }
}
