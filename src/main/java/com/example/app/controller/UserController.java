package com.example.app.controller;

import com.example.app.dto.UserDtos.UserRequest;
import com.example.app.dto.UserDtos.UserResponse;
import com.example.app.security.AccessGuard;
import com.example.app.service.UserService;
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
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Admin-only user management")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @PostMapping
  @Operation(summary = "Create a user (Admin only)")
  public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest request) {
    AccessGuard.requireAdmin();
    return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
  }

  @GetMapping
  @Operation(summary = "List users with pagination (Admin only)")
  public ResponseEntity<Page<UserResponse>> list(
      @RequestParam(defaultValue = "0") int offset, @RequestParam(defaultValue = "20") int limit) {
    AccessGuard.requireAdmin();
    return ResponseEntity.ok(
        userService.list(
            OffsetPageRequest.of(offset, limit, Sort.by(Sort.Direction.DESC, "createdAt"))));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a user by id (Admin only)")
  public ResponseEntity<UserResponse> get(@PathVariable Long id) {
    AccessGuard.requireAdmin();
    return ResponseEntity.ok(userService.get(id));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update a user (Admin only)")
  public ResponseEntity<UserResponse> update(
      @PathVariable Long id, @Valid @RequestBody UserRequest request) {
    AccessGuard.requireAdmin();
    return ResponseEntity.ok(userService.update(id, request));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Soft delete a user (Admin only)")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    AccessGuard.requireAdmin();
    userService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
