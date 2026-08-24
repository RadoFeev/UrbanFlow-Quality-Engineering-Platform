package io.urbanflow.tracking.api;

import io.urbanflow.tracking.application.TrackingNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Clock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps expected tracking failures to a stable observable error contract. */
@RestControllerAdvice
public class TrackingExceptionHandler {

  private final Clock clock;

  TrackingExceptionHandler(Clock clock) {
    this.clock = clock;
  }

  /** Returns 404 when no projection exists for the requested tracking number. */
  @ExceptionHandler(TrackingNotFoundException.class)
  ResponseEntity<ApiError> notFound(
      TrackingNotFoundException exception, HttpServletRequest request) {
    return error("TRACKING_NOT_FOUND", exception.getMessage(), HttpStatus.NOT_FOUND, request);
  }

  /** Returns 400 for invalid tracking-number syntax. */
  @ExceptionHandler(ConstraintViolationException.class)
  ResponseEntity<ApiError> invalidTrackingNumber(HttpServletRequest request) {
    return error(
        "INVALID_TRACKING_NUMBER",
        "Tracking number format is invalid",
        HttpStatus.BAD_REQUEST,
        request);
  }

  private ResponseEntity<ApiError> error(
      String code, String message, HttpStatus status, HttpServletRequest request) {
    ApiError body =
        new ApiError(
            code,
            message,
            status.value(),
            request.getRequestURI(),
            clock.instant(),
            attribute(request, RequestTracingFilter.CORRELATION_ATTRIBUTE),
            attribute(request, RequestTracingFilter.TRACE_ATTRIBUTE));
    return ResponseEntity.status(status).body(body);
  }

  private static String attribute(HttpServletRequest request, String name) {
    Object value = request.getAttribute(name);
    return value == null ? "unavailable" : value.toString();
  }
}
