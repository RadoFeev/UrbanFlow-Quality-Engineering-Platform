package io.urbanflow.warehouse.application;

/** Indicates a stale optimistic entity version. */
public class VersionConflictException extends RuntimeException {

  /** Creates a stable conflict error. */
  public VersionConflictException(long expected, long actual) {
    super("expected version " + expected + " but found " + actual);
  }
}
