package io.urbanflow.routing.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class EntityTagVersionTest {

  @Test
  void roundTripsSupportedBoundaries() {
    assertThat(EntityTagVersion.parse(EntityTagVersion.format(0))).isZero();
    assertThat(EntityTagVersion.parse(EntityTagVersion.format(Long.MAX_VALUE)))
        .isEqualTo(Long.MAX_VALUE);
  }

  @ParameterizedTest
  @MethodSource("invalidTags")
  void rejectsMalformedOrWeakTags(String value) {
    assertThatThrownBy(() -> EntityTagVersion.parse(value))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNegativeFormatVersion() {
    assertThatThrownBy(() -> EntityTagVersion.format(-1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static Stream<String> invalidTags() {
    return Stream.of("*", "W/\"v1\"", "v1", "\"v-1\"", "\"v999999999999999999999\"");
  }
}
