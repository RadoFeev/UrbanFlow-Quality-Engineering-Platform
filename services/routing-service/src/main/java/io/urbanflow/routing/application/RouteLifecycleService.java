package io.urbanflow.routing.application;

import io.urbanflow.routing.domain.Courier;
import io.urbanflow.routing.domain.DeliveryRoute;
import io.urbanflow.routing.persistence.CourierRepository;
import io.urbanflow.routing.persistence.DeliveryRouteRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Route reads and version-checked start/completion workflows. */
@Service
public class RouteLifecycleService {

  private final DeliveryRouteRepository routes;
  private final CourierRepository couriers;
  private final Clock clock;

  /** Creates the route lifecycle workflow. */
  public RouteLifecycleService(
      DeliveryRouteRepository routes, CourierRepository couriers, Clock clock) {
    this.routes = routes;
    this.couriers = couriers;
    this.clock = clock;
  }

  /** Returns one route. */
  @Transactional(readOnly = true)
  public DeliveryRoute find(UUID id) {
    return routes.findById(id).orElseThrow(() -> new RouteNotFoundException(id));
  }

  /** Returns a courier's stable assignment list. */
  @Transactional(readOnly = true)
  public List<DeliveryRoute> assignments(UUID courierId) {
    return List.copyOf(routes.findByCourierIdOrderByCreatedAtAsc(courierId));
  }

  /** Starts a route with explicit optimistic concurrency. */
  @Transactional
  public DeliveryRoute start(UUID id, long expectedVersion) {
    DeliveryRoute route = find(id);
    route.start(expectedVersion, clock.instant());
    return routes.save(route);
  }

  /** Completes a route and releases courier capacity in the same transaction. */
  @Transactional
  public DeliveryRoute complete(UUID id, long expectedVersion) {
    DeliveryRoute route = find(id);
    route.complete(expectedVersion, clock.instant());
    Courier courier = couriers.findById(route.courierId()).orElseThrow();
    courier.release(clock.instant());
    couriers.save(courier);
    return routes.save(route);
  }
}
