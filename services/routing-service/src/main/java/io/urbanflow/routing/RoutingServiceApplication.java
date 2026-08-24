package io.urbanflow.routing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Courier capacity and delivery-route assignment boundary. */
@SpringBootApplication
@EnableScheduling
public class RoutingServiceApplication {

  /** Starts the routing service. */
  public static void main(String[] args) {
    SpringApplication.run(RoutingServiceApplication.class, args);
  }
}
