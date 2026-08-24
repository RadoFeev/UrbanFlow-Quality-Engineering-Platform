package io.urbanflow.testcore.resilience;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/** Bounded retry policy restricted to classified infrastructure failures. */
public final class RetryPolicy {

  private final int maximumAttempts;
  private final Duration baseDelay;
  private final Predicate<Throwable> classifier;

  /** Creates a bounded policy with explicit failure classification. */
  public RetryPolicy(int maximumAttempts, Duration baseDelay, Predicate<Throwable> classifier) {
    if (maximumAttempts < 1 || maximumAttempts > 3) {
      throw new IllegalArgumentException("maximumAttempts must be between 1 and 3");
    }
    if (baseDelay == null
        || baseDelay.isNegative()
        || baseDelay.compareTo(Duration.ofSeconds(10)) > 0) {
      throw new IllegalArgumentException("baseDelay must be between PT0S and PT10S");
    }
    this.maximumAttempts = maximumAttempts;
    this.baseDelay = baseDelay;
    this.classifier = Objects.requireNonNull(classifier);
  }

  /** Creates the default infrastructure-only retry policy. */
  public static RetryPolicy infrastructureOnly() {
    return new RetryPolicy(2, Duration.ofMillis(250), InfrastructureFailure.class::isInstance);
  }

  /** Executes a callable, never retrying product or assertion failures. */
  public <T> T execute(Callable<T> operation) throws Exception {
    Throwable last = null;
    for (int attempt = 1; attempt <= maximumAttempts; attempt++) {
      try {
        return operation.call();
      } catch (Throwable failure) {
        last = failure;
        if (attempt == maximumAttempts || !classifier.test(failure)) {
          rethrow(failure);
        }
        Duration delay = baseDelay.multipliedBy(1L << (attempt - 1));
        CompletableFuture.runAsync(
                () -> {},
                CompletableFuture.delayedExecutor(delay.toMillis(), TimeUnit.MILLISECONDS))
            .join();
      }
    }
    throw new IllegalStateException("retry policy exhausted without a result", last);
  }

  private static void rethrow(Throwable failure) throws Exception {
    if (failure instanceof Exception exception) {
      throw exception;
    }
    throw (Error) failure;
  }
}
