package io.urbanflow.testcore.context;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Thread-safe state owned by one scenario rather than by the test JVM. */
public final class ScenarioContext {

  private final Map<ContextKey<?>, Object> values = new ConcurrentHashMap<>();

  /** Stores a non-null typed value. */
  public <T> void put(ContextKey<T> key, T value) {
    if (!key.type().isInstance(value)) {
      throw new IllegalArgumentException("value does not match context key type " + key.type());
    }
    values.put(key, value);
  }

  /** Returns an optional typed value. */
  public <T> Optional<T> find(ContextKey<T> key) {
    return Optional.ofNullable(key.type().cast(values.get(key)));
  }

  /** Returns a required typed value. */
  public <T> T required(ContextKey<T> key) {
    return find(key)
        .orElseThrow(() -> new IllegalStateException("scenario value is missing: " + key.name()));
  }

  /** Removes all scenario-owned values during cleanup. */
  public void clear() {
    values.clear();
  }

  /** Returns the number of currently owned values. */
  public int size() {
    return values.size();
  }
}
