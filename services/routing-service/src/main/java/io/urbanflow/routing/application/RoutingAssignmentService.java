package io.urbanflow.routing.application;

import io.micrometer.core.instrument.MeterRegistry;
import io.urbanflow.routing.domain.Courier;
import io.urbanflow.routing.domain.CourierCapacityException;
import io.urbanflow.routing.domain.DeliveryRoute;
import io.urbanflow.routing.event.ShipmentEventEnvelope;
import io.urbanflow.routing.persistence.CourierRepository;
import io.urbanflow.routing.persistence.DeliveryRouteRepository;
import io.urbanflow.routing.persistence.ProcessedShipmentEvent;
import io.urbanflow.routing.persistence.ProcessedShipmentEventRepository;
import io.urbanflow.routing.persistence.RoutingOutboxEvent;
import io.urbanflow.routing.persistence.RoutingOutboxRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Atomically assigns confirmed shipments and stages the corresponding domain event. */
@Service
public class RoutingAssignmentService {

  private final CourierRepository couriers;
  private final DeliveryRouteRepository routes;
  private final ProcessedShipmentEventRepository processedEvents;
  private final RoutingOutboxRepository outbox;
  private final Clock clock;
  private final MeterRegistry meters;

  /** Creates the assignment workflow from transactional persistence ports. */
  public RoutingAssignmentService(
      CourierRepository couriers,
      DeliveryRouteRepository routes,
      ProcessedShipmentEventRepository processedEvents,
      RoutingOutboxRepository outbox,
      Clock clock,
      MeterRegistry meters) {
    this.couriers = couriers;
    this.routes = routes;
    this.processedEvents = processedEvents;
    this.outbox = outbox;
    this.clock = clock;
    this.meters = meters;
  }

  /** Handles one event with durable duplicate protection and deterministic capacity selection. */
  @Transactional
  public AssignmentResult assign(ShipmentEventEnvelope event) {
    if (!event.isAssignmentTrigger()) {
      return result(AssignmentResult.IGNORED);
    }
    if (processedEvents.existsById(event.eventId())) {
      return result(AssignmentResult.DUPLICATE);
    }
    Instant now = clock.instant();
    if (routes.findByShipmentId(event.aggregateId()).isPresent()) {
      processedEvents.save(ProcessedShipmentEvent.from(event, now));
      return result(AssignmentResult.DUPLICATE);
    }
    Courier courier =
        couriers.findAssignable(PageRequest.of(0, 1)).stream()
            .findFirst()
            .orElseThrow(CourierCapacityException::new);
    courier.assign(now);
    DeliveryRoute route =
        DeliveryRoute.plan(UUID.randomUUID(), event.aggregateId(), courier.id(), now);
    couriers.save(courier);
    routes.save(route);
    processedEvents.save(ProcessedShipmentEvent.from(event, now));
    outbox.save(
        RoutingOutboxEvent.courierAssigned(route, event.correlationId(), event.traceId(), now));
    return result(AssignmentResult.ASSIGNED);
  }

  private AssignmentResult result(AssignmentResult result) {
    meters
        .counter("urbanflow.routing.assignments", "result", result.name().toLowerCase())
        .increment();
    return result;
  }
}
