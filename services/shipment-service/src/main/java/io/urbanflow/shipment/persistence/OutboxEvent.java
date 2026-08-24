package io.urbanflow.shipment.persistence;

import io.urbanflow.shipment.domain.Shipment;
import io.urbanflow.shipment.domain.ShipmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
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

  @Column(name = "publication_attempts", nullable = false)
  private int publicationAttempts;

  @Column(name = "next_attempt_at", nullable = false)
  private Instant nextAttemptAt;

  @Column(name = "claimed_at")
  private Instant claimedAt;

  @Column(name = "claimed_by", length = 100)
  private String claimedBy;

  @Column(name = "last_error", length = 1000)
  private String lastError;

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
    event.nextAttemptAt = occurredAt;
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

  /** Returns the number of completed Kafka publication attempts. */
  public int publicationAttempts() {
    return publicationAttempts;
  }

  /** Returns the earliest time at which this event may be attempted again. */
  public Instant nextAttemptAt() {
    return nextAttemptAt;
  }

  /** Returns when the current publication lease was acquired. */
  public Instant claimedAt() {
    return claimedAt;
  }

  /** Returns the worker that owns the current publication lease. */
  public String claimedBy() {
    return claimedBy;
  }

  /** Returns the sanitized failure summary from the last failed attempt. */
  public String lastError() {
    return lastError;
  }

  /** Acquires or replaces an expired publication lease. */
  public void claim(String workerId, Instant claimTime) {
    ensureUnpublished();
    claimedBy = requireText(workerId, "workerId", 100);
    claimedAt = Objects.requireNonNull(claimTime, "claimTime must not be null");
  }

  /** Marks this event as published when the caller still owns its lease. */
  public void markPublished(String workerId, Instant publicationTime) {
    requireClaimedBy(workerId);
    publishedAt = Objects.requireNonNull(publicationTime, "publicationTime must not be null");
    publicationAttempts++;
    claimedAt = null;
    claimedBy = null;
    lastError = null;
  }

  /** Releases a failed lease and schedules the next capped-backoff attempt. */
  public void markFailed(
      String workerId, Instant failureTime, Instant retryAt, String failureSummary) {
    requireClaimedBy(workerId);
    Instant failedAt = Objects.requireNonNull(failureTime, "failureTime must not be null");
    Instant next = Objects.requireNonNull(retryAt, "retryAt must not be null");
    if (next.isBefore(failedAt)) {
      throw new IllegalArgumentException("retryAt must not precede failureTime");
    }
    publicationAttempts++;
    nextAttemptAt = next;
    lastError = requireText(failureSummary, "failureSummary", 1000);
    claimedAt = null;
    claimedBy = null;
  }

  private static String buildPayload(UUID shipmentId, ShipmentStatus status, long version) {
    return "{\"shipmentId\":\"%s\",\"status\":\"%s\",\"version\":%d}"
        .formatted(shipmentId, status, version);
  }

  private void ensureUnpublished() {
    if (publishedAt != null) {
      throw new IllegalStateException("Published outbox event cannot be claimed");
    }
  }

  private void requireClaimedBy(String workerId) {
    ensureUnpublished();
    if (!Objects.equals(claimedBy, workerId) || claimedAt == null) {
      throw new IllegalStateException("Outbox lease is not owned by worker");
    }
  }

  private static String requireText(String value, String field, int maxLength) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    String normalized = value.trim();
    return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
  }
}
