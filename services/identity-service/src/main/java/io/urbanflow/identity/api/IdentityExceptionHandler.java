package io.urbanflow.identity.api;

import io.urbanflow.identity.application.AccountUnavailableException;
import io.urbanflow.identity.application.DuplicateEmailException;
import io.urbanflow.identity.application.InvalidCredentialsException;
import io.urbanflow.identity.application.InvalidSessionException;
import io.urbanflow.identity.application.UserNotFoundException;
import io.urbanflow.identity.application.VersionConflictException;
import io.urbanflow.identity.application.WeakPasswordException;
import jakarta.servlet.http.HttpServletRequest;
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

/** Normalizes identity application, validation, and concurrency failures. */
@RestControllerAdvice
public class IdentityExceptionHandler {

  private final Clock clock;

  /** Creates the handler with deterministic UTC time. */
  public IdentityExceptionHandler(Clock clock) {
    this.clock = clock;
  }

  @ExceptionHandler(DuplicateEmailException.class)
  ResponseEntity<ApiError> duplicateEmail(
      DuplicateEmailException exception, HttpServletRequest request) {
    return error(HttpStatus.CONFLICT, "email_conflict", exception.getMessage(), request);
  }

  @ExceptionHandler({InvalidCredentialsException.class, InvalidSessionException.class})
  ResponseEntity<ApiError> unauthorized(RuntimeException exception, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
        .body(
            body(
                HttpStatus.UNAUTHORIZED, "authentication_failed", exception.getMessage(), request));
  }

  @ExceptionHandler(AccountUnavailableException.class)
  ResponseEntity<ApiError> unavailable(
      AccountUnavailableException exception, HttpServletRequest request) {
    return error(HttpStatus.LOCKED, "account_unavailable", exception.getMessage(), request);
  }

  @ExceptionHandler(WeakPasswordException.class)
  ResponseEntity<ApiError> weakPassword(
      WeakPasswordException exception, HttpServletRequest request) {
    return error(
        HttpStatus.UNPROCESSABLE_CONTENT, "weak_password", exception.getMessage(), request);
  }

  @ExceptionHandler(UserNotFoundException.class)
  ResponseEntity<ApiError> notFound(UserNotFoundException exception, HttpServletRequest request) {
    return error(HttpStatus.NOT_FOUND, "user_not_found", exception.getMessage(), request);
  }

  @ExceptionHandler({VersionConflictException.class, OptimisticLockingFailureException.class})
  ResponseEntity<ApiError> conflict(RuntimeException exception, HttpServletRequest request) {
    return error(HttpStatus.CONFLICT, "version_conflict", exception.getMessage(), request);
  }

  @ExceptionHandler(MissingRequestHeaderException.class)
  ResponseEntity<ApiError> precondition(
      MissingRequestHeaderException exception, HttpServletRequest request) {
    return error(
        HttpStatus.PRECONDITION_REQUIRED,
        "precondition_required",
        exception.getHeaderName() + " header is required",
        request);
  }

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    HttpMessageNotReadableException.class,
    IllegalArgumentException.class
  })
  ResponseEntity<ApiError> badRequest(Exception exception, HttpServletRequest request) {
    return error(HttpStatus.BAD_REQUEST, "invalid_request", validationMessage(exception), request);
  }

  private ResponseEntity<ApiError> error(
      HttpStatus status, String code, String message, HttpServletRequest request) {
    return ResponseEntity.status(status).body(body(status, code, message, request));
  }

  private ApiError body(
      HttpStatus status, String code, String message, HttpServletRequest request) {
    return new ApiError(
        code,
        message,
        status.value(),
        request.getRequestURI(),
        attribute(request, RequestTracingFilter.CORRELATION_ATTRIBUTE),
        attribute(request, RequestTracingFilter.TRACE_ATTRIBUTE),
        clock.instant());
  }

  private static String validationMessage(Exception exception) {
    if (exception instanceof MethodArgumentNotValidException validation
        && validation.getBindingResult().getFieldError() != null) {
      return validation.getBindingResult().getFieldError().getField() + " is invalid";
    }
    if (exception instanceof HttpMessageNotReadableException) {
      return "request body is malformed";
    }
    return exception.getMessage() == null ? "request is invalid" : exception.getMessage();
  }

  private static String attribute(HttpServletRequest request, String name) {
    Object value = request.getAttribute(name);
    return value == null ? "unavailable" : value.toString();
  }
}
