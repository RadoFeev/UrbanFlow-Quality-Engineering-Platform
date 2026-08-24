# Changelog

All notable changes follow [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and Semantic Versioning.

## [Unreleased]

No unreleased changes.

## [0.1.0] - 2026-08-24

### Added

- Java 25/Maven 3.9.16 multi-module platform with six Spring Boot services, three responsive
  portals, reusable test libraries, and twelve dedicated automation-suite modules.
- Identity, shipment, tracking, routing, notification, and warehouse behavior with PostgreSQL/Flyway,
  Kafka event processing, Redis tracking cache, optimistic locking, idempotency, audit, RBAC, and
  OpenAPI 3.1 contracts.
- Thread-safe Selenium, REST Assured, JDBC, Kafka, Redis, data-generation, orchestration, cleanup,
  masking, tracing, and Allure evidence components.
- Behavior coverage above every fixed target: 96 UI, 157 API, 28 component, 25 integration, 20
  contract, 48 event, 29 database, 20 E2E, 15 accessibility, 15 visual, 10 performance, and 20
  security scenarios.
- Five Docker Compose profiles, Selenium Grid, Pact Broker, Kafka UI, OpenTelemetry, Prometheus,
  Grafana, Kubernetes resources, a reusable Helm chart, and 15 GitHub Actions workflow entry points.
- Maven Wrapper, stable dependency/plugin catalog, static-analysis and coverage gates, semantic
  release workflow, repository governance, architecture and test diagrams, traceability matrix,
  runbooks, and release-readiness evidence.
- Runtime Allure JUnit adapter, unified local result generation, and CI Pages report publication.

### Fixed

- Remote Selenium execution now uses network-resolvable deployed portal URLs without starting an
  unreachable loopback portal fixture on Grid clients.
- Dependency scanning now requires an environment-backed NVD key and fails explicitly before an
  unauthenticated or incomplete vulnerability update can be mistaken for a clean result.

[Unreleased]: https://github.com/your-org/urbanflow-quality-platform/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/your-org/urbanflow-quality-platform/releases/tag/v0.1.0
