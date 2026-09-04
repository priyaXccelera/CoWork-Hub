package com.coworkhub.api.controller;

import com.coworkhub.api.dto.ApiKeyDtos.ApiKeyResponse;
import com.coworkhub.api.dto.ApiKeyDtos.CreateApiKeyRequest;
import com.coworkhub.api.service.ApiKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/api-keys")
@Tag(name = "API Keys", description = "Admin-gated API key management")
public class ApiKeyController {

  private final ApiKeyService apiKeyService;

  @Value("${admin.api-key}")
  private String configuredAdminKey;

  public ApiKeyController(ApiKeyService apiKeyService) {
    this.apiKeyService = apiKeyService;
  }

  @PostMapping
  @Operation(summary = "Create a new API key (requires X-Admin-Key header)")
  public ResponseEntity<ApiKeyResponse> create(
      @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
      @Valid @RequestBody CreateApiKeyRequest request) {
    verifyAdminKey(adminKey);
    return ResponseEntity.status(HttpStatus.CREATED).body(apiKeyService.create(request));
  }

  @GetMapping
  @Operation(summary = "List all API keys (requires X-Admin-Key header)")
  public ResponseEntity<List<ApiKeyResponse>> list(
      @RequestHeader(value = "X-Admin-Key", required = false) String adminKey) {
    verifyAdminKey(adminKey);
    return ResponseEntity.ok(apiKeyService.list());
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Revoke an API key (requires X-Admin-Key header)")
  public ResponseEntity<Void> revoke(
      @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
      @PathVariable Long id) {
    verifyAdminKey(adminKey);
    apiKeyService.revoke(id);
    return ResponseEntity.noContent().build();
  }

  private void verifyAdminKey(String adminKey) {
    if (adminKey == null
        || !MessageDigest.isEqual(
            adminKey.getBytes(StandardCharsets.UTF_8),
            configuredAdminKey.getBytes(StandardCharsets.UTF_8))) {
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED, "Invalid or missing X-Admin-Key header");
    }
  }
}
