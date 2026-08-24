package io.urbanflow.identity.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Validated security lifetimes, issuer metadata, and lockout policy. */
@ConfigurationProperties("urbanflow.identity.security")
public record IdentitySecurityProperties(
    String issuer,
    String audience,
    String signingSecret,
    Duration accessTtl,
    Duration refreshTtl,
    Duration resetTtl,
    Duration lockoutDuration,
    int maximumLoginAttempts) {

  /** Rejects unsafe or nonsensical security configuration at startup. */
  public IdentitySecurityProperties {
    issuer = requireText(issuer, "issuer");
    audience = requireText(audience, "audience");
    if (signingSecret == null || signingSecret.length() < 32) {
      throw new IllegalArgumentException("signingSecret must contain at least 32 characters");
    }
    requirePositive(accessTtl, "accessTtl");
    requirePositive(refreshTtl, "refreshTtl");
    requirePositive(resetTtl, "resetTtl");
    requirePositive(lockoutDuration, "lockoutDuration");
    if (maximumLoginAttempts < 2) {
      throw new IllegalArgumentException("maximumLoginAttempts must be at least 2");
    }
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value.trim();
  }

  private static void requirePositive(Duration value, String field) {
    if (value == null || value.isZero() || value.isNegative()) {
      throw new IllegalArgumentException(field + " must be positive");
    }
  }
}
