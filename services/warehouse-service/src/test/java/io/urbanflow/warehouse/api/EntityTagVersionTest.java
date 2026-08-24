package io.urbanflow.warehouse.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EntityTagVersionTest {

  @Test
  void parsesAndFormatsStrongVersion() {
    assertThat(EntityTagVersion.parse(" \"v42\" ")).isEqualTo(42);
    assertThat(EntityTagVersion.format(42)).isEqualTo("\"v42\"");
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "42", "v42", "W/\"v42\"", "\"42\"", "\"v-1\""})
  void rejectsMalformedEntityTags(String value) {
    assertThatThrownBy(() -> EntityTagVersion.parse(value))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsVersionOverflow() {
    assertThatThrownBy(() -> EntityTagVersion.parse("\"v999999999999999999999999\""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("too large");
  }
}
