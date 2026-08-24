# Architecture

## Quality attributes

The architecture prioritizes testability, deterministic recovery, observability, data integrity, horizontal scaling, least privilege, and independent deployment. Demo-system complexity exists only where it creates meaningful quality risks to test.

## Bounded ownership

| Service | Owns | Primary interactions |
|---|---|---|
| Identity | User, Role, Permission, Customer, authentication sessions | HTTP; Redis sessions/rate counters; audit events |
| Shipment | Shipment, Parcel, Address snapshot, lifecycle policy | HTTP; PostgreSQL; shipment domain events |
| Tracking | TrackingEvent and customer-facing timeline projection | HTTP; Kafka consumer; PostgreSQL; Redis cache |
| Routing | DeliveryRoute, RouteStop, courier assignment | HTTP; Kafka consumer/producer; warehouse lookup |
| Notification | Notification and delivery attempts | Kafka consumer; provider adapter; retry/DLT |
| Warehouse | Warehouse, LogisticsHub, capacity/operational data | HTTP; PostgreSQL; warehouse events |

DeliveryProof and Incident are handled by shipment/courier flows; AuditRecord is append-only and receives security and business actions. FeatureFlag is administered through operations and consumed through a cached, auditable interface.

No service reads another service's tables. Cross-service queries use HTTP; asynchronous state changes use Kafka. API composition belongs at the channel boundary, not in repositories.

## Shipment aggregate

`Shipment` is the consistency boundary for address changes, cancellation, courier assignment, and status transition. It contains a UUID, customer ID, immutable parcel snapshot, pickup/delivery address snapshots, delivery type, tracking number, status, version, timestamps, and cancellation metadata. A central transition policy validates current state, target state, actor permission, required evidence/reason, and version.

Optimistic conflicts return HTTP 409 with a stable error code, current version, correlation ID, and trace ID. Idempotent write endpoints persist the idempotency key, normalized request hash, response status, and response body reference.

## Persistence and messaging consistency

State changes and outbox records commit in one PostgreSQL transaction. Publisher workers claim ready rows with `FOR UPDATE SKIP LOCKED`, a process-unique owner, and an expiring lease. A complete batch is dispatched before bounded acknowledgment waits; acknowledged rows are then marked published. Failures release their lease with capped exponential backoff, and abandoned leases become reclaimable after a crash. This boundary is deliberately at-least-once, so consumers persist processed event IDs with their state change and duplicates become acknowledged no-ops. Exhausted consumer retries route an enriched original envelope to a dead-letter topic.

## HTTP conventions

- OpenAPI 3.1, JSON, UTC ISO-8601 timestamps, UUID identifiers.
- `Idempotency-Key` on retriable writes.
- `If-Match` or explicit version on concurrent updates.
- `X-Correlation-ID` accepted or created; W3C `traceparent` propagated.
- Consistent error body: code, message, field violations, timestamp, path, correlation ID, trace ID.
- Pagination uses zero-based `page`, bounded `size`, stable sort, and total metadata.
- JWT authentication; RBAC at both route and domain-operation layers.

## Event envelope

Every domain event contains `eventId`, `eventType`, `aggregateId`, `aggregateVersion`, `occurredAt`, `schemaVersion`, `correlationId`, `traceId`, producer, and payload. Kafka key is the aggregate UUID to preserve per-shipment ordering. Consumers reject unsupported major schemas and tolerate additive minor fields.

## Test seams

- Clock, ID generator, notification provider, and file storage are injected ports.
- External dependencies are replaceable with WireMock in component tests.
- Containers provide PostgreSQL, Kafka, and Redis for integration tests.
- Stable `data-testid` selectors are an explicit UI contract.
- Admin-only test support endpoints exist only under the `test` profile and require a generated run token.

## Parallel safety

Every run receives a run ID; every test receives a case ID. Unique business keys embed these IDs. Drivers, soft assertions, correlation IDs, evidence paths, and cleanup registries are per-test resources. Database cleanup targets only rows labeled with the run ID. Kafka consumers use unique group IDs, and topics are namespaced where isolation needs exceed key isolation.

## Observability path

HTTP filters attach correlation IDs to response headers and structured logs. OpenTelemetry propagates spans across HTTP and Kafka. Test steps record trace IDs in Allure. Metrics expose response distributions, consumer lag/processing delay, retry/DLT counts, and test outcomes.

## Deployment model

Local execution uses direct processes or Compose profiles. CI uses isolated Compose projects and sharded jobs. Kubernetes uses a dedicated namespace, least-privilege service accounts, ConfigMaps, secret templates, Deployments/Services, a test Job, nightly CronJob, persistent Allure result storage, and autoscaled Selenium nodes. Helm parameterizes images, resources, storage, and suite settings.
