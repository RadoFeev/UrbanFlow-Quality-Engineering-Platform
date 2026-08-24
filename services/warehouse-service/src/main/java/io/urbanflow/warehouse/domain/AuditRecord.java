package io.urbanflow.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Append-only operational audit record. */
@Entity
@Table(name = "audit_records")
public class AuditRecord {

  @Id private UUID id;

  @Column(name = "actor_id", nullable = false)
  private UUID actorId;

  @Column(nullable = false, length = 80)
  private String action;

  @Column(name = "entity_type", nullable = false, length = 80)
  private String entityType;

  @Column(name = "entity_id", nullable = false)
  private UUID entityId;

  @Column(nullable = false, length = 500)
  private String details;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  /** Constructor reserved for persistence. */
  protected AuditRecord() {}

  /** Creates a sanitized append-only audit record. */
  public static AuditRecord create(
      UUID id,
      UUID actorId,
      String action,
      String entityType,
      UUID entityId,
      String details,
      Instant now) {
    AuditRecord record = new AuditRecord();
    record.id = Objects.requireNonNull(id, "id must not be null");
    record.actorId = Objects.requireNonNull(actorId, "actorId must not be null");
    record.action = requireText(action, "action", 80);
    record.entityType = requireText(entityType, "entityType", 80);
    record.entityId = Objects.requireNonNull(entityId, "entityId must not be null");
    record.details = requireText(details, "details", 500).replaceAll("[\\r\\n\\t]", " ");
    record.occurredAt = Objects.requireNonNull(now, "now must not be null");
    return record;
  }

  /** Returns the record identifier. */
  public UUID id() {
    return id;
  }

  /** Returns the actor identifier. */
  public UUID actorId() {
    return actorId;
  }

  /** Returns the audited action. */
  public String action() {
    return action;
  }

  /** Returns the entity type. */
  public String entityType() {
    return entityType;
  }

  /** Returns the entity identifier. */
  public UUID entityId() {
    return entityId;
  }

  /** Returns sanitized details. */
  public String details() {
    return details;
  }

  /** Returns occurrence time. */
  public Instant occurredAt() {
    return occurredAt;
  }

  private static String requireText(String value, String field, int maximum) {
    if (value == null || value.isBlank() || value.trim().length() > maximum) {
      throw new IllegalArgumentException(field + " must contain 1 to " + maximum + " characters");
    }
    return value.trim();
  }
}
