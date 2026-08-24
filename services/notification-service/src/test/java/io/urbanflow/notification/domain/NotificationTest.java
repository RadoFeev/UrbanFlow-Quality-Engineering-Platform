package io.urbanflow.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationTest {

  private static final Instant NOW = Instant.parse("2026-08-24T12:00:00Z");

  @Test
  void createsDuePendingNotification() {
    Notification notification = notification();

    assertThat(notification.status()).isEqualTo(NotificationStatus.PENDING);
    assertThat(notification.attempts()).isZero();
    assertThat(notification.isDue(NOW)).isTrue();
    assertThat(notification.isDue(NOW.minusNanos(1))).isFalse();
    assertThat(notification.version()).isZero();
  }

  @Test
  void marksProviderAcknowledgedDelivery() {
    Notification notification = notification();

    notification.markDelivered("provider-001", NOW.plusSeconds(1));

    assertThat(notification.status()).isEqualTo(NotificationStatus.DELIVERED);
    assertThat(notification.providerMessageId()).isEqualTo("provider-001");
    assertThat(notification.deliveredAt()).isEqualTo(NOW.plusSeconds(1));
    assertThat(notification.attempts()).isEqualTo(1);
    assertThat(notification.isDue(NOW.plusSeconds(2))).isFalse();
  }

  @Test
  void schedulesExponentialRetryThenDeadLettersAtLimit() {
    Notification notification = notification();

    notification.markFailed("temporary failure", 3, NOW);
    assertThat(notification.status()).isEqualTo(NotificationStatus.RETRY);
    assertThat(notification.nextAttemptAt()).isEqualTo(NOW.plusSeconds(1));

    notification.markFailed("temporary failure", 3, NOW.plusSeconds(1));
    assertThat(notification.nextAttemptAt()).isEqualTo(NOW.plusSeconds(3));

    notification.markFailed("permanent failure", 3, NOW.plusSeconds(3));
    assertThat(notification.status()).isEqualTo(NotificationStatus.DEAD_LETTER);
    assertThat(notification.attempts()).isEqualTo(3);
  }

  @Test
  void truncatesStoredFailureSummary() {
    Notification notification = notification();
    notification.markFailed("x".repeat(700), 2, NOW);
    assertThat(notification.lastError()).hasSize(500);
  }

  @Test
  void rejectsMutationOfTerminalStates() {
    Notification delivered = notification();
    delivered.markDelivered("provider-001", NOW);
    assertThatThrownBy(() -> delivered.markFailed("late", 3, NOW))
        .isInstanceOf(IllegalStateException.class);

    Notification deadLetter = notification();
    deadLetter.markFailed("failed", 1, NOW);
    assertThatThrownBy(() -> deadLetter.markDelivered("provider-002", NOW))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void rejectsBlankMessageFields() {
    assertThatThrownBy(
            () ->
                Notification.create(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    " ",
                    "subject",
                    "body",
                    NOW))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("type");
  }

  private static Notification notification() {
    return Notification.create(
        UUID.fromString("ae18476e-c9fc-4db5-9aac-09956532aa60"),
        UUID.fromString("153fa8bd-330f-426f-a767-b3602753b1a3"),
        UUID.fromString("1e096247-e29c-48e7-aab7-67f7e4c4e8d0"),
        UUID.fromString("07f9e6be-fb52-426f-9b30-3d52cf25b6cf"),
        "SHIPMENT_CONFIRMED",
        "Shipment confirmed",
        "Your shipment is confirmed.",
        NOW);
  }
}
