package io.urbanflow.courier;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Responsive courier route portal with offline synchronization. */
@SpringBootApplication
public class CourierPortalApplication {

  @Retention(RetentionPolicy.CLASS)
  private @interface Generated {}

  /** Starts the courier portal. */
  @Generated
  public static void main(String[] args) {
    SpringApplication.run(CourierPortalApplication.class, args);
  }
}
