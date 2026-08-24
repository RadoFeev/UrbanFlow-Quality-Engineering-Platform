package io.urbanflow.identity.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Single-use reset token and replacement password. */
public record PasswordResetConfirmation(
    @NotBlank @Size(max = 256) String resetToken, @NotBlank @Size(max = 128) String newPassword) {}
