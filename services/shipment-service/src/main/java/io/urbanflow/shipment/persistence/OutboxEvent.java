package io.urbanflow.shipment.persistence;

import io.urbanflow.shipment.domain.Shipment;
import io.urbanflow.shipment.domain.ShipmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Durable event staged in the same transaction as its shipment state change. */
@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

  @Id private UUID id;

  @Column(name = "aggregate_id", nullable = false)
  private UUID aggregateId;

  @Column(name = "aggregate_version", nullable = false)
  private long aggregateVersion;

  @Column(name = "event_type", nullable = false, length = 80)
  private String eventType;

  @Column(name = "schema_version", nullable = false, length = 16)
  private String schemaVersion;

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

  /** Constructor reserved for persistence materialization. */
  protected OutboxEvent() {}

  /** Creates a versioned shipment event envelope in unpublished state. */
  public static OutboxEvent shipmentEvent(
      Shipment shipment,
      String eventType,
      String correlationId,
      String traceId,
      Instant occurredAt) {
    OutboxEvent event = new OutboxEvent();
    event.id = UUID.randomUUID();
    event.aggregateId = shipment.id();
    event.aggregateVersion = shipment.version();
    event.eventType = eventType;
    event.schemaVersion = "1.0";
    event.payload = buildPayload(shipment.id(), shipment.status(), shipment.version());
    event.correlationId = correlationId;
    event.traceId = traceId;
    event.occurredAt = occurredAt;
    return event;
  }

  /** Returns the globally unique event identifier. */
  public UUID id() {
    return id;
  }

  /** Returns the owning shipment aggregate identifier. */
  public UUID aggregateId() {
    return aggregateId;
  }

  /** Returns the shipment version represented by the event. */
  public long aggregateVersion() {
    return aggregateVersion;
  }

  /** Returns the domain event type. */
  public String eventType() {
    return eventType;
  }

  /** Returns the payload schema version. */
  public String schemaVersion() {
    return schemaVersion;
  }

  /** Returns the JSON payload stored for publication. */
  public String payload() {
    return payload;
  }

  /** Returns the business correlation identifier. */
  public String correlationId() {
    return correlationId;
  }

  /** Returns the distributed trace identifier. */
  public String traceId() {
    return traceId;
  }

  /** Returns when the domain event occurred. */
  public Instant occurredAt() {
    return occurredAt;
  }

  /** Returns when the event was published, or null while pending. */
  public Instant publishedAt() {
    return publishedAt;
  }

  /** Marks this event as published without changing its original envelope. */
  public void markPublished(Instant publicationTime) {
    publishedAt = publicationTime;
  }

  private static String buildPayload(UUID shipmentId, ShipmentStatus status, long version) {
    return "{\"shipmentId\":\"%s\",\"status\":\"%s\",\"version\":%d}"
        .formatted(shipmentId, status, version);
  }
}
