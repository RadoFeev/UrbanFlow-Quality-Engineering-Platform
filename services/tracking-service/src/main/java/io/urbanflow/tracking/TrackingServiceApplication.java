package io.urbanflow.tracking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Starts the UrbanFlow tracking projection service. */
@SpringBootApplication
public class TrackingServiceApplication {

  /** Starts the application. */
  public static void main(String[] args) {
    SpringApplication.run(TrackingServiceApplication.class, args);
  }
}
