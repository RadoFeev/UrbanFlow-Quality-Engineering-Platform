# Changelog

All notable changes follow [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and Semantic Versioning.

## [Unreleased]

### Planned

- Demo services, portals, Test Core, behavior suites, infrastructure, CI/CD, observability, Kubernetes, and Helm according to the implementation plan.

### Added

- Shipment-service vertical slice with lifecycle policy, optimistic locking, idempotent creation, PostgreSQL/Flyway schema, status history, traceable HTTP API, and OpenAPI 3.1 contract.
- Lease-safe transactional outbox publication with Spring Kafka, stable envelopes and ordering keys, broker acknowledgments, capped retry, bounded metrics, and 76 passing shipment unit cases.

## [0.1.0] - 2026-08-24

### Added

- Java 25/Maven 3.9.16 multi-module repository foundation.
- Pinned stable dependency and build-plugin catalog.
- Architecture, domain lifecycle, event flow, risk analysis, strategy, quality gates, environment, observability, non-functional, contribution, and troubleshooting documentation.
- Maven Wrapper, repository hygiene, governance, and milestone/status tracking.
