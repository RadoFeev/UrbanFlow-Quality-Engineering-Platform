package io.urbanflow.warehouse.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Deterministic infrastructure dependencies. */
@Configuration
public class WarehouseConfiguration {

  /** Supplies UTC time to domain services. */
  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }
}
