package com.example.app.security;

import com.example.app.entity.User;
import com.example.app.entity.UserStatus;
import com.example.app.repository.UserRepository;
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
import org.springframework.context.annotation.Lazy;
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
  private final UserRepository userRepository;

  /**
   * The repositories are injected lazily (proxied, not resolved eagerly) because Spring Boot's
   * {@code ServletContextInitializerBeans} scans for all {@code Filter} beans -- including this
   * one, since it implements {@link jakarta.servlet.Filter} via {@link OncePerRequestFilter} --
   * during Tomcat context startup, which happens before the normal singleton pre-instantiation
   * order that guarantees Flyway migrations run before the JPA {@code entityManagerFactory} is
   * created. Without {@code @Lazy}, that early filter lookup would force-create the JPA
   * repositories (and therefore entityManagerFactory) ahead of Flyway, tripping Spring's
   * "Circular depends-on relationship between 'flyway' and 'entityManagerFactory'" startup guard.
   */
  public ApiKeyFilter(
      @Lazy ApiKeyRepository apiKeyRepository, @Lazy UserRepository userRepository) {
    this.apiKeyRepository = apiKeyRepository;
    this.userRepository = userRepository;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String rawKey = request.getHeader(HEADER_NAME);

    if (rawKey != null && !rawKey.isBlank()) {
      String hashed = hash(rawKey);
      Optional<ApiKey> found = apiKeyRepository.findByKeyHash(hashed);

      if (found.isPresent() && found.get().isActive() && isUsable(found.get())) {
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

  /**
   * An API key tied to a user must not authenticate once that user has been soft-deleted or
   * deactivated, even if the key row itself is still marked active.
   */
  private boolean isUsable(ApiKey apiKey) {
    if (apiKey.getUserId() == null) {
      return true;
    }
    Optional<User> user = userRepository.findByIdAndDeletedFalse(apiKey.getUserId());
    return user.isPresent() && user.get().getStatus() == UserStatus.ACTIVE;
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
