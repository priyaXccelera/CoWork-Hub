package com.coworkhub.api.controller;

import com.coworkhub.api.dto.NotificationDtos.NotificationResponse;
import com.coworkhub.api.dto.NotificationDtos.UnreadCountResponse;
import com.coworkhub.api.security.AccessGuard;
import com.coworkhub.api.service.NotificationService;
import com.coworkhub.api.util.OffsetPageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Personal in-app notification log")
public class NotificationController {

  private final NotificationService notificationService;

  public NotificationController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @GetMapping
  @Operation(summary = "List your notifications, paginated and newest first; optionally filter by read state")
  public ResponseEntity<Page<NotificationResponse>> list(
      @RequestParam(required = false) Boolean isRead,
      @RequestParam(defaultValue = "0") int offset,
      @RequestParam(defaultValue = "20") int limit) {
    Long userId = AccessGuard.requireAuthenticatedUserId();
    return ResponseEntity.ok(
        notificationService.list(
            userId, isRead, OffsetPageRequest.of(offset, limit, Sort.by(Sort.Direction.DESC, "createdAt"))));
  }

  @GetMapping("/unread-count")
  @Operation(summary = "Get your unread notification count")
  public ResponseEntity<UnreadCountResponse> unreadCount() {
    return ResponseEntity.ok(
        new UnreadCountResponse(notificationService.unreadCount(AccessGuard.requireAuthenticatedUserId())));
  }

  @PatchMapping("/{id}/read")
  @Operation(summary = "Mark one of your own notifications as read")
  public ResponseEntity<NotificationResponse> markRead(@PathVariable Long id) {
    return ResponseEntity.ok(notificationService.markRead(id, AccessGuard.requireAuthenticatedUserId()));
  }

  @PatchMapping("/read-all")
  @Operation(summary = "Mark all of your notifications as read")
  public ResponseEntity<Void> markAllRead() {
    notificationService.markAllRead(AccessGuard.requireAuthenticatedUserId());
    return ResponseEntity.noContent().build();
  }
}
