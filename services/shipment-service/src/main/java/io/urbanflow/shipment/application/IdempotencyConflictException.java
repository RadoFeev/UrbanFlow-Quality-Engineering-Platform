package io.urbanflow.shipment.application;

/** Indicates that an idempotency key was reused for a different request body. */
public final class IdempotencyConflictException extends RuntimeException {

  private final String idempotencyKey;

  /** Creates a conflict for the reused key. */
  public IdempotencyConflictException(String idempotencyKey) {
    super("Idempotency key was already used with a different request");
    this.idempotencyKey = idempotencyKey;
  }

  /** Returns the reused idempotency key. */
  public String idempotencyKey() {
    return idempotencyKey;
  }
}
