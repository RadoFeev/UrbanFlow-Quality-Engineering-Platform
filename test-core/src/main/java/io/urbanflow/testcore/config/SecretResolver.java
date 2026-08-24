package io.urbanflow.testcore.config;

/** Resolves secrets without exposing their values in configuration reports. */
@FunctionalInterface
public interface SecretResolver {

  /** Returns the required secret or fails without including its value. */
  char[] required(String name);
}
