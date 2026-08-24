package io.urbanflow.orchestrator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UnifiedReportWriterTest {

  @TempDir Path workspace;

  @Test
  void writesAllureEnvironmentAndExecutorMetadata() throws Exception {
    OrchestratorConfig config = config(workspace.resolve("target/report"));
    Path results =
        new UnifiedReportWriter(Clock.fixed(Instant.parse("2026-08-24T10:00:00Z"), ZoneOffset.UTC))
            .write(workspace, config);

    assertThat(Files.readString(results.resolve("environment.properties")))
        .contains("Environment=local", "Suite=API", "Browser=chrome");
    assertThat(Files.readString(results.resolve("executor.json")))
        .contains("UrbanFlow Test Orchestrator", "2026-08-24T10:00:00Z");
    assertThat(Files.readString(results.resolve("categories.json")))
        .contains("Infrastructure failure", "Assertion failure", "Product defect");
  }

  @Test
  void rejectsReportDirectoryOutsideWorkspace() {
    OrchestratorConfig config = config(workspace.getParent().resolve("outside-report"));
    assertThatThrownBy(() -> new UnifiedReportWriter(Clock.systemUTC()).write(workspace, config))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("workspace");
  }

  private static OrchestratorConfig config(Path report) {
    return new OrchestratorConfig(
        "local",
        Suite.API,
        "chrome",
        true,
        4,
        List.of("smoke"),
        0,
        URI.create("http://grid:4444"),
        false,
        true,
        report,
        false);
  }
}
