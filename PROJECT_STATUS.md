# Project Status

**Version:** 0.1.0

**Status date:** 2026-08-24

**Current milestone:** Milestone 1 complete; Milestone 2 in progress

## Implemented modules

The Maven reactor and parent relationships are implemented for:

- `apps`: customer portal, courier portal, operations dashboard.
- `services`: identity, shipment, tracking, routing, notification, warehouse.
- `test-core`, `test-data`, `test-orchestrator`.
- UI, API, contract, component, integration, event, database, end-to-end, accessibility, visual, performance, and security suites.

Most modules currently establish the executable build boundary only. `identity-service`, `shipment-service`, and `tracking-service` now contain independently executable Phase 2 vertical slices; no placeholder classes or empty tests have been added.

## Phase 2 progress

The identity service currently implements:

- Customer registration with canonical email uniqueness, central password policy, and bcrypt hashing.
- Enumeration-resistant login, configurable temporary lockout, and account-state enforcement.
- Signed HS256 access credentials with verified issuer, audience, subject, role, permissions, token ID, issue time, and expiry.
- 256-bit opaque refresh/reset credentials with irreversible stored hashes, refresh rotation/replay prevention, single-use resets, and full session revocation after password change.
- Customer, courier, operator, and admin RBAC with immutable fine-grained permission sets.
- Authenticated profile reads and permission-gated, `If-Match` version-checked role administration.
- PostgreSQL/Flyway persistence, stable security/error responses, restrictive CORS, health/Prometheus endpoints, and an OpenAPI 3.1 contract.

The shipment service currently implements:

- Spring Boot 4.1.1 executable service packaging and health/Prometheus endpoints.
- Versioned OpenAPI 3.1 contract for create, read, track, transition, and delivery-address operations.
- All 14 shipment statuses with one authoritative transition and actor-permission policy.
- Required reasons for cancellation, failed delivery, and return; required delivery proof for delivery.
- UUID aggregate identity, tracking numbers, optimistic version checks, and address-change cutoff.
- Idempotent creation with a deterministic request fingerprint and original-result replay.
- PostgreSQL/Flyway schema with constraints, indexes, soft-delete field, history, idempotency, and outbox tables.
- Transactional status history and event outbox staging with correlation and trace identifiers.
- Lease-based outbox claiming with PostgreSQL `FOR UPDATE SKIP LOCKED`, crash recovery, and local overlap protection.
- At-least-once Kafka publication with aggregate ordering keys, stable versioned envelopes, broker acknowledgment, capped exponential retry, and result/duration metrics.
- Stable error envelope plus validated/replaced correlation and W3C trace identifiers.

The tracking service currently implements:

- Consumer-owned validation of the versioned shipment event envelope and payload.
- Kafka consumption with malformed-record and unsupported-schema failure classification.
- Idempotent, monotonic PostgreSQL timeline projection keyed by event and aggregate version.
- Redis cache-aside timeline reads with bounded TTL, event-driven invalidation, corrupt-value recovery, and database fallback.
- Public `GET /api/v1/tracking/{trackingNumber}` API with no-store semantics, deterministic ordering, current status, and correlation/trace context.
- Flyway schema, executable Spring Boot packaging, health/Prometheus endpoints, OpenAPI 3.1 contract, and resilient observability metrics.

## Completed Phase 1 work

- Analyzed the initially empty workspace and initialized Git on `main`.
- Detected the original local toolchain: Java 17, no Maven, no Docker, no kubectl, no Helm; Git 2.50.0 and k6 1.4.2 available.
- Provisioned a non-system portable Temurin JDK 25.0.4.1 and Maven 3.9.16 for validation, with checksum verification.
- Added Maven Wrapper pinned to Maven 3.9.16.
- Created the multi-module reactor and centralized dependency/plugin management.
- Added Java 25, Maven, and release-dependency enforcement.
- Defined architecture, service ownership, shipment lifecycle, event flow, test strategy, risks, quality gates, data strategy, environments, CI/CD, observability, and non-functional strategies.
- Added repository hygiene, contribution policy, traceability model, and milestone plan.

## Test inventory

| Suite | Implemented behavior tests | Required final target |
|---|---:|---:|
| Product/domain unit | 192 | Risk-driven, no fixed final quota |
| UI | 0 | 60 |
| API | 0 | 100 |
| Component | 0 | 25 |
| Integration | 0 | 25 |
| Contract | 0 | 20 |
| Event/Kafka | 0 | 30 |
| Database | 0 | 20 |
| End-to-end | 0 | 20 |
| Accessibility | 0 | 15 |
| Visual | 0 | 15 |
| Performance scenarios | 0 | 10 |
| Security | 0 | 20 |

Zero is intentional at this milestone. Architecture-only tests would not verify product behavior and are not used to inflate coverage.

## Verification performed

