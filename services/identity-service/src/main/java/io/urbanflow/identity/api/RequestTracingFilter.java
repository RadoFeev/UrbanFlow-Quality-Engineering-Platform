package io.urbanflow.identity.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Validates or creates correlation and W3C trace identifiers for every request. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestTracingFilter extends OncePerRequestFilter {

  public static final String CORRELATION_HEADER = "X-Correlation-ID";
  public static final String TRACE_HEADER = "X-Trace-ID";
  public static final String CORRELATION_ATTRIBUTE = "urbanflow.correlationId";
  public static final String TRACE_ATTRIBUTE = "urbanflow.traceId";

  private static final Pattern SAFE_CORRELATION = Pattern.compile("[A-Za-z0-9._:-]{1,64}");
  private static final Pattern TRACE_PARENT =
      Pattern.compile("[0-9a-f]{2}-([0-9a-f]{32})-[0-9a-f]{16}-[0-9a-f]{2}");
  private static final String ZERO_TRACE = "00000000000000000000000000000000";
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

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
    MDC.put("correlationId", correlationId);
    MDC.put("traceId", traceId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove("correlationId");
      MDC.remove("traceId");
    }
  }

  private static String correlationId(String supplied) {
    return supplied != null && SAFE_CORRELATION.matcher(supplied).matches()
        ? supplied
        : UUID.randomUUID().toString();
  }

  private static String traceId(String traceparent) {
    if (traceparent != null) {
      Matcher matcher = TRACE_PARENT.matcher(traceparent);
      if (matcher.matches() && !ZERO_TRACE.equals(matcher.group(1))) {
        return matcher.group(1);
      }
    }
    byte[] bytes = new byte[16];
    SECURE_RANDOM.nextBytes(bytes);
    return HexFormat.of().formatHex(bytes);
  }
}
