package io.urbanflow.tracking.event;

/** Signals an event schema major version that this consumer cannot process safely. */
public class UnsupportedEventSchemaException extends RuntimeException {

  /** Creates a schema rejection without exposing the event body. */
  public UnsupportedEventSchemaException(String schemaVersion) {
    super("Unsupported shipment event schema: " + schemaVersion);
  }
}
