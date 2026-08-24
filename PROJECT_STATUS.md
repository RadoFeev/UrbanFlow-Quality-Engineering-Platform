# Project Status

**Version:** 0.1.0

**Status date:** 2026-08-24

**Current milestone:** Milestone 1 complete; Milestone 2 not started

## Implemented modules

The Maven reactor and parent relationships are implemented for:

- `apps`: customer portal, courier portal, operations dashboard.
- `services`: identity, shipment, tracking, routing, notification, warehouse.
- `test-core`, `test-data`, `test-orchestrator`.
- UI, API, contract, component, integration, event, database, end-to-end, accessibility, visual, performance, and security suites.

These modules currently establish the executable build boundary only. Product and test source code begins in Milestone 2 and Milestone 3; no placeholder classes or empty tests have been added.

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
| Effective dependency versions | Passed: JUnit 6.1.3, Selenium 4.47.0, REST Assured 6.0.1, Testcontainers 2.0.5, Jackson 3.2.2, OpenTelemetry 1.65.0 |
| POM XML validation | Passed: 27 files |
| Governance YAML parse | Passed: 3 files |
| Internal Markdown links | Passed |
| Forbidden-pattern and credential signature scan | Passed |
| Git whitespace validation | Passed |
| Docker Compose validation | Not runnable: Docker is not installed |
| Kubernetes/Helm validation | Not runnable: kubectl and Helm are not installed |

## Known limitations

- Demo services and UI applications are not implemented yet.
- Test Core and behavior suites are not implemented yet.
- Docker, CI workflows, Kubernetes, Helm, and dashboards are scheduled for Milestone 8.
- The checked machine requires the portable JDK used during validation or a separately installed JDK 25.
- Repository URLs and `CODEOWNERS` identities use safe example organization values until a real GitHub owner is assigned.

## Next milestone

Milestone 2 begins with shared service conventions, identity and shipment vertical slices, Flyway migrations, PostgreSQL persistence, the transactional outbox, Kafka event envelopes, Redis-backed concerns, OpenAPI, and minimum responsive portals.

## Exact commands

With JDK 25 on `JAVA_HOME`:

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd help:effective-pom -Doutput=target/effective-pom.xml
```

Portable validation toolchain used on this machine:

```powershell
$env:JAVA_HOME = "$env:LOCALAPPDATA\UrbanFlowToolchain\jdk-25.0.4.1+1"
.\mvnw.cmd clean verify
```
