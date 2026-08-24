package io.urbanflow.identity.application;

/** Signals a credential that does not satisfy the configured password policy. */
public class WeakPasswordException extends RuntimeException {

  /** Creates a password-policy failure. */
  public WeakPasswordException(String message) {
    super(message);
  }
}
