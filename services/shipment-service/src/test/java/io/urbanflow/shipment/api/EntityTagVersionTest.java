package io.urbanflow.shipment.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class EntityTagVersionTest {

  @ParameterizedTest
  @CsvSource({"0,0", "12,12", "'\"42\"',42", "'W/\"7\"',7"})
  void parsesSupportedVersionRepresentations(String input, long expected) {
    assertThat(EntityTagVersion.parse(input)).isEqualTo(expected);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "abc", "\"broken", "-1"})
  void rejectsInvalidVersionRepresentations(String input) {
    assertThatThrownBy(() -> EntityTagVersion.parse(input))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
