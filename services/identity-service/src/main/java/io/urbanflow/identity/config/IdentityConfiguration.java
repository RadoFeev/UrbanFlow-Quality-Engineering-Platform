package io.urbanflow.identity.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Identity runtime primitives with injectable time and adaptive password hashing. */
@Configuration
@EnableConfigurationProperties({IdentitySecurityProperties.class, IdentityWebProperties.class})
public class IdentityConfiguration {

  /** Supplies UTC time for deterministic security lifetime decisions. */
  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  /** Uses bcrypt with a cost suitable for the demo deployment. */
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(11);
  }
}
