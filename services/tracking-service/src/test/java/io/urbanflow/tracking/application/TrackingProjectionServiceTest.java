package io.urbanflow.tracking.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.urbanflow.tracking.cache.TrackingTimelineCache;
import io.urbanflow.tracking.domain.TrackingStatus;
import io.urbanflow.tracking.event.ShipmentEventEnvelope;
import io.urbanflow.tracking.event.ShipmentEventPayload;
import io.urbanflow.tracking.event.UnsupportedEventSchemaException;
import io.urbanflow.tracking.persistence.TrackingEventProjection;
import io.urbanflow.tracking.persistence.TrackingEventRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrackingProjectionServiceTest {

  private static final Instant NOW = Instant.parse("2026-08-24T12:30:00Z");
  private static final UUID SHIPMENT_ID = UUID.fromString("6ef1220c-c67a-4ab3-98b1-7f73a521df22");
  private static final UUID EVENT_ID = UUID.fromString("fa7f01ee-293f-47ab-898f-33fe522e9de9");
  private static final String TRACKING_NUMBER = "UF1123456789ABCDEF01";

  @Mock private TrackingEventRepository repository;
  @Mock private TrackingTimelineCache cache;
  @Mock private TrackingEventProjection latest;

  private SimpleMeterRegistry meterRegistry;
  private TrackingProjectionService service;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    service =
        new TrackingProjectionService(
            repository, cache, Clock.fixed(NOW, ZoneOffset.UTC), meterRegistry);
  }

  @Test
  void persistsValidEventAndInvalidatesTimelineCache() {
    ShipmentEventEnvelope event = event("ShipmentCreated", "1.0", 0, TrackingStatus.CREATED);
    when(repository.findTopByShipmentIdOrderByAggregateVersionDescOccurredAtDesc(SHIPMENT_ID))
        .thenReturn(Optional.empty());

    ProjectionResult result = service.project(event);

    ArgumentCaptor<TrackingEventProjection> saved =
        ArgumentCaptor.forClass(TrackingEventProjection.class);
    verify(repository).save(saved.capture());
    assertThat(saved.getValue().id()).isEqualTo(EVENT_ID);
    assertThat(saved.getValue().trackingNumber()).isEqualTo(TRACKING_NUMBER);
    assertThat(saved.getValue().receivedAt()).isEqualTo(NOW);
    verify(cache).evict(TRACKING_NUMBER);
    assertThat(result).isEqualTo(ProjectionResult.PROJECTED);
    assertThat(counter(ProjectionResult.PROJECTED)).isEqualTo(1.0);
  }

  @Test
  void acknowledgesDuplicateEventIdWithoutMutation() {
    ShipmentEventEnvelope event = event("ShipmentCreated", "1.0", 0, TrackingStatus.CREATED);
    when(repository.existsById(EVENT_ID)).thenReturn(true);

    assertThat(service.project(event)).isEqualTo(ProjectionResult.DUPLICATE);

    verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    verify(cache, never()).evict(TRACKING_NUMBER);
  }

  @Test
  void ignoresStaleOrRepeatedAggregateVersion() {
    ShipmentEventEnvelope event = event("ShipmentConfirmed", "1.0", 2, TrackingStatus.CONFIRMED);
    when(repository.findTopByShipmentIdOrderByAggregateVersionDescOccurredAtDesc(SHIPMENT_ID))
        .thenReturn(Optional.of(latest));
    when(latest.aggregateVersion()).thenReturn(3L);

    assertThat(service.project(event)).isEqualTo(ProjectionResult.STALE);

    verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void ignoresNonTimelineShipmentEvent() {
    ShipmentEventEnvelope event =
        event("ShipmentDeliveryAddressChanged", "1.0", 2, TrackingStatus.CONFIRMED);

    assertThat(service.project(event)).isEqualTo(ProjectionResult.IGNORED);

    verify(repository, never()).existsById(EVENT_ID);
    verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void rejectsUnsupportedMajorSchema() {
    assertThatThrownBy(
            () -> service.project(event("ShipmentCreated", "2.0", 0, TrackingStatus.CREATED)))
        .isInstanceOf(UnsupportedEventSchemaException.class)
        .hasMessageContaining("2.0");
  }

  @Test
  void rejectsAggregateIdentifierMismatch() {
    ShipmentEventEnvelope valid = event("ShipmentCreated", "1.0", 0, TrackingStatus.CREATED);
    ShipmentEventEnvelope mismatched =
        new ShipmentEventEnvelope(
            valid.eventId(),
            valid.eventType(),
            UUID.randomUUID(),
            valid.aggregateVersion(),
            valid.occurredAt(),
            valid.schemaVersion(),
            valid.correlationId(),
            valid.traceId(),
            valid.producer(),
            valid.payload());

    assertThatThrownBy(() -> service.project(mismatched))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("aggregateId");
  }

  @Test
  void rejectsAggregateVersionMismatch() {
    ShipmentEventEnvelope valid = event("ShipmentConfirmed", "1.0", 1, TrackingStatus.CONFIRMED);
    ShipmentEventEnvelope mismatched =
        new ShipmentEventEnvelope(
            valid.eventId(),
            valid.eventType(),
            valid.aggregateId(),
            2,
            valid.occurredAt(),
            valid.schemaVersion(),
            valid.correlationId(),
            valid.traceId(),
            valid.producer(),
            valid.payload());

    assertThatThrownBy(() -> service.project(mismatched))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("aggregateVersion");
  }

  @Test
  void rejectsUnexpectedProducer() {
    ShipmentEventEnvelope valid = event("ShipmentCreated", "1.0", 0, TrackingStatus.CREATED);
    ShipmentEventEnvelope mismatched =
        new ShipmentEventEnvelope(
            valid.eventId(),
            valid.eventType(),
            valid.aggregateId(),
            valid.aggregateVersion(),
            valid.occurredAt(),
            valid.schemaVersion(),
            valid.correlationId(),
            valid.traceId(),
            "unknown-service",
            valid.payload());

    assertThatThrownBy(() -> service.project(mismatched))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("producer");
  }

  private double counter(ProjectionResult result) {
    return meterRegistry
        .get("urbanflow.tracking.projections")
        .tag("result", result.name().toLowerCase())
        .counter()
        .count();
  }

  private static ShipmentEventEnvelope event(
      String eventType, String schemaVersion, long version, TrackingStatus status) {
    return new ShipmentEventEnvelope(
        EVENT_ID,
        eventType,
        SHIPMENT_ID,
        version,
        NOW.minusSeconds(5),
        schemaVersion,
        "corr-tracking-002",
        "abcdef0123456789abcdef0123456789",
        "shipment-service",
        new ShipmentEventPayload(SHIPMENT_ID, TRACKING_NUMBER, status, version));
  }
}
