package io.urbanflow.routing.domain;

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

/** Versioned route assignment linking exactly one shipment to one courier. */
@Entity
@Table(name = "delivery_routes")
public class DeliveryRoute {

  @Id private UUID id;

  @Column(name = "shipment_id", nullable = false, unique = true)
  private UUID shipmentId;

  @Column(name = "courier_id", nullable = false)
  private UUID courierId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private RouteStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Version
  @Column(nullable = false)
  private long version;

  /** Constructor reserved for persistence. */
  protected DeliveryRoute() {}

  /** Plans one unique shipment assignment. */
  public static DeliveryRoute plan(UUID id, UUID shipmentId, UUID courierId, Instant now) {
    DeliveryRoute route = new DeliveryRoute();
    route.id = Objects.requireNonNull(id, "id must not be null");
    route.shipmentId = Objects.requireNonNull(shipmentId, "shipmentId must not be null");
    route.courierId = Objects.requireNonNull(courierId, "courierId must not be null");
    route.status = RouteStatus.PLANNED;
    route.createdAt = Objects.requireNonNull(now, "now must not be null");
    return route;
  }

  /** Starts a planned route at the supplied optimistic version. */
  public void start(long expectedVersion, Instant now) {
    requireVersion(expectedVersion);
    if (status != RouteStatus.PLANNED) {
      throw new IllegalStateException("only a planned route can be started");
    }
    status = RouteStatus.STARTED;
    startedAt = Objects.requireNonNull(now, "now must not be null");
  }

  /** Completes a started route at the supplied optimistic version. */
  public void complete(long expectedVersion, Instant now) {
    requireVersion(expectedVersion);
    if (status != RouteStatus.STARTED) {
      throw new IllegalStateException("only a started route can be completed");
    }
    status = RouteStatus.COMPLETED;
    completedAt = Objects.requireNonNull(now, "now must not be null");
  }

  /** Returns the route identifier. */
  public UUID id() {
    return id;
  }

  /** Returns the unique assigned shipment. */
  public UUID shipmentId() {
    return shipmentId;
  }

  /** Returns the assigned courier. */
  public UUID courierId() {
    return courierId;
  }

  /** Returns the route lifecycle status. */
  public RouteStatus status() {
    return status;
  }

  /** Returns the route creation time. */
  public Instant createdAt() {
    return createdAt;
  }

  /** Returns the start time or null before start. */
  public Instant startedAt() {
    return startedAt;
  }

  /** Returns the completion time or null before completion. */
  public Instant completedAt() {
    return completedAt;
  }

  /** Returns the optimistic persistence version. */
  public long version() {
    return version;
  }

  private void requireVersion(long expectedVersion) {
    if (version != expectedVersion) {
      throw new RouteVersionConflictException(id, expectedVersion, version);
    }
  }
}
