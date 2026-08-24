# Project Status

**Version:** 0.1.0

**Status date:** 2026-08-24

**Current milestone:** All nine phases implemented and release verification complete

## Implemented modules

The Maven reactor and parent relationships are implemented for:

- `apps`: customer portal, courier portal, operations dashboard.
- `services`: identity, shipment, tracking, routing, notification, warehouse.
- `test-core`, `test-data`, `test-orchestrator`.
- UI, API, contract, component, integration, event, database, end-to-end, accessibility, visual, performance, and security suites.

The API, component, integration, database, consumer-contract, UI, event, and E2E suites are fully
implemented. Local and browser-backed suites are verified; PostgreSQL, Kafka broker, and deployed
journey checks compile and discover all tests but require their documented external runtime to
execute. No placeholder classes or empty tests have been added.

## Phase 9 release verification

- Cross-document traceability now maps every fixed requirement to implemented modules, tests, and
  operational evidence; all 52 internal Markdown documents have valid local links.
- The JUnit runtime adapter emits real Allure results for every Maven test module. A clean full run
  produced 989 results: 914 passed, 75 capability-aware skips, and zero failed or broken tests.
- Remote browser execution detects `remote.url` and relies on network-resolvable deployed portal
  origins instead of starting a loopback-only fixture that Grid nodes cannot reach.
- All 27 POM files parse as XML; tracked files pass credential, unfinished-work, forbidden-pattern,
  duplicate-source, and Git whitespace audits.
- The final Java 25 `mvn -Pquality clean verify` completed all 27 reactor projects with Enforcer,
  Spotless, Checkstyle, SpotBugs, JaCoCo, packaging, and applicable behavior suites passing.

## Phase 8 delivery platform

- One version-pinned Compose definition exposes 20 components through `minimal`, `integration`,
  `grid`, `observability`, and `full` profiles with health dependencies, named volumes, an isolated
  network, restart/resource policy, and environment-only secrets.
- A non-root Java 25 runtime image packages all six services and three portals; release builds can tag
  and publish every application image to GHCR.
- All 15 requested GitHub Actions workflows are implemented with JDK/Maven caching, least privilege,
  concurrency, timeouts, evidence retention, Chrome/Firefox UI sharding, scheduled/manual lanes,
  controlled infrastructure retry, security scanning, releases, and Allure Pages publication.
- Raw Kubernetes resources provide a restricted namespace, ConfigMap, empty Secret template, Allure
  PVC, probed Selenium Deployments/Service, Chrome/Firefox HPAs, a one-off Job, and nightly CronJob.
- The reusable Helm chart parameterizes images, resources, suite execution, Grid capacity,
  autoscaling, storage, retention, scheduling, and external Secret references, with minimal values.
- OpenTelemetry, Prometheus, eight alert rules, Grafana provisioning, and an eight-panel quality
  dashboard are versioned and mounted by Compose.
- 34 parser/inventory checks validate delivery YAML, the Grafana JSON model, workflow inventory,
  Kubernetes resource kinds, and Helm resource coverage. PowerShell and Bash syntax gates pass.

## Phase 7 non-functional suites

- 15 Selenium/axe-core checks scan all three portals across five responsive breakpoints against WCAG
  A/AA rule families. The gate exposed and drove a real courier-portal color-contrast correction.
- 15 reviewed PNG baselines cover mobile, tablet, laptop, desktop, and wide-screen entry states with
  bounded per-channel tolerance, a 0.1% diff gate, and actual/diff failure evidence.
- 10 independently thresholded k6 scenarios cover health and business-read boundaries across all six
  services with smoke, load, stress, spike, and soak profiles. `k6 inspect` validates the bundle.
- 20 executable security controls protect password hashing, enumeration resistance, lockout, JWT,
  opaque credentials, rotation/reset, CORS, default deny, permissions, cache policy, concurrency,
  idempotency, trace parsing, HTTPS providers, uploads, and audit behavior.
- An allowlisted, non-destructive OWASP ZAP passive-baseline runner produces HTML and JSON evidence
  when a local Docker runtime is available.

## Phase 6 distributed-flow suites

- 47 local Kafka checks validate eleven event types, envelopes, headers, topic ownership, payloads,
  schema rejection, ordering, idempotency, retry, dead-letter handling, delay, and consumer restart.
