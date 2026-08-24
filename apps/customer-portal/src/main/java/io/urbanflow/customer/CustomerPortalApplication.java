package io.urbanflow.customer;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Responsive customer shipment portal. */
@SpringBootApplication
public class CustomerPortalApplication {

  @Retention(RetentionPolicy.CLASS)
  private @interface Generated {}

  /** Starts the customer portal. */
  @Generated
  public static void main(String[] args) {
    SpringApplication.run(CustomerPortalApplication.class, args);
  }
}
