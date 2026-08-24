package io.urbanflow.identity.application;

import java.time.Instant;

/** Access and rotating refresh credentials returned after authentication. */
public record AuthTokens(
    String tokenType, String accessToken, String refreshToken, Instant accessExpiresAt) {

  /** Creates a bearer token response. */
  public static AuthTokens bearer(
      String accessToken, String refreshToken, Instant accessExpiresAt) {
    return new AuthTokens("Bearer", accessToken, refreshToken, accessExpiresAt);
  }
}
