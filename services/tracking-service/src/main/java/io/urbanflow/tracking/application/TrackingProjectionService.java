package io.urbanflow.tracking.application;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.urbanflow.tracking.cache.TrackingTimelineCache;
import io.urbanflow.tracking.event.ShipmentEventEnvelope;
import io.urbanflow.tracking.event.UnsupportedEventSchemaException;
import io.urbanflow.tracking.persistence.TrackingEventProjection;
import io.urbanflow.tracking.persistence.TrackingEventRepository;
import java.time.Clock;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Projects ordered shipment events idempotently into the tracking read model. */
@Service
public class TrackingProjectionService {

  private static final Set<String> TRACKED_EVENT_TYPES =
      Set.of(
          "ShipmentCreated",
          "ShipmentConfirmed",
          "CourierAssigned",
          "ShipmentPickedUp",
          "ShipmentArrivedAtHub",
          "ShipmentDispatched",
          "DeliveryAttempted",
          "ShipmentDelivered",
          "ShipmentCancelled",
          "ShipmentReturnStarted",
          "ShipmentStatusChanged");

  private final TrackingEventRepository repository;
  private final TrackingTimelineCache cache;
  private final Clock clock;
  private final Map<ProjectionResult, Counter> counters;

  TrackingProjectionService(
      TrackingEventRepository repository,
      TrackingTimelineCache cache,
      Clock clock,
      MeterRegistry meterRegistry) {
    this.repository = repository;
    this.cache = cache;
    this.clock = clock;
    counters = new EnumMap<>(ProjectionResult.class);
    for (ProjectionResult result : ProjectionResult.values()) {
      counters.put(
          result,
          Counter.builder("urbanflow.tracking.projections")
              .tag("result", result.name().toLowerCase())
              .register(meterRegistry));
    }
  }

  /** Applies one event or returns a non-mutating duplicate/stale/ignored outcome. */
  @Transactional
  public ProjectionResult project(ShipmentEventEnvelope event) {
    validateEnvelope(event);
    if (!TRACKED_EVENT_TYPES.contains(event.eventType())) {
      return measured(ProjectionResult.IGNORED);
    }
    if (repository.existsById(event.eventId())) {
      return measured(ProjectionResult.DUPLICATE);
    }
    boolean stale =
        repository
            .findTopByShipmentIdOrderByAggregateVersionDescOccurredAtDesc(event.aggregateId())
            .map(latest -> event.aggregateVersion() <= latest.aggregateVersion())
            .orElse(false);
    if (stale) {
      return measured(ProjectionResult.STALE);
    }
    repository.save(TrackingEventProjection.from(event, clock.instant()));
    cache.evict(event.payload().trackingNumber());
    return measured(ProjectionResult.PROJECTED);
  }

  private void validateEnvelope(ShipmentEventEnvelope event) {
    if (!event.hasSupportedSchema()) {
      throw new UnsupportedEventSchemaException(event.schemaVersion());
    }
    if (!"shipment-service".equals(event.producer())) {
      throw new IllegalArgumentException("Unexpected shipment event producer");
    }
    if (!event.aggregateId().equals(event.payload().shipmentId())) {
      throw new IllegalArgumentException("aggregateId does not match payload shipmentId");
    }
    if (event.aggregateVersion() != event.payload().version()) {
      throw new IllegalArgumentException("aggregateVersion does not match payload version");
    }
  }

  private ProjectionResult measured(ProjectionResult result) {
    counters.get(result).increment();
    return result;
  }
}
