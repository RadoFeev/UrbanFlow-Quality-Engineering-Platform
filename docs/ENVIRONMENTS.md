# Environments

## Environment tiers

| Tier | Purpose | Infrastructure | Data policy |
|---|---|---|---|
| local | Developer feedback | Processes or selected Compose profiles | Per-run synthetic data |
| ci | Pull request and scheduled automation | Ephemeral Compose project or Kubernetes namespace | Per-job synthetic data; destroyed after retention |
| test | Shared exploratory and broader compatibility | Long-lived controlled deployment | Namespaced synthetic data; targeted cleanup |
| staging | Release rehearsal | Production-like topology and configuration | Approved synthetic/anonymized fixtures only |

Production is never a target for destructive, performance, active security, or data-creating suites.

## Configuration contract

Configuration precedence is JVM system property → environment variable → selected profile file → safe default. Key settings include environment, base URLs, browser, headless, remote URL, parallelism, tags, retry policy, video, cleanup, timeouts, evidence path, and run ID.

Secrets are resolved at runtime from environment variables or a configured secret provider. Repository templates contain variable names only. Startup validation reports missing keys without printing values.

## Health gates

Before a suite starts, the orchestrator verifies build identity, service readiness, PostgreSQL migration version, Kafka metadata, Redis connectivity where required, browser/Grid capacity, available disk for evidence, and clock skew. A failed capability check skips only truly unsupported optional capability (for example local Edge); a required dependency aborts the run.

## Docker profiles

- `minimal`: applications plus PostgreSQL.
- `integration`: PostgreSQL, Redis, Kafka, Kafka UI, services.
- `grid`: Selenium Router, Chrome and Firefox nodes, optional video.
- `observability`: Collector, Prometheus, Grafana.
- `full`: all components including Pact Broker.

## Kubernetes local execution

Milestone 8 supplies kind/minikube instructions, namespace-scoped resources, Helm values for local storage and reduced resources, and a test Job. Secrets must be created from local environment or an external secret solution; example manifests never contain credentials.
