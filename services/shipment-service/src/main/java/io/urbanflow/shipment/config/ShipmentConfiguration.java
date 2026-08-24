package io.urbanflow.shipment.config;

import io.urbanflow.shipment.domain.ShipmentTransitionPolicy;
import io.urbanflow.shipment.messaging.OutboxPublicationProperties;
import java.time.Clock;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Provides deterministic domain collaborators that can be replaced in tests. */
@Configuration
@EnableConfigurationProperties(OutboxPublicationProperties.class)
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

  /** Provides a process-unique identity for outbox lease ownership. */
  @Bean
  @Qualifier("outboxWorkerId")
  String outboxWorkerId() {
    return "shipment-" + UUID.randomUUID();
  }
}
