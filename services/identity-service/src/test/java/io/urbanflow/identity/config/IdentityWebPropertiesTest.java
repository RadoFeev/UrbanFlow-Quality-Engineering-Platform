package io.urbanflow.identity.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class IdentityWebPropertiesTest {

  @Test
  void copiesExplicitOrigins() {
    List<String> origins = new ArrayList<>(List.of("https://portal.urbanflow.test"));
    IdentityWebProperties properties = new IdentityWebProperties(origins);
    origins.clear();

    assertThat(properties.allowedOrigins()).containsExactly("https://portal.urbanflow.test");
  }

  @Test
  void rejectsEmptyAndWildcardOrigins() {
    assertThatThrownBy(() -> new IdentityWebProperties(List.of()))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new IdentityWebProperties(List.of("*")))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
