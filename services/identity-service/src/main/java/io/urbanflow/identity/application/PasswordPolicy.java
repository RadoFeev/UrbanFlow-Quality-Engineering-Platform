package io.urbanflow.identity.application;

/** Central password complexity and length policy. */
public final class PasswordPolicy {

  private static final int MINIMUM_LENGTH = 12;
  private static final int MAXIMUM_LENGTH = 128;

  private PasswordPolicy() {}

  /** Rejects passwords outside length and four-character-class requirements. */
  public static void validate(String password) {
    if (password == null
        || password.length() < MINIMUM_LENGTH
        || password.length() > MAXIMUM_LENGTH) {
      throw new WeakPasswordException("password length must be between 12 and 128 characters");
    }
    boolean upper = password.chars().anyMatch(Character::isUpperCase);
    boolean lower = password.chars().anyMatch(Character::isLowerCase);
    boolean digit = password.chars().anyMatch(Character::isDigit);
    boolean symbol = password.chars().anyMatch(value -> !Character.isLetterOrDigit(value));
    if (!upper || !lower || !digit || !symbol) {
      throw new WeakPasswordException(
          "password must contain upper-case, lower-case, numeric, and symbol characters");
    }
  }
}
