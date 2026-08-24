package io.urbanflow.routing.api;

import io.urbanflow.routing.domain.DeliveryRoute;
import io.urbanflow.routing.domain.RouteStatus;
import java.time.Instant;
import java.util.UUID;

/** Public route representation. */
public record RouteResponse(
    UUID id,
    UUID shipmentId,
    UUID courierId,
    RouteStatus status,
    Instant createdAt,
    Instant startedAt,
    Instant completedAt,
    long version) {

  /** Maps a route aggregate without exposing persistence internals. */
  public static RouteResponse from(DeliveryRoute route) {
    return new RouteResponse(
        route.id(),
        route.shipmentId(),
        route.courierId(),
        route.status(),
        route.createdAt(),
        route.startedAt(),
        route.completedAt(),
        route.version());
  }
}
