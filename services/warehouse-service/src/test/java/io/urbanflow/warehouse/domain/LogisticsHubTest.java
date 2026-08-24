package io.urbanflow.warehouse.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LogisticsHubTest {

  private static final Instant NOW = Instant.parse("2026-08-24T10:00:00Z");

  @Test
  void createsActiveHubForWarehouse() {
    UUID warehouseId = UUID.randomUUID();
    LogisticsHub hub =
        LogisticsHub.create(UUID.randomUUID(), warehouseId, " North ", " Sofia ", NOW);

    assertThat(hub.warehouseId()).isEqualTo(warehouseId);
    assertThat(hub.name()).isEqualTo("North");
    assertThat(hub.city()).isEqualTo("Sofia");
    assertThat(hub.active()).isTrue();
  }

  @Test
  void changesAvailabilityAndTimestamp() {
    LogisticsHub hub =
        LogisticsHub.create(UUID.randomUUID(), UUID.randomUUID(), "Hub", "City", NOW);

    hub.setActive(false, NOW.plusSeconds(1));

    assertThat(hub.active()).isFalse();
    assertThat(hub.updatedAt()).isEqualTo(NOW.plusSeconds(1));
  }

  @Test
  void rejectsMissingWarehouse() {
    assertThatThrownBy(() -> LogisticsHub.create(UUID.randomUUID(), null, "Hub", "City", NOW))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void rejectsBlankName() {
    assertThatThrownBy(
            () -> LogisticsHub.create(UUID.randomUUID(), UUID.randomUUID(), " ", "City", NOW))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsOverlongCity() {
    assertThatThrownBy(
            () ->
                LogisticsHub.create(
                    UUID.randomUUID(), UUID.randomUUID(), "Hub", "x".repeat(121), NOW))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
