package io.urbanflow.identity.application;

/** Signals an unknown, revoked, expired, or reused refresh/reset credential. */
public class InvalidSessionException extends RuntimeException {

  /** Creates a session failure with a safe client-facing message. */
  public InvalidSessionException(String message) {
    super(message);
  }
}
