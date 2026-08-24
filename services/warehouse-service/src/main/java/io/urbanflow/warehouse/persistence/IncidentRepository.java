package io.urbanflow.warehouse.persistence;

import io.urbanflow.warehouse.domain.Incident;
import io.urbanflow.warehouse.domain.IncidentStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Incident persistence port. */
public interface IncidentRepository extends JpaRepository<Incident, UUID> {

  /** Lists incidents by lifecycle status. */
  Page<Incident> findByStatusOrderByCreatedAtDesc(IncidentStatus status, Pageable pageable);
}
