package io.urbanflow.identity.application;

/** Signals an explicit optimistic version mismatch before mutation. */
public class VersionConflictException extends RuntimeException {

  /** Creates an explicit expected-versus-actual failure. */
  public VersionConflictException(long expected, long actual) {
    super("user version conflict: expected " + expected + " but was " + actual);
  }
}