- One additional Testcontainers check performs an actual keyed Kafka publish/consume round trip when
  Docker is available and is explicitly reported as skipped when it is not.
- 20 independent black-box E2E journeys register and authenticate unique customers, create and replay
  idempotent shipments, read the protected aggregate, and verify the public tracking projection.
- Every E2E assertion operates on actual HTTP responses and covers security headers, trace and
  correlation propagation, optimistic versions, idempotency, tracking, and customer isolation.
- Deployed E2E execution is opt-in through `e2e.enabled=true` and explicit identity and shipment
  service URLs so a developer build never reports simulated integration as a pass.

## Phase 5 browser suite

- 96 Selenium scenarios drive the real customer, courier, and operations portal assets through a
  loopback HTTP fixture, stable locators, page objects, and reusable form/toast/pagination components.
- Customer coverage includes registration, lockout, session expiry, reset privacy, shipment boundary
  validation, tracking, cancellation, address changes, profile/address book, incidents, and XSS-safe
  rendering.
- Courier coverage includes route lifecycle, legal transitions, unsuccessful-attempt reasons,
  delivery-proof MIME validation, offline queues, reconnect synchronization, filters, and logout.
- Operations coverage includes authentication, RBAC, metrics, search/filter/sort/pagination/export,
  optimistic conflicts, privileged cancellation, user roles, feature flags, network resources,
  incidents, audit records, and logout.
- JUnit parallelism and maximum pool size are both bounded; each invocation owns its driver, storage,
  trace/correlation context, cleanup, and failure evidence.
- The authentication smoke is verified in Chrome, Microsoft Edge, and Firefox. Selenium Manager runs
  offline by default after an explicit one-time driver bootstrap; remote Grid and video capabilities
  are supported through configuration.

## Phase 4 service-boundary suites

- 157 API checks cover all 33 published operations plus the identity HTTP adapter's success, failure,
  validation, method, content-type, error-envelope, correlation, and trace behavior.
- 28 WireMock component checks exercise the production notification gateway, acknowledgments,
  failure codes, idempotency, UTF-8 content, and privacy minimization.
- 20 Pact V4 consumer tests cover five owned consumer/provider pairs; a Failsafe compatibility gate
  verifies every generated request method, path, and response status against provider OpenAPI.
- 29 PostgreSQL migration, constraint, transaction, indexing, idempotency, outbox, audit, and cleanup
  checks apply all seven owned migration sets to a real PostgreSQL Testcontainer.
- 25 Spring Boot integration checks wire the production shipment application, Flyway, Spring Data,
  aggregate lifecycle, history, idempotency, and outbox against PostgreSQL.
- Service builds now produce a dependency-safe thin JAR and a separately classified `-exec.jar` for
  executable deployment packaging.

## Phase 3 test platform

The reusable test platform currently implements:

- Immutable property/environment configuration, environment-backed secret resolution, and validated
  environment profiles.
- Thread-local local/remote Selenium lifecycle with zero implicit wait and Chrome, Firefox, and Edge
  option factories.
- Reusable REST Assured, JDBC, Kafka, Redis, JSON Schema, polling, retry, cleanup, and soft-assertion
  components.
- Automatic screenshot, page-source, log, video-link, Allure, correlation-ID, and trace-ID evidence.
- JUnit lifecycle extensions, annotations, listeners, quarantine metadata, and stable failure
  classification.
- Immutable synthetic data models, fluent builders, object mothers, valid/invalid boundary datasets,
  deterministic seeded random generation, run-scoped uniqueness, cleanup, anonymization, and masking.
- A validated cross-platform suite orchestrator with preview/execute modes and unified Allure
  environment, executor, and defect-category metadata.

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

The routing service currently implements:

- Consumer-owned validation of confirmed shipment events and durable processed-event idempotency.
- Deterministic least-loaded courier selection with bounded capacity and optimistic locking.
- Unique shipment-to-route assignment, version-checked route start/completion, and atomic courier-capacity release.
- Transactional `CourierAssigned` outbox staging without customer contact/address data.
- Kafka publication with shipment ordering keys, trace/correlation headers, broker acknowledgment, local overlap suppression, bounded batches, and capped retry.
- PostgreSQL/Flyway schema, route/courier APIs, health/Prometheus endpoints, executable packaging, and OpenAPI 3.1 contract.

The notification service currently implements:

