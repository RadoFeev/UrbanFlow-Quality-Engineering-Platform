package io.urbanflow.courier;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class CourierPortalAssetTest {

  @Test
  void constructsApplicationBoundary() {
    assertThat(new CourierPortalApplication()).isNotNull();
  }

  @Test
  void exposesRouteAndAssignmentContract() throws IOException {
    String html = resource("static/index.html");
    assertThat(html)
        .contains("data-testid=\"start-route\"")
        .contains("data-testid=\"assignment-list\"")
        .contains("data-testid=\"assignment-filter\"");
  }

  @Test
  void exposesOfflineSynchronizationContract() throws IOException {
    String html = resource("static/index.html");
    assertThat(html)
        .contains("data-testid=\"offline-toggle\"")
        .contains("data-testid=\"offline-queue-count\"")
        .contains("data-testid=\"sync-button\"");
  }

  @Test
  void centralizesAllowedCourierTransitions() throws IOException {
    String script = resource("static/app.js");
    assertThat(script)
        .contains("COURIER_ASSIGNED: [\"PICKUP_IN_PROGRESS\"]")
        .contains("OUT_FOR_DELIVERY: [\"DELIVERED\", \"DELIVERY_ATTEMPTED\"]")
        .contains("A reason is required")
        .contains("Delivery proof is required");
  }

  @Test
  void validatesSafeProofTypesAndSize() throws IOException {
    String script = resource("static/app.js");
    assertThat(script)
        .contains("image/jpeg")
        .contains("application/pdf")
        .contains("5 * 1024 * 1024")
        .doesNotContain("Thread.sleep");
  }

  private static String resource(String path) throws IOException {
    try (var stream = CourierPortalAssetTest.class.getClassLoader().getResourceAsStream(path)) {
      assertThat(stream).as("classpath resource %s", path).isNotNull();
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
