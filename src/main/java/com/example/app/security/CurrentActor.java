package com.example.app.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentActor {

  private CurrentActor() {}

  public static boolean isAdmin() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      return false;
    }
    return authentication.getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
  }

  public static Long currentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || authentication.getName() == null) {
      return null;
    }
    try {
      return Long.parseLong(authentication.getName());
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
