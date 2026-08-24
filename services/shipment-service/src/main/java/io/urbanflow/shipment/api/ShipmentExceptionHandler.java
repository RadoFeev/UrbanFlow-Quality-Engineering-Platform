package io.urbanflow.shipment.api;

import io.urbanflow.shipment.application.IdempotencyConflictException;
import io.urbanflow.shipment.application.ShipmentNotFoundException;
import io.urbanflow.shipment.domain.DeliveryAddressChangeNotAllowedException;
import io.urbanflow.shipment.domain.InvalidShipmentTransitionException;
import io.urbanflow.shipment.domain.ShipmentVersionConflictException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps expected shipment failures to a consistent, traceable HTTP error contract. */
@RestControllerAdvice
public class ShipmentExceptionHandler {

  private final Clock clock;

  /** Creates the handler with the application clock. */
  public ShipmentExceptionHandler(Clock clock) {
    this.clock = clock;
  }

  /** Maps missing shipments to HTTP 404. */
  @ExceptionHandler(ShipmentNotFoundException.class)
  ResponseEntity<ApiError> notFound(
      ShipmentNotFoundException exception, HttpServletRequest request) {
    return error(HttpStatus.NOT_FOUND, "SHIPMENT_NOT_FOUND", exception.getMessage(), request);
  }

  /** Maps rejected lifecycle transitions to HTTP 409. */
  @ExceptionHandler(InvalidShipmentTransitionException.class)
  ResponseEntity<ApiError> invalidTransition(
      InvalidShipmentTransitionException exception, HttpServletRequest request) {
    return error(HttpStatus.CONFLICT, exception.code().name(), exception.getMessage(), request);
  }

  /** Maps optimistic version conflicts to HTTP 409. */
  @ExceptionHandler(ShipmentVersionConflictException.class)
  ResponseEntity<ApiError> versionConflict(
      ShipmentVersionConflictException exception, HttpServletRequest request) {
    return error(HttpStatus.CONFLICT, "VERSION_CONFLICT", exception.getMessage(), request);
  }

  /** Maps reused idempotency keys with different payloads to HTTP 409. */
  @ExceptionHandler(IdempotencyConflictException.class)
  ResponseEntity<ApiError> idempotencyConflict(
      IdempotencyConflictException exception, HttpServletRequest request) {
    return error(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT", exception.getMessage(), request);
  }

  /** Maps an address change after the cutoff to HTTP 409. */
  @ExceptionHandler(DeliveryAddressChangeNotAllowedException.class)
  ResponseEntity<ApiError> addressChangeRejected(
      DeliveryAddressChangeNotAllowedException exception, HttpServletRequest request) {
    return error(
        HttpStatus.CONFLICT, "ADDRESS_CHANGE_NOT_ALLOWED", exception.getMessage(), request);
  }

  /** Maps malformed JSON and invalid entity tags to HTTP 400. */
  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    IllegalArgumentException.class,
    MissingRequestHeaderException.class
  })
  ResponseEntity<ApiError> badRequest(Exception exception, HttpServletRequest request) {
    return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", exception.getMessage(), request);
  }

  /** Maps body validation failures with field-level details to HTTP 400. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiError> invalidBody(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    List<ApiError.FieldViolation> violations =
        exception.getBindingResult().getFieldErrors().stream()
            .map(
                fieldError ->
                    new ApiError.FieldViolation(
                        fieldError.getField(), fieldError.getDefaultMessage()))
            .toList();
    return error(
        HttpStatus.BAD_REQUEST,
        "VALIDATION_FAILED",
        "Request validation failed",
        request,
        violations);
  }

  /** Maps path and header constraint violations to HTTP 400. */
  @ExceptionHandler(ConstraintViolationException.class)
  ResponseEntity<ApiError> invalidConstraint(
      ConstraintViolationException exception, HttpServletRequest request) {
    List<ApiError.FieldViolation> violations =
        exception.getConstraintViolations().stream()
            .map(
                violation ->
                    new ApiError.FieldViolation(
                        violation.getPropertyPath().toString(), violation.getMessage()))
            .toList();
    return error(
        HttpStatus.BAD_REQUEST,
        "VALIDATION_FAILED",
        "Request validation failed",
        request,
        violations);
  }

  /** Maps unsupported media types to HTTP 415. */
  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  ResponseEntity<ApiError> unsupportedMediaType(
      HttpMediaTypeNotSupportedException exception, HttpServletRequest request) {
    return error(
        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
        "UNSUPPORTED_MEDIA_TYPE",
        exception.getMessage(),
        request);
  }

  /** Maps unsupported HTTP methods to HTTP 405. */
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  ResponseEntity<ApiError> unsupportedMethod(
      HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
    return error(
        HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", exception.getMessage(), request);
  }

  private ResponseEntity<ApiError> error(
      HttpStatus status, String code, String message, HttpServletRequest request) {
    return error(status, code, message, request, List.of());
  }

  private ResponseEntity<ApiError> error(
      HttpStatus status,
      String code,
      String message,
      HttpServletRequest request,
      List<ApiError.FieldViolation> violations) {
    ApiError body =
        new ApiError(
            code,
            message,
            status.value(),
            request.getRequestURI(),
            Instant.now(clock),
            attribute(request, RequestTracingFilter.CORRELATION_ATTRIBUTE),
            attribute(request, RequestTracingFilter.TRACE_ATTRIBUTE),
            violations);
    return ResponseEntity.status(status).body(body);
  }

  private static String attribute(HttpServletRequest request, String name) {
    Object value = request.getAttribute(name);
    return value == null ? "unavailable" : value.toString();
  }
}
