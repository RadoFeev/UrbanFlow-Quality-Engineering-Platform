package io.urbanflow.identity.application;

/** Signals a locked or administratively disabled account. */
public class AccountUnavailableException extends RuntimeException {

  /** Creates the enumeration-safe account-state failure. */
  public AccountUnavailableException() {
    super("account is temporarily unavailable");
  }
}
