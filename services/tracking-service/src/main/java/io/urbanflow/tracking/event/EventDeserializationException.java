package io.urbanflow.tracking.event;

/** Signals that a Kafka record cannot be mapped to the supported event contract. */
public class EventDeserializationException extends RuntimeException {

  /** Creates an exception without retaining the event payload in its message. */
  public EventDeserializationException(Throwable cause) {
    super("Shipment event is malformed", cause);
  }
}
