package io.urbanflow.testcore.config;

import java.util.Map;

/** Reads secrets from an isolated environment map and returns defensive character arrays. */
public final class EnvironmentSecretResolver implements SecretResolver {

  private final Map<String, String> environment;

  /** Creates the environment-backed resolver. */
  public EnvironmentSecretResolver(Map<String, String> environment) {
    this.environment = Map.copyOf(environment);
  }

  @Override
  public char[] required(String name) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("secret name must not be blank");
    }
    String value = environment.get(name);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("required secret is not configured: " + name);
    }
    return value.toCharArray();
  }
}
