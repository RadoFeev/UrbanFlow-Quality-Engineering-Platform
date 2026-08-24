package io.urbanflow.warehouse.persistence;

import io.urbanflow.warehouse.domain.AuditRecord;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Append-only audit persistence port. */
public interface AuditRecordRepository extends JpaRepository<AuditRecord, UUID> {

  /** Lists newest records for an entity. */
  Page<AuditRecord> findByEntityIdOrderByOccurredAtDesc(UUID entityId, Pageable pageable);
}
