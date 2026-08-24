package io.urbanflow.identity.api;

import io.urbanflow.identity.application.AuthService;
import io.urbanflow.identity.application.AuthTokens;
import io.urbanflow.identity.application.UserProfile;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public identity lifecycle endpoints with explicit cache controls. */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthService authService;

  /** Creates the HTTP adapter for authentication workflows. */
  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  /** Registers a customer without issuing an implicit session. */
  @PostMapping("/register")
  public ResponseEntity<UserProfile> register(@Valid @RequestBody RegisterRequest request) {
    UserProfile profile =
        authService.register(request.email(), request.displayName(), request.password());
    return ResponseEntity.status(HttpStatus.CREATED)
        .cacheControl(CacheControl.noStore())
        .body(profile);
  }

  /** Exchanges credentials for access and rotating refresh credentials. */
  @PostMapping("/login")
  public ResponseEntity<AuthTokens> login(@Valid @RequestBody LoginRequest request) {
    return tokenResponse(authService.login(request.email(), request.password()));
  }

  /** Rotates a valid refresh credential. */
  @PostMapping("/refresh")
  public ResponseEntity<AuthTokens> refresh(@Valid @RequestBody TokenRequest request) {
    return tokenResponse(authService.refresh(request.refreshToken()));
  }

  /** Revokes a refresh credential idempotently. */
  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@Valid @RequestBody TokenRequest request) {
    authService.logout(request.refreshToken());
    return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
  }

  /** Accepts reset requests with a uniform response regardless of account existence. */
  @PostMapping("/password-reset/request")
  public ResponseEntity<Void> requestReset(@Valid @RequestBody PasswordResetRequest request) {
    authService.requestPasswordReset(request.email());
    return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).build();
  }

  /** Consumes a single-use reset token and invalidates live refresh sessions. */
  @PostMapping("/password-reset/confirm")
  public ResponseEntity<Void> confirmReset(@Valid @RequestBody PasswordResetConfirmation request) {
    authService.resetPassword(request.resetToken(), request.newPassword());
    return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
  }

  private static ResponseEntity<AuthTokens> tokenResponse(AuthTokens tokens) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(tokens);
  }
}
