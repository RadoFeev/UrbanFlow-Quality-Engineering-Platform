# Test Plan

## Scope

The release scope covers identity, customer shipment operations, tracking, courier execution including offline synchronization, operations administration, hub/warehouse management, routing, notifications, incidents, audit records, and feature flags across supported local and distributed deployments.

## Entry criteria

- Accepted OpenAPI and event schemas are versioned.
- Migrations apply to a clean database.
- Target services report readiness, not merely process liveness.
- Required reference data and synthetic test actors are provisioned.
- Browser, container, and environment capability checks pass.

## Execution waves

1. Static validation, domain unit tests, migration validation.
2. Component and provider contract verification.
3. API, database, Redis, Kafka, and integration tests.
4. UI smoke in Chrome and Firefox; accessibility critical paths.
5. End-to-end critical journeys and failure recovery.
6. Visual, performance, and safe security suites in isolated jobs.

## Critical business scenarios

- Registration, login, session expiry, password reset, and role enforcement.
- Shipment creation with quote, idempotency, address and parcel boundaries.
- Legal lifecycle execution through delivered; attempted delivery and return.
- Cancellation/address-change cutoff and optimistic conflict.
- Courier assignment, route start, proof upload, offline queue and sync conflict.
- Tracking timeline order, delayed processing, cache invalidation, and refresh.
- Operations filters/sort/pagination/export, manual override, flags, and audit.
- Event duplication, retry/DLT, unsupported schema, and consumer restart.
- Redis outage fallback and PostgreSQL transaction/isolation behavior.

## Exit criteria

- All blocking quality gates pass.
- No open critical defect; high defects require documented release decision.
- Required suite counts are met by enabled behavior tests.
- Regression pass rate is at least 98%, excluding approved quarantine.
- Flaky rate is below 2% over the last 20 comparable runs.
- Critical journeys meet accessibility and performance thresholds.
- Reports, environment manifest, artifacts, and trace links are retained.

## Suspension and resumption

Execution is suspended when the environment health check fails, data cleanup cannot guarantee isolation, more than 20% of cases fail with one infrastructure signature, or the target build changes mid-run. Resume requires a new run ID after health and build identity are revalidated.

## Defect data

Every defect includes build/environment, minimal reproduction, expected/actual behavior, risk, correlation/trace IDs, evidence links, affected requirement, and whether the failure is product, test, infrastructure, security, accessibility, or performance related.
