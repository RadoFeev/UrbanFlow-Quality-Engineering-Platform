package io.urbanflow.tracking.persistence;

import io.urbanflow.tracking.domain.TrackingStatus;
import io.urbanflow.tracking.event.ShipmentEventEnvelope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Immutable persisted timeline item produced from one unique shipment event. */
@Entity
@Table(name = "tracking_events")
public class TrackingEventProjection {

  @Id private UUID id;

  @Column(name = "shipment_id", nullable = false)
  private UUID shipmentId;

  @Column(name = "tracking_number", nullable = false, length = 24)
  private String trackingNumber;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private TrackingStatus status;

  @Column(name = "aggregate_version", nullable = false)
  private long aggregateVersion;

  @Column(name = "event_type", nullable = false, length = 80)
  private String eventType;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @Column(name = "received_at", nullable = false)
  private Instant receivedAt;

  @Column(name = "correlation_id", nullable = false, length = 64)
  private String correlationId;

  @Column(name = "trace_id", nullable = false, length = 32)
  private String traceId;

  /** Constructor reserved for persistence materialization. */
  protected TrackingEventProjection() {}

  /** Creates an immutable timeline item from a validated event. */
  public static TrackingEventProjection from(ShipmentEventEnvelope event, Instant receivedAt) {
    TrackingEventProjection projection = new TrackingEventProjection();
    projection.id = event.eventId();
    projection.shipmentId = event.payload().shipmentId();
    projection.trackingNumber = event.payload().trackingNumber();
    projection.status = event.payload().status();
    projection.aggregateVersion = event.aggregateVersion();
    projection.eventType = event.eventType();
    projection.occurredAt = event.occurredAt();
    projection.receivedAt = receivedAt;
    projection.correlationId = event.correlationId();
    projection.traceId = event.traceId();
    return projection;
  }

  /** Returns the source event identifier. */
  public UUID id() {
    return id;
  }

  /** Returns the shipment identifier. */
  public UUID shipmentId() {
    return shipmentId;
  }

  /** Returns the public tracking number. */
  public String trackingNumber() {
    return trackingNumber;
  }

  /** Returns the projected shipment status. */
  public TrackingStatus status() {
    return status;
  }

  /** Returns the source aggregate version used for ordering. */
  public long aggregateVersion() {
    return aggregateVersion;
  }

  /** Returns the source event type. */
  public String eventType() {
    return eventType;
  }

  /** Returns when the business event occurred. */
  public Instant occurredAt() {
    return occurredAt;
  }

  /** Returns when the tracking consumer persisted the event. */
  public Instant receivedAt() {
    return receivedAt;
  }

  /** Returns the business correlation identifier. */
  public String correlationId() {
    return correlationId;
  }

  /** Returns the trace identifier. */
  public String traceId() {
    return traceId;
  }
}
