package io.urbanflow.identity.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Validated credential exchange payload. */
public record LoginRequest(
    @NotBlank @Email @Size(max = 254) String email, @NotBlank @Size(max = 128) String password) {}
