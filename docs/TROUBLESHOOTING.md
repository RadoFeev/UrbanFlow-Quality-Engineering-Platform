# Troubleshooting

## Build requires Java 25

Check `java -version` and `JAVA_HOME`. Maven Enforcer intentionally rejects Java 17/21 and Java 26+. On Windows:

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-25'
.\mvnw.cmd -version
.\mvnw.cmd clean verify
```

## Wrapper cannot download Maven

Verify access to Maven Central, corporate proxy settings in `~/.m2/settings.xml`, and the distribution URL in `.mvn/wrapper/maven-wrapper.properties`. Do not bypass TLS validation or commit proxy credentials.

## Testcontainers cannot start

Confirm Docker is installed/running and `docker version` works for the same user. Inspect Docker context and available disk/memory. Do not disable resource cleanup globally; capture container logs and the environment manifest.

## Browser session fails

For local runs, confirm the browser exists and Selenium Manager can access driver metadata. For Grid, inspect `/status`, requested capabilities, node slots, and video sidecar health. Edge is optional only when the environment capability check declares it unavailable.

## Async test times out

Use the attached last-observed state, correlation ID, trace ID, Kafka topic/partition/offset, and service logs. Do not add a fixed sleep. Determine whether publication, consumption, projection, cache invalidation, or the assertion condition is wrong.

## Port collision

Use Compose project names and dynamically mapped test-container ports. Do not hardcode local service ports in tests; resolve endpoints through environment configuration.

## Stale data or cleanup failure

Search by run ID, inspect the cleanup attachment, and remove only resources owned by that run. Never truncate shared databases or delete broad directories as a recovery shortcut.

## Missing report evidence

Ensure the suite writes Allure results under the module `target/allure-results`, and verify always-run artifact collection. Evidence services must log attachment errors without masking the original test failure.
