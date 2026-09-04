package com.example.app.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

  private static final String HEADER_NAME = "X-API-Key";

  private final ApiKeyRepository apiKeyRepository;

  public ApiKeyFilter(ApiKeyRepository apiKeyRepository) {
    this.apiKeyRepository = apiKeyRepository;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String rawKey = request.getHeader(HEADER_NAME);

    if (rawKey != null && !rawKey.isBlank()) {
      String hashed = hash(rawKey);
      Optional<ApiKey> found = apiKeyRepository.findByKeyHash(hashed);

      if (found.isPresent() && found.get().isActive()) {
        ApiKey apiKey = found.get();
        apiKey.setLastUsedAt(LocalDateTime.now());
        apiKeyRepository.save(apiKey);

        String principal =
            apiKey.getUserId() != null ? apiKey.getUserId().toString() : apiKey.getName();

        List<GrantedAuthority> authorities =
            List.of(new SimpleGrantedAuthority("ROLE_" + apiKey.getRole().name()));

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(principal, null, authorities);

        SecurityContextHolder.getContext().setAuthentication(authentication);
      }
    }

    filterChain.doFilter(request, response);
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
