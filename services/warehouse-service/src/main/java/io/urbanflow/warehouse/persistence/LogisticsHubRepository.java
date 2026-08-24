package io.urbanflow.warehouse.persistence;

import io.urbanflow.warehouse.domain.LogisticsHub;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Logistics hub persistence port. */
public interface LogisticsHubRepository extends JpaRepository<LogisticsHub, UUID> {

  /** Lists hubs for a warehouse in deterministic order. */
  List<LogisticsHub> findByWarehouseIdOrderByNameAsc(UUID warehouseId);
}
