package io.urbanflow.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** One-time refresh-token session; rotation revokes the previous token. */
@Entity
@Table(name = "refresh_sessions")
public class RefreshSession {

  @Id private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "token_hash", nullable = false, unique = true, length = 64)
  private String tokenHash;

  @Column(name = "issued_at", nullable = false)
  private Instant issuedAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  /** Constructor reserved for persistence. */
  protected RefreshSession() {}

  /** Issues a refresh session storing only the irreversible token hash. */
  public static RefreshSession issue(
      UUID id, UUID userId, String tokenHash, Instant issuedAt, Instant expiresAt) {
    if (!expiresAt.isAfter(issuedAt)) {
      throw new IllegalArgumentException("expiresAt must be after issuedAt");
    }
    RefreshSession session = new RefreshSession();
    session.id = Objects.requireNonNull(id, "id must not be null");
    session.userId = Objects.requireNonNull(userId, "userId must not be null");
    session.tokenHash = requireText(tokenHash, "tokenHash");
    session.issuedAt = Objects.requireNonNull(issuedAt, "issuedAt must not be null");
    session.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    return session;
  }

  /** Returns whether this session can be exchanged at the supplied time. */
  public boolean isActive(Instant now) {
    return revokedAt == null && now.isBefore(expiresAt);
  }

  /** Revokes the session idempotently. */
  public void revoke(Instant now) {
    if (revokedAt == null) {
      revokedAt = Objects.requireNonNull(now, "now must not be null");
    }
  }

  /** Returns the session identifier. */
  public UUID id() {
    return id;
  }

  /** Returns the owning user identifier. */
  public UUID userId() {
    return userId;
  }

  /** Returns the irreversible refresh credential hash. */
  public String tokenHash() {
    return tokenHash;
  }

  /** Returns when the session was issued. */
  public Instant issuedAt() {
    return issuedAt;
  }

  /** Returns the exclusive session expiry. */
  public Instant expiresAt() {
    return expiresAt;
  }

  /** Returns the revocation time or null while not revoked. */
  public Instant revokedAt() {
    return revokedAt;
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }
}
