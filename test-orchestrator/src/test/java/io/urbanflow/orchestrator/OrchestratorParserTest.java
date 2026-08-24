package io.urbanflow.orchestrator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import org.junit.jupiter.api.Test;

class OrchestratorParserTest {

  @Test
  void resolvesSafeDefaults() {
    OrchestratorConfig config = new OrchestratorParser().parse();
    assertThat(config.environment()).isEqualTo("local");
    assertThat(config.suite()).isEqualTo(Suite.ALL);
    assertThat(config.browser()).isEqualTo("chrome");
    assertThat(config.headless()).isTrue();
    assertThat(config.parallelism()).isEqualTo(4);
    assertThat(config.execute()).isFalse();
  }

  @Test
  void parsesCompleteExecutionPlan() {
    OrchestratorConfig config =
        new OrchestratorParser()
            .parse(
                "--environment=staging",
                "--suite=ui",
                "--browser=firefox",
                "--headless=false",
                "--parallelism=8",
                "--tags=smoke,regression",
                "--retries=1",
                "--remote-url=http://grid:4444",
                "--video=true",
                "--cleanup=false",
                "--execute=true");

    assertThat(config.suite()).isEqualTo(Suite.UI);
    assertThat(config.browser()).isEqualTo("firefox");
    assertThat(config.tags()).containsExactly("smoke", "regression");
    assertThat(config.remoteUrl()).isEqualTo(URI.create("http://grid:4444"));
    assertThat(config.video()).isTrue();
    assertThat(config.cleanup()).isFalse();
    assertThat(config.execute()).isTrue();
  }

  @Test
  void rejectsUnknownOption() {
    assertThatThrownBy(() -> new OrchestratorParser().parse("--unknown=value"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("unknown");
  }

  @Test
  void rejectsDuplicateOption() {
    assertThatThrownBy(() -> new OrchestratorParser().parse("--suite=api", "--suite=ui"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("duplicate");
  }

  @Test
  void rejectsInvalidBoolean() {
    assertThatThrownBy(() -> new OrchestratorParser().parse("--headless=maybe"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("headless");
  }

  @Test
  void rejectsInvalidTag() {
    assertThatThrownBy(() -> new OrchestratorParser().parse("--tags=smoke,../unsafe"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("tag");
  }

  @Test
  void rejectsUnboundedRetry() {
    assertThatThrownBy(() -> new OrchestratorParser().parse("--retries=3"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("retries");
  }
}
