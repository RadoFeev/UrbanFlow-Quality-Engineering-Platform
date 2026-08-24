package io.urbanflow.tracking.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persists idempotent customer-facing tracking timeline projections. */
public interface TrackingEventRepository extends JpaRepository<TrackingEventProjection, UUID> {

  /** Returns the latest projection version for stale-event rejection. */
  Optional<TrackingEventProjection> findTopByShipmentIdOrderByAggregateVersionDescOccurredAtDesc(
      UUID shipmentId);

  /** Returns a deterministic timeline for the public tracking number. */
  List<TrackingEventProjection> findByTrackingNumberOrderByAggregateVersionAscOccurredAtAscIdAsc(
      String trackingNumber);
}
