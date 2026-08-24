# Performance Strategy

## Workloads

- Smoke: one to five virtual users for script correctness and basic thresholds.
- Load: expected concurrency and arrival rates with representative operation mix.
- Stress: stepwise increase until an SLO or resource limit identifies capacity.
- Spike: abrupt surge and recovery, emphasizing queues and connection pools.
- Soak: sustained normal-to-high load for leaks, backlog, and degradation.

The ten final scenarios cover login, create/get/track shipment, status update, courier assignments, mixed customer journey, mixed courier journey, notification/event throughput, and tracking read/cache behavior across workload types.

## Data and isolation

Performance runs use an isolated environment, pre-generated synthetic identities, bounded creation volume, and a cleanup plan. Functional CI and performance tests never share the same service deployment or database. Active workload execution requires an allowlisted host.

## Thresholds

Each k6 scenario defines endpoint-specific p95, p99, maximum response time, error rate, and throughput. Initial values are in [QUALITY_GATES.md](QUALITY_GATES.md). Checks distinguish expected negative responses from transport/server failures.

## Method

1. Warm services and validate environment health.
2. Record build/config/resource identity.
3. Run performance smoke.
4. Execute the target workload with a controlled ramp.
5. Correlate k6 metrics with application, JVM, database, Redis, and Kafka telemetry.
6. Compare to the same-workload rolling baseline.
7. Publish HTML/Grafana links and a threshold summary.

## Interpretation

A threshold failure is not retried away. Investigation separates client saturation, test-data contention, application regression, database/resource saturation, and environmental noise. Capacity claims include workload, dataset, topology, duration, and confidence limitations.
