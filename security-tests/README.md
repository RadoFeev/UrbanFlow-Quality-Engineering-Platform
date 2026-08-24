# Security suite

Twenty executable control checks guard authentication, authorization, token lifecycle, credential
storage, lockout, CORS, cache policy, optimistic concurrency, idempotency, tracing input, HTTPS-only
delivery, upload validation, and append-only audit behavior in production source boundaries.

```powershell
.\mvnw.cmd -pl :security-tests -Dgroups=security test
```

The `zap` folder provides a passive baseline scan for an explicitly allowlisted local target. It is
non-destructive and never enables active scanning:

```powershell
.\security-tests\zap\run-baseline.ps1 -Target http://host.docker.internal:8080
```

Dependency vulnerability, secret, image, and source scans are executed by the security CI workflow.
