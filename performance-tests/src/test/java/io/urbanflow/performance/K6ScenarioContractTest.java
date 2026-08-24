package io.urbanflow.performance;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Guards the executable k6 inventory and per-scenario quality gates. */
@Tag("performance")
class K6ScenarioContractTest {

  @ParameterizedTest(name = "k6 scenario {0} owns an exported journey and thresholds")
  @MethodSource("scenarios")
  void scenarioIsExecutableAndThresholded(String scenario, String endpointMarker)
      throws IOException {
    String script = Files.readString(script());

    assertThat(script).contains("'" + scenario + "'");
    assertThat(script).contains("export function " + scenario + "()");
    assertThat(script).contains(endpointMarker);
    assertThat(script).contains("http_req_failed{scenario:${name}}");
    assertThat(script).contains("http_req_duration{scenario:${name}}");
    assertThat(script).contains("checks{scenario:${name}}");
  }

  private static Stream<Arguments> scenarios() {
    return Stream.of(
        Arguments.of("identityHealth", "health(origins.identity"),
        Arguments.of("identityAuthBoundary", "identity.auth-boundary"),
        Arguments.of("shipmentHealth", "health(origins.shipment"),
        Arguments.of("shipmentReadBoundary", "shipment.read-boundary"),
        Arguments.of("trackingHealth", "health(origins.tracking"),
        Arguments.of("publicTrackingRead", "tracking.public-read"),
        Arguments.of("routingHealth", "health(origins.routing"),
        Arguments.of("notificationHealth", "health(origins.notification"),
        Arguments.of("warehouseHealth", "health(origins.warehouse"),
        Arguments.of("warehouseSearch", "warehouse.search"));
  }

  private static Path script() {
    return Path.of(System.getProperty("workspace.root"), "performance-tests", "k6", "urbanflow.js");
  }
}
