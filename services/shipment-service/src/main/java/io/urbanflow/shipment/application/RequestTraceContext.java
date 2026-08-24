package io.urbanflow.shipment.application;

/** Carries request identifiers into transactional events and diagnostics. */
public record RequestTraceContext(String correlationId, String traceId) {}
