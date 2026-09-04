package com.example.app.controller;

import com.example.app.dto.SpaceDtos.SpaceRequest;
import com.example.app.dto.SpaceDtos.SpaceResponse;
import com.example.app.entity.SpaceType;
import com.example.app.security.AccessGuard;
import com.example.app.service.SpaceService;
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
@RequestMapping("/api/v1/spaces")
@Tag(
    name = "Spaces",
    description = "Space management (read access for any authenticated user, writes Admin-only)")
public class SpaceController {

  private final SpaceService spaceService;

  public SpaceController(SpaceService spaceService) {
    this.spaceService = spaceService;
  }

  @PostMapping
  @Operation(summary = "Create a space (Admin only)")
  public ResponseEntity<SpaceResponse> create(@Valid @RequestBody SpaceRequest request) {
    AccessGuard.requireAdmin();
    return ResponseEntity.status(HttpStatus.CREATED).body(spaceService.create(request));
  }

  @GetMapping
  @Operation(
      summary =
          "List spaces with pagination and optional type filter (any authenticated user, so"
              + " Members can discover spaces to book)")
  public ResponseEntity<Page<SpaceResponse>> list(
      @RequestParam(required = false) SpaceType type,
      @RequestParam(defaultValue = "0") int offset,
      @RequestParam(defaultValue = "20") int limit) {
    return ResponseEntity.ok(
        spaceService.list(
            type, OffsetPageRequest.of(offset, limit, Sort.by(Sort.Direction.ASC, "id"))));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a space by id (any authenticated user)")
  public ResponseEntity<SpaceResponse> get(@PathVariable Long id) {
    return ResponseEntity.ok(spaceService.get(id));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update a space (Admin only)")
  public ResponseEntity<SpaceResponse> update(
      @PathVariable Long id, @Valid @RequestBody SpaceRequest request) {
    AccessGuard.requireAdmin();
    return ResponseEntity.ok(spaceService.update(id, request));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Soft delete a space (Admin only)")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    AccessGuard.requireAdmin();
    spaceService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
