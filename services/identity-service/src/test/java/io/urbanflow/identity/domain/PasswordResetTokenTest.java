package io.urbanflow.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PasswordResetTokenTest {

  private static final Instant NOW = Instant.parse("2026-08-24T12:00:00Z");

  @Test
  void consumesActiveTokenExactlyOnce() {
    PasswordResetToken token = token();
    assertThat(token.isActive(NOW)).isTrue();

    token.consume(NOW.plusSeconds(1));

    assertThat(token.usedAt()).isEqualTo(NOW.plusSeconds(1));
    assertThat(token.isActive(NOW.plusSeconds(2))).isFalse();
    assertThatThrownBy(() -> token.consume(NOW.plusSeconds(2)))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void expiresAtBoundary() {
    PasswordResetToken token = token();
    assertThat(token.isActive(NOW.plusSeconds(59))).isTrue();
    assertThat(token.isActive(NOW.plusSeconds(60))).isFalse();
  }

  @Test
  void rejectsBlankHash() {
    assertThatThrownBy(
            () ->
                PasswordResetToken.issue(
                    UUID.randomUUID(), UUID.randomUUID(), " ", NOW.plusSeconds(1)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("tokenHash");
  }

  private static PasswordResetToken token() {
    return PasswordResetToken.issue(
        UUID.fromString("92b3eff5-cf93-44ed-90cd-d35a29f8c91e"),
        UUID.fromString("79d170dc-42a6-4b76-a2b7-cb64ddb78075"),
        "b".repeat(64),
        NOW.plusSeconds(60));
  }
}
