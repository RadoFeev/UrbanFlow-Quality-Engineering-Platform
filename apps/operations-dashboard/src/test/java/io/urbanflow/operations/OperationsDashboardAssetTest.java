package io.urbanflow.operations;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class OperationsDashboardAssetTest {

  @Test
  void constructsApplicationBoundary() {
    assertThat(new OperationsDashboardApplication()).isNotNull();
  }

  @Test
  void exposesSearchSortPaginationAndExportContract() throws IOException {
    String html = resource("static/index.html");
    assertThat(html)
        .contains("data-testid=\"ops-shipment-search\"")
        .contains("data-testid=\"shipment-sort\"")
        .contains("data-testid=\"page-status\"")
        .contains("data-testid=\"export-csv\"");
  }

  @Test
  void exposesGovernanceAndAuditContract() throws IOException {
    String html = resource("static/index.html");
    assertThat(html)
        .contains("data-testid=\"users-table\"")
        .contains("data-testid=\"feature-flag-list\"")
        .contains("data-testid=\"audit-table\"");
  }

  @Test
  void enforcesRoleAndVersionChecks() throws IOException {
    String script = resource("static/app.js");
    assertThat(script)
        .contains("state.session?.role === \"ADMIN\"")
        .contains("shipment.version !== expected")
        .contains("Version conflict")
        .contains("Administrator permission is required");
  }

  @Test
  void recordsOperationalMutationsInAuditHistory() throws IOException {
    String script = resource("static/app.js");
    assertThat(script)
        .contains("SHIPMENT_STATUS_CHANGED")
        .contains("USER_ROLE_CHANGED")
        .contains("FEATURE_FLAG_CHANGED")
        .contains("INCIDENT_RESOLVED");
  }

  private static String resource(String path) throws IOException {
    try (var stream =
        OperationsDashboardAssetTest.class.getClassLoader().getResourceAsStream(path)) {
      assertThat(stream).as("classpath resource %s", path).isNotNull();
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
