package io.urbanflow.warehouse.api;

import static io.urbanflow.warehouse.api.WarehouseDtos.AuditResponse;
import static io.urbanflow.warehouse.api.WarehouseDtos.FlagResponse;
import static io.urbanflow.warehouse.api.WarehouseDtos.HubResponse;
import static io.urbanflow.warehouse.api.WarehouseDtos.IncidentResponse;
import static io.urbanflow.warehouse.api.WarehouseDtos.PageResponse;
import static io.urbanflow.warehouse.api.WarehouseDtos.WarehouseResponse;

import io.urbanflow.warehouse.application.WarehouseApplicationService;
import io.urbanflow.warehouse.domain.IncidentStatus;
import io.urbanflow.warehouse.persistence.AuditRecordRepository;
import io.urbanflow.warehouse.persistence.FeatureFlagRepository;
import io.urbanflow.warehouse.persistence.IncidentRepository;
import io.urbanflow.warehouse.persistence.WarehouseRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Operations APIs for warehouses, hubs, incidents, flags, and audit records. */
@Validated
@RestController
@RequestMapping("/api/v1")
public class WarehouseController {

  private static final Set<String> WAREHOUSE_SORTS = Set.of("name", "code", "city", "updatedAt");
  private static final String ACTOR_HEADER = "X-Actor-Id";

  private final WarehouseApplicationService service;
  private final WarehouseRepository warehouses;
  private final IncidentRepository incidents;
  private final FeatureFlagRepository flags;
  private final AuditRecordRepository audit;

  /** Creates the operations HTTP adapter. */
  public WarehouseController(
      WarehouseApplicationService service,
      WarehouseRepository warehouses,
      IncidentRepository incidents,
      FeatureFlagRepository flags,
      AuditRecordRepository audit) {
    this.service = service;
    this.warehouses = warehouses;
    this.incidents = incidents;
    this.flags = flags;
    this.audit = audit;
  }

  /** Creates a warehouse. */
  @PostMapping("/warehouses")
  public ResponseEntity<WarehouseResponse> createWarehouse(
      @RequestHeader(ACTOR_HEADER) UUID actorId,
      @Valid @RequestBody WarehouseRequests.CreateWarehouse request) {
    WarehouseResponse response =
        WarehouseResponse.from(
            service.createWarehouse(
                actorId, request.code(), request.name(), request.city(), request.capacity()));
    return ResponseEntity.created(URI.create("/api/v1/warehouses/" + response.id()))
        .eTag(EntityTagVersion.format(response.version()))
        .body(response);
  }

  /** Reads one warehouse. */
  @GetMapping("/warehouses/{id}")
  public ResponseEntity<WarehouseResponse> warehouse(@PathVariable UUID id) {
    return versioned(WarehouseResponse.from(service.findWarehouse(id)));
  }

