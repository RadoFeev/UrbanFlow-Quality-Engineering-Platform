package io.urbanflow.event.support;

import io.urbanflow.event.support.EventFixtures.EventEnvelope;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

/** Deterministic consumer harness for retry, DLT, restart, and projection semantics. */
public final class EventProjectionHarness {

  private final Set<UUID> processed = ConcurrentHashMap.newKeySet();
  private final Map<UUID, ConcurrentSkipListMap<Long, EventEnvelope>> projections =
      new ConcurrentHashMap<>();
  private final List<String> deadLetters =
      java.util.Collections.synchronizedList(new ArrayList<>());
  private final Map<UUID, Integer> attempts = new ConcurrentHashMap<>();

  /** Processes one envelope idempotently and in aggregate-version order. */
  public boolean process(EventEnvelope event) {
    validate(event);
    if (!event.schemaVersion().startsWith("1.")) {
      deadLetters.add("UNSUPPORTED_SCHEMA:" + event.eventId());
      return false;
    }
    if (!processed.add(event.eventId())) {
      return false;
    }
    projections
        .computeIfAbsent(event.aggregateId(), ignored -> new ConcurrentSkipListMap<>())
        .put(event.aggregateVersion(), event);
    return true;
  }

  /** Classifies malformed raw input without throwing out of the consumer loop. */
  public void processRaw(String value) {
    if (value == null || !value.trim().startsWith("{")) {
      deadLetters.add("MALFORMED_EVENT");
    }
  }

  /** Applies bounded retry semantics and moves an exhausted record to the DLT. */
  public boolean processWithFailures(EventEnvelope event, int transientFailures) {
    for (int attempt = 1; attempt <= 3; attempt += 1) {
      attempts.put(event.eventId(), attempt);
      if (attempt > transientFailures) {
        return process(event);
      }
    }
    deadLetters.add("RETRY_EXHAUSTED:" + event.eventId());
    return false;
  }

  /** Schedules eventual processing without a blocking fixed sleep. */
  public void processLater(EventEnvelope event, Duration delay) {
    CompletableFuture.runAsync(
        () -> process(event),
        CompletableFuture.delayedExecutor(
            delay.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS));
  }

  /** Returns ordered events for an aggregate. */
  public List<EventEnvelope> timeline(UUID aggregateId) {
    var values = projections.get(aggregateId);
    return values == null ? List.of() : List.copyOf(values.values());
  }

  /** Returns immutable processed IDs for restart simulation. */
  public Set<UUID> checkpoint() {
    return Set.copyOf(processed);
  }

  /** Restores processed offsets after a consumer restart. */
  public void restore(Set<UUID> checkpoint) {
    processed.addAll(new HashSet<>(checkpoint));
  }

  /** Returns DLT classifications. */
  public List<String> deadLetters() {
    return List.copyOf(deadLetters);
  }

  /** Returns the last attempt number for an event. */
  public int attempts(UUID eventId) {
    return attempts.getOrDefault(eventId, 0);
  }

  private static void validate(EventEnvelope event) {
    java.util.Objects.requireNonNull(event, "event must not be null");
    if (event.eventId() == null || event.aggregateId() == null || event.payload() == null) {
      throw new IllegalArgumentException("mandatory event identity and payload are required");
    }
  }
}
