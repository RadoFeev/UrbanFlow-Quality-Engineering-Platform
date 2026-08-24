# Test Core

`test-core` is the reusable execution foundation shared by every UrbanFlow automated suite.

It provides immutable environment configuration, environment-backed secret resolution, thread-local
browser lifecycle, local and remote Selenium creation, reusable REST Assured specifications, JDBC,
Kafka, and Redis clients, bounded retry and polling, test cleanup, evidence attachments, correlation
and trace identifiers, JSON Schema validation, soft assertions, and JUnit extensions.

## Configuration precedence

Values are resolved in this order: JVM system property, environment variable, built-in default.
Environment variables use uppercase names with dots and hyphens converted to underscores. For
example, `-Dremote.url=http://localhost:4444` and `REMOTE_URL` address the same key.

No global WebDriver is used. `DriverManager` owns one driver per executing thread and always removes
the thread-local reference during cleanup. Browser timeouts use explicit waits; implicit wait is zero.

## Verification

```powershell
.\mvnw.cmd -pl :test-core -am -Pquality clean verify
```
