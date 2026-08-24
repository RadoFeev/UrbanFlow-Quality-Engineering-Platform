package io.urbanflow.identity.security;

import io.urbanflow.identity.domain.Permission;
import io.urbanflow.identity.domain.Role;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Verified access-token identity and authorization claims. */
public record AccessTokenClaims(
    UUID userId,
    String email,
    Role role,
    Set<Permission> permissions,
    UUID tokenId,
    Instant issuedAt,
    Instant expiresAt) {

  /** Defensively copies permissions. */
  public AccessTokenClaims {
    permissions = Set.copyOf(permissions);
  }
}
