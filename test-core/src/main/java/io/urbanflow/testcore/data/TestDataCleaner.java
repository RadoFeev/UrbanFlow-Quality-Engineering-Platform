package io.urbanflow.testcore.data;

import java.util.ArrayDeque;
import java.util.Deque;

/** Executes registered cleanup operations in reverse creation order. */
public final class TestDataCleaner implements AutoCloseable {

  private final Deque<Cleanup> operations = new ArrayDeque<>();

  /** Registers a named cleanup action. */
  public synchronized void register(String description, Runnable action) {
    if (description == null || description.isBlank() || action == null) {
      throw new IllegalArgumentException("cleanup description and action are required");
    }
    operations.push(new Cleanup(description, action));
  }

  /** Returns pending cleanup count. */
  public synchronized int pending() {
    return operations.size();
  }

  /** Runs every cleanup even if an earlier cleanup fails. */
  @Override
  public synchronized void close() {
    RuntimeException aggregate = null;
    while (!operations.isEmpty()) {
      Cleanup cleanup = operations.pop();
      try {
        cleanup.action().run();
      } catch (RuntimeException exception) {
        if (aggregate == null) {
          aggregate = new IllegalStateException("test cleanup failed: " + cleanup.description());
        }
        aggregate.addSuppressed(exception);
      }
    }
    if (aggregate != null) {
      throw aggregate;
    }
  }

  private record Cleanup(String description, Runnable action) {}
}
