package io.urbanflow.routing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.urbanflow.routing.domain.Courier;
import io.urbanflow.routing.domain.DeliveryRoute;
import io.urbanflow.routing.domain.RouteStatus;
import io.urbanflow.routing.persistence.CourierRepository;
import io.urbanflow.routing.persistence.DeliveryRouteRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RouteLifecycleServiceTest {

  private static final Instant NOW = Instant.parse("2026-08-24T12:30:00Z");
  private static final UUID ROUTE_ID = UUID.fromString("547f06cf-c1dd-4ad0-9780-359af05811b9");
  private static final UUID COURIER_ID = UUID.fromString("6be406c1-022e-4d36-bb5c-2da15e311fb0");

  @Mock private DeliveryRouteRepository routes;
  @Mock private CourierRepository couriers;

  @Test
  void findsRouteAndReturnsImmutableAssignments() {
    DeliveryRoute route = route();
    when(routes.findById(ROUTE_ID)).thenReturn(Optional.of(route));
    when(routes.findByCourierIdOrderByCreatedAtAsc(COURIER_ID)).thenReturn(List.of(route));

    assertThat(service().find(ROUTE_ID)).isSameAs(route);
    assertThat(service().assignments(COURIER_ID)).containsExactly(route);
  }

  @Test
  void startsPlannedRoute() {
    DeliveryRoute route = route();
    when(routes.findById(ROUTE_ID)).thenReturn(Optional.of(route));
    when(routes.save(route)).thenReturn(route);

    assertThat(service().start(ROUTE_ID, 0).status()).isEqualTo(RouteStatus.STARTED);
    verify(routes).save(route);
  }

  @Test
  void completesRouteAndReleasesCourierCapacity() {
    DeliveryRoute route = route();
    route.start(0, NOW.minusSeconds(1));
    Courier courier = Courier.register(COURIER_ID, "Courier", 2, NOW.minusSeconds(2));
    courier.assign(NOW.minusSeconds(2));
    when(routes.findById(ROUTE_ID)).thenReturn(Optional.of(route));
    when(couriers.findById(COURIER_ID)).thenReturn(Optional.of(courier));
    when(routes.save(route)).thenReturn(route);

    assertThat(service().complete(ROUTE_ID, 0).status()).isEqualTo(RouteStatus.COMPLETED);
    assertThat(courier.activeAssignments()).isZero();
    verify(couriers).save(courier);
  }

  @Test
  void rejectsUnknownRoute() {
    when(routes.findById(ROUTE_ID)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service().find(ROUTE_ID)).isInstanceOf(RouteNotFoundException.class);
  }

  private RouteLifecycleService service() {
    return new RouteLifecycleService(routes, couriers, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private static DeliveryRoute route() {
    return DeliveryRoute.plan(
        ROUTE_ID,
        UUID.fromString("7d38bce9-4f03-4269-b1db-a19fc9358679"),
        COURIER_ID,
        NOW.minusSeconds(10));
  }
}
