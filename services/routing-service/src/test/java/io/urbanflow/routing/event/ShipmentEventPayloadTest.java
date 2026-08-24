package io.urbanflow.routing.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ShipmentEventPayloadTest {

  private static final UUID SHIPMENT_ID = UUID.fromString("7d38bce9-4f03-4269-b1db-a19fc9358679");

  @Test
  void acceptsConfirmedPayload() {
    ShipmentEventPayload payload =
        new ShipmentEventPayload(SHIPMENT_ID, "UF0123456789ABCDEF01", "CONFIRMED", 1);
    assertThat(payload.shipmentId()).isEqualTo(SHIPMENT_ID);
  }

  @ParameterizedTest
  @MethodSource("invalidTrackingNumbers")
  void rejectsInvalidTrackingPartitions(String trackingNumber) {
    assertThatThrownBy(() -> new ShipmentEventPayload(SHIPMENT_ID, trackingNumber, "CONFIRMED", 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("trackingNumber");
  }

  @Test
  void rejectsBlankStatusAndNegativeVersion() {
    assertThatThrownBy(() -> new ShipmentEventPayload(SHIPMENT_ID, "UF0123456789ABCDEF01", " ", 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("status");
    assertThatThrownBy(
            () -> new ShipmentEventPayload(SHIPMENT_ID, "UF0123456789ABCDEF01", "CONFIRMED", -1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("version");
  }

  private static Stream<String> invalidTrackingNumbers() {
    return Stream.of("", "UF123", "uf0123456789abcdef01", "XX0123456789ABCDEF01");
  }
}
