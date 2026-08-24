package io.urbanflow.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Sortation hub belonging to one warehouse. */
@Entity
@Table(name = "logistics_hubs")
public class LogisticsHub {

  @Id private UUID id;

  @Column(name = "warehouse_id", nullable = false)
  private UUID warehouseId;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(nullable = false, length = 120)
  private String city;

  @Column(nullable = false)
  private boolean active;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  @Column(nullable = false)
  private long version;

  /** Constructor reserved for persistence. */
  protected LogisticsHub() {}

  /** Creates an active logistics hub. */
  public static LogisticsHub create(
      UUID id, UUID warehouseId, String name, String city, Instant now) {
    LogisticsHub hub = new LogisticsHub();
    hub.id = Objects.requireNonNull(id, "id must not be null");
    hub.warehouseId = Objects.requireNonNull(warehouseId, "warehouseId must not be null");
    hub.name = requireText(name, "name");
    hub.city = requireText(city, "city");
    hub.active = true;
    hub.updatedAt = Objects.requireNonNull(now, "now must not be null");
    return hub;
  }

  /** Changes operational availability. */
  public void setActive(boolean active, Instant now) {
    this.active = active;
    this.updatedAt = Objects.requireNonNull(now, "now must not be null");
  }

  /** Returns the identifier. */
  public UUID id() {
    return id;
  }

  /** Returns the owning warehouse identifier. */
  public UUID warehouseId() {
    return warehouseId;
  }

  /** Returns the hub name. */
  public String name() {
    return name;
  }

  /** Returns the hub city. */
  public String city() {
    return city;
  }

  /** Returns whether the hub is active. */
  public boolean active() {
    return active;
  }

  /** Returns last update time. */
  public Instant updatedAt() {
    return updatedAt;
  }

  /** Returns optimistic version. */
  public long version() {
    return version;
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank() || value.trim().length() > 120) {
      throw new IllegalArgumentException(field + " must contain 1 to 120 characters");
    }
    return value.trim();
  }
}