- Consumer-owned validation and idempotent processing of versioned shipment events.
- Customer-scoped notification templates that deliberately exclude address and contact information.
- PostgreSQL/Flyway delivery state with optimistic locking, bounded batches, and newest-first customer feeds.
- HTTPS-only provider delivery with stable idempotency keys, acknowledgment validation, and strict timeouts.
- Capped exponential retry, terminal dead-letter state, local overlap suppression, and delivery metrics.
- Health/Prometheus endpoints, executable Spring Boot packaging, and a linted OpenAPI 3.1 contract.

The warehouse service currently implements:

- Capacity-controlled warehouses and warehouse-owned logistics hubs with stable UUID identifiers.
- Search, sorting, pagination, and no-store operational read models.
- Version-preconditioned warehouse, hub, incident, and feature-flag commands using strong entity tags.
- Problem-delivery incidents with an explicit open/resolved lifecycle.
- Transactional append-only audit records for every state-changing operation.
- PostgreSQL constraints and indexes, Flyway migration, health/Prometheus endpoints, and OpenAPI 3.1 contract.

The application layer currently implements:

- Independently executable customer, courier, and operations Spring Boot web applications.
- Responsive desktop, tablet, and mobile layouts with skip links, focus styling, reduced-motion handling, semantic forms, live regions, and stable `data-testid` locators.
- Customer registration/login/reset, profile/address book, shipment validation/creation/list/tracking/cancellation/address change, language/theme, and incident reporting flows.
- Courier assignment filtering, route lifecycle, centralized status transitions, required failure reasons and delivery proof, offline update queues, and synchronization.
- Operations RBAC, shipment search/sort/filter/pagination/CSV export, manual versioned commands, user roles, couriers, hubs, incidents, flags, audit history, and problem-delivery metrics.

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
| Product/domain unit and asset contract | 345 | Risk-driven, no fixed final quota |
| Framework, data, and orchestrator unit | 126 | Risk-driven, no fixed final quota |
| UI | 96 | 60 |
| API | 157 | 100 |
| Component | 28 | 25 |
| Integration | 25 implemented; Docker-blocked locally | 25 |
| Contract | 20 | 20 |
| Event/Kafka | 48 implemented; 47 local + 1 Docker-backed | 30 |
| Database | 29 implemented; Docker-blocked locally | 20 |
| End-to-end | 20 implemented; deployed-stack opt-in | 20 |
| Accessibility | 15 | 15 |
| Visual | 15 | 15 |
| Performance scenarios | 10 | 10 |
| Security | 20 | 20 |
| Delivery/infrastructure contracts | 34 | Risk-driven, no fixed final quota |

