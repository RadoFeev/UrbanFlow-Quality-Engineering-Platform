# Technology Decisions

## Decision record

Versions were checked on 2026-08-24 against project release pages or Maven Central metadata. Snapshot, alpha, beta, milestone, preview, early-access, and release-candidate artifacts are excluded. Dependabot and the scheduled dependency workflow may propose later GA versions, but upgrades require compilation, relevant regression suites, and vulnerability review.

| Capability | Pinned version | Rationale and authoritative source |
|---|---:|---|
| Java | 25.0.4.1 LTS | Current Java 25 GA update; [Oracle JDK 25 release notes](https://www.oracle.com/java/technologies/javase/25all-relnotes.html) and [Oracle support roadmap](https://www.oracle.com/java/technologies/java-se-support-roadmap.html) |
| Maven | 3.9.16 | Current production Maven 3; Maven 4 is pre-GA; [Apache Maven downloads](https://maven.apache.org/download.cgi) |
| Spring Boot | 4.1.1 | Demo-service runtime and dependency alignment; [Maven Central metadata](https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-dependencies/maven-metadata.xml) |
| Selenium Java/Grid | 4.47.0 | Required and current stable; [Selenium downloads](https://www.selenium.dev/downloads/) |
| JUnit | 6.1.3 | Required GA; [JUnit release notes](https://docs.junit.org/6.1.3/release-notes.html) |
| REST Assured | 6.0.1 | Required GA; [REST Assured releases](https://rest-assured.io/) |
| AssertJ | 3.27.7 | Fluent Java assertions; [Maven Central metadata](https://repo1.maven.org/maven2/org/assertj/assertj-core/maven-metadata.xml) |
| Awaitility | 4.3.0 | Polling for eventual consistency without fixed waits; [Maven Central metadata](https://repo1.maven.org/maven2/org/awaitility/awaitility/maven-metadata.xml) |
| Jackson | 3.2.2 | Current GA line used for JSON mapping and schemas; [Maven Central metadata](https://repo1.maven.org/maven2/tools/jackson/jackson-bom/maven-metadata.xml) |
| Allure adapters | 2.35.4 | Unified evidence and history; [Maven Central metadata](https://repo1.maven.org/maven2/io/qameta/allure/allure-bom/maven-metadata.xml) |
| Testcontainers | 2.0.5 | Disposable real infrastructure; [Maven Central metadata](https://repo1.maven.org/maven2/org/testcontainers/testcontainers-bom/maven-metadata.xml) |
| WireMock | 3.13.2 | Component dependency virtualization; [WireMock releases](https://github.com/wiremock/wiremock/releases) |
| Pact JVM | 4.7.5 | Consumer-driven contracts and provider verification; [Maven Central metadata](https://repo1.maven.org/maven2/au/com/dius/pact/consumer/junit5/maven-metadata.xml) |
| PostgreSQL driver | 42.7.13 | PostgreSQL integration and database checks; [Maven Central metadata](https://repo1.maven.org/maven2/org/postgresql/postgresql/maven-metadata.xml) |
| Kafka clients | 4.3.1 | Event publication and test client; [Maven Central metadata](https://repo1.maven.org/maven2/org/apache/kafka/kafka-clients/maven-metadata.xml) |
| Lettuce | 7.7.0.RELEASE | Thread-safe Redis client; [Maven Central metadata](https://repo1.maven.org/maven2/io/lettuce/lettuce-core/maven-metadata.xml) |
| Flyway | 13.3.0 | Versioned, validated PostgreSQL migrations; [Maven Central metadata](https://repo1.maven.org/maven2/org/flywaydb/flyway-core/maven-metadata.xml) |
| axe Selenium | 4.13.0 | Automated accessibility analysis; [Maven Central metadata](https://repo1.maven.org/maven2/com/deque/html/axe-core/selenium/maven-metadata.xml) |
| OpenTelemetry BOM | 1.65.0 | Standard trace propagation and export; [Maven Central metadata](https://repo1.maven.org/maven2/io/opentelemetry/opentelemetry-bom/maven-metadata.xml) |
| Micrometer Prometheus | 1.17.1 | Application and test metrics; [Maven Central metadata](https://repo1.maven.org/maven2/io/micrometer/micrometer-registry-prometheus/maven-metadata.xml) |
| k6 | 1.4.2 | Available locally; JavaScript scenarios, thresholds, Prometheus/Grafana integration; [k6 releases](https://github.com/grafana/k6/releases) |

Build plugins are pinned in the root POM: Clean 3.5.0, Resources 3.5.0, Compiler 3.15.0, Surefire/Failsafe 3.5.6, Jar 3.5.1, Install/Deploy 3.1.4, Source 3.4.0, Javadoc 3.12.0, Enforcer 3.6.3, JaCoCo 0.8.15, Spotless 3.10.0, Checkstyle plugin 3.6.0 with Checkstyle 14.0.0, SpotBugs 4.10.4.0, and OWASP Dependency-Check 13.0.0. Their exact metadata sources are under `https://repo1.maven.org/maven2/` using their published group and artifact paths.

## Architectural choices

### Multi-module monorepo

One reactor keeps the demo product, test framework, test suites, contracts, schemas, and deployment assets versioned together. Module boundaries still enforce ownership and allow targeted `-pl` builds.

### Spring Boot only for demo services

The test framework remains plain Java and does not require a product framework. Services use Spring Boot for realistic HTTP, validation, persistence, security, Kafka, Redis, metrics, and health behavior without custom infrastructure code obscuring the quality-engineering focus.

### PostgreSQL source of truth; Kafka integration; Redis acceleration

Business state is durable in PostgreSQL. Events use a transactional outbox so database commit and eventual publication cannot diverge silently. Redis holds reconstructable data only: caches, sessions, rate limits, and leases.

### OpenAPI and versioned event schemas

HTTP APIs are contract-first OpenAPI 3.1. Events use versioned schemas and envelopes. Compatibility checks run before merge; schema evolution favors additive change.

### Selenium WebDriver lifecycle

Drivers are owned by a JUnit extension and kept in a per-test thread-confined resource. There is no global static driver, PageFactory, implicit wait, or fixed sleep. Remote sessions use Selenium Grid and attach video where enabled.

### k6 rather than Gatling

k6 cleanly separates performance workload code from Java functional suites, provides explicit thresholds, supports Prometheus/Grafana, and is already available in the validated environment.

### Safe security automation

ZAP baseline and purpose-built regression checks target only the local demo system. Active scanning requires an explicit opt-in environment guard and an allowlisted base URL.
