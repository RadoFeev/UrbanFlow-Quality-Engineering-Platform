package io.urbanflow.testcore.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

/** Enables isolated context, cleanup, evidence, and outcome classification for a test. */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Tag("regression")
@ExtendWith({TestContextExtension.class, EvidenceExtension.class, TestOutcomeListener.class})
public @interface UrbanFlowTest {}
