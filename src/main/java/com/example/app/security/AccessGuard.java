package com.example.app.security;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class AccessGuard {

  private AccessGuard() {}

  public static void requireAdmin() {
    if (!CurrentActor.isAdmin()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This operation requires ADMIN role");
    }
  }

  public static Long requireAuthenticatedUserId() {
    Long userId = CurrentActor.currentUserId();
    if (userId == null) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Unable to resolve the authenticated user");
    }
    return userId;
  }
}
