package io.urbanflow.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RefreshSessionTest {

  private static final Instant NOW = Instant.parse("2026-08-24T12:00:00Z");

  @Test
  void activeOnlyBeforeExpiryAndRevocation() {
    RefreshSession session = session();

    assertThat(session.isActive(NOW)).isTrue();
    assertThat(session.isActive(NOW.plusSeconds(60))).isFalse();

    session.revoke(NOW.plusSeconds(1));
    assertThat(session.isActive(NOW.plusSeconds(2))).isFalse();
    assertThat(session.revokedAt()).isEqualTo(NOW.plusSeconds(1));
  }

  @Test
  void revocationIsIdempotent() {
    RefreshSession session = session();
    session.revoke(NOW.plusSeconds(1));
    session.revoke(NOW.plusSeconds(2));

    assertThat(session.revokedAt()).isEqualTo(NOW.plusSeconds(1));
  }

  @Test
  void rejectsNonIncreasingLifetime() {
    assertThatThrownBy(
            () -> RefreshSession.issue(UUID.randomUUID(), UUID.randomUUID(), "hash", NOW, NOW))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("expiresAt");
  }

  private static RefreshSession session() {
    return RefreshSession.issue(
        UUID.fromString("03f2b30c-9761-4582-bd66-ebd468b89c85"),
        UUID.fromString("8bfc2e4e-d783-41ae-9712-a97d63dddc37"),
        "a".repeat(64),
        NOW,
        NOW.plusSeconds(60));
  }
}
