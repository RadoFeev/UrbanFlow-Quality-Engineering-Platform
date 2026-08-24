package io.urbanflow.warehouse.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class WarehouseTest {

  private static final Instant NOW = Instant.parse("2026-08-24T10:00:00Z");

  @Test
  void createsNormalizedActiveWarehouse() {
    Warehouse warehouse =
        Warehouse.create(UUID.randomUUID(), " sof01 ", " Sofia ", " Sofia ", 500, NOW);

    assertThat(warehouse.code()).isEqualTo("SOF01");
    assertThat(warehouse.name()).isEqualTo("Sofia");
    assertThat(warehouse.city()).isEqualTo("Sofia");
    assertThat(warehouse.capacity()).isEqualTo(500);
    assertThat(warehouse.active()).isTrue();
    assertThat(warehouse.createdAt()).isEqualTo(NOW);
  }

  @Test
  void updatesMutableFieldsAndTimestamp() {
    Warehouse warehouse = Warehouse.create(UUID.randomUUID(), "SOF01", "Old", "Sofia", 10, NOW);
    Instant later = NOW.plusSeconds(60);

    warehouse.update("New", "Plovdiv", 20, false, later);

    assertThat(warehouse.name()).isEqualTo("New");
    assertThat(warehouse.city()).isEqualTo("Plovdiv");
    assertThat(warehouse.capacity()).isEqualTo(20);
    assertThat(warehouse.active()).isFalse();
    assertThat(warehouse.updatedAt()).isEqualTo(later);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "A", "SOF-01", "code with spaces", "abcdefghijklmnopqrstu"})
  void rejectsInvalidCodes(String code) {
    assertThatThrownBy(() -> Warehouse.create(UUID.randomUUID(), code, "Name", "City", 1, NOW))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @ValueSource(ints = {Integer.MIN_VALUE, -1, 0, 1_000_001, Integer.MAX_VALUE})
  void rejectsCapacityOutsideOperationalBounds(int capacity) {
    assertThatThrownBy(
            () -> Warehouse.create(UUID.randomUUID(), "SOF01", "Name", "City", capacity, NOW))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void acceptsCapacityBoundaryValues() {
    assertThat(Warehouse.create(UUID.randomUUID(), "AA", "A", "A", 1, NOW).capacity()).isEqualTo(1);
    assertThat(Warehouse.create(UUID.randomUUID(), "ZZ99", "Z", "Z", 1_000_000, NOW).capacity())
        .isEqualTo(1_000_000);
  }
}
