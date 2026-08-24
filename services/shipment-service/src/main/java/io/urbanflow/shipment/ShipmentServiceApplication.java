package io.urbanflow.shipment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Starts the UrbanFlow shipment service. */
@EnableScheduling
@SpringBootApplication
public class ShipmentServiceApplication {

  /**
   * Starts the application.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(ShipmentServiceApplication.class, args);
  }
}
