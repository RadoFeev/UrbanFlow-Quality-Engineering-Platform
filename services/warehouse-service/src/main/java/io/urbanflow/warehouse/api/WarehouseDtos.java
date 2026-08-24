package io.urbanflow.warehouse.api;

import io.urbanflow.warehouse.domain.AuditRecord;
import io.urbanflow.warehouse.domain.FeatureFlag;
import io.urbanflow.warehouse.domain.Incident;
import io.urbanflow.warehouse.domain.IncidentStatus;
import io.urbanflow.warehouse.domain.LogisticsHub;
import io.urbanflow.warehouse.domain.Warehouse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;

/** Stable response DTOs without persistence internals. */
public final class WarehouseDtos {

  private WarehouseDtos() {}

  /** Warehouse response. */
  public record WarehouseResponse(
      UUID id,
      String code,
      String name,
      String city,
      int capacity,
      boolean active,
      Instant createdAt,
      Instant updatedAt,
      long version) {

    /** Maps a warehouse. */
    public static WarehouseResponse from(Warehouse value) {
      return new WarehouseResponse(
          value.id(),
          value.code(),
          value.name(),
          value.city(),
          value.capacity(),
          value.active(),
          value.createdAt(),
          value.updatedAt(),
          value.version());
    }
  }

  /** Logistics hub response. */
  public record HubResponse(
      UUID id,
      UUID warehouseId,
      String name,
      String city,
      boolean active,
      Instant updatedAt,
      long version) {

    /** Maps a logistics hub. */
    public static HubResponse from(LogisticsHub value) {
      return new HubResponse(
          value.id(),
          value.warehouseId(),
          value.name(),
          value.city(),
          value.active(),
          value.updatedAt(),
          value.version());
    }
  }

  /** Operational incident response. */
  public record IncidentResponse(
      UUID id,
      UUID shipmentId,
      String type,
      String description,
      IncidentStatus status,
      Instant createdAt,
      Instant resolvedAt,
      long version) {

    /** Maps an incident. */
    public static IncidentResponse from(Incident value) {
      return new IncidentResponse(
          value.id(),
          value.shipmentId(),
          value.type(),
          value.description(),
          value.status(),
          value.createdAt(),
          value.resolvedAt(),
          value.version());
    }
  }

  /** Feature-flag response. */
  public record FlagResponse(
      UUID id, String key, String description, boolean enabled, Instant updatedAt, long version) {

    /** Maps a feature flag. */
    public static FlagResponse from(FeatureFlag value) {
      return new FlagResponse(
          value.id(),
          value.key(),
          value.description(),
          value.enabled(),
          value.updatedAt(),
          value.version());
    }
  }

  /** Append-only audit response. */
  public record AuditResponse(
      UUID id,
      UUID actorId,
      String action,
      String entityType,
      UUID entityId,
      String details,
      Instant occurredAt) {

    /** Maps an audit record. */
    public static AuditResponse from(AuditRecord value) {
      return new AuditResponse(
          value.id(),
          value.actorId(),
          value.action(),
          value.entityType(),
          value.entityId(),
          value.details(),
          value.occurredAt());
    }
  }

  /** Stable generic page response. */
  public record PageResponse<T>(
      List<T> items, int page, int size, long totalElements, int totalPages) {

    /** Defensively copies items. */
    public PageResponse {
      items = List.copyOf(items);
    }

    /** Maps a Spring Data page with pre-mapped values. */
    public static <T> PageResponse<T> from(Page<T> page) {
      return new PageResponse<>(
          page.getContent(),
          page.getNumber(),
          page.getSize(),
          page.getTotalElements(),
          page.getTotalPages());
    }
  }
}
