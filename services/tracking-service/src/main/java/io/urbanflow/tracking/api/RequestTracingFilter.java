package io.urbanflow.tracking.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Validates or creates request identifiers and exposes them in logs and responses. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class RequestTracingFilter extends OncePerRequestFilter {

  public static final String CORRELATION_HEADER = "X-Correlation-ID";
  public static final String TRACE_HEADER = "X-Trace-ID";
  public static final String CORRELATION_ATTRIBUTE = "urbanflow.correlationId";
  public static final String TRACE_ATTRIBUTE = "urbanflow.traceId";

  private static final Pattern SAFE_CORRELATION_ID =
      Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,63}");
  private static final Pattern TRACE_PARENT =
      Pattern.compile("^[0-9a-f]{2}-([0-9a-f]{32})-[0-9a-f]{16}-[0-9a-f]{2}$");

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String correlationId = correlationId(request.getHeader(CORRELATION_HEADER));
    String traceId = traceId(request.getHeader("traceparent"));
    request.setAttribute(CORRELATION_ATTRIBUTE, correlationId);
    request.setAttribute(TRACE_ATTRIBUTE, traceId);
    response.setHeader(CORRELATION_HEADER, correlationId);
    response.setHeader(TRACE_HEADER, traceId);
    try (MDC.MDCCloseable ignoredCorrelation = MDC.putCloseable("correlationId", correlationId);
        MDC.MDCCloseable ignoredTrace = MDC.putCloseable("traceId", traceId)) {
      filterChain.doFilter(request, response);
    }
  }

  private static String correlationId(String candidate) {
    return candidate != null && SAFE_CORRELATION_ID.matcher(candidate).matches()
        ? candidate
        : UUID.randomUUID().toString();
  }

  private static String traceId(String traceParent) {
    if (traceParent != null) {
      Matcher matcher = TRACE_PARENT.matcher(traceParent.toLowerCase(Locale.ROOT));
      if (matcher.matches() && !matcher.group(1).equals("00000000000000000000000000000000")) {
        return matcher.group(1);
      }
    }
    return UUID.randomUUID().toString().replace("-", "");
  }
}
