package io.urbanflow.routing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.urbanflow.routing.domain.Courier;
import io.urbanflow.routing.domain.CourierCapacityException;
import io.urbanflow.routing.domain.DeliveryRoute;
import io.urbanflow.routing.event.ShipmentEventEnvelope;
import io.urbanflow.routing.event.ShipmentEventPayload;
import io.urbanflow.routing.persistence.CourierRepository;
import io.urbanflow.routing.persistence.DeliveryRouteRepository;
import io.urbanflow.routing.persistence.ProcessedShipmentEvent;
import io.urbanflow.routing.persistence.ProcessedShipmentEventRepository;
import io.urbanflow.routing.persistence.RoutingOutboxEvent;
import io.urbanflow.routing.persistence.RoutingOutboxRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoutingAssignmentServiceTest {

  private static final Instant NOW = Instant.parse("2026-08-24T12:30:00Z");

  @Mock private CourierRepository couriers;
  @Mock private DeliveryRouteRepository routes;
  @Mock private ProcessedShipmentEventRepository processedEvents;
  @Mock private RoutingOutboxRepository outbox;

  private SimpleMeterRegistry meters;
  private RoutingAssignmentService service;

  @BeforeEach
  void setUp() {
    meters = new SimpleMeterRegistry();
    service =
        new RoutingAssignmentService(
            couriers, routes, processedEvents, outbox, Clock.fixed(NOW, ZoneOffset.UTC), meters);
  }

  @Test
  void atomicallyAssignsLeastLoadedCandidateAndStagesEvent() {
    ShipmentEventEnvelope event = confirmed();
    Courier courier = courier();
    when(routes.findByShipmentId(event.aggregateId())).thenReturn(Optional.empty());
    when(couriers.findAssignable(any())).thenReturn(List.of(courier));

    assertThat(service.assign(event)).isEqualTo(AssignmentResult.ASSIGNED);

    assertThat(courier.activeAssignments()).isEqualTo(1);
    ArgumentCaptor<DeliveryRoute> route = ArgumentCaptor.forClass(DeliveryRoute.class);
    verify(routes).save(route.capture());
    assertThat(route.getValue().shipmentId()).isEqualTo(event.aggregateId());
    assertThat(route.getValue().courierId()).isEqualTo(courier.id());
    verify(processedEvents).save(any(ProcessedShipmentEvent.class));
    verify(outbox).save(any(RoutingOutboxEvent.class));
    assertThat(metric("assigned")).isEqualTo(1.0);
  }

  @Test
  void acknowledgesDuplicateEventWithoutMutation() {
    ShipmentEventEnvelope event = confirmed();
    when(processedEvents.existsById(event.eventId())).thenReturn(true);

    assertThat(service.assign(event)).isEqualTo(AssignmentResult.DUPLICATE);

    verifyNoInteractions(couriers, routes, outbox);
  }

  @Test
  void recordsDifferentEventIdForAlreadyAssignedShipment() {
    ShipmentEventEnvelope event = confirmed();
    when(routes.findByShipmentId(event.aggregateId()))
        .thenReturn(
            Optional.of(
                DeliveryRoute.plan(
                    UUID.randomUUID(), event.aggregateId(), UUID.randomUUID(), NOW)));

    assertThat(service.assign(event)).isEqualTo(AssignmentResult.DUPLICATE);

    verify(processedEvents).save(any(ProcessedShipmentEvent.class));
    verify(outbox, never()).save(any());
  }

  @Test
  void ignoresUnrelatedShipmentEvents() {
    ShipmentEventEnvelope event = event("ShipmentCreated", "CREATED");

    assertThat(service.assign(event)).isEqualTo(AssignmentResult.IGNORED);

    verifyNoInteractions(couriers, routes, processedEvents, outbox);
  }

  @Test
  void leavesEventUnprocessedWhenNoCapacityExists() {
    ShipmentEventEnvelope event = confirmed();
    when(routes.findByShipmentId(event.aggregateId())).thenReturn(Optional.empty());
    when(couriers.findAssignable(any())).thenReturn(List.of());

    assertThatThrownBy(() -> service.assign(event)).isInstanceOf(CourierCapacityException.class);

    verify(processedEvents, never()).save(any());
    verify(outbox, never()).save(any());
  }

  private double metric(String result) {
    return meters.get("urbanflow.routing.assignments").tag("result", result).counter().count();
  }

  private static ShipmentEventEnvelope confirmed() {
    return event("ShipmentConfirmed", "CONFIRMED");
  }

  private static ShipmentEventEnvelope event(String eventType, String status) {
    UUID shipmentId = UUID.fromString("7d38bce9-4f03-4269-b1db-a19fc9358679");
    return new ShipmentEventEnvelope(
        UUID.fromString("5df93d33-9eed-4a1f-95eb-362cdfdbd524"),
        eventType,
        shipmentId,
        1,
        NOW,
        "1.0",
        "corr-routing-001",
        "0123456789abcdef0123456789abcdef",
        "shipment-service",
        new ShipmentEventPayload(shipmentId, "UF0123456789ABCDEF01", status, 1));
  }

  private static Courier courier() {
    return Courier.register(
        UUID.fromString("6be406c1-022e-4d36-bb5c-2da15e311fb0"), "Courier One", 5, NOW);
  }
}
