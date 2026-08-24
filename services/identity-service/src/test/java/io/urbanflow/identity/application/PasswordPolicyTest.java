package io.urbanflow.identity.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class PasswordPolicyTest {

  @Test
  void acceptsAllRequiredCharacterClasses() {
    assertThatCode(() -> PasswordPolicy.validate("Valid-Pass123")).doesNotThrowAnyException();
  }

  @ParameterizedTest
  @MethodSource("weakPasswords")
  void rejectsWeakPartitions(String password) {
    assertThatThrownBy(() -> PasswordPolicy.validate(password))
        .isInstanceOf(WeakPasswordException.class);
  }

  private static Stream<String> weakPasswords() {
    return Stream.of(
        "Short1!",
        "lowercaseonly1!",
        "UPPERCASEONLY1!",
        "NoNumbersHere!",
        "NoSymbolsHere1",
        "A".repeat(129) + "a1!");
  }
}
