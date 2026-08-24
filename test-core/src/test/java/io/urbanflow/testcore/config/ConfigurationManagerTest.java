package io.urbanflow.testcore.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class ConfigurationManagerTest {

  @Test
  void resolvesSafeLocalDefaults() {
    EnvironmentConfig config = new ConfigurationManager(Map.of(), new Properties()).resolve();

    assertThat(config.environment()).isEqualTo("local");
    assertThat(config.customerPortalUrl()).isEqualTo(URI.create("http://localhost:3001"));
    assertThat(config.browser()).isEqualTo("chrome");
    assertThat(config.headless()).isTrue();
    assertThat(config.remoteUrl()).isNull();
    assertThat(config.parallelism()).isEqualTo(4);
    assertThat(config.timeout()).isEqualTo(Duration.ofSeconds(20));
    assertThat(config.cleanupEnabled()).isTrue();
  }

  @Test
  void environmentOverridesDefaults() {
    EnvironmentConfig config =
        new ConfigurationManager(
                Map.of(
                    "BROWSER", "firefox",
                    "HEADLESS", "false",
                    "PARALLELISM", "8",
                    "REMOTE_URL", "http://grid:4444"),
                new Properties())
            .resolve();

    assertThat(config.browser()).isEqualTo("firefox");
    assertThat(config.headless()).isFalse();
    assertThat(config.parallelism()).isEqualTo(8);
    assertThat(config.remoteUrl()).isEqualTo(URI.create("http://grid:4444"));
  }

  @Test
  void systemPropertiesTakePrecedenceOverEnvironment() {
    Properties properties = new Properties();
    properties.setProperty("browser", "edge");
    properties.setProperty("parallelism", "2");

    EnvironmentConfig config =
        new ConfigurationManager(Map.of("BROWSER", "firefox", "PARALLELISM", "9"), properties)
            .resolve();

    assertThat(config.browser()).isEqualTo("edge");
    assertThat(config.parallelism()).isEqualTo(2);
  }

  @Test
  void rejectsNonBooleanValue() {
    assertThatThrownBy(
            () ->
                new ConfigurationManager(Map.of("HEADLESS", "sometimes"), new Properties())
                    .resolve())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("headless");
  }

  @Test
  void rejectsNonNumericParallelism() {
    assertThatThrownBy(
            () ->
                new ConfigurationManager(Map.of("PARALLELISM", "many"), new Properties()).resolve())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("parallelism");
  }
}
