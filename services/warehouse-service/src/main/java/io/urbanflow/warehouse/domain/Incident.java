package io.urbanflow.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Versioned shipment incident managed by operations. */
@Entity
@Table(name = "incidents")
public class Incident {

  @Id private UUID id;

  @Column(name = "shipment_id", nullable = false)
  private UUID shipmentId;

  @Column(nullable = false, length = 60)
  private String type;

  @Column(nullable = false, length = 1000)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private IncidentStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "resolved_at")
  private Instant resolvedAt;

  @Version
  @Column(nullable = false)
  private long version;

  /** Constructor reserved for persistence. */
  protected Incident() {}

  /** Opens an incident for a shipment. */
  public static Incident open(
      UUID id, UUID shipmentId, String type, String description, Instant now) {
    Incident incident = new Incident();
    incident.id = Objects.requireNonNull(id, "id must not be null");
    incident.shipmentId = Objects.requireNonNull(shipmentId, "shipmentId must not be null");
    incident.type = requireText(type, "type", 60);
    incident.description = requireText(description, "description", 1000);
    incident.status = IncidentStatus.OPEN;
    incident.createdAt = Objects.requireNonNull(now, "now must not be null");
    return incident;
  }

  /** Resolves the incident exactly once. */
  public void resolve(Instant now) {
    if (status == IncidentStatus.RESOLVED) {
      throw new IllegalStateException("incident is already resolved");
    }
    status = IncidentStatus.RESOLVED;
    resolvedAt = Objects.requireNonNull(now, "now must not be null");
  }

  /** Returns the incident identifier. */
  public UUID id() {
    return id;
  }

  /** Returns the shipment identifier. */
  public UUID shipmentId() {
    return shipmentId;
  }

  /** Returns the incident type. */
  public String type() {
    return type;
  }

  /** Returns the description. */
  public String description() {
    return description;
  }

  /** Returns lifecycle status. */
  public IncidentStatus status() {
    return status;
  }

  /** Returns creation time. */
  public Instant createdAt() {
    return createdAt;
  }

  /** Returns resolution time or null. */
  public Instant resolvedAt() {
    return resolvedAt;
  }

  /** Returns optimistic version. */
  public long version() {
    return version;
  }

  private static String requireText(String value, String field, int maximum) {
    if (value == null || value.isBlank() || value.trim().length() > maximum) {
      throw new IllegalArgumentException(field + " must contain 1 to " + maximum + " characters");
    }
    return value.trim();
  }
}
