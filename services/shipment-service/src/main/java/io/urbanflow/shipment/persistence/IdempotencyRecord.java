package io.urbanflow.shipment.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Persists the outcome identity and request hash for an idempotent operation. */
@Entity
@Table(name = "idempotency_records")
public class IdempotencyRecord {

  @Id
  @Column(name = "idempotency_key", length = 200)
  private String key;

  @Column(name = "request_fingerprint", nullable = false, length = 64)
  private String requestFingerprint;

  @Column(name = "shipment_id", nullable = false)
  private UUID shipmentId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  /** Constructor reserved for persistence materialization. */
  protected IdempotencyRecord() {}

  /** Creates an idempotency record for a successfully created shipment. */
  public IdempotencyRecord(
      String key, String requestFingerprint, UUID shipmentId, Instant createdAt) {
    this.key = key;
    this.requestFingerprint = requestFingerprint;
    this.shipmentId = shipmentId;
    this.createdAt = createdAt;
  }

  /** Returns the client-provided idempotency key. */
  public String key() {
    return key;
  }

  /** Returns the canonical request fingerprint. */
  public String requestFingerprint() {
    return requestFingerprint;
  }

  /** Returns the shipment produced by the original request. */
  public UUID shipmentId() {
    return shipmentId;
  }

  /** Returns when the original request completed. */
  public Instant createdAt() {
    return createdAt;
  }
}
