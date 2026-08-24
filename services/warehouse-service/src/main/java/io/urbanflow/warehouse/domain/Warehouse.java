package io.urbanflow.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/** Capacity-controlled physical warehouse aggregate. */
@Entity
@Table(name = "warehouses")
public class Warehouse {

  @Id private UUID id;

  @Column(nullable = false, unique = true, length = 20)
  private String code;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(nullable = false, length = 120)
  private String city;

  @Column(nullable = false)
  private int capacity;

  @Column(nullable = false)
  private boolean active;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  @Column(nullable = false)
  private long version;

  /** Constructor reserved for persistence. */
  protected Warehouse() {}

  /** Creates an active warehouse. */
  public static Warehouse create(
      UUID id, String code, String name, String city, int capacity, Instant now) {
    Warehouse warehouse = new Warehouse();
    warehouse.id = Objects.requireNonNull(id, "id must not be null");
    warehouse.code = requireCode(code);
    warehouse.name = requireText(name, "name", 120);
    warehouse.city = requireText(city, "city", 120);
    warehouse.capacity = requireCapacity(capacity);
    warehouse.active = true;
    warehouse.createdAt = Objects.requireNonNull(now, "now must not be null");
    warehouse.updatedAt = now;
    return warehouse;
  }

  /** Updates mutable warehouse properties. */
  public void update(String name, String city, int capacity, boolean active, Instant now) {
    this.name = requireText(name, "name", 120);
    this.city = requireText(city, "city", 120);
    this.capacity = requireCapacity(capacity);
    this.active = active;
    this.updatedAt = Objects.requireNonNull(now, "now must not be null");
  }

  /** Returns the identifier. */
  public UUID id() {
    return id;
  }

  /** Returns the stable code. */
  public String code() {
    return code;
  }

  /** Returns the display name. */
  public String name() {
    return name;
  }

  /** Returns the city. */
  public String city() {
    return city;
  }

  /** Returns the parcel capacity. */
  public int capacity() {
    return capacity;
  }

  /** Returns whether intake is enabled. */
  public boolean active() {
    return active;
  }

  /** Returns creation time. */
  public Instant createdAt() {
    return createdAt;
  }

  /** Returns last update time. */
  public Instant updatedAt() {
    return updatedAt;
  }

  /** Returns optimistic version. */
  public long version() {
    return version;
  }

  private static String requireCode(String value) {
    String code = requireText(value, "code", 20).toUpperCase(Locale.ROOT);
    if (!code.matches("[A-Z0-9]{2,20}")) {
      throw new IllegalArgumentException("code must contain 2 to 20 letters or digits");
    }
    return code;
  }

  private static String requireText(String value, String field, int maximum) {
    if (value == null || value.isBlank() || value.trim().length() > maximum) {
      throw new IllegalArgumentException(field + " must contain 1 to " + maximum + " characters");
    }
    return value.trim();
  }

  private static int requireCapacity(int value) {
    if (value < 1 || value > 1_000_000) {
      throw new IllegalArgumentException("capacity must be between 1 and 1000000");
    }
    return value;
  }
}
