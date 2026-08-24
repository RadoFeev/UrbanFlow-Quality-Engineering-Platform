package io.urbanflow.testcore.api;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.response.Response;
import java.time.Duration;
import java.util.Objects;

/** Reusable REST response assertions for platform-wide invariants. */
public final class ResponseValidator {

  /** Validates status, media type, correlation, trace, and response-time boundaries. */
  public Response validate(Response response, int expectedStatus, Duration maximumDuration) {
    Objects.requireNonNull(response, "response must not be null");
    assertThat(response.statusCode()).isEqualTo(expectedStatus);
    assertThat(response.contentType()).startsWith("application/json");
    assertThat(response.time()).isLessThanOrEqualTo(maximumDuration.toMillis());
    assertThat(response.header("X-Correlation-ID")).as("correlation response header").isNotBlank();
    assertThat(response.header("X-Trace-ID")).as("trace response header").matches("[0-9a-f]{32}");
    return response;
  }

  /** Validates the stable error envelope. */
  public Response validateError(Response response, int expectedStatus, String expectedCode) {
    assertThat(response.statusCode()).isEqualTo(expectedStatus);
    assertThat(response.jsonPath().getString("code")).isEqualTo(expectedCode);
    assertThat(response.jsonPath().getString("message")).isNotBlank();
    assertThat(response.jsonPath().getInt("status")).isEqualTo(expectedStatus);
    assertThat(response.jsonPath().getString("path")).startsWith("/");
    assertThat(response.jsonPath().getString("timestamp")).isNotBlank();
    return response;
  }
}
