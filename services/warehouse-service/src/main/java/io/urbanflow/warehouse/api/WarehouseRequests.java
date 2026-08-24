package io.urbanflow.warehouse.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Validated warehouse HTTP request contracts. */
public final class WarehouseRequests {

  private WarehouseRequests() {}

  /** Warehouse creation payload. */
  public record CreateWarehouse(
      @NotBlank @Size(max = 20) String code,
      @NotBlank @Size(max = 120) String name,
      @NotBlank @Size(max = 120) String city,
      @Min(1) @Max(1_000_000) int capacity) {}

  /** Warehouse update payload. */
  public record UpdateWarehouse(
      @NotBlank @Size(max = 120) String name,
      @NotBlank @Size(max = 120) String city,
      @Min(1) @Max(1_000_000) int capacity,
      boolean active) {}

  /** Hub creation payload. */
  public record CreateHub(
      @NotBlank @Size(max = 120) String name, @NotBlank @Size(max = 120) String city) {}

  /** Boolean operational state payload. */
  public record ActiveState(boolean active) {}

  /** Shipment incident creation payload. */
  public record CreateIncident(
      @NotNull UUID shipmentId,
      @NotBlank @Size(max = 60) String type,
      @NotBlank @Size(max = 1000) String description) {}

  /** Feature-flag creation payload. */
  public record CreateFlag(
      @NotBlank @Size(max = 80) String key,
      @NotBlank @Size(max = 300) String description,
      boolean enabled) {}

  /** Boolean feature state payload. */
  public record EnabledState(boolean enabled) {}
}
