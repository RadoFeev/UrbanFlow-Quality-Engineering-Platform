package io.urbanflow.shipment.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.urbanflow.shipment.persistence.OutboxEvent;
import io.urbanflow.shipment.persistence.OutboxEventRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxStateServiceTest {

  private static final Instant NOW = Instant.parse("2026-08-24T10:30:00Z");
  private static final Duration LEASE = Duration.ofSeconds(30);
  private static final UUID EVENT_ID = UUID.fromString("4244f7c7-19d3-44a2-a743-3071bc7df2ba");
  private static final UUID AGGREGATE_ID = UUID.fromString("40c7f127-b2c1-42bd-b2cd-c80a30331b95");
  private static final String WORKER = "shipment-worker-1";

  @Mock private OutboxEventRepository repository;
  @Mock private OutboxEvent event;

  private OutboxStateService service;

  @BeforeEach
  void setUp() {
    service = new OutboxStateService(repository);
  }

  @Test
  void claimsWithExpiredLeaseBoundaryAndReturnsDetachedMessage() {
    when(repository.lockPublishableBatch(NOW, NOW.minus(LEASE), 25)).thenReturn(List.of(event));
    stubEnvelope();

    List<OutboxEventMessage> messages = service.claimBatch(WORKER, NOW, LEASE, 25);

    verify(event).claim(WORKER, NOW);
    assertThat(messages)
        .singleElement()
        .satisfies(
            message -> {
              assertThat(message.eventId()).isEqualTo(EVENT_ID);
              assertThat(message.aggregateId()).isEqualTo(AGGREGATE_ID);
              assertThat(message.publicationAttempts()).isEqualTo(2);
            });
  }

  @Test
  void completesAcknowledgedEventThroughOwningEntity() {
    when(repository.findById(EVENT_ID)).thenReturn(Optional.of(event));

    service.markPublished(EVENT_ID, WORKER, NOW);

    verify(event).markPublished(WORKER, NOW);
  }

  @Test
  void recordsFailedAttemptThroughOwningEntity() {
    Instant retryAt = NOW.plusSeconds(8);
    when(repository.findById(EVENT_ID)).thenReturn(Optional.of(event));

    service.markFailed(EVENT_ID, WORKER, NOW, retryAt, "TimeoutException");

    verify(event).markFailed(WORKER, NOW, retryAt, "TimeoutException");
  }

  @Test
  void rejectsCompletionWhenClaimedEventDisappears() {
    when(repository.findById(EVENT_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.markPublished(EVENT_ID, WORKER, NOW))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("no longer exists");
  }

  private void stubEnvelope() {
    when(event.id()).thenReturn(EVENT_ID);
    when(event.eventType()).thenReturn("ShipmentCreated");
    when(event.aggregateId()).thenReturn(AGGREGATE_ID);
    when(event.aggregateVersion()).thenReturn(3L);
    when(event.schemaVersion()).thenReturn("1.0");
    when(event.payload()).thenReturn("{\"shipmentId\":\"" + AGGREGATE_ID + "\"}");
    when(event.correlationId()).thenReturn("corr-outbox-002");
    when(event.traceId()).thenReturn("abcdef0123456789abcdef0123456789");
    when(event.occurredAt()).thenReturn(NOW.minusSeconds(5));
    when(event.publicationAttempts()).thenReturn(2);
  }
}
