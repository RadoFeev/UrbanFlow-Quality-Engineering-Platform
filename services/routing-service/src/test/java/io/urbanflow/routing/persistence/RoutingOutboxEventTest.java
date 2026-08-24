package io.urbanflow.routing.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.urbanflow.routing.domain.DeliveryRoute;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RoutingOutboxEventTest {

  private static final Instant NOW = Instant.parse("2026-08-24T12:00:00Z");

  @Test
  void stagesPrivacySafeCourierAssignmentPayload() {
    RoutingOutboxEvent event = event();

    assertThat(event.eventType()).isEqualTo("CourierAssigned");
    assertThat(event.payload())
        .contains("\"shipmentId\"")
        .contains("\"courierId\"")
        .contains("\"routeId\"")
        .doesNotContain("address", "phone", "email");
    assertThat(event.attempts()).isZero();
    assertThat(event.nextAttemptAt()).isEqualTo(NOW);
  }

  @Test
  void marksPublicationIdempotently() {
    RoutingOutboxEvent event = event();
    event.markPublished(NOW.plusSeconds(1));
    event.markPublished(NOW.plusSeconds(2));

    assertThat(event.publishedAt()).isEqualTo(NOW.plusSeconds(1));
    assertThat(event.attempts()).isEqualTo(1);
  }

  @Test
  void schedulesCappedExponentialFailureBackoff() {
    RoutingOutboxEvent event = event();
    for (int attempt = 1; attempt <= 8; attempt++) {
      event.markFailed(NOW);
    }

    assertThat(event.attempts()).isEqualTo(8);
    assertThat(event.nextAttemptAt()).isEqualTo(NOW.plusSeconds(60));
  }

  static RoutingOutboxEvent event() {
    DeliveryRoute route =
        DeliveryRoute.plan(
            UUID.fromString("547f06cf-c1dd-4ad0-9780-359af05811b9"),
            UUID.fromString("7d38bce9-4f03-4269-b1db-a19fc9358679"),
            UUID.fromString("6be406c1-022e-4d36-bb5c-2da15e311fb0"),
            NOW);
    return RoutingOutboxEvent.courierAssigned(
        route, "corr-routing-001", "0123456789abcdef0123456789abcdef", NOW);
  }
}
