package io.urbanflow.routing.event;

/** Signals a malformed shipment event value. */
public class RoutingEventDeserializationException extends RuntimeException {

  /** Creates a deserialization failure. */
  public RoutingEventDeserializationException(Throwable cause) {
    super("shipment event could not be deserialized", cause);
  }
}