| Check | Result |
|---|---|
| Temurin JDK archive SHA-256 | Passed |
| Maven archive SHA-512 | Passed |
| Java runtime | Temurin 25.0.4.1 LTS |
| Maven runtime | 3.9.16 |
| Maven reactor model / effective POM | Passed |
| `mvn clean verify` | Passed: 27-project reactor, 21.448 s |
| `mvn -Pquality clean verify` | Passed: Enforcer, Spotless, Checkstyle (0 violations), SpotBugs; 1:33 min |
| Post-slice full-reactor `mvn -Pquality clean verify` | Passed: 27/27 projects, 48 tests, all quality gates; 39.353 s |
| Outbox/Kafka targeted `mvn -Pquality clean verify` | Passed: 76 tests, all quality gates; 51.606 s |
| Post-outbox full-reactor `mvn -Pquality clean verify` | Passed: 27/27 projects, 76 tests, all quality gates; 1:14 min |
| Shipment/tracking targeted `mvn -Pquality clean verify` | Passed: 112 tests, packaged jars, all quality gates; 1:26 min |
| Effective dependency versions | Passed: JUnit 6.1.3, Selenium 4.47.0, REST Assured 6.0.1, Testcontainers 2.0.5, Jackson 3.2.2, OpenTelemetry 1.65.0 |
| Effective messaging versions | Passed: Spring Kafka 4.1.1, Kafka Client 4.3.1 |
| POM XML validation | Passed: 27 files |
| Governance YAML parse | Passed: 3 files |
| Repository YAML parse | Passed: 5 files |
| Internal Markdown links | Passed |
| Forbidden-pattern and credential signature scan | Passed |
| Git whitespace validation | Passed |
| Shipment service compile/package | Passed: 46 production Java files, executable Spring Boot jar |
| Shipment unit tests | Passed: 76 tests, 0 failures/errors/skips |
| Shipment quality profile | Passed: Spotless clean, Checkstyle 0, SpotBugs 0 |
| Shipment JaCoCo line coverage | Passed: 71.19% against current 40% milestone gate |
| Shipment OpenAPI lint | Passed: Redocly CLI 2.47.0, 0 warnings |
| Tracking service compile/package | Passed: 23 production Java files, executable Spring Boot jar |
| Tracking unit tests | Passed: 36 tests, 0 failures/errors/skips |
| Tracking quality profile | Passed: Spotless clean, Checkstyle 0, SpotBugs 0 |
| Tracking JaCoCo line coverage | Passed: 90.57% against current 40% milestone gate |
| Tracking OpenAPI lint | Passed: Redocly CLI 2.47.0, 0 warnings |
| Identity service compile/package | Passed: 44 production Java files, executable Spring Boot jar |
| Identity unit tests | Passed: 80 tests, 0 failures/errors/skips |
| Identity quality profile | Passed: Spotless clean, Checkstyle 0, SpotBugs 0 |
| Identity JaCoCo line coverage | Passed: 76.08% against current 40% milestone gate |
| Identity OpenAPI lint | Passed: Redocly CLI 2.47.0, 0 warnings |
| Docker Compose validation | Not runnable: Docker is not installed |
| Kubernetes/Helm validation | Not runnable: kubectl and Helm are not installed |

## Known limitations

- Routing, notification, and warehouse services and all UI applications are not implemented yet.
- Shipment persistence/migration startup and API behavior have not been container-tested because Docker is unavailable on this machine.
- Kafka broker delivery, lease recovery, and PostgreSQL `SKIP LOCKED` behavior are unit-tested but not yet verified against real containers because Docker is unavailable.
- Concurrent first-use races for the same idempotency key require the planned PostgreSQL integration test and conflict-normalization hardening.
- Black-box tracking behavior still needs container-backed Kafka/PostgreSQL/Redis coverage; Test Core and the dedicated behavior suites are not implemented yet.
- Docker, CI workflows, Kubernetes, Helm, and dashboards are scheduled for Milestone 8.
- The checked machine requires the portable JDK used during validation or a separately installed JDK 25.
- Repository URLs and `CODEOWNERS` identities use safe example organization values until a real GitHub owner is assigned.

## Next milestone

Milestone 2 continues with routing, notification, warehouse, and minimum responsive portals, followed by container-backed validation when a Docker runtime is available.

## Exact commands

With JDK 25 on `JAVA_HOME`:

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd -Pquality clean verify
.\mvnw.cmd help:effective-pom -Doutput=target/effective-pom.xml
.\mvnw.cmd -pl :shipment-service -am -Pquality clean verify
.\mvnw.cmd -pl :shipment-service,:tracking-service -am -Pquality clean verify
.\mvnw.cmd -pl :identity-service -Pquality clean verify
```

Portable validation toolchain used on this machine:

```powershell
$env:JAVA_HOME = "$env:LOCALAPPDATA\UrbanFlowToolchain\jdk-25.0.4.1+1"
.\mvnw.cmd clean verify
```
