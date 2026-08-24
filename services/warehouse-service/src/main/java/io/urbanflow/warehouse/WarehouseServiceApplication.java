package io.urbanflow.warehouse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** UrbanFlow warehouse, hub, incident, flag, and audit service. */
@SpringBootApplication
public class WarehouseServiceApplication {

  /** Starts the warehouse service. */
  public static void main(String[] args) {
    SpringApplication.run(WarehouseServiceApplication.class, args);
  }
}
