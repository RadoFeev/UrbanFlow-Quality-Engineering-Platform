package io.urbanflow.routing.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence port for pending routing events. */
public interface RoutingOutboxRepository extends JpaRepository<RoutingOutboxEvent, UUID> {

  /** Returns a bounded due batch in deterministic occurrence order. */
  List<RoutingOutboxEvent>
      findTop50ByPublishedAtIsNullAndNextAttemptAtLessThanEqualOrderByOccurredAtAsc(Instant now);
}
