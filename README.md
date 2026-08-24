# UrbanFlow Quality Engineering Platform

UrbanFlow is a production-style quality engineering monorepo for a distributed courier and logistics platform. It combines realistic customer, courier, and operations applications with independent services and a reusable automation platform spanning UI, API, contracts, components, integrations, events, databases, end-to-end flows, accessibility, visual regression, performance, and security.

> **Delivery status:** Phase 1 (architecture and executable repository foundation) is complete. Phase 2 is underway with compiled and quality-gated shipment publication plus tracking projection/cache vertical slices. Remaining application code and product tests are delivered incrementally according to [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md). The current verified scope is recorded in [PROJECT_STATUS.md](PROJECT_STATUS.md).

## Business domain

The platform models customer registration, shipment creation, courier assignment, hub processing, route execution, live tracking, delivery proof, notifications, incidents, audit records, and feature flags. UUID identifiers are used throughout. Concurrently modified aggregates use optimistic locking, and shipment transitions are controlled by one authoritative state machine.

## Key capabilities

- Three responsive web applications: customer portal, courier portal, and operations dashboard.
- Six independently deployable services: identity, shipment, tracking, routing, notification, and warehouse.
- PostgreSQL persistence, Kafka domain events, and Redis caching, sessions, rate limits, and distributed locks.
- Thread-safe Java test framework with isolated browser, API, data, evidence, and cleanup lifecycles.
- Local, Docker Compose, Selenium Grid, Kubernetes, and Helm execution models.
- Allure reporting plus OpenTelemetry, Prometheus, and Grafana observability.
- CI quality gates for correctness, contracts, accessibility, security, code quality, and reports.

## System architecture

```mermaid
flowchart LR
    subgraph Channels
        CP[Customer portal]
        CR[Courier portal]
        OD[Operations dashboard]
    end
    GW[HTTP API boundary]
    ID[Identity service]
    SH[Shipment service]
    TR[Tracking service]
    RT[Routing service]
    NT[Notification service]
    WH[Warehouse service]
    PG[(PostgreSQL)]
    RD[(Redis)]
    KF[(Kafka)]
    OBS[OpenTelemetry / Prometheus / Grafana]

    CP --> GW
    CR --> GW
    OD --> GW
    GW --> ID
    GW --> SH
    GW --> TR
    GW --> RT
    GW --> WH
    ID --> PG
    SH --> PG
    TR --> PG
    WH --> PG
    ID --> RD
    TR --> RD
    SH <--> KF
    RT <--> KF
    TR <--> KF
    NT <--> KF
    WH <--> KF
    GW -. traces .-> OBS
    KF -. metrics and traces .-> OBS
```

## Test architecture

```mermaid
flowchart TB
    ORCH[Test orchestrator]
    CORE[Test Core]
    DATA[Test Data]
    ORCH --> UI[UI / accessibility / visual]
    ORCH --> API[API / component / contract]
    ORCH --> INT[Integration / database / event]
    ORCH --> E2E[End-to-end]
    ORCH --> NFR[Performance / security]
    CORE --> UI
    CORE --> API
    CORE --> INT
    CORE --> E2E
    DATA --> UI
    DATA --> API
    DATA --> INT
    DATA --> E2E
    UI --> REPORT[Allure evidence]
    API --> REPORT
    INT --> REPORT
    E2E --> REPORT
    NFR --> REPORT
```

## Shipment lifecycle

```mermaid
stateDiagram-v2
    [*] --> CREATED
    CREATED --> CONFIRMED
    CREATED --> CANCELLED
    CONFIRMED --> COURIER_ASSIGNED
    CONFIRMED --> CANCELLED
    COURIER_ASSIGNED --> PICKUP_IN_PROGRESS
    COURIER_ASSIGNED --> CANCELLED
    PICKUP_IN_PROGRESS --> PICKED_UP
    PICKED_UP --> AT_ORIGIN_HUB
    AT_ORIGIN_HUB --> IN_TRANSIT
    IN_TRANSIT --> AT_DESTINATION_HUB
    AT_DESTINATION_HUB --> OUT_FOR_DELIVERY
    OUT_FOR_DELIVERY --> DELIVERED
    OUT_FOR_DELIVERY --> DELIVERY_ATTEMPTED
    DELIVERY_ATTEMPTED --> OUT_FOR_DELIVERY
    DELIVERY_ATTEMPTED --> RETURN_IN_PROGRESS
    RETURN_IN_PROGRESS --> RETURNED
    DELIVERED --> [*]
    CANCELLED --> [*]
    RETURNED --> [*]
```

All transitions not shown are rejected. Terminal states cannot transition. The definitive rules and actor permissions live in the shipment domain, not in UI code.

## Event flow

```mermaid
sequenceDiagram
    participant C as Customer portal
    participant S as Shipment service
    participant P as PostgreSQL
    participant K as Kafka
    participant R as Routing service
    participant T as Tracking service
    participant N as Notification service
    C->>S: Create shipment + idempotency key
    S->>P: Persist shipment and outbox atomically
    S-->>C: 201 + correlation/trace IDs
    S->>K: ShipmentCreated
    K->>R: ShipmentCreated
    R->>K: CourierAssigned
    K->>T: CourierAssigned / status events
    T->>P: Append tracking history
    K->>N: NotificationRequested
    N-->>C: Delivery notification
```

## Technology stack

Java 25 LTS, Maven 3.9.16, Spring Boot 4.1.1 for demo services, Selenium 4.47.0, JUnit 6.1.3, REST Assured 6.0.1, AssertJ, Awaitility, Jackson, Allure, Testcontainers, WireMock, Pact JVM, PostgreSQL, Redis, Kafka, axe-core, k6, OWASP ZAP, OpenTelemetry, Prometheus, Grafana, Docker Compose, Selenium Grid, Kubernetes, Helm, GitHub Actions, JaCoCo, Spotless, Checkstyle, SpotBugs, and OWASP Dependency-Check.

