package io.urbanflow.identity.api;

import io.urbanflow.identity.domain.Role;
import jakarta.validation.constraints.NotNull;

/** Administrative role assignment payload. */
public record RoleUpdateRequest(@NotNull Role role) {}
