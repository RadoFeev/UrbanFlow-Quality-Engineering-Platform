package io.urbanflow.routing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class CourierTest {

  private static final Instant NOW = Instant.parse("2026-08-24T12:00:00Z");

  @Test
  void registersAvailableCourierWithCapacity() {
    Courier courier = courier(2);

    assertThat(courier.status()).isEqualTo(CourierStatus.AVAILABLE);
    assertThat(courier.maximumAssignments()).isEqualTo(2);
    assertThat(courier.activeAssignments()).isZero();
    assertThat(courier.isAssignable()).isTrue();
  }

  @Test
  void reservesUntilCapacityAndThenRejects() {
    Courier courier = courier(2);

    courier.assign(NOW.plusSeconds(1));
    courier.assign(NOW.plusSeconds(2));

    assertThat(courier.activeAssignments()).isEqualTo(2);
    assertThat(courier.isAssignable()).isFalse();
    assertThatThrownBy(() -> courier.assign(NOW.plusSeconds(3)))
        .isInstanceOf(CourierCapacityException.class);
  }

  @Test
  void releasesReservedCapacityWithoutGoingNegative() {
    Courier courier = courier(1);
    courier.assign(NOW);

    courier.release(NOW.plusSeconds(1));

    assertThat(courier.activeAssignments()).isZero();
    assertThat(courier.isAssignable()).isTrue();
    assertThatThrownBy(() -> courier.release(NOW.plusSeconds(2)))
        .isInstanceOf(IllegalStateException.class);
  }

  @ParameterizedTest
  @MethodSource("invalidCapacities")
  void rejectsCapacityOutsideOperationalBounds(int capacity) {
    assertThatThrownBy(() -> courier(capacity))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("maximumAssignments");
  }

  @Test
  void rejectsBlankDisplayName() {
    assertThatThrownBy(() -> Courier.register(UUID.randomUUID(), " ", 1, NOW))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("displayName");
  }

  private static Stream<Integer> invalidCapacities() {
    return Stream.of(-1, 0, 101);
  }

  private static Courier courier(int capacity) {
    return Courier.register(
        UUID.fromString("6be406c1-022e-4d36-bb5c-2da15e311fb0"), "Courier One", capacity, NOW);
  }
}
