package io.urbanflow.shipment.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class OutboxRetryPolicyTest {

  @ParameterizedTest
  @MethodSource("delays")
  void doublesDelayAndCapsAtConfiguredMaximum(int attempt, Duration expected) {
    OutboxRetryPolicy policy = new OutboxRetryPolicy(properties());

    assertThat(policy.delayAfter(attempt)).isEqualTo(expected);
  }

  @Test
  void rejectsNonPositiveAttemptNumber() {
    OutboxRetryPolicy policy = new OutboxRetryPolicy(properties());

    assertThatThrownBy(() -> policy.delayAfter(0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("positive");
  }

  private static Stream<Arguments> delays() {
    return Stream.of(
        Arguments.of(1, Duration.ofSeconds(1)),
        Arguments.of(2, Duration.ofSeconds(2)),
        Arguments.of(3, Duration.ofSeconds(4)),
        Arguments.of(4, Duration.ofSeconds(8)),
        Arguments.of(5, Duration.ofSeconds(10)),
        Arguments.of(20, Duration.ofSeconds(10)));
  }

  private static OutboxPublicationProperties properties() {
    return new OutboxPublicationProperties(
        true,
        "urbanflow.shipment.events.v1",
        50,
        Duration.ofSeconds(1),
        Duration.ofSeconds(30),
        Duration.ofSeconds(10),
        Duration.ofSeconds(1),
        Duration.ofSeconds(10));
  }
}
