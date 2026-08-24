package io.urbanflow.notification.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Notification runtime primitives and bounded HTTP client. */
@Configuration
@EnableConfigurationProperties(NotificationProperties.class)
public class NotificationConfiguration {

  /** Supplies UTC time for retry scheduling. */
  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  /** Builds the internal communications gateway client with request timeout. */
  @Bean
  RestClient notificationRestClient(NotificationProperties properties) {
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
    factory.setReadTimeout(properties.gatewayTimeout());
    return RestClient.builder()
        .baseUrl(properties.gatewayUrl().toString())
        .requestFactory(factory)
        .build();
  }
}
