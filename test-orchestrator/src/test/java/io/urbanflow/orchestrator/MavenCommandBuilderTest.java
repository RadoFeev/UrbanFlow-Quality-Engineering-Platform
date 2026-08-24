package io.urbanflow.orchestrator;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class MavenCommandBuilderTest {

  @Test
  void buildsApiModuleCommand() {
    List<String> command = new MavenCommandBuilder().build(config(Suite.API, null, List.of()));
    assertThat(command)
        .containsSubsequence("clean", "verify", "-pl", ":api-tests", "-am")
        .contains("-Dgroups=api", "-Dbrowser=chrome", "-Dheadless=true");
  }

  @Test
  void combinesSuiteAndAdditionalTags() {
    assertThat(new MavenCommandBuilder().build(config(Suite.UI, null, List.of("smoke", "mobile"))))
        .contains("-Dgroups=ui,smoke,mobile");
  }

  @Test
  void includesRemoteGridOnlyWhenConfigured() {
    List<String> local = new MavenCommandBuilder().build(config(Suite.SMOKE, null, List.of()));
    List<String> remote =
        new MavenCommandBuilder()
            .build(config(Suite.SMOKE, URI.create("http://grid:4444"), List.of()));
    assertThat(local).noneMatch(argument -> argument.startsWith("-Dremote.url="));
    assertThat(remote).contains("-Dremote.url=http://grid:4444");
  }

  @Test
  void allSuiteDoesNotAddGroupFilter() {
    assertThat(new MavenCommandBuilder().build(config(Suite.ALL, null, List.of())))
        .noneMatch(argument -> argument.startsWith("-Dgroups="));
  }

  @Test
  void qualifiesWrapperAgainstTheWorkspace() {
    Path workspace = Path.of("build-workspace").toAbsolutePath();
    List<String> command =
        new MavenCommandBuilder().build(config(Suite.API, null, List.of()), workspace);
    assertThat(command).anyMatch(argument -> argument.startsWith(workspace.toString()));
  }

  private static OrchestratorConfig config(Suite suite, URI remote, List<String> tags) {
    return new OrchestratorConfig(
        "local",
        suite,
        "chrome",
        true,
        4,
        tags,
        0,
        remote,
        false,
        true,
        Path.of("target/unified-report"),
        false);
  }
}
