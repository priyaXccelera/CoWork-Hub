package com.example.app.controller;

import com.example.app.dto.WaitlistDtos.WaitlistResponse;
import com.example.app.security.AccessGuard;
import com.example.app.security.CurrentActor;
import com.example.app.service.WaitlistService;
import com.example.app.util.OffsetPageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/waitlist")
@Tag(
    name = "Waitlist",
    description = "Waitlist entries created automatically when a booking conflicts")
public class WaitlistController {

  private final WaitlistService waitlistService;

  public WaitlistController(WaitlistService waitlistService) {
    this.waitlistService = waitlistService;
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
