package io.urbanflow.routing.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Routing runtime primitives. */
@Configuration
@EnableConfigurationProperties(RoutingOutboxProperties.class)
public class RoutingConfiguration {

  /** Supplies UTC time for route lifecycle decisions. */
  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }
}
