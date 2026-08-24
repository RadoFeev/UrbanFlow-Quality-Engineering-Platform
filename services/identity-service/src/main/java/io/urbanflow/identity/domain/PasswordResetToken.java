package io.urbanflow.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Single-use password reset authorization storing only an irreversible token hash. */
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

  @Id private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "token_hash", nullable = false, unique = true, length = 64)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "used_at")
  private Instant usedAt;

  /** Constructor reserved for persistence. */
  protected PasswordResetToken() {}

  /** Creates a single-use reset authorization. */
  public static PasswordResetToken issue(
      UUID id, UUID userId, String tokenHash, Instant expiresAt) {
    PasswordResetToken token = new PasswordResetToken();
    token.id = Objects.requireNonNull(id, "id must not be null");
    token.userId = Objects.requireNonNull(userId, "userId must not be null");
    token.tokenHash = requireText(tokenHash);
    token.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    return token;
  }

  /** Returns whether the token is unused and unexpired. */
  public boolean isActive(Instant now) {
    return usedAt == null && now.isBefore(expiresAt);
  }

  /** Consumes the token exactly once. */
  public void consume(Instant now) {
    if (!isActive(now)) {
      throw new IllegalStateException("password reset token is not active");
    }
    usedAt = now;
  }

  /** Returns the reset authorization identifier. */
  public UUID id() {
    return id;
  }

  /** Returns the owning user identifier. */
  public UUID userId() {
    return userId;
  }

  /** Returns the irreversible reset credential hash. */
  public String tokenHash() {
    return tokenHash;
  }

  /** Returns the exclusive reset expiry. */
  public Instant expiresAt() {
    return expiresAt;
  }

  /** Returns the consumption time or null while unused. */
  public Instant usedAt() {
    return usedAt;
  }

  private static String requireText(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("tokenHash must not be blank");
    }
    return value;
  }
}
