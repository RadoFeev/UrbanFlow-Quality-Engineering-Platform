package io.urbanflow.routing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeliveryRouteTest {

  private static final Instant NOW = Instant.parse("2026-08-24T12:00:00Z");

  @Test
  void plansStartsAndCompletesValidLifecycle() {
    DeliveryRoute route = route();
    assertThat(route.status()).isEqualTo(RouteStatus.PLANNED);

    route.start(0, NOW.plusSeconds(1));
    assertThat(route.status()).isEqualTo(RouteStatus.STARTED);
    assertThat(route.startedAt()).isEqualTo(NOW.plusSeconds(1));

    route.complete(0, NOW.plusSeconds(2));
    assertThat(route.status()).isEqualTo(RouteStatus.COMPLETED);
    assertThat(route.completedAt()).isEqualTo(NOW.plusSeconds(2));
  }

  @Test
  void rejectsInvalidStateTransitions() {
    DeliveryRoute route = route();
    assertThatThrownBy(() -> route.complete(0, NOW)).isInstanceOf(IllegalStateException.class);

    route.start(0, NOW);
    assertThatThrownBy(() -> route.start(0, NOW.plusSeconds(1)))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void rejectsStaleVersionBeforeMutation() {
    DeliveryRoute route = route();

    assertThatThrownBy(() -> route.start(9, NOW))
        .isInstanceOf(RouteVersionConflictException.class)
        .hasMessageContaining("expected version 9");
    assertThat(route.status()).isEqualTo(RouteStatus.PLANNED);
  }

  @Test
  void exposesStableAssignmentIdentity() {
    DeliveryRoute route = route();
    assertThat(route.id()).isEqualTo(UUID.fromString("547f06cf-c1dd-4ad0-9780-359af05811b9"));
    assertThat(route.shipmentId())
        .isEqualTo(UUID.fromString("7d38bce9-4f03-4269-b1db-a19fc9358679"));
    assertThat(route.courierId())
        .isEqualTo(UUID.fromString("6be406c1-022e-4d36-bb5c-2da15e311fb0"));
    assertThat(route.createdAt()).isEqualTo(NOW);
    assertThat(route.version()).isZero();
  }

  private static DeliveryRoute route() {
    return DeliveryRoute.plan(
        UUID.fromString("547f06cf-c1dd-4ad0-9780-359af05811b9"),
        UUID.fromString("7d38bce9-4f03-4269-b1db-a19fc9358679"),
        UUID.fromString("6be406c1-022e-4d36-bb5c-2da15e311fb0"),
        NOW);
  }
}
