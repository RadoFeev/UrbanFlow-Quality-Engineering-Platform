package io.urbanflow.identity.security;

/** Signals an untrusted, malformed, expired, or otherwise invalid access token. */
public class TokenValidationException extends RuntimeException {

  /** Creates a validation failure without a nested cause. */
  public TokenValidationException(String message) {
    super(message);
  }

  /** Creates a validation failure while retaining the low-level cause for diagnostics. */
  public TokenValidationException(String message, Throwable cause) {
    super(message, cause);
  }
}
