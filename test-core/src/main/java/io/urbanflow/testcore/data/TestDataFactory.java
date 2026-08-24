package io.urbanflow.testcore.data;

/** Produces isolated domain data for one test scenario. */
@FunctionalInterface
public interface TestDataFactory<T> {

  /** Creates a new independent test value. */
  T create();
}
