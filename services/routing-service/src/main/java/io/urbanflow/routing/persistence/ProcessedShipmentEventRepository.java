package io.urbanflow.routing.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence port for consumed-event idempotency. */
public interface ProcessedShipmentEventRepository
    extends JpaRepository<ProcessedShipmentEvent, UUID> {}
