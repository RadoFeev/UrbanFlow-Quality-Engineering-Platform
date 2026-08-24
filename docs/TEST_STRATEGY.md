# Test Strategy

## Objectives

The strategy provides fast defect localization near the owning boundary, cross-layer confidence for critical flows, and operational evidence when asynchronous failures occur. Coverage follows business risk, not a pyramid quota.

## Levels

| Level | Purpose | Dependencies | Merge behavior |
|---|---|---|---|
| Unit | Domain rules, parsing, masking, classifiers | In-memory only | Blocking on every PR |
| Component | One service with real framework and controlled outbound adapters | WireMock, optional container DB | Blocking on affected service |
| API | Deployed black-box HTTP behavior and schemas | Running service environment | Smoke blocking; full scheduled |
| Contract | Consumer expectations and provider compatibility | Pact files/broker, provider | Blocking |
| Database | Migrations, constraints, isolation, persistence | Fresh PostgreSQL container | Blocking on data changes |
| Integration | Real adapter combinations | PostgreSQL/Kafka/Redis containers | Blocking on affected adapters |
| Event | Envelope, schema, ordering, retries, DLT, idempotency | Kafka and service consumers | Blocking core subset; full nightly |
| UI | Browser-visible journeys and RBAC | Running system and browser | Smoke blocking; regression sharded |
| E2E | Critical outcomes across UI/API/events/storage | Full stack | Smoke blocking; full nightly |
| Accessibility | WCAG 2.2 AA automated checks | Browser and target pages | Critical violations block |
| Visual | Layout/theme/viewports | Browser and approved baselines | Non-blocking by default; opt-in gate |
| Performance | Capacity and resilience thresholds | Isolated full stack | Scheduled/release gate |
| Security | Safe negative tests, ZAP baseline, dependency scan | Local/CI demo target | Critical findings block |

## Taxonomy and pipelines

Every JUnit test has exactly one primary level tag and may also have `smoke` or `regression`. `quarantine` excludes a test from blocking suites but never from reporting.

- PR: compile, unit, affected component, contract, smoke API, static quality, dependency policy.
- Post-merge smoke: `smoke & !quarantine` across API/UI/E2E.
- Nightly: `regression & !quarantine`, sharded UI/API/integration/event/E2E, visual compare, security, performance smoke.
- Weekly: soak, full ZAP baseline, full dependency database refresh, broad browser matrix.
- Release: all blocking gates, performance comparison, Helm/Compose/Kubernetes validation.

## Test design

Cases use risk-based positive and negative paths, boundary values, equivalence partitions, decision tables, state-transition coverage, concurrency schedules, failure injection, and metamorphic invariants. Parameterized rows document the unique partition they cover. Assertions validate business outcomes and observable side effects, not implementation call counts unless the interaction is the contract.

## Independence

Tests create unique synthetic data, record owned resources, and delete only those resources. A test may run alone, in any order, or concurrently. No suite depends on seed records except immutable reference data explicitly provisioned by migrations.

## Async behavior

Awaitility polls observable conditions with bounded, environment-aware timeouts and diagnostic messages. Fixed sleeps are forbidden. Eventual-consistency assertions include the last observed state and trace/correlation IDs on timeout.

## Retry policy

Product assertions are not retried. A JUnit extension may retry once only after the failure classifier identifies a known transient infrastructure signature. Both attempts and the classification remain visible. See [FLAKY_TEST_POLICY.md](FLAKY_TEST_POLICY.md).

## Evidence

Allure metadata includes Epic, Feature, Story, Severity, Owner, Tags, Description, and Steps. Failure evidence is type-specific: browser artifacts for UI, masked traffic for API, envelope/offset/partition for Kafka, query/result/transaction state for database, and trace/correlation IDs everywhere.
