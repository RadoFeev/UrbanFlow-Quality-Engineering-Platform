package io.urbanflow.testdata.model;

import java.util.UUID;

/** Synthetic user registration data. */
public record UserData(UUID id, String name, String email, String password, String role) {

  /** Validates required registration fields. */
  public UserData {
    if (id == null
        || name == null
        || name.isBlank()
        || email == null
        || !email.contains("@")
        || password == null
        || password.length() < 12
        || role == null
        || role.isBlank()) {
      throw new IllegalArgumentException("user test data is incomplete");
    }
  }
}
