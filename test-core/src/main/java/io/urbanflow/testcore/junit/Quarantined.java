package io.urbanflow.testcore.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Tag;

/** Marks a documented non-blocking test without disabling its visibility. */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Tag("quarantine")
public @interface Quarantined {

  /** Returns the tracking issue or investigation reason. */
  String reason();
}
