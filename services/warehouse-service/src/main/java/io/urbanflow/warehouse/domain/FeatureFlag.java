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

/** Versioned operational feature switch. */
@Entity
@Table(name = "feature_flags")
public class FeatureFlag {

  @Id private UUID id;

  @Column(name = "flag_key", nullable = false, unique = true, length = 80)
  private String key;

  @Column(nullable = false, length = 300)
  private String description;

  @Column(nullable = false)
  private boolean enabled;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  @Column(nullable = false)
  private long version;

  /** Constructor reserved for persistence. */
  protected FeatureFlag() {}

  /** Creates a named feature flag. */
  public static FeatureFlag create(
      UUID id, String key, String description, boolean enabled, Instant now) {
    FeatureFlag flag = new FeatureFlag();
    flag.id = Objects.requireNonNull(id, "id must not be null");
    flag.key = requireKey(key);
    flag.description = requireDescription(description);
    flag.enabled = enabled;
    flag.updatedAt = Objects.requireNonNull(now, "now must not be null");
    return flag;
  }

  /** Updates the enabled state. */
  public void setEnabled(boolean enabled, Instant now) {
    this.enabled = enabled;
    this.updatedAt = Objects.requireNonNull(now, "now must not be null");
  }

  /** Returns the identifier. */
  public UUID id() {
    return id;
  }

  /** Returns the stable key. */
  public String key() {
    return key;
  }

  /** Returns the description. */
  public String description() {
    return description;
  }

  /** Returns the enabled state. */
  public boolean enabled() {
    return enabled;
  }

  /** Returns last update time. */
  public Instant updatedAt() {
    return updatedAt;
  }

  /** Returns optimistic version. */
  public long version() {
    return version;
  }

  private static String requireKey(String value) {
    if (value == null) {
      throw new IllegalArgumentException("key must not be null");
    }
    String key = value.trim().toLowerCase(Locale.ROOT);
    if (!key.matches("[a-z][a-z0-9.-]{2,79}")) {
      throw new IllegalArgumentException("key must be a dotted or dashed lowercase identifier");
    }
    return key;
  }

  private static String requireDescription(String value) {
    if (value == null || value.isBlank() || value.trim().length() > 300) {
      throw new IllegalArgumentException("description must contain 1 to 300 characters");
    }
    return value.trim();
  }
}
