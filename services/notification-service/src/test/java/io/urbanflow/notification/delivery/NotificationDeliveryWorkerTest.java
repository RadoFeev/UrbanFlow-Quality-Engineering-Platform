package io.urbanflow.notification.delivery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.urbanflow.notification.config.NotificationProperties;
import io.urbanflow.notification.domain.Notification;
import io.urbanflow.notification.domain.NotificationStatus;
import io.urbanflow.notification.persistence.NotificationRepository;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationDeliveryWorkerTest {

  private static final Instant NOW = Instant.parse("2026-08-24T12:00:00Z");

  @Mock private NotificationRepository notifications;
  @Mock private NotificationGateway gateway;

  private SimpleMeterRegistry meters;
  private NotificationDeliveryWorker worker;

  @BeforeEach
  void setUp() {
    meters = new SimpleMeterRegistry();
    worker =
        new NotificationDeliveryWorker(
            notifications,
            gateway,
            new NotificationProperties(
                URI.create("https://communications.urbanflow.test"), Duration.ofSeconds(1), 2),
            Clock.fixed(NOW, ZoneOffset.UTC),
            meters);
  }

  @Test
  void marksSuccessfulGatewayAcknowledgment() {
    Notification notification = notification();
    when(notifications.findTop50ByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            any(), any()))
        .thenReturn(List.of(notification));
    when(gateway.deliver(notification)).thenReturn("provider-001");

    assertThat(worker.deliverDue()).isEqualTo(1);

    assertThat(notification.status()).isEqualTo(NotificationStatus.DELIVERED);
    assertThat(notification.providerMessageId()).isEqualTo("provider-001");
    verify(notifications).save(notification);
    assertThat(metric("delivered")).isEqualTo(1.0);
  }

  @Test
  void schedulesRetryAfterFirstFailure() {
    Notification notification = notification();
    when(notifications.findTop50ByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            any(), any()))
        .thenReturn(List.of(notification));
    when(gateway.deliver(notification)).thenThrow(new IllegalStateException("gateway down"));

    assertThat(worker.deliverDue()).isZero();

    assertThat(notification.status()).isEqualTo(NotificationStatus.RETRY);
    assertThat(notification.nextAttemptAt()).isEqualTo(NOW.plusSeconds(1));
    assertThat(metric("retry")).isEqualTo(1.0);
  }

  @Test
  void deadLettersAtMaximumAttempt() {
    Notification notification = notification();
    notification.markFailed("first", 2, NOW.minusSeconds(1));
    when(notifications.findTop50ByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            any(), any()))
        .thenReturn(List.of(notification));
    when(gateway.deliver(notification)).thenThrow(new IllegalStateException("gateway down"));

    worker.deliverDue();

    assertThat(notification.status()).isEqualTo(NotificationStatus.DEAD_LETTER);
    assertThat(metric("dead_letter")).isEqualTo(1.0);
  }

  @Test
  void returnsZeroForEmptyBatch() {
    when(notifications.findTop50ByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            any(), any()))
        .thenReturn(List.of());
    assertThat(worker.deliverDue()).isZero();
  }

  private double metric(String result) {
    return meters.get("urbanflow.notification.delivery").tag("result", result).counter().count();
  }

  private static Notification notification() {
    return Notification.create(
        UUID.fromString("ae18476e-c9fc-4db5-9aac-09956532aa60"),
        UUID.fromString("153fa8bd-330f-426f-a767-b3602753b1a3"),
        UUID.fromString("1e096247-e29c-48e7-aab7-67f7e4c4e8d0"),
        UUID.fromString("07f9e6be-fb52-426f-9b30-3d52cf25b6cf"),
        "SHIPMENT_CONFIRMED",
        "Confirmed",
        "Tracking UF0123456789ABCDEF01",
        NOW);
  }
}
