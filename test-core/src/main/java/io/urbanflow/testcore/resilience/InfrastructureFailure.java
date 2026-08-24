package io.urbanflow.testcore.resilience;

/** Marks a proven transient infrastructure failure eligible for bounded retry. */
public class InfrastructureFailure extends RuntimeException {

  /** Wraps the infrastructure cause. */
  public InfrastructureFailure(String message, Throwable cause) {
    super(message, cause);
  }

  /** Creates an infrastructure failure without a nested cause. */
  public InfrastructureFailure(String message) {
    super(message);
  }
}
