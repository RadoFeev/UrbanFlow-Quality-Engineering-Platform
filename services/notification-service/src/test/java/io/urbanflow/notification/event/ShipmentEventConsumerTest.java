package io.urbanflow.notification.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.urbanflow.notification.application.NotificationProcessingService;
import io.urbanflow.notification.application.ProcessingResult;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ShipmentEventConsumerTest {

  @Mock private ObjectMapper objectMapper;
  @Mock private NotificationProcessingService processing;

  @Test
  void deserializesAndProcessesEvent() {
    ShipmentEventEnvelope event = event();
    when(objectMapper.readValue("json", ShipmentEventEnvelope.class)).thenReturn(event);
    when(processing.process(event)).thenReturn(ProcessingResult.CREATED);

    assertThat(new ShipmentEventConsumer(objectMapper, processing).consume("json"))
        .isEqualTo(ProcessingResult.CREATED);
  }

  @Test
  void classifiesMalformedEvent() {
    when(objectMapper.readValue("bad", ShipmentEventEnvelope.class))
        .thenThrow(new IllegalArgumentException("bad JSON"));

    assertThatThrownBy(() -> new ShipmentEventConsumer(objectMapper, processing).consume("bad"))
        .isInstanceOf(NotificationEventDeserializationException.class)
        .hasCauseInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(processing);
  }

  @Test
  void preservesProcessingFailure() {
    ShipmentEventEnvelope event = event();
    IllegalStateException failure = new IllegalStateException("database failed");
    when(objectMapper.readValue("json", ShipmentEventEnvelope.class)).thenReturn(event);
    when(processing.process(event)).thenThrow(failure);

    assertThatThrownBy(() -> new ShipmentEventConsumer(objectMapper, processing).consume("json"))
        .isSameAs(failure);
  }

  private static ShipmentEventEnvelope event() {
    UUID shipmentId = UUID.fromString("07f9e6be-fb52-426f-9b30-3d52cf25b6cf");
    return new ShipmentEventEnvelope(
        UUID.fromString("153fa8bd-330f-426f-a767-b3602753b1a3"),
        "ShipmentConfirmed",
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
