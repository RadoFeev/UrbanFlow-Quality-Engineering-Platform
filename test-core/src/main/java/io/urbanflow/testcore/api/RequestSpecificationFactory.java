package io.urbanflow.testcore.api;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.urbanflow.testcore.observability.CorrelationIdManager;
import io.urbanflow.testcore.observability.TraceIdManager;
import java.net.URI;
import java.time.Duration;
import java.util.Objects;

/** Creates reusable REST Assured request specifications with trace headers. */
public final class RequestSpecificationFactory {

  private final CorrelationIdManager correlations;
  private final TraceIdManager traces;
  private final Duration timeout;

  /** Creates the specification factory. */
  public RequestSpecificationFactory(
      CorrelationIdManager correlations, TraceIdManager traces, Duration timeout) {
    this.correlations = Objects.requireNonNull(correlations);
    this.traces = Objects.requireNonNull(traces);
    this.timeout = Objects.requireNonNull(timeout);
  }

  /** Creates a JSON specification for an absolute service URI. */
  public RequestSpecification json(URI baseUri) {
    if (baseUri == null || !baseUri.isAbsolute()) {
      throw new IllegalArgumentException("baseUri must be absolute");
    }
    return new RequestSpecBuilder()
        .setBaseUri(baseUri)
        .setContentType(ContentType.JSON)
        .setAccept(ContentType.JSON)
        .addHeader("X-Correlation-ID", correlations.current())
        .addHeader("traceparent", "00-" + traces.current() + "-0000000000000001-01")
        .setConfig(
            io.restassured.RestAssured.config()
                .httpClient(
                    io.restassured.config.HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", Math.toIntExact(timeout.toMillis()))
                        .setParam("http.socket.timeout", Math.toIntExact(timeout.toMillis()))))
        .build();
  }
}
