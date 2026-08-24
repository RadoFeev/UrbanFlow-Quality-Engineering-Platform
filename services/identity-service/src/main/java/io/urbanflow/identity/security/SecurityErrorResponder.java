package io.urbanflow.identity.security;

import io.urbanflow.identity.api.ApiError;
import io.urbanflow.identity.api.RequestTracingFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Emits the stable API envelope for unauthenticated and unauthorized requests. */
@Component
public class SecurityErrorResponder implements AuthenticationEntryPoint, AccessDeniedHandler {

  private final ObjectMapper objectMapper;
  private final Clock clock;

  /** Creates the security error writer with deterministic timestamps. */
  public SecurityErrorResponder(ObjectMapper objectMapper, Clock clock) {
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authenticationException)
      throws IOException {
    write(request, response, 401, "authentication_required", "authentication is required");
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {
    write(request, response, 403, "access_denied", "the operation is not permitted");
  }

  private void write(
      HttpServletRequest request,
      HttpServletResponse response,
      int status,
      String code,
      String message)
      throws IOException {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    objectMapper.writeValue(
        response.getOutputStream(),
        new ApiError(
            code,
            message,
            status,
            request.getRequestURI(),
            attribute(request, RequestTracingFilter.CORRELATION_ATTRIBUTE),
            attribute(request, RequestTracingFilter.TRACE_ATTRIBUTE),
            clock.instant()));
  }

  private static String attribute(HttpServletRequest request, String name) {
    Object value = request.getAttribute(name);
    return value == null ? "unavailable" : value.toString();
  }
}
