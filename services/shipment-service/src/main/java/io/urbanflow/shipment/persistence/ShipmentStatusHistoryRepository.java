package io.urbanflow.shipment.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Provides append-only persistence for shipment status history. */
public interface ShipmentStatusHistoryRepository
    extends JpaRepository<ShipmentStatusHistory, UUID> {}
