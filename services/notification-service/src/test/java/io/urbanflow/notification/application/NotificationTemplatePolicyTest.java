package io.urbanflow.notification.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.urbanflow.notification.event.ShipmentEventEnvelope;
import io.urbanflow.notification.event.ShipmentEventPayload;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class NotificationTemplatePolicyTest {

  private final NotificationTemplatePolicy policy = new NotificationTemplatePolicy();

  @ParameterizedTest
  @MethodSource("notifiableEvents")
  void selectsSafeTrackingTemplateForAllowlistedEvents(String eventType) {
    assertThat(policy.select(event(eventType)))
        .hasValueSatisfying(
            template -> {
              assertThat(template.type()).isNotBlank();
              assertThat(template.subject()).isNotBlank();
              assertThat(template.body())
                  .contains("UF0123456789ABCDEF01")
                  .doesNotContain("@", "+359", "street");
            });
  }

  @ParameterizedTest
  @MethodSource("ignoredEvents")
  void ignoresEventsWithoutCustomerMessage(String eventType) {
    assertThat(policy.select(event(eventType))).isEmpty();
  }

  private static Stream<String> notifiableEvents() {
    return Stream.of(
        "ShipmentCreated",
        "ShipmentConfirmed",
        "ShipmentOutForDelivery",
        "ShipmentDelivered",
        "DeliveryAttempted",
        "ShipmentCancelled",
        "ShipmentReturnStarted");
  }

  private static Stream<String> ignoredEvents() {
    return Stream.of("ShipmentPickedUp", "ShipmentArrivedAtHub", "ShipmentDispatched");
  }

  static ShipmentEventEnvelope event(String eventType) {
    UUID shipmentId = UUID.fromString("07f9e6be-fb52-426f-9b30-3d52cf25b6cf");
    return new ShipmentEventEnvelope(
        UUID.fromString("153fa8bd-330f-426f-a767-b3602753b1a3"),
        eventType,
        shipmentId,
        1,
        Instant.parse("2026-08-24T12:00:00Z"),
        "1.0",
        "corr-notification-001",
        "0123456789abcdef0123456789abcdef",
        "shipment-service",
        new ShipmentEventPayload(
            shipmentId,
            UUID.fromString("1e096247-e29c-48e7-aab7-67f7e4c4e8d0"),
            "UF0123456789ABCDEF01",
            "CONFIRMED",
            1));
  }
}
