package io.urbanflow.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Identity and access boundary for UrbanFlow actors. */
@SpringBootApplication
public class IdentityServiceApplication {

  /** Starts the identity service. */
  public static void main(String[] args) {
    SpringApplication.run(IdentityServiceApplication.class, args);
  }
}
