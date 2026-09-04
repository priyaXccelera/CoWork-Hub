package com.example.app.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS is restricted to an explicit, configurable allow-list of origins (see
 * app.cors.allowed-origins / APP_CORS_ALLOWED_ORIGINS) and a specific set of headers/methods,
 * rather than "*" for everything combined with credentials.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

  @Value("${app.cors.allowed-origins:}")
  private String allowedOrigins;

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    List<String> origins =
        Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();

    if (origins.isEmpty()) {
      // No browser origins configured: do not register any CORS mapping, so cross-origin
      // browser requests are rejected by default while server-to-server / curl calls (which
      // don't send an Origin header) are unaffected.
      return;
    }

    registry
        .addMapping("/api/v1/**")
        .allowedOrigins(origins.toArray(new String[0]))
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("X-API-Key", "Content-Type", "Accept", "Authorization")
        .allowCredentials(true)
        .maxAge(3600);
  }
}
