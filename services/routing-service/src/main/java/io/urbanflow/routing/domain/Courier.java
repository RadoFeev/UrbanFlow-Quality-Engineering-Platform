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

/** Versioned courier capacity aggregate used for safe concurrent assignment. */
@Entity
@Table(name = "couriers")
public class Courier {

  @Id private UUID id;

  @Column(name = "display_name", nullable = false, length = 100)
  private String displayName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CourierStatus status;

  @Column(name = "maximum_assignments", nullable = false)
  private int maximumAssignments;

  @Column(name = "active_assignments", nullable = false)
  private int activeAssignments;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  @Column(nullable = false)
  private long version;

  /** Constructor reserved for persistence. */
  protected Courier() {}

  /** Registers an available courier with bounded positive capacity. */
  public static Courier register(UUID id, String displayName, int maximumAssignments, Instant now) {
    if (maximumAssignments < 1 || maximumAssignments > 100) {
      throw new IllegalArgumentException("maximumAssignments must be between 1 and 100");
    }
    Courier courier = new Courier();
    courier.id = Objects.requireNonNull(id, "id must not be null");
    courier.displayName = requireText(displayName);
    courier.status = CourierStatus.AVAILABLE;
    courier.maximumAssignments = maximumAssignments;
    courier.updatedAt = Objects.requireNonNull(now, "now must not be null");
    return courier;
  }

  /** Reserves one slot or fails when this courier cannot accept more work. */
  public void assign(Instant now) {
    if (!isAssignable()) {
      throw new CourierCapacityException(id);
    }
    activeAssignments++;
    updatedAt = Objects.requireNonNull(now, "now must not be null");
  }

  /** Releases one active assignment without allowing a negative count. */
  public void release(Instant now) {
    if (activeAssignments == 0) {
      throw new IllegalStateException("courier has no active assignment to release");
    }
    activeAssignments--;
    updatedAt = Objects.requireNonNull(now, "now must not be null");
  }

  /** Returns whether availability and capacity both allow assignment. */
  public boolean isAssignable() {
    return status == CourierStatus.AVAILABLE && activeAssignments < maximumAssignments;
  }

  /** Returns the courier identifier. */
  public UUID id() {
    return id;
  }

  /** Returns the courier display name. */
  public String displayName() {
    return displayName;
  }

  /** Returns the current operational status. */
  public CourierStatus status() {
    return status;
  }

  /** Returns the maximum concurrent assignment count. */
  public int maximumAssignments() {
    return maximumAssignments;
  }

  /** Returns the current active assignment count. */
  public int activeAssignments() {
    return activeAssignments;
  }

  /** Returns the optimistic persistence version. */
  public long version() {
    return version;
  }

  private static String requireText(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("displayName must not be blank");
    }
    return value.trim();
  }
}
