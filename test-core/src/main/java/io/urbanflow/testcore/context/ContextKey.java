package io.urbanflow.testcore.context;

/** Typed scenario-context key that prevents stringly typed test state. */
public record ContextKey<T>(String name, Class<T> type) {

  /** Validates the key. */
  public ContextKey {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("context key name must not be blank");
    }
    if (type == null) {
      throw new IllegalArgumentException("context key type must not be null");
    }
  }
}
