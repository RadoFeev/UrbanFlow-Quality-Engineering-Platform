package io.urbanflow.warehouse.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IncidentTest {

  private static final Instant NOW = Instant.parse("2026-08-24T10:00:00Z");

  @Test
  void opensIncident() {
    UUID shipmentId = UUID.randomUUID();
    Incident incident = Incident.open(UUID.randomUUID(), shipmentId, "DELAY", " Hub delay ", NOW);

    assertThat(incident.shipmentId()).isEqualTo(shipmentId);
    assertThat(incident.type()).isEqualTo("DELAY");
    assertThat(incident.description()).isEqualTo("Hub delay");
    assertThat(incident.status()).isEqualTo(IncidentStatus.OPEN);
    assertThat(incident.resolvedAt()).isNull();
  }

  @Test
  void resolvesOpenIncident() {
    Incident incident = Incident.open(UUID.randomUUID(), UUID.randomUUID(), "DELAY", "Delay", NOW);

    incident.resolve(NOW.plusSeconds(30));

    assertThat(incident.status()).isEqualTo(IncidentStatus.RESOLVED);
    assertThat(incident.resolvedAt()).isEqualTo(NOW.plusSeconds(30));
  }

  @Test
  void preventsRepeatedResolution() {
    Incident incident = Incident.open(UUID.randomUUID(), UUID.randomUUID(), "DELAY", "Delay", NOW);
    incident.resolve(NOW.plusSeconds(1));

    assertThatThrownBy(() -> incident.resolve(NOW.plusSeconds(2)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("already resolved");
  }

  @Test
  void rejectsMissingShipment() {
    assertThatThrownBy(() -> Incident.open(UUID.randomUUID(), null, "DELAY", "Delay", NOW))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void rejectsBlankType() {
    assertThatThrownBy(() -> Incident.open(UUID.randomUUID(), UUID.randomUUID(), " ", "Delay", NOW))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsOverlongDescription() {
    assertThatThrownBy(
            () ->
                Incident.open(UUID.randomUUID(), UUID.randomUUID(), "DELAY", "x".repeat(1001), NOW))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
