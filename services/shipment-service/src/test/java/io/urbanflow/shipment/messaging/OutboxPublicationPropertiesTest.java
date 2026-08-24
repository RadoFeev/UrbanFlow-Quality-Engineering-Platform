package io.urbanflow.shipment.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class OutboxPublicationPropertiesTest {

  @Test
  void rejectsLeaseThatCannotCoverPublishTimeout() {
    assertThatThrownBy(
            () ->
                properties(
                    "events",
                    Duration.ofSeconds(10),
                    Duration.ofSeconds(10),
                    Duration.ofMinutes(1)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("claimLease");
  }

  @Test
  void rejectsMaximumRetryShorterThanInitialDelay() {
    assertThatThrownBy(
            () ->
                new OutboxPublicationProperties(
                    true,
                    "events",
                    10,
                    Duration.ofSeconds(1),
                    Duration.ofSeconds(30),
                    Duration.ofSeconds(5),
                    Duration.ofSeconds(10),
                    Duration.ofSeconds(5)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("maxRetryDelay");
  }

  @Test
  void rejectsBlankTopic() {
    assertThatThrownBy(
            () ->
                properties(
                    " ", Duration.ofSeconds(30), Duration.ofSeconds(5), Duration.ofMinutes(1)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("topic");
  }

  private static OutboxPublicationProperties properties(
      String topic, Duration lease, Duration timeout, Duration maximumRetry) {
    return new OutboxPublicationProperties(
        true,
        topic,
        10,
        Duration.ofSeconds(1),
        lease,
        timeout,
        Duration.ofSeconds(1),
        maximumRetry);
  }
}
