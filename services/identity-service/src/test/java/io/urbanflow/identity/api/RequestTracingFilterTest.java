package io.urbanflow.identity.api;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestTracingFilterTest {

  private final RequestTracingFilter filter = new RequestTracingFilter();

  @Test
  void preservesSafeCorrelationAndW3cTraceIdentifiers() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
    request.addHeader(RequestTracingFilter.CORRELATION_HEADER, "corr-identity_001");
    request.addHeader("traceparent", "00-0123456789abcdef0123456789abcdef-0123456789abcdef-01");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain =
        (servletRequest, servletResponse) -> {
          assertThat(MDC.get("correlationId")).isEqualTo("corr-identity_001");
          assertThat(MDC.get("traceId")).isEqualTo("0123456789abcdef0123456789abcdef");
        };

    filter.doFilter(request, response, chain);

    assertThat(response.getHeader(RequestTracingFilter.CORRELATION_HEADER))
        .isEqualTo("corr-identity_001");
    assertThat(response.getHeader(RequestTracingFilter.TRACE_HEADER))
        .isEqualTo("0123456789abcdef0123456789abcdef");
    assertThat(MDC.get("correlationId")).isNull();
  }

  @Test
  void replacesUnsafeAndZeroIdentifiers() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
    request.addHeader(RequestTracingFilter.CORRELATION_HEADER, "unsafe value\r\nheader");
    request.addHeader("traceparent", "00-00000000000000000000000000000000-0123456789abcdef-01");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, (servletRequest, servletResponse) -> {});

    assertThat(response.getHeader(RequestTracingFilter.CORRELATION_HEADER))
        .matches("[0-9a-f-]{36}");
    assertThat(response.getHeader(RequestTracingFilter.TRACE_HEADER))
        .matches("[0-9a-f]{32}")
        .isNotEqualTo("00000000000000000000000000000000");
  }
}
