package io.urbanflow.identity.application;

/** Generic authentication failure that avoids account enumeration. */
public class InvalidCredentialsException extends RuntimeException {

  /** Creates the enumeration-safe credential failure. */
  public InvalidCredentialsException() {
    super("email or password is invalid");
  }
}
