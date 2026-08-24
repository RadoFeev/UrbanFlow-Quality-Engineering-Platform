package io.urbanflow.shipment.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Provides persistence access to transactional outbox events. */
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

  /**
   * Locks a deterministic publishable batch while allowing other workers to skip claimed rows.
   *
   * @param now retry readiness boundary
   * @param claimExpiredBefore lease-expiration boundary
   * @param batchSize maximum rows to claim
   * @return locked unpublished events ordered by occurrence
   */
  @Query(
      value =
          """
          SELECT *
          FROM outbox_events
          WHERE published_at IS NULL
            AND next_attempt_at <= :now
            AND (claimed_at IS NULL OR claimed_at < :claimExpiredBefore)
          ORDER BY occurred_at, id
          LIMIT :batchSize
          FOR UPDATE SKIP LOCKED
          """,
      nativeQuery = true)
  List<OutboxEvent> lockPublishableBatch(
      @Param("now") Instant now,
      @Param("claimExpiredBefore") Instant claimExpiredBefore,
      @Param("batchSize") int batchSize);
}
