package io.urbanflow.routing.persistence;

import io.urbanflow.routing.domain.DeliveryRoute;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence port for route assignments. */
public interface DeliveryRouteRepository extends JpaRepository<DeliveryRoute, UUID> {

  /** Finds the unique route assigned to a shipment. */
  Optional<DeliveryRoute> findByShipmentId(UUID shipmentId);

  /** Lists assignments for a courier ordered for stable UI rendering. */
  List<DeliveryRoute> findByCourierIdOrderByCreatedAtAsc(UUID courierId);
}
