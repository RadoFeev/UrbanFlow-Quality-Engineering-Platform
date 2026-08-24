# Performance testing

## Workload model

Ten scenarios isolate identity, shipment, tracking, routing, notification, and warehouse behavior.
Smoke runs one iteration per scenario. Load applies a steady arrival rate, stress ramps beyond the
expected peak, spike introduces a sharp burst and recovery, and soak sustains the configured rate for
30 minutes. `RATE` is per scenario and therefore must be capacity-planned as an aggregate.

## Gates

Each scenario requires less than 1% transport failures, at least 99% successful semantic checks, and
p95 latency below 500 ms for health/readiness or 800 ms for business reads. Release tests also review
p99, throughput, saturation, error classification, JVM/DB/Kafka/Redis telemetry, and recovery after
load removal.

## Safety and reproducibility

Only approved non-production origins and synthetic identifiers are allowed. The exact build, profile,
rate, duration, dataset seed, environment, and observability links are retained with results. A run is
invalid when generators saturate, telemetry is missing, or upstream dependencies are outside their
agreed state.
