package io.urbanflow.testcore.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EnvironmentConfigTest {

  @ParameterizedTest
  @ValueSource(strings = {"safari", "", "CHROME_BETA"})
  void rejectsUnsupportedBrowser(String browser) {
    assertThatThrownBy(() -> config(browser, 4, Duration.ofSeconds(20)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @ValueSource(ints = {Integer.MIN_VALUE, 0, 65, Integer.MAX_VALUE})
  void rejectsUnsafeParallelism(int parallelism) {
    assertThatThrownBy(() -> config("chrome", parallelism, Duration.ofSeconds(20)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"PT0S", "PT-1S", "PT5M1S"})
  void rejectsUnsafeTimeout(String timeout) {
    assertThatThrownBy(() -> config("chrome", 4, Duration.parse(timeout)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static EnvironmentConfig config(String browser, int parallelism, Duration timeout) {
    URI local = URI.create("http://localhost:8080");
    return new EnvironmentConfig(
        "local",
        local,
        local,
        local,
        local,
        local,
        local,
        local,
        local,
        local,
        browser,
        true,
        null,
        parallelism,
        timeout,
        false,
        true);
  }
}
