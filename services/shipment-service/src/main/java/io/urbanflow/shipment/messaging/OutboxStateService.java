package io.urbanflow.shipment.messaging;

import io.urbanflow.shipment.persistence.OutboxEvent;
import io.urbanflow.shipment.persistence.OutboxEventRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns the short database transactions around outbox lease state. */
@Service
public class OutboxStateService {

  private final OutboxEventRepository repository;

  OutboxStateService(OutboxEventRepository repository) {
    this.repository = repository;
  }

  /** Claims a publishable batch and returns detached immutable messages. */
  @Transactional
  public List<OutboxEventMessage> claimBatch(
      String workerId, Instant now, Duration claimLease, int batchSize) {
    List<OutboxEvent> events =
        repository.lockPublishableBatch(now, now.minus(claimLease), batchSize);
    events.forEach(event -> event.claim(workerId, now));
    return events.stream().map(OutboxEventMessage::from).toList();
  }

  /** Completes publication only when the caller still owns the event lease. */
  @Transactional
  public void markPublished(UUID eventId, String workerId, Instant publishedAt) {
    requiredEvent(eventId).markPublished(workerId, publishedAt);
  }

  /** Releases a failed lease and persists its next retry boundary. */
  @Transactional
  public void markFailed(
      UUID eventId, String workerId, Instant failedAt, Instant retryAt, String failureSummary) {
    requiredEvent(eventId).markFailed(workerId, failedAt, retryAt, failureSummary);
  }

  private OutboxEvent requiredEvent(UUID eventId) {
    return repository
        .findById(eventId)
        .orElseThrow(() -> new IllegalStateException("Claimed outbox event no longer exists"));
  }
}
