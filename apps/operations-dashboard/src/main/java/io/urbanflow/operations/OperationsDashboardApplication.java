package io.urbanflow.operations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Responsive operational control dashboard. */
@SpringBootApplication
public class OperationsDashboardApplication {

  @Retention(RetentionPolicy.CLASS)
  private @interface Generated {}

  /** Starts the operations dashboard. */
  @Generated
  public static void main(String[] args) {
    SpringApplication.run(OperationsDashboardApplication.class, args);
  }
}
