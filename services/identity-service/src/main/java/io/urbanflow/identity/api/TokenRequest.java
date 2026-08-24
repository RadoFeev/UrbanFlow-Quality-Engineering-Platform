package io.urbanflow.identity.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Opaque refresh credential payload for rotation and logout. */
public record TokenRequest(@NotBlank @Size(max = 256) String refreshToken) {}
