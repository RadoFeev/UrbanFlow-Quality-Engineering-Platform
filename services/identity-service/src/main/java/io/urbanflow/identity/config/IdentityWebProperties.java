package io.urbanflow.identity.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Explicit browser origins allowed to call the identity API. */
@ConfigurationProperties("urbanflow.identity.web")
public record IdentityWebProperties(List<String> allowedOrigins) {

  /** Defensively copies and requires at least one explicit origin. */
  public IdentityWebProperties {
    allowedOrigins = List.copyOf(allowedOrigins);
    if (allowedOrigins.isEmpty() || allowedOrigins.stream().anyMatch("*"::equals)) {
      throw new IllegalArgumentException("allowedOrigins must contain explicit origins");
    }
  }
}
