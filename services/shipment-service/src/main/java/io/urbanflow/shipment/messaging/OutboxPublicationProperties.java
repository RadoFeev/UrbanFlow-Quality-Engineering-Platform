package io.urbanflow.shipment.messaging;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Type-safe operational limits for transactional outbox publication. */
@ConfigurationProperties("urbanflow.outbox")
public record OutboxPublicationProperties(
    @DefaultValue("true") boolean enabled,
    @DefaultValue("urbanflow.shipment.events.v1") String topic,
    @DefaultValue("50") int batchSize,
    @DefaultValue("1s") Duration pollInterval,
    @DefaultValue("30s") Duration claimLease,
    @DefaultValue("10s") Duration publishTimeout,
    @DefaultValue("1s") Duration initialRetryDelay,
    @DefaultValue("5m") Duration maxRetryDelay) {

  /** Validates settings that protect lease ownership and bounded retries. */
  public OutboxPublicationProperties {
    topic = requireText(topic, "topic");
    requirePositive(batchSize, "batchSize");
    requirePositive(pollInterval, "pollInterval");
    requirePositive(claimLease, "claimLease");
    requirePositive(publishTimeout, "publishTimeout");
    requirePositive(initialRetryDelay, "initialRetryDelay");
    requirePositive(maxRetryDelay, "maxRetryDelay");
    if (claimLease.compareTo(publishTimeout) <= 0) {
      throw new IllegalArgumentException("claimLease must exceed publishTimeout");
    }
    if (maxRetryDelay.compareTo(initialRetryDelay) < 0) {
      throw new IllegalArgumentException(
          "maxRetryDelay must not be shorter than initialRetryDelay");
    }
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value.trim();
  }

  private static void requirePositive(Duration value, String field) {
    if (value == null || value.isZero() || value.isNegative()) {
      throw new IllegalArgumentException(field + " must be positive");
    }
  }

  private static void requirePositive(int value, String field) {
    if (value <= 0) {
      throw new IllegalArgumentException(field + " must be positive");
    }
  }
}
