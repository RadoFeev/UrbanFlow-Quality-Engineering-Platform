package io.urbanflow.routing.api;

import io.urbanflow.routing.application.RouteNotFoundException;
import io.urbanflow.routing.domain.CourierCapacityException;
import io.urbanflow.routing.domain.RouteVersionConflictException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps routing failures to stable HTTP error semantics. */
@RestControllerAdvice
public class RoutingExceptionHandler {

  private final Clock clock;

  /** Creates the exception handler with deterministic time. */
  public RoutingExceptionHandler(Clock clock) {
    this.clock = clock;
  }

  /** Maps missing route identifiers. */
  @ExceptionHandler(RouteNotFoundException.class)
  ResponseEntity<ApiError> notFound(RouteNotFoundException exception, HttpServletRequest request) {
    return error(HttpStatus.NOT_FOUND, "route_not_found", exception, request);
  }

  /** Maps exhausted courier capacity. */
  @ExceptionHandler(CourierCapacityException.class)
  ResponseEntity<ApiError> unavailable(
      CourierCapacityException exception, HttpServletRequest request) {
    return error(
        HttpStatus.SERVICE_UNAVAILABLE, "courier_capacity_unavailable", exception, request);
  }

  /** Maps stale route versions. */
  @ExceptionHandler({RouteVersionConflictException.class, OptimisticLockingFailureException.class})
  ResponseEntity<ApiError> conflict(RuntimeException exception, HttpServletRequest request) {
    return error(HttpStatus.CONFLICT, "version_conflict", exception, request);
  }

  /** Maps invalid entity tags and route state transitions. */
  @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
  ResponseEntity<ApiError> invalid(RuntimeException exception, HttpServletRequest request) {
    return error(HttpStatus.BAD_REQUEST, "invalid_request", exception, request);
  }

  /** Maps missing optimistic preconditions. */
  @ExceptionHandler(MissingRequestHeaderException.class)
  ResponseEntity<ApiError> precondition(
      MissingRequestHeaderException exception, HttpServletRequest request) {
    return error(HttpStatus.PRECONDITION_REQUIRED, "precondition_required", exception, request);
  }

  private ResponseEntity<ApiError> error(
      HttpStatus status, String code, Exception exception, HttpServletRequest request) {
    return ResponseEntity.status(status)
        .body(
            new ApiError(
                code,
                exception.getMessage(),
                status.value(),
                request.getRequestURI(),
                clock.instant()));
  }
}