  /** Searches and pages warehouses. */
  @GetMapping("/warehouses")
  public ResponseEntity<PageResponse<WarehouseResponse>> warehouses(
      @RequestParam(defaultValue = "") String query,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
      @RequestParam(defaultValue = "name") String sort,
      @RequestParam(defaultValue = "asc") String direction) {
    String sortProperty = requireSort(sort);
    Sort.Direction sortDirection = requireDirection(direction);
    PageRequest pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortProperty));
    var result =
        query.isBlank()
            ? warehouses.findAll(pageable)
            : warehouses
                .findByNameContainingIgnoreCaseOrCodeContainingIgnoreCaseOrCityContainingIgnoreCase(
                    query.trim(), query.trim(), query.trim(), pageable);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(PageResponse.from(result.map(WarehouseResponse::from)));
  }

  /** Updates one warehouse. */
  @PutMapping("/warehouses/{id}")
  public ResponseEntity<WarehouseResponse> updateWarehouse(
      @PathVariable UUID id,
      @RequestHeader(ACTOR_HEADER) UUID actorId,
      @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch,
      @Valid @RequestBody WarehouseRequests.UpdateWarehouse request) {
    return versioned(
        WarehouseResponse.from(
            service.updateWarehouse(
                actorId,
                id,
                EntityTagVersion.parse(ifMatch),
                request.name(),
                request.city(),
                request.capacity(),
                request.active())));
  }

  /** Creates a hub within a warehouse. */
  @PostMapping("/warehouses/{warehouseId}/hubs")
  public ResponseEntity<HubResponse> createHub(
      @PathVariable UUID warehouseId,
      @RequestHeader(ACTOR_HEADER) UUID actorId,
      @Valid @RequestBody WarehouseRequests.CreateHub request) {
    HubResponse response =
        HubResponse.from(service.createHub(actorId, warehouseId, request.name(), request.city()));
    return ResponseEntity.created(URI.create("/api/v1/hubs/" + response.id()))
        .eTag(EntityTagVersion.format(response.version()))
        .body(response);
  }

  /** Lists hubs within a warehouse. */
  @GetMapping("/warehouses/{warehouseId}/hubs")
  public ResponseEntity<List<HubResponse>> hubs(@PathVariable UUID warehouseId) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(service.hubs(warehouseId).stream().map(HubResponse::from).toList());
  }

  /** Changes hub availability. */
  @PatchMapping("/hubs/{id}/active")
  public ResponseEntity<HubResponse> setHubActive(
      @PathVariable UUID id,
      @RequestHeader(ACTOR_HEADER) UUID actorId,
      @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch,
      @Valid @RequestBody WarehouseRequests.ActiveState request) {
    HubResponse response =
        HubResponse.from(
            service.setHubActive(actorId, id, EntityTagVersion.parse(ifMatch), request.active()));
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .eTag(EntityTagVersion.format(response.version()))
        .body(response);
  }

  /** Opens a shipment incident. */
  @PostMapping("/incidents")
  public ResponseEntity<IncidentResponse> openIncident(
      @RequestHeader(ACTOR_HEADER) UUID actorId,
      @Valid @RequestBody WarehouseRequests.CreateIncident request) {
    IncidentResponse response =
        IncidentResponse.from(
            service.openIncident(
                actorId, request.shipmentId(), request.type(), request.description()));
    return ResponseEntity.created(URI.create("/api/v1/incidents/" + response.id()))
        .eTag(EntityTagVersion.format(response.version()))
        .body(response);
  }

  /** Lists incidents by status. */
  @GetMapping("/incidents")
  public ResponseEntity<PageResponse<IncidentResponse>> incidents(
      @RequestParam(defaultValue = "OPEN") IncidentStatus status,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    var result =
        incidents
            .findByStatusOrderByCreatedAtDesc(status, PageRequest.of(page, size))
            .map(IncidentResponse::from);
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(PageResponse.from(result));
  }

  /** Resolves an incident. */
  @PostMapping("/incidents/{id}/resolve")
  public ResponseEntity<IncidentResponse> resolveIncident(
      @PathVariable UUID id,
      @RequestHeader(ACTOR_HEADER) UUID actorId,
      @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch) {
    IncidentResponse response =
        IncidentResponse.from(
            service.resolveIncident(actorId, id, EntityTagVersion.parse(ifMatch)));
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .eTag(EntityTagVersion.format(response.version()))
        .body(response);
  }

  /** Creates a feature flag. */
  @PostMapping("/feature-flags")
  public ResponseEntity<FlagResponse> createFlag(
      @RequestHeader(ACTOR_HEADER) UUID actorId,
      @Valid @RequestBody WarehouseRequests.CreateFlag request) {
    FlagResponse response =
        FlagResponse.from(
            service.createFlag(actorId, request.key(), request.description(), request.enabled()));
    return ResponseEntity.created(URI.create("/api/v1/feature-flags/" + response.id()))
        .eTag(EntityTagVersion.format(response.version()))
        .body(response);
  }

  /** Lists all flags by stable key. */
  @GetMapping("/feature-flags")
  public ResponseEntity<List<FlagResponse>> flags() {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(flags.findAllByOrderByKeyAsc().stream().map(FlagResponse::from).toList());
  }

  /** Changes feature-flag state. */
  @PatchMapping("/feature-flags/{id}")
  public ResponseEntity<FlagResponse> setFlagEnabled(
      @PathVariable UUID id,
      @RequestHeader(ACTOR_HEADER) UUID actorId,
      @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch,
      @Valid @RequestBody WarehouseRequests.EnabledState request) {
    FlagResponse response =
        FlagResponse.from(
            service.setFlagEnabled(
                actorId, id, EntityTagVersion.parse(ifMatch), request.enabled()));
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .eTag(EntityTagVersion.format(response.version()))
        .body(response);
  }

  /** Lists newest audit records for one entity. */
  @GetMapping("/audit-records")
  public ResponseEntity<PageResponse<AuditResponse>> audit(
      @RequestParam UUID entityId,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
    var result =
        audit
            .findByEntityIdOrderByOccurredAtDesc(entityId, PageRequest.of(page, size))
            .map(AuditResponse::from);
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(PageResponse.from(result));
  }

  private static ResponseEntity<WarehouseResponse> versioned(WarehouseResponse response) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .eTag(EntityTagVersion.format(response.version()))
        .body(response);
  }

  private static String requireSort(String value) {
    if (!WAREHOUSE_SORTS.contains(value)) {
      throw new IllegalArgumentException("unsupported warehouse sort: " + value);
    }
    return value;
  }

  private static Sort.Direction requireDirection(String value) {
    return switch (value.toLowerCase(Locale.ROOT)) {
      case "asc" -> Sort.Direction.ASC;
      case "desc" -> Sort.Direction.DESC;
      default -> throw new IllegalArgumentException("direction must be asc or desc");
    };
  }
}
