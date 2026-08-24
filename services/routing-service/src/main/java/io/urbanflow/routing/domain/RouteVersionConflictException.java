package io.urbanflow.routing.domain;

import java.util.UUID;

/** Signals a stale route mutation. */
public class RouteVersionConflictException extends RuntimeException {

  /** Creates a route version conflict. */
  public RouteVersionConflictException(UUID id, long expected, long actual) {
    super("route " + id + " expected version " + expected + " but was " + actual);
  }
}