Version choices, verification dates, and source links are maintained in [docs/TECHNOLOGY_DECISIONS.md](docs/TECHNOLOGY_DECISIONS.md).

## Repository structure

```text
apps/                    Customer, courier, and operations web applications
services/                Identity, shipment, tracking, routing, notification, warehouse
test-core/               Reusable thread-safe automation framework
test-data/               Builders, factories, fixtures, anonymization, cleanup
ui-tests/                Selenium user-journey tests
api-tests/               REST Assured black-box API tests
contract-tests/          Pact consumer and provider verification
component-tests/         Service tests with controlled adapters
integration-tests/       Cross-adapter tests with real infrastructure
event-tests/             Kafka schema, ordering, retry, DLT, idempotency tests
database-tests/          PostgreSQL migration, constraint, transaction tests
e2e-tests/               Full business flows across UI, API, events, and storage
accessibility-tests/      axe-core WCAG 2.2 AA checks
visual-tests/             Multi-viewport baseline and diff checks
performance-tests/        k6 smoke, load, stress, spike, and soak scenarios
security-tests/           Safe local security regression suite
test-orchestrator/        Suite selection, sharding, environment, unified reporting
observability/            Collector, Prometheus, Grafana, dashboards
infrastructure/           Docker Compose and local platform configuration
kubernetes/               Raw Kubernetes manifests
helm/                     Parameterized test-infrastructure chart
docs/                     Strategy, architecture, operations, and governance
scripts/                  Cross-platform developer and CI entry points
```

## Prerequisites

- JDK 25 LTS (`java -version` must report 25.x).
- Git 2.40 or newer.
- Docker Engine 27+ with Compose v2 for container-backed suites.
- Chrome or Chromium, Firefox, and optionally Edge for local UI execution.
- k6 1.4+ for performance execution.
- kubectl, Helm, and kind or minikube only for Kubernetes workflows.

Maven does not need to be installed; the repository pins Maven 3.9.16 through Maven Wrapper.

## Quick start

```bash
git clone https://github.com/your-org/urbanflow-quality-platform.git
cd urbanflow-quality-platform
./mvnw clean verify
```

Windows PowerShell:

```powershell
git clone https://github.com/your-org/urbanflow-quality-platform.git
Set-Location urbanflow-quality-platform
.\mvnw.cmd clean verify
```

The current Phase 1 reactor contains no product tests; a successful build validates the module graph, dependency catalog, build prerequisites, and repository model. Test counts are never inflated with empty tests.

## Local and Docker execution

Planned execution contracts are stable even while suites are implemented:

```bash
./mvnw clean verify
./mvnw clean verify -Dgroups=smoke
./mvnw clean verify -Dgroups=api
./mvnw clean verify -Dgroups=e2e
./mvnw clean verify -Dbrowser=firefox -Dheadless=false
./mvnw clean verify -Dremote.url=http://localhost:4444
```

Docker Compose profiles will be exposed as `minimal`, `integration`, `grid`, `observability`, and `full`. Until Phase 8, Docker commands are intentionally not advertised as runnable; see [PROJECT_STATUS.md](PROJECT_STATUS.md).

## Test suites and taxonomy

JUnit tags are `smoke`, `regression`, `ui`, `api`, `contract`, `component`, `integration`, `event`, `database`, `e2e`, `accessibility`, `visual`, `performance`, `security`, and `quarantine`. The suite-to-pipeline rules and retry policy are defined in [docs/TEST_STRATEGY.md](docs/TEST_STRATEGY.md) and [docs/FLAKY_TEST_POLICY.md](docs/FLAKY_TEST_POLICY.md).

## Configuration

Configuration resolution follows this precedence: JVM system property, environment variable, environment profile file, safe default. Secrets are resolved from environment or a configured secret provider and are never committed. Logs and Allure attachments mask passwords, tokens, authorization headers, email local parts, phone numbers, and delivery-proof metadata.

## Reports and observability

All Java suites emit JUnit XML and Allure results. Failed UI flows attach a screenshot, page source, browser logs, and available network/video evidence. API and event failures attach masked request/response or event envelopes. Correlation and trace IDs are carried through HTTP, Kafka, logs, and Allure to support a single-flow investigation.

## CI/CD

```mermaid
flowchart LR
    PR[Pull request] --> STATIC[Compile + style + static analysis]
    STATIC --> FAST[Unit + component + smoke]
    FAST --> CONTRACT[Contract compatibility]
    CONTRACT --> GATES[Security + accessibility gates]
    GATES --> MERGE[Merge eligible]
    MERGE --> NIGHTLY[Sharded nightly regression]
    NIGHTLY --> REPORT[Allure Pages + trend metrics]
    REPORT --> RELEASE[Versioned release]
```

Fifteen workflow entry points are delivered in Phase 8. Their required quality gates are already specified in [docs/QUALITY_GATES.md](docs/QUALITY_GATES.md) and [docs/CI_CD.md](docs/CI_CD.md).

## Troubleshooting

Start with [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md). The most common Phase 1 failure is running the wrapper with Java 17 or 21; the build intentionally requires Java 25.

## Roadmap

The nine delivery phases, dependencies, exit criteria, test targets, and verification commands are tracked in [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md). No milestone is marked complete before its applicable build and tests pass.

## Contributing

Read [docs/CONTRIBUTING.md](docs/CONTRIBUTING.md) before opening a change. Conventional Commits, small reviewable changes, independent tests, deterministic data, and evidence-backed quality gates are required.

## License

This project is licensed under the [MIT License](LICENSE).
