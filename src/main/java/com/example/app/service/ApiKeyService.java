package com.example.app.service;

import com.example.app.dto.ApiKeyDtos.ApiKeyResponse;
import com.example.app.dto.ApiKeyDtos.CreateApiKeyRequest;
import com.example.app.entity.Role;
import com.example.app.exception.ResourceNotFoundException;
import com.example.app.security.ApiKey;
import com.example.app.security.ApiKeyRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ApiKeyService {

  private final ApiKeyRepository apiKeyRepository;

  public ApiKeyService(ApiKeyRepository apiKeyRepository) {
    this.apiKeyRepository = apiKeyRepository;
  }

  public ApiKeyResponse create(CreateApiKeyRequest request) {
    String rawKey =
        UUID.randomUUID().toString().replace("-", "")
            + UUID.randomUUID().toString().replace("-", "");

    ApiKey apiKey = new ApiKey();
    apiKey.setName(request.getName());
    apiKey.setRole(request.getRole() != null ? request.getRole() : Role.ADMIN);
    apiKey.setUserId(request.getUserId());
    apiKey.setKeyHash(hash(rawKey));
    apiKey.setActive(true);

    ApiKey saved = apiKeyRepository.save(apiKey);

    ApiKeyResponse response = toResponse(saved);
    response.setApiKey(rawKey);
    return response;
  }

  public List<ApiKeyResponse> list() {
    return apiKeyRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
  }

  public void revoke(Long id) {
    ApiKey apiKey =
        apiKeyRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("API key not found with id: " + id));
    apiKey.setActive(false);
    apiKeyRepository.save(apiKey);
  }

  /** Revokes every API key tied to a given user, e.g. when the user is soft-deleted. */
  public void revokeAllForUser(Long userId) {
    List<ApiKey> keys = apiKeyRepository.findByUserId(userId);
    for (ApiKey key : keys) {
      key.setActive(false);
    }
    apiKeyRepository.saveAll(keys);
  }

  public String generateAndStoreRawKeyForUser(Long userId, Role role, String name) {
    String rawKey =
        UUID.randomUUID().toString().replace("-", "")
            + UUID.randomUUID().toString().replace("-", "");
    ApiKey apiKey = new ApiKey();
    apiKey.setName(name);
    apiKey.setRole(role);
    apiKey.setUserId(userId);
    apiKey.setKeyHash(hash(rawKey));
    apiKey.setActive(true);
    apiKeyRepository.save(apiKey);
    return rawKey;
  }

  private ApiKeyResponse toResponse(ApiKey apiKey) {
    ApiKeyResponse response = new ApiKeyResponse();
    response.setId(apiKey.getId());
    response.setName(apiKey.getName());
    response.setRole(apiKey.getRole());
    response.setUserId(apiKey.getUserId());
    response.setActive(apiKey.isActive());
    response.setCreatedAt(apiKey.getCreatedAt());
    response.setLastUsedAt(apiKey.getLastUsedAt());
    return response;
  }

  private String hash(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashBytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashBytes);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm not available", e);
    }
  }
}
