package io.urbanflow.tracking.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.urbanflow.tracking.application.ProjectionResult;
import io.urbanflow.tracking.application.TrackingProjectionService;
import io.urbanflow.tracking.domain.TrackingStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ShipmentEventConsumerTest {

  private static final String VALUE = "{\"eventId\":\"event-value\"}";

  @Mock private ObjectMapper objectMapper;
  @Mock private TrackingProjectionService projectionService;

  private ShipmentEventConsumer consumer;

  @BeforeEach
  void setUp() {
    consumer = new ShipmentEventConsumer(objectMapper, projectionService);
  }

  @Test
  void deserializesAndProjectsKafkaValue() {
    ShipmentEventEnvelope event = event("1.0");
    when(objectMapper.readValue(VALUE, ShipmentEventEnvelope.class)).thenReturn(event);
    when(projectionService.project(event)).thenReturn(ProjectionResult.PROJECTED);

    assertThat(consumer.consume(VALUE)).isEqualTo(ProjectionResult.PROJECTED);

    verify(projectionService).project(event);
  }

  @Test
  void reportsMalformedValueWithoutInvokingProjection() {
    when(objectMapper.readValue(VALUE, ShipmentEventEnvelope.class))
        .thenThrow(new IllegalArgumentException("invalid JSON"));

    assertThatThrownBy(() -> consumer.consume(VALUE))
        .isInstanceOf(EventDeserializationException.class)
        .hasCauseInstanceOf(IllegalArgumentException.class);

    verifyNoInteractions(projectionService);
  }

  @Test
  void preservesProjectionFailuresForDeadLetterClassification() {
    ShipmentEventEnvelope event = event("2.0");
    UnsupportedEventSchemaException failure = new UnsupportedEventSchemaException("2.0");
    when(objectMapper.readValue(VALUE, ShipmentEventEnvelope.class)).thenReturn(event);
    when(projectionService.project(event)).thenThrow(failure);

    assertThatThrownBy(() -> consumer.consume(VALUE)).isSameAs(failure);
  }

  private static ShipmentEventEnvelope event(String schemaVersion) {
    UUID shipmentId = UUID.fromString("b2b506c5-bf80-46e6-a899-c616bcfb0c73");
    return new ShipmentEventEnvelope(
        UUID.fromString("7b5b6c28-5e04-457b-81fc-bf10284be133"),
        "ShipmentCreated",
        shipmentId,
        0,
        Instant.parse("2026-08-24T13:00:00Z"),
        schemaVersion,
        "corr-consumer-001",
        "0123456789abcdef0123456789abcdef",
        "shipment-service",
        new ShipmentEventPayload(shipmentId, "UF1123456789ABCDEF01", TrackingStatus.CREATED, 0));
  }
}
