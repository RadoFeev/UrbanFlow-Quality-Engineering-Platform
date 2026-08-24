package io.urbanflow.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Durable customer notification and delivery-retry boundary. */
@SpringBootApplication
@EnableScheduling
public class NotificationServiceApplication {

  /** Starts the notification service. */
  public static void main(String[] args) {
    SpringApplication.run(NotificationServiceApplication.class, args);
  }
}
