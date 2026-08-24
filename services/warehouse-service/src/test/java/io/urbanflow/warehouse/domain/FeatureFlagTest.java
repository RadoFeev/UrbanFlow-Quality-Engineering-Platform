package io.urbanflow.warehouse.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FeatureFlagTest {

  private static final Instant NOW = Instant.parse("2026-08-24T10:00:00Z");

  @Test
  void normalizesKeyAndPreservesState() {
    FeatureFlag flag = FeatureFlag.create(UUID.randomUUID(), " New.Flow ", " Rollout ", true, NOW);

    assertThat(flag.key()).isEqualTo("new.flow");
    assertThat(flag.description()).isEqualTo("Rollout");
    assertThat(flag.enabled()).isTrue();
    assertThat(flag.updatedAt()).isEqualTo(NOW);
  }

  @Test
  void changesStateAndTimestamp() {
    FeatureFlag flag = FeatureFlag.create(UUID.randomUUID(), "new.flow", "Rollout", false, NOW);

    flag.setEnabled(true, NOW.plusSeconds(5));

    assertThat(flag.enabled()).isTrue();
    assertThat(flag.updatedAt()).isEqualTo(NOW.plusSeconds(5));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "ab", "9invalid", "has space", "UPPER_CASE", "x/y"})
  void rejectsInvalidKeys(String key) {
    assertThatThrownBy(() -> FeatureFlag.create(UUID.randomUUID(), key, "Valid", false, NOW))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsMissingKey() {
    assertThatThrownBy(() -> FeatureFlag.create(UUID.randomUUID(), null, "Valid", false, NOW))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsBlankDescription() {
    assertThatThrownBy(() -> FeatureFlag.create(UUID.randomUUID(), "new.flow", " ", false, NOW))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
