# Database test suite

This suite starts PostgreSQL 18.6 through Testcontainers, applies every production SQL migration, and
uses independent fixtures to verify persistence, relationships, constraints, timestamps, versioned
updates, soft deletion, rollback, isolation, idempotency, JSONB, audit immutability, and cleanup.

Docker must be running. When Docker is unavailable the JUnit extension reports the class as skipped;
CI treats an unexpected skip as an infrastructure failure before publishing a release.

```powershell
.\mvnw.cmd -pl :database-tests -am -Dgroups=database clean verify
```
