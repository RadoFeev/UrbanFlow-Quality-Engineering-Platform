package io.urbanflow.shipment.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Provides persistence access to transactional outbox events. */
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {}
