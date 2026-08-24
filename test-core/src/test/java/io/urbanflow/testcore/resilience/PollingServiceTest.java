package io.urbanflow.testcore.resilience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class PollingServiceTest {

  @Test
  void pollsUntilConditionMatches() {
    AtomicInteger value = new AtomicInteger();
    int result =
        new PollingService()
            .until(
                "counter",
                Duration.ofSeconds(1),
                Duration.ofMillis(5),
                value::incrementAndGet,
                current -> current >= 3);
    assertThat(result).isGreaterThanOrEqualTo(3);
  }

  @Test
  void rejectsBlankDescription() {
    assertThatThrownBy(
            () ->
                new PollingService()
                    .until(
                        "",
                        Duration.ofSeconds(1),
                        Duration.ofMillis(5),
                        () -> true,
                        value -> value))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNonPositiveTimeout() {
    assertThatThrownBy(
            () ->
                new PollingService()
                    .until(
                        "value", Duration.ZERO, Duration.ofMillis(5), () -> true, value -> value))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
