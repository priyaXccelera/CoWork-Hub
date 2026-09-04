package com.coworkhub.api.security;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

  Optional<ApiKey> findByKeyHash(String keyHash);

  List<ApiKey> findByUserId(Long userId);
}
