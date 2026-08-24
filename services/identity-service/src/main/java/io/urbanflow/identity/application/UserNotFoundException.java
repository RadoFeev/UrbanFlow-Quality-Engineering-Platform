package io.urbanflow.identity.application;

import java.util.UUID;

/** Signals an unknown user identifier. */
public class UserNotFoundException extends RuntimeException {

  /** Creates a failure for the requested identifier. */
  public UserNotFoundException(UUID id) {
    super("user not found: " + id);
  }
}
