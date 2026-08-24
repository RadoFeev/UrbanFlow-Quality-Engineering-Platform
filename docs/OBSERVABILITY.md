# Observability

## Context propagation

`X-Correlation-ID` represents a business/request correlation chosen by the caller or generated at ingress. W3C `traceparent` carries distributed trace context. Invalid inbound identifiers are replaced and logged as metadata, never reflected unsafely.

Both identifiers are added to HTTP responses, structured application logs, Kafka envelope/headers, test logs, and Allure attachments. Test context records the first correlation/trace ID and updates child-step links without using shared static state.

## Telemetry pipeline

Services and test infrastructure export OTLP to the OpenTelemetry Collector. The Collector routes traces to the configured backend and metrics to Prometheus-compatible storage. Grafana provisions data sources and a versioned UrbanFlow dashboard.

## Required dashboard panels

- Tests executed, passed, failed, skipped, quarantined.
- Pass rate, failure rate, and flaky rate over time.
- Suite and test execution duration percentiles.
- API response-time p50/p95/p99 and error rate by endpoint.
- Kafka consumer processing delay, lag, retry, and DLT counts.
- End-to-end duration grouped by business journey.

## Failure investigation

An E2E failure starts from the Allure test and trace ID, opens the distributed trace, correlates service logs with the trace/correlation IDs, inspects Kafka event IDs and offsets, and verifies database audit/history rows. Evidence contains enough identifiers to follow this path without exposing secrets.

## Cardinality and privacy

Metrics never use user IDs, shipment IDs, emails, tracking numbers, event IDs, or trace IDs as labels. High-cardinality identifiers belong in traces/logs. Payload bodies and authentication data are excluded from telemetry by default.

## SLO-oriented alerts

Initial alerts cover sustained API error rate, latency threshold breach, outbox backlog, Kafka DLT growth, notification exhaustion, database pool saturation, Redis fallback rate, Grid capacity, and regression/flaky-rate degradation.
