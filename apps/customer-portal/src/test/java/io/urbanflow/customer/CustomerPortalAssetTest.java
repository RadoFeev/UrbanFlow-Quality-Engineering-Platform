package io.urbanflow.customer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class CustomerPortalAssetTest {

  @Test
  void constructsApplicationBoundary() {
    assertThat(new CustomerPortalApplication()).isNotNull();
  }

  @Test
  void exposesAccessibleAuthenticationContract() throws IOException {
    String html = resource("static/index.html");
    assertThat(html)
        .contains("Skip to content")
        .contains("data-testid=\"login-email\"")
        .contains("data-testid=\"register-submit\"")
        .contains("role=\"alert\"");
  }

  @Test
  void exposesShipmentAndTrackingContract() throws IOException {
    String html = resource("static/index.html");
    assertThat(html)
        .contains("data-testid=\"create-shipment-submit\"")
        .contains("data-testid=\"parcel-weight\"")
        .contains("data-testid=\"tracking-number\"")
        .contains("data-testid=\"shipment-list\"");
  }

  @Test
  void enforcesBusinessValidationAndDuplicateProtection() throws IOException {
    String script = resource("static/app.js");
    assertThat(script)
        .contains("weight >= 0.1 && weight <= 50")
        .contains("if (submitting) return")
        .contains("CANCELLED")
        .doesNotContain("Thread.sleep");
  }

  @Test
  void definesMobileAndReducedMotionLayouts() throws IOException {
    String styles = resource("static/styles.css");
    assertThat(styles)
        .contains("@media (max-width: 760px)")
        .contains("@media (max-width: 480px)")
        .contains("prefers-reduced-motion");
  }

  private static String resource(String path) throws IOException {
    try (var stream = CustomerPortalAssetTest.class.getClassLoader().getResourceAsStream(path)) {
      assertThat(stream).as("classpath resource %s", path).isNotNull();
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
