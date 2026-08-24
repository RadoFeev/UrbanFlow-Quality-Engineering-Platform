package io.urbanflow.warehouse.persistence;

import io.urbanflow.warehouse.domain.Warehouse;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Warehouse persistence port. */
public interface WarehouseRepository extends JpaRepository<Warehouse, UUID> {

  /** Finds a warehouse by stable code. */
  Optional<Warehouse> findByCode(String code);

  /** Searches name, code, or city case-insensitively. */
  Page<Warehouse>
      findByNameContainingIgnoreCaseOrCodeContainingIgnoreCaseOrCityContainingIgnoreCase(
          String name, String code, String city, Pageable pageable);
}
