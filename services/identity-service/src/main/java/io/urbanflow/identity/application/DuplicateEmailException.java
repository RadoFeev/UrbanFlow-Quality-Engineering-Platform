package io.urbanflow.identity.application;

/** Signals a registration attempt for an existing canonical email. */
public class DuplicateEmailException extends RuntimeException {

  /** Creates the canonical email conflict. */
  public DuplicateEmailException() {
    super("an account with this email already exists");
  }
}
