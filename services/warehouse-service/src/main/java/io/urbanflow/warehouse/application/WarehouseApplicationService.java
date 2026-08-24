package io.urbanflow.warehouse.application;

import io.urbanflow.warehouse.domain.AuditRecord;
import io.urbanflow.warehouse.domain.FeatureFlag;
import io.urbanflow.warehouse.domain.Incident;
import io.urbanflow.warehouse.domain.LogisticsHub;
import io.urbanflow.warehouse.domain.Warehouse;
import io.urbanflow.warehouse.persistence.AuditRecordRepository;
import io.urbanflow.warehouse.persistence.FeatureFlagRepository;
import io.urbanflow.warehouse.persistence.IncidentRepository;
import io.urbanflow.warehouse.persistence.LogisticsHubRepository;
import io.urbanflow.warehouse.persistence.WarehouseRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Transaction boundary for operational warehouse commands. */
@Service
public class WarehouseApplicationService {

  private final WarehouseRepository warehouses;
  private final LogisticsHubRepository hubs;
  private final IncidentRepository incidents;
  private final FeatureFlagRepository flags;
  private final AuditRecordRepository audit;
  private final Clock clock;

  /** Creates the application service. */
  public WarehouseApplicationService(
      WarehouseRepository warehouses,
      LogisticsHubRepository hubs,
      IncidentRepository incidents,
      FeatureFlagRepository flags,
      AuditRecordRepository audit,
      Clock clock) {
    this.warehouses = warehouses;
    this.hubs = hubs;
    this.incidents = incidents;
    this.flags = flags;
    this.audit = audit;
    this.clock = clock;
  }

  /** Creates a warehouse with a unique code. */
  @Transactional
  public Warehouse createWarehouse(
      UUID actorId, String code, String name, String city, int capacity) {
    Warehouse warehouse =
        Warehouse.create(UUID.randomUUID(), code, name, city, capacity, clock.instant());
    try {
      Warehouse saved = warehouses.save(warehouse);
      record(actorId, "WAREHOUSE_CREATED", "Warehouse", saved.id(), saved.code());
      return saved;
    } catch (DataIntegrityViolationException exception) {
      throw new IllegalArgumentException("warehouse code already exists", exception);
    }
  }

  /** Updates a warehouse with an optimistic version precondition. */
  @Transactional
  public Warehouse updateWarehouse(
      UUID actorId,
      UUID id,
      long expectedVersion,
      String name,
      String city,
      int capacity,
      boolean active) {
    Warehouse warehouse = findWarehouse(id);
    requireVersion(expectedVersion, warehouse.version());
    warehouse.update(name, city, capacity, active, clock.instant());
    record(actorId, "WAREHOUSE_UPDATED", "Warehouse", id, "active=" + active);
    return warehouse;
  }

  /** Creates a hub only for an existing warehouse. */
  @Transactional
  public LogisticsHub createHub(UUID actorId, UUID warehouseId, String name, String city) {
    findWarehouse(warehouseId);
    LogisticsHub hub =
        hubs.save(LogisticsHub.create(UUID.randomUUID(), warehouseId, name, city, clock.instant()));
    record(actorId, "HUB_CREATED", "LogisticsHub", hub.id(), name);
    return hub;
  }

  /** Toggles a hub with an optimistic version precondition. */
  @Transactional
  public LogisticsHub setHubActive(UUID actorId, UUID id, long expectedVersion, boolean active) {
    LogisticsHub hub =
        hubs.findById(id).orElseThrow(() -> new WarehouseNotFoundException("hub", id));
    requireVersion(expectedVersion, hub.version());
    hub.setActive(active, clock.instant());
    record(actorId, "HUB_AVAILABILITY_CHANGED", "LogisticsHub", id, "active=" + active);
    return hub;
  }

  /** Opens a shipment incident. */
  @Transactional
  public Incident openIncident(UUID actorId, UUID shipmentId, String type, String description) {
    Incident incident =
        incidents.save(
            Incident.open(UUID.randomUUID(), shipmentId, type, description, clock.instant()));
    record(actorId, "INCIDENT_OPENED", "Incident", incident.id(), type);
    return incident;
  }

  /** Resolves an open incident with an optimistic version precondition. */
  @Transactional
  public Incident resolveIncident(UUID actorId, UUID id, long expectedVersion) {
    Incident incident =
        incidents.findById(id).orElseThrow(() -> new WarehouseNotFoundException("incident", id));
    requireVersion(expectedVersion, incident.version());
    incident.resolve(clock.instant());
    record(actorId, "INCIDENT_RESOLVED", "Incident", id, incident.type());
    return incident;
  }

  /** Creates a globally unique feature flag. */
  @Transactional
  public FeatureFlag createFlag(UUID actorId, String key, String description, boolean enabled) {
    FeatureFlag flag =
        FeatureFlag.create(UUID.randomUUID(), key, description, enabled, clock.instant());
    try {
      FeatureFlag saved = flags.save(flag);
      record(actorId, "FEATURE_FLAG_CREATED", "FeatureFlag", saved.id(), saved.key());
      return saved;
    } catch (DataIntegrityViolationException exception) {
      throw new IllegalArgumentException("feature flag key already exists", exception);
    }
  }

  /** Changes a feature flag with an optimistic version precondition. */
  @Transactional
  public FeatureFlag setFlagEnabled(UUID actorId, UUID id, long expectedVersion, boolean enabled) {
    FeatureFlag flag =
        flags.findById(id).orElseThrow(() -> new WarehouseNotFoundException("feature flag", id));
    requireVersion(expectedVersion, flag.version());
    flag.setEnabled(enabled, clock.instant());
    record(actorId, "FEATURE_FLAG_CHANGED", "FeatureFlag", id, "enabled=" + enabled);
    return flag;
  }

  /** Returns a warehouse or a stable missing-entity error. */
  @Transactional(readOnly = true)
  public Warehouse findWarehouse(UUID id) {
    return warehouses
        .findById(id)
        .orElseThrow(() -> new WarehouseNotFoundException("warehouse", id));
  }

  /** Lists stable ordered hubs for one warehouse. */
  @Transactional(readOnly = true)
  public List<LogisticsHub> hubs(UUID warehouseId) {
    findWarehouse(warehouseId);
    return List.copyOf(hubs.findByWarehouseIdOrderByNameAsc(warehouseId));
  }

  private void record(
      UUID actorId, String action, String entityType, UUID entityId, String details) {
    audit.save(
        AuditRecord.create(
            UUID.randomUUID(), actorId, action, entityType, entityId, details, clock.instant()));
  }

  private static void requireVersion(long expected, long actual) {
    if (expected != actual) {
      throw new VersionConflictException(expected, actual);
    }
  }
}
