package io.urbanflow.orchestrator;

import java.nio.file.Path;
import java.time.Clock;
import java.util.List;

/** Command-line entrypoint for unified suite selection and report configuration. */
public final class TestOrchestrator {

  private TestOrchestrator() {}

  /** Parses, reports, previews, and optionally executes a test plan. */
  public static void main(String[] args) throws Exception {
    OrchestratorConfig config = new OrchestratorParser().parse(args);
    Path workspace = Path.of("").toAbsolutePath().normalize();
    new UnifiedReportWriter(Clock.systemUTC()).write(workspace, config);
    List<String> command = new MavenCommandBuilder().build(config, workspace);
    System.out.println("Execution plan: " + String.join(" ", command));
    if (config.execute()) {
      int exitCode =
          new ProcessBuilder(command).directory(workspace.toFile()).inheritIO().start().waitFor();
      if (exitCode != 0) {
        throw new IllegalStateException("test execution failed with exit code " + exitCode);
      }
    }
  }
}