Every fixed suite target is implemented. Architecture-only tests are not used to inflate the
inventory, and environment-dependent checks report explicit skips instead of simulated passes.

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
| Routing service compile/package | Passed: 29 production Java files, executable Spring Boot jar |
| Routing unit tests | Passed: 47 tests, 0 failures/errors/skips |
| Routing quality profile | Passed: Spotless clean, Checkstyle 0, SpotBugs 0 |
| Routing JaCoCo line coverage | Passed: 84.19% against current 40% milestone gate |
| Routing OpenAPI lint | Passed: Redocly CLI 2.47.0, 0 warnings |
| Notification service compile/package | Passed: 21 production Java files, executable Spring Boot jar |
| Notification unit tests | Passed: 32 tests, 0 failures/errors/skips |
| Notification quality profile | Passed: Spotless clean, Checkstyle 0, SpotBugs 0 |
| Notification JaCoCo line coverage | Passed: 73.23% against current 40% milestone gate |
| Notification OpenAPI lint | Passed: Redocly CLI 2.47.0, 0 warnings |
| Warehouse service compile/package | Passed: 22 production Java files, executable Spring Boot jar |
| Warehouse unit tests | Passed: 59 tests, 0 failures/errors/skips |
| Warehouse quality profile | Passed: Spotless clean, Checkstyle 0, SpotBugs 0 |
| Warehouse JaCoCo line coverage | Passed: 52.94% against current 40% milestone gate |
| Warehouse OpenAPI lint | Passed: Redocly CLI 2.47.0, 0 warnings |
| Portal JavaScript syntax | Passed: Node syntax validation for all 3 applications |
| Portal asset contract tests | Passed: 15 tests, 0 failures/errors/skips |
| Portal packaging | Passed: 3 executable Spring Boot jars |
| Portal quality profile | Passed: Spotless clean, Checkstyle 0, SpotBugs 0, JaCoCo gate met |
| Test Core unit tests | Passed: 72 tests, 0 failures/errors/skips |
| Test Core quality profile | Passed: Spotless clean, Checkstyle 0, SpotBugs 0, JaCoCo 59.57% |
| Test Data unit tests | Passed: 40 tests, 0 failures/errors/skips |
| Test Data quality profile | Passed: Spotless clean, Checkstyle 0, SpotBugs 0, JaCoCo 97.01% |
| Test Orchestrator unit tests | Passed: 14 tests, 0 failures/errors/skips |
| Test Orchestrator quality profile | Passed: Spotless clean, Checkstyle 0, SpotBugs 0, JaCoCo 79.39% |
| API suite | Passed: 157 tests, 0 failures/errors/skips |
| Component suite | Passed: 28 tests against a real WireMock server, 0 failures/errors/skips |
| Contract suite | Passed: 20 Pact V4 consumer tests, 0 failures/errors/skips |
| Pact/OpenAPI compatibility | Passed: all 20 generated interactions verified against provider contracts |
| Phase 4 static gates | Passed: Spotless clean, Checkstyle 0 violations, SpotBugs 0 findings |
| Phase 4 OpenAPI lint | Passed: shipment and tracking contracts valid, 0 warnings |
| Database suite discovery | Passed: 29 tests compiled/discovered; skipped because Docker is unavailable |
| Integration suite discovery | Passed: 25 tests compiled/discovered; skipped because Docker is unavailable |
| Phase 5 Chrome UI regression | Passed: 96 tests, 0 failures/errors/skips, four bounded browser workers |
| Phase 5 Edge smoke | Passed: authenticated customer journey |
| Phase 5 Firefox smoke | Passed: authenticated customer journey |
| Phase 5 quality profile | Passed: Spotless clean, Checkstyle 0 violations, SpotBugs 0 findings |
| Phase 6 event suite | Passed: 47 executed, 1 Docker-aware skip, 0 failures/errors |
| Phase 6 E2E discovery | Passed: 20 deployed journeys discovered and explicitly skipped locally |
| Phase 6 quality profile | Passed: Spotless clean, Checkstyle 0 violations, SpotBugs 0 findings |
| Phase 7 axe-core accessibility | Passed: 15 tests, 0 failures/errors/skips |
| Phase 7 visual comparison | Passed: 15 committed baselines, 0 failures/errors/skips |
| Phase 7 k6 bundle inspection | Passed: 10 scenarios and 30 scenario-specific thresholds |
| Phase 7 performance contracts | Passed: 10 tests, 0 failures/errors/skips |
| Phase 7 security controls | Passed: 20 tests, 0 failures/errors/skips |
| Phase 7 quality profile | Passed: Spotless clean, Checkstyle 0 violations, SpotBugs 0 findings |
| Phase 8 YAML and inventory contracts | Passed: 34 tests, 0 failures/errors/skips |
| Phase 8 Grafana dashboard JSON | Passed: parsed model with 8 panels |
| Phase 8 script syntax | Passed: PowerShell parser and Bash `-n` |
| Phase 8 quality profile | Passed: Spotless clean, Checkstyle 0 violations, SpotBugs 0 findings |
| Portal JavaScript syntax after UI hardening | Passed: all three application scripts |
| Phase 9 full quality reactor | Passed: 27/27 projects in 6:12, 0 failed projects |
| Unified Allure result set | Passed: 989 total, 914 passed, 75 explicit skips, 0 failed/broken |
| Final repository static audit | Passed: 52 Markdown documents, 27 POMs, no credentials, forbidden patterns, unfinished markers, or duplicate source/config files |
| OWASP Dependency-Check update | Correctly blocked before analysis without an NVD key; CI now requires the environment-backed `NVD_API_KEY` secret |
| Docker Compose validation | Not runnable: Docker is not installed |
| Kubernetes/Helm validation | Not runnable: kubectl and Helm are not installed |

## Known limitations

- Portal UI tests use deterministic browser storage; the separate Phase 6 E2E suite exercises the
  real identity and shipment HTTP boundaries against an explicitly supplied deployed environment.
- The 29 database and 25 Spring Boot integration tests are ready but cannot execute on this machine
  until a Docker-compatible runtime is available.
