package io.urbanflow.testcore.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.restassured.response.Response;
import io.urbanflow.testcore.observability.CorrelationIdManager;
import io.urbanflow.testcore.observability.TraceIdManager;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class ApiSupportTest {

  @Test
  void buildsJsonSpecificationWithTraceHeaders() {
    CorrelationIdManager correlations = new CorrelationIdManager();
    correlations.set("run-2026-case-01");
    var specification =
        new RequestSpecificationFactory(correlations, new TraceIdManager(), Duration.ofSeconds(2))
            .json(URI.create("http://localhost:8082"));

    assertThat(specification).isNotNull();
  }

  @Test
  void rejectsRelativeBaseUri() {
    assertThatThrownBy(
            () ->
                new RequestSpecificationFactory(
                        new CorrelationIdManager(), new TraceIdManager(), Duration.ofSeconds(2))
                    .json(URI.create("/relative")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsBlankBearerCredential() {
    io.restassured.specification.RequestSpecification specification =
        mock(io.restassured.specification.RequestSpecification.class);
    assertThatThrownBy(() -> new ApiClient(specification).authenticated(" "))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void validatesSuccessfulResponseInvariants() {
    Response response = mock(Response.class);
    when(response.statusCode()).thenReturn(200);
    when(response.contentType()).thenReturn("application/json;charset=UTF-8");
    when(response.time()).thenReturn(120L);
    when(response.header("X-Correlation-ID")).thenReturn("run-2026-case-01");
    when(response.header("X-Trace-ID")).thenReturn("0123456789abcdef0123456789abcdef");

    assertThat(new ResponseValidator().validate(response, 200, Duration.ofMillis(500)))
        .isSameAs(response);
  }
}
