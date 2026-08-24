package io.urbanflow.shipment.persistence;

import io.urbanflow.shipment.domain.Shipment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Provides persistence access to shipment aggregates. */
public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {

  /** Finds a shipment by its public tracking number. */
  Optional<Shipment> findByTrackingNumber(String trackingNumber);
}
