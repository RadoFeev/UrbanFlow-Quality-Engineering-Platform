package io.urbanflow.testcore.resilience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RetryPolicyTest {

  @Test
  void retriesClassifiedInfrastructureFailure() throws Exception {
    AtomicInteger attempts = new AtomicInteger();
    RetryPolicy policy = new RetryPolicy(3, Duration.ZERO, InfrastructureFailure.class::isInstance);

    String result =
        policy.execute(
            () -> {
              if (attempts.incrementAndGet() < 3) {
                throw new InfrastructureFailure("grid unavailable");
              }
              return "ready";
            });

    assertThat(result).isEqualTo("ready");
    assertThat(attempts).hasValue(3);
  }

  @Test
  void neverRetriesAssertionFailure() {
    AtomicInteger attempts = new AtomicInteger();
    RetryPolicy policy = RetryPolicy.infrastructureOnly();

    assertThatThrownBy(
            () ->
                policy.execute(
                    () -> {
                      attempts.incrementAndGet();
                      throw new AssertionError("product mismatch");
                    }))
        .isInstanceOf(AssertionError.class);
    assertThat(attempts).hasValue(1);
  }

  @Test
  void stopsAtMaximumAttempts() {
    AtomicInteger attempts = new AtomicInteger();
    RetryPolicy policy = new RetryPolicy(2, Duration.ZERO, ignored -> true);

    assertThatThrownBy(
            () ->
                policy.execute(
                    () -> {
                      attempts.incrementAndGet();
                      throw new InfrastructureFailure("down");
                    }))
        .isInstanceOf(InfrastructureFailure.class);
    assertThat(attempts).hasValue(2);
  }

  @Test
  void rejectsExcessiveRetryCount() {
    assertThatThrownBy(() -> new RetryPolicy(4, Duration.ZERO, ignored -> true))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsExcessiveDelay() {
    assertThatThrownBy(() -> new RetryPolicy(2, Duration.ofSeconds(11), ignored -> true))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
