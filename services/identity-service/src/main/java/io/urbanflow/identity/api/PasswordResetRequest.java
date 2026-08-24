package io.urbanflow.identity.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Enumeration-resistant password reset request. */
public record PasswordResetRequest(@NotBlank @Email @Size(max = 254) String email) {}
