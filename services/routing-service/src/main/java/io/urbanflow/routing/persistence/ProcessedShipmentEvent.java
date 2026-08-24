package io.urbanflow.routing.persistence;

import io.urbanflow.routing.event.ShipmentEventEnvelope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Durable idempotency ledger entry for a consumed shipment event. */
@Entity
@Table(name = "processed_shipment_events")
public class ProcessedShipmentEvent {

  @Id private UUID id;

  @Column(name = "shipment_id", nullable = false)
  private UUID shipmentId;

  @Column(name = "processed_at", nullable = false)
  private Instant processedAt;

  /** Constructor reserved for persistence. */
  protected ProcessedShipmentEvent() {}

  /** Creates a ledger entry from a successfully handled event. */
  public static ProcessedShipmentEvent from(ShipmentEventEnvelope event, Instant processedAt) {
    ProcessedShipmentEvent processed = new ProcessedShipmentEvent();
    processed.id = event.eventId();
    processed.shipmentId = event.aggregateId();
    processed.processedAt = processedAt;
    return processed;
  }

  /** Returns the source event identifier. */
  public UUID id() {
    return id;
  }
}
