package io.urbanflow.testdata.builder;

import io.urbanflow.testdata.generation.UniqueEmailGenerator;
import io.urbanflow.testdata.model.UserData;
import java.util.UUID;

/** Fluent builder for independent synthetic user registrations. */
public final class UserBuilder {

  private final UniqueEmailGenerator emails;
  private UUID id = UUID.randomUUID();
  private String name = "Test Customer";
  private String email;
  private String password = "Valid-Test-42!";
  private String role = "CUSTOMER";

  /** Creates a builder with a run-scoped unique email generator. */
  public UserBuilder(UniqueEmailGenerator emails) {
    this.emails = emails;
  }

  /** Changes the stable user identifier. */
  public UserBuilder withId(UUID value) {
    id = value;
    return this;
  }

  /** Changes the user name. */
  public UserBuilder withName(String value) {
    name = value;
    return this;
  }

  /** Changes the email explicitly. */
  public UserBuilder withEmail(String value) {
    email = value;
    return this;
  }

  /** Changes the password. */
  public UserBuilder withPassword(String value) {
    password = value;
    return this;
  }

  /** Changes the platform role. */
  public UserBuilder withRole(String value) {
    role = value;
    return this;
  }

  /** Builds an immutable synthetic user. */
  public UserData build() {
    String resolvedEmail = email == null ? emails.next(role) : email;
    return new UserData(id, name, resolvedEmail, password, role);
  }
}
