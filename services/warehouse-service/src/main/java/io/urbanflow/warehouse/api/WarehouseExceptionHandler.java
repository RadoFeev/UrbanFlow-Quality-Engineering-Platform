package io.urbanflow.warehouse.api;

import io.urbanflow.warehouse.application.VersionConflictException;
import io.urbanflow.warehouse.application.WarehouseNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Clock;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps warehouse failures to stable HTTP semantics. */
@RestControllerAdvice
public class WarehouseExceptionHandler {

  private final Clock clock;

  /** Creates the handler with deterministic time. */
  public WarehouseExceptionHandler(Clock clock) {
    this.clock = clock;
  }

  /** Maps missing entities. */
  @ExceptionHandler(WarehouseNotFoundException.class)
  ResponseEntity<ApiError> notFound(
      WarehouseNotFoundException exception, HttpServletRequest request) {
    return error(HttpStatus.NOT_FOUND, "entity_not_found", exception.getMessage(), request);
  }

  /** Maps stale concurrent writes. */
  @ExceptionHandler({VersionConflictException.class, OptimisticLockingFailureException.class})
  ResponseEntity<ApiError> conflict(RuntimeException exception, HttpServletRequest request) {
    return error(HttpStatus.CONFLICT, "version_conflict", exception.getMessage(), request);
  }

  /** Maps missing version preconditions separately from other headers. */
  @ExceptionHandler(MissingRequestHeaderException.class)
  ResponseEntity<ApiError> missingHeader(
      MissingRequestHeaderException exception, HttpServletRequest request) {
    HttpStatus status =
        HttpHeaders.IF_MATCH.equalsIgnoreCase(exception.getHeaderName())
            ? HttpStatus.PRECONDITION_REQUIRED
            : HttpStatus.BAD_REQUEST;
    String code =
        status == HttpStatus.PRECONDITION_REQUIRED ? "precondition_required" : "invalid_request";
    return error(status, code, exception.getMessage(), request);
  }

  /** Maps malformed JSON and validation failures. */
  @ExceptionHandler({
    IllegalArgumentException.class,
    IllegalStateException.class,
    MethodArgumentNotValidException.class,
    ConstraintViolationException.class,
    HttpMessageNotReadableException.class
  })
  ResponseEntity<ApiError> invalid(Exception exception, HttpServletRequest request) {
    return error(HttpStatus.BAD_REQUEST, "invalid_request", exception.getMessage(), request);
  }

  private ResponseEntity<ApiError> error(
      HttpStatus status, String code, String message, HttpServletRequest request) {
    return ResponseEntity.status(status)
        .body(
            new ApiError(code, message, status.value(), request.getRequestURI(), clock.instant()));
  }
}
