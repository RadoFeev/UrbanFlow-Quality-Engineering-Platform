package io.urbanflow.shipment.persistence;

import io.urbanflow.shipment.domain.ShipmentActor;
import io.urbanflow.shipment.domain.ShipmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Append-only record of one shipment status observation or transition. */
@Entity
@Table(name = "shipment_status_history")
public class ShipmentStatusHistory {

  @Id private UUID id;

  @Column(name = "shipment_id", nullable = false)
  private UUID shipmentId;

  @Enumerated(EnumType.STRING)
  @Column(name = "previous_status", length = 32)
  private ShipmentStatus previousStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "new_status", nullable = false, length = 32)
  private ShipmentStatus newStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "actor", nullable = false, length = 32)
  private ShipmentActor actor;

  @Column(name = "reason", length = 500)
  private String reason;

  @Column(name = "delivery_proof_id")
  private UUID deliveryProofId;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  /** Constructor reserved for persistence materialization. */
  protected ShipmentStatusHistory() {}

  private ShipmentStatusHistory(
      UUID id,
      UUID shipmentId,
      ShipmentStatus previousStatus,
      ShipmentStatus newStatus,
      ShipmentActor actor,
      String reason,
      UUID deliveryProofId,
      Instant occurredAt) {
    this.id = id;
    this.shipmentId = shipmentId;
    this.previousStatus = previousStatus;
    this.newStatus = newStatus;
    this.actor = actor;
    this.reason = reason;
    this.deliveryProofId = deliveryProofId;
    this.occurredAt = occurredAt;
  }

  /** Creates the first history record for a new shipment. */
  public static ShipmentStatusHistory created(UUID shipmentId, Instant occurredAt) {
    return new ShipmentStatusHistory(
        UUID.randomUUID(),
        shipmentId,
        null,
        ShipmentStatus.CREATED,
        ShipmentActor.SYSTEM,
        null,
        null,
        occurredAt);
  }

  /** Creates a history record for an accepted transition. */
  public static ShipmentStatusHistory transition(
      UUID shipmentId,
      ShipmentStatus previousStatus,
      ShipmentStatus newStatus,
      ShipmentActor actor,
      String reason,
      UUID deliveryProofId,
      Instant occurredAt) {
    return new ShipmentStatusHistory(
        UUID.randomUUID(),
        shipmentId,
        previousStatus,
        newStatus,
        actor,
        reason,
        deliveryProofId,
        occurredAt);
  }

  /** Returns the history-record identifier. */
  public UUID id() {
    return id;
  }

  /** Returns the shipment whose status changed. */
  public UUID shipmentId() {
    return shipmentId;
  }

  /** Returns the previous status, or null for the creation record. */
  public ShipmentStatus previousStatus() {
    return previousStatus;
  }

  /** Returns the status recorded by this history entry. */
  public ShipmentStatus newStatus() {
    return newStatus;
  }

  /** Returns the actor responsible for the status observation. */
  public ShipmentActor actor() {
    return actor;
  }

  /** Returns the optional transition reason. */
  public String reason() {
    return reason;
  }

  /** Returns the optional proof-of-delivery identifier. */
  public UUID deliveryProofId() {
    return deliveryProofId;
  }

  /** Returns when the status change occurred. */
  public Instant occurredAt() {
    return occurredAt;
  }
}
