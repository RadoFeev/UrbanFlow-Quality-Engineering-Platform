package io.urbanflow.orchestrator;

/** Supported independently selectable test suites. */
public enum Suite {
  ALL,
  SMOKE,
  REGRESSION,
  API,
  UI,
  CONTRACT,
  COMPONENT,
  INTEGRATION,
  EVENT,
  DATABASE,
  E2E,
  ACCESSIBILITY,
  VISUAL,
  PERFORMANCE,
  SECURITY
}
