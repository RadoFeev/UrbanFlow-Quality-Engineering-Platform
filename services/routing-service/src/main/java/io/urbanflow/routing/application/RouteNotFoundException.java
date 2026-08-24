package io.urbanflow.routing.application;

import java.util.UUID;

/** Signals an unknown route identifier. */
public class RouteNotFoundException extends RuntimeException {

  /** Creates a route lookup failure. */
  public RouteNotFoundException(UUID id) {
    super("route not found: " + id);
  }
}
