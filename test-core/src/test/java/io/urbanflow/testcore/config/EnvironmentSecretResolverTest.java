package io.urbanflow.testcore.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

class EnvironmentSecretResolverTest {

  @Test
  void resolvesDefensiveSecretArray() {
    EnvironmentSecretResolver resolver =
        new EnvironmentSecretResolver(Map.of("TEST_SECRET", "value"));

    char[] first = resolver.required("TEST_SECRET");
    first[0] = 'X';

    assertThat(resolver.required("TEST_SECRET")).containsExactly('v', 'a', 'l', 'u', 'e');
  }

  @Test
  void reportsOnlyMissingSecretName() {
    assertThatThrownBy(() -> new EnvironmentSecretResolver(Map.of()).required("MISSING_SECRET"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("required secret is not configured: MISSING_SECRET");
  }

  @Test
  void rejectsBlankSecretName() {
    assertThatThrownBy(() -> new EnvironmentSecretResolver(Map.of()).required(" "))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