- Kafka broker round-trip and PostgreSQL `SKIP LOCKED` checks are implemented but cannot execute on
  this machine because Docker is unavailable.
- Concurrent first-use conflict normalization for the same idempotency key remains a future
  PostgreSQL hardening opportunity beyond the implemented sequential replay and uniqueness checks.
- Black-box tracking is covered by deployed E2E journeys; a complete local stack still needs Docker.
- Routing HTTP endpoints do not yet share the identity service's bearer-token adapter; the current
  route-domain and event boundaries are implemented and tested independently.
- Docker, CI workflows, Kubernetes, Helm, and dashboards are implemented; runtime rehearsals remain
  environment-dependent.
- The ZAP passive baseline is implemented but cannot run on this machine without Docker.
- Compose schema/startup, Kubernetes server dry-run, and Helm lint/template cannot run on this
  machine because Docker, kubectl, and Helm are not installed. Parser and inventory gates pass.
- The checked machine requires the portable JDK used during validation or a separately installed JDK 25.
- OWASP Dependency-Check 13.0.0 requires a valid NVD API key for a complete vulnerability-data
  update. CI fails closed when the `NVD_API_KEY` repository secret is absent.
- Repository URLs and `CODEOWNERS` identities use safe example organization values until a real GitHub owner is assigned.

## Future improvements

- Rehearse the full Compose, Kubernetes, and Helm deployments on a Docker-capable workstation.
- Add the shared bearer-token adapter to routing HTTP boundaries.
- Add a concurrent first-use PostgreSQL idempotency contention test and explicit loser normalization.
- Replace example repository ownership values before public release.

## Exact commands

With JDK 25 on `JAVA_HOME`:

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd -Pquality clean verify
.\mvnw.cmd help:effective-pom -Doutput=target/effective-pom.xml
.\mvnw.cmd -pl :shipment-service -am -Pquality clean verify
.\mvnw.cmd -pl :shipment-service,:tracking-service -am -Pquality clean verify
.\mvnw.cmd -pl :identity-service -Pquality clean verify
.\mvnw.cmd -pl :routing-service -Pquality clean verify
.\mvnw.cmd -pl :notification-service,:shipment-service -am -Pquality clean verify
.\mvnw.cmd -pl :warehouse-service -Pquality clean verify
.\mvnw.cmd -pl :customer-portal,:courier-portal,:operations-dashboard -am -Pquality clean verify
.\mvnw.cmd -pl :api-tests -am -Dgroups=api clean test
.\mvnw.cmd -pl :component-tests -am -Dgroups=component clean test
.\mvnw.cmd -pl :contract-tests -Dgroups=contract clean verify
.\mvnw.cmd -pl :database-tests -Dgroups=database clean test
.\mvnw.cmd -pl :integration-tests -am -Dgroups=integration clean test
.\mvnw.cmd -pl :ui-tests -am -Dgroups=ui -Dui.parallelism=4 test
.\mvnw.cmd -pl :ui-tests -am -Dgroups=ui -Dbrowser=edge test
.\mvnw.cmd -pl :ui-tests -am -Dgroups=ui -Dbrowser=firefox test
.\mvnw.cmd -pl :event-tests -am -Dgroups=event test
.\mvnw.cmd -pl :e2e-tests -am -Dgroups=e2e test
.\mvnw.cmd -pl :e2e-tests -am -Dgroups=e2e -De2e.enabled=true test
.\mvnw.cmd -pl :accessibility-tests -am -Dgroups=accessibility test
.\mvnw.cmd -pl :visual-tests -am -Dgroups=visual test
.\mvnw.cmd -pl :performance-tests -Dgroups=performance test
.\mvnw.cmd -pl :security-tests -Dgroups=security test
npx --yes allure-commandline@2.34.1 generate ui-tests/target/allure-results --clean --output target/allure-report
k6 inspect performance-tests/k6/urbanflow.js
.\scripts\check-prerequisites.ps1
.\scripts\stack.ps1 -Action Validate -Profile full
.\scripts\validate-repository.ps1
docker compose -f infrastructure/compose.yml --profile full up -d --build --wait
helm lint helm/urbanflow-test-infrastructure
```

Portable validation toolchain used on this machine:

```powershell
$env:JAVA_HOME = "$env:LOCALAPPDATA\UrbanFlowToolchain\jdk-25.0.4.1+1"
.\mvnw.cmd clean verify
```
