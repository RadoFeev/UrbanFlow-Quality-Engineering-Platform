package io.urbanflow.shipment.config;

import io.urbanflow.shipment.domain.ShipmentTransitionPolicy;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Provides deterministic domain collaborators that can be replaced in tests. */
@Configuration
public class ShipmentConfiguration {

  /** Provides the clock used for shipment and event timestamps. */
  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  /** Provides the authoritative shipment transition policy. */
  @Bean
  ShipmentTransitionPolicy shipmentTransitionPolicy() {
    return new ShipmentTransitionPolicy();
  }
}
