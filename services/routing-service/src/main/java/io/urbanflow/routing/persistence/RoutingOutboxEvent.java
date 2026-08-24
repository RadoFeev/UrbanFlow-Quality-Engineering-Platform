package io.urbanflow.routing.persistence;

import io.urbanflow.routing.domain.DeliveryRoute;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Transactional `CourierAssigned` event awaiting Kafka publication. */
@Entity
@Table(name = "routing_outbox_events")
public class RoutingOutboxEvent {

  @Id private UUID id;

  @Column(name = "shipment_id", nullable = false)
  private UUID shipmentId;

  @Column(name = "event_type", nullable = false, length = 80)
  private String eventType;

  @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
  private String payload;

  @Column(name = "correlation_id", nullable = false, length = 64)
  private String correlationId;

  @Column(name = "trace_id", nullable = false, length = 32)
  private String traceId;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @Column(name = "published_at")
  private Instant publishedAt;

  @Column(name = "attempts", nullable = false)
  private int attempts;

  @Column(name = "next_attempt_at", nullable = false)
  private Instant nextAttemptAt;

  /** Constructor reserved for persistence. */
  protected RoutingOutboxEvent() {}

  /** Stages a courier assignment event without including customer data. */
  public static RoutingOutboxEvent courierAssigned(
      DeliveryRoute route, String correlationId, String traceId, Instant now) {
    RoutingOutboxEvent event = new RoutingOutboxEvent();
    event.id = UUID.randomUUID();
    event.shipmentId = route.shipmentId();
    event.eventType = "CourierAssigned";
    event.payload =
        "{\"shipmentId\":\"%s\",\"courierId\":\"%s\",\"routeId\":\"%s\"}"
            .formatted(route.shipmentId(), route.courierId(), route.id());
    event.correlationId = correlationId;
    event.traceId = traceId;
    event.occurredAt = now;
    event.nextAttemptAt = now;
    return event;
  }

  /** Marks a broker-acknowledged publication. */
  public void markPublished(Instant now) {
    if (publishedAt != null) {
      return;
    }
    attempts++;
    publishedAt = now;
  }

  /** Schedules capped exponential retry after a failed broker interaction. */
  public void markFailed(Instant now) {
    attempts++;
    long delaySeconds = Math.min(60, 1L << Math.min(attempts - 1, 6));
    nextAttemptAt = now.plusSeconds(delaySeconds);
  }

  /** Returns the event identifier. */
  public UUID id() {
    return id;
  }

  /** Returns the Kafka ordering key. */
  public UUID shipmentId() {
    return shipmentId;
  }

  /** Returns the event type. */
  public String eventType() {
    return eventType;
  }

  /** Returns the JSON payload. */
  public String payload() {
    return payload;
  }

  /** Returns the correlation identifier. */
  public String correlationId() {
    return correlationId;
  }

  /** Returns the trace identifier. */
  public String traceId() {
    return traceId;
  }

  /** Returns the occurrence timestamp. */
  public Instant occurredAt() {
    return occurredAt;
  }

  /** Returns the publication timestamp or null. */
  public Instant publishedAt() {
    return publishedAt;
  }

  /** Returns completed publication attempts. */
  public int attempts() {
    return attempts;
  }

  /** Returns the earliest retry time. */
  public Instant nextAttemptAt() {
    return nextAttemptAt;
  }
}
