package io.urbanflow.warehouse.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.urbanflow.warehouse.domain.AuditRecord;
import io.urbanflow.warehouse.domain.FeatureFlag;
import io.urbanflow.warehouse.domain.Incident;
import io.urbanflow.warehouse.domain.IncidentStatus;
import io.urbanflow.warehouse.domain.LogisticsHub;
import io.urbanflow.warehouse.domain.Warehouse;
import io.urbanflow.warehouse.persistence.AuditRecordRepository;
import io.urbanflow.warehouse.persistence.FeatureFlagRepository;
import io.urbanflow.warehouse.persistence.IncidentRepository;
import io.urbanflow.warehouse.persistence.LogisticsHubRepository;
import io.urbanflow.warehouse.persistence.WarehouseRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataIntegrityViolationException;

class WarehouseApplicationServiceTest {

  private static final Instant NOW = Instant.parse("2026-08-24T10:00:00Z");
  private static final UUID ACTOR = UUID.fromString("10000000-0000-0000-0000-000000000001");

  @Mock private WarehouseRepository warehouses;
  @Mock private LogisticsHubRepository hubs;
  @Mock private IncidentRepository incidents;
  @Mock private FeatureFlagRepository flags;
  @Mock private AuditRecordRepository audit;

  private WarehouseApplicationService service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    when(warehouses.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(hubs.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(incidents.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(flags.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(audit.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    service =
        new WarehouseApplicationService(
            warehouses, hubs, incidents, flags, audit, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void createsWarehouseAndAuditRecord() {
    Warehouse warehouse = service.createWarehouse(ACTOR, "SOF01", "Sofia", "Sofia", 100);

    assertThat(warehouse.code()).isEqualTo("SOF01");
    verify(warehouses).save(warehouse);
    verify(audit).save(any(AuditRecord.class));
  }

  @Test
  void normalizesDuplicateWarehouseCodeFailure() {
    when(warehouses.save(any())).thenThrow(new DataIntegrityViolationException("unique"));

    assertThatThrownBy(() -> service.createWarehouse(ACTOR, "SOF01", "Sofia", "Sofia", 100))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("warehouse code already exists");
  }

  @Test
  void updatesWarehouseWhenVersionMatches() {
    Warehouse warehouse = warehouse();
    when(warehouses.findById(warehouse.id())).thenReturn(Optional.of(warehouse));

    Warehouse updated =
        service.updateWarehouse(ACTOR, warehouse.id(), 0, "New", "Plovdiv", 200, false);

    assertThat(updated.name()).isEqualTo("New");
    assertThat(updated.active()).isFalse();
    verify(audit).save(any(AuditRecord.class));
  }

  @Test
  void rejectsStaleWarehouseVersionWithoutAudit() {
    Warehouse warehouse = warehouse();
    when(warehouses.findById(warehouse.id())).thenReturn(Optional.of(warehouse));

    assertThatThrownBy(
            () -> service.updateWarehouse(ACTOR, warehouse.id(), 7, "New", "City", 1, true))
        .isInstanceOf(VersionConflictException.class)
        .hasMessageContaining("expected version 7");
  }

  @Test
  void createsHubForExistingWarehouse() {
    Warehouse warehouse = warehouse();
    when(warehouses.findById(warehouse.id())).thenReturn(Optional.of(warehouse));

    LogisticsHub hub = service.createHub(ACTOR, warehouse.id(), "North", "Sofia");

    assertThat(hub.warehouseId()).isEqualTo(warehouse.id());
    verify(hubs).save(hub);
  }

  @Test
  void rejectsHubForMissingWarehouse() {
    UUID id = UUID.randomUUID();
    when(warehouses.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.createHub(ACTOR, id, "North", "Sofia"))
        .isInstanceOf(WarehouseNotFoundException.class);
  }

  @Test
  void changesHubAvailability() {
    LogisticsHub hub =
        LogisticsHub.create(UUID.randomUUID(), UUID.randomUUID(), "North", "Sofia", NOW);
    when(hubs.findById(hub.id())).thenReturn(Optional.of(hub));

    LogisticsHub changed = service.setHubActive(ACTOR, hub.id(), 0, false);

    assertThat(changed.active()).isFalse();
    verify(audit).save(any(AuditRecord.class));
  }

  @Test
  void opensAndResolvesIncident() {
    Incident incident = service.openIncident(ACTOR, UUID.randomUUID(), "DELAY", "Weather");
    when(incidents.findById(incident.id())).thenReturn(Optional.of(incident));

    Incident resolved = service.resolveIncident(ACTOR, incident.id(), 0);

    assertThat(resolved.status()).isEqualTo(IncidentStatus.RESOLVED);
    verify(incidents).save(incident);
  }

  @Test
  void preventsResolvingMissingIncident() {
    UUID id = UUID.randomUUID();
    when(incidents.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.resolveIncident(ACTOR, id, 0))
        .isInstanceOf(WarehouseNotFoundException.class);
  }

  @Test
  void createsAndChangesFeatureFlag() {
    FeatureFlag flag = service.createFlag(ACTOR, "routing.v2", "New routing", false);
    when(flags.findById(flag.id())).thenReturn(Optional.of(flag));

    FeatureFlag changed = service.setFlagEnabled(ACTOR, flag.id(), 0, true);

    assertThat(changed.enabled()).isTrue();
    verify(flags).save(flag);
  }

  @Test
  void normalizesDuplicateFeatureFlagFailure() {
    when(flags.save(any())).thenThrow(new DataIntegrityViolationException("unique"));

    assertThatThrownBy(() -> service.createFlag(ACTOR, "routing.v2", "New routing", false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("feature flag key already exists");
  }

  @Test
  void returnsDefensiveOrderedHubList() {
    Warehouse warehouse = warehouse();
    LogisticsHub hub = LogisticsHub.create(UUID.randomUUID(), warehouse.id(), "A", "Sofia", NOW);
    when(warehouses.findById(warehouse.id())).thenReturn(Optional.of(warehouse));
    when(hubs.findByWarehouseIdOrderByNameAsc(warehouse.id())).thenReturn(List.of(hub));

    List<LogisticsHub> result = service.hubs(warehouse.id());

    assertThat(result).containsExactly(hub);
    assertThatThrownBy(() -> result.add(hub)).isInstanceOf(UnsupportedOperationException.class);
  }

  private static Warehouse warehouse() {
    return Warehouse.create(UUID.randomUUID(), "SOF01", "Sofia", "Sofia", 100, NOW);
  }
}
