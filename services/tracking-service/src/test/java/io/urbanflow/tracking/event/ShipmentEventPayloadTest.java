package io.urbanflow.tracking.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.urbanflow.tracking.domain.TrackingStatus;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ShipmentEventPayloadTest {

  private static final UUID SHIPMENT_ID = UUID.fromString("6feb810a-97a6-47f9-824d-2d5562d3335a");

  @Test
  void acceptsValidConsumerPayload() {
    ShipmentEventPayload payload =
        new ShipmentEventPayload(SHIPMENT_ID, "UF1123456789ABCDEF01", TrackingStatus.CREATED, 0);

    assertThat(payload.shipmentId()).isEqualTo(SHIPMENT_ID);
    assertThat(payload.trackingNumber()).isEqualTo("UF1123456789ABCDEF01");
    assertThat(payload.status()).isEqualTo(TrackingStatus.CREATED);
  }

  @ParameterizedTest
  @MethodSource("invalidTrackingNumbers")
  void rejectsInvalidTrackingNumberPartitions(String trackingNumber) {
    assertThatThrownBy(
            () -> new ShipmentEventPayload(SHIPMENT_ID, trackingNumber, TrackingStatus.CREATED, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("trackingNumber");
  }

  @Test
  void rejectsNegativeAggregateVersion() {
    assertThatThrownBy(
            () ->
                new ShipmentEventPayload(
                    SHIPMENT_ID, "UF1123456789ABCDEF01", TrackingStatus.CREATED, -1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("version");
  }

  private static Stream<String> invalidTrackingNumbers() {
    return Stream.of("", "UF123", "uf1123456789abcdef01", "XX1123456789ABCDEF01", null);
  }
}
