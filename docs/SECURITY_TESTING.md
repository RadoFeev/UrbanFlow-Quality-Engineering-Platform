# Security Testing

## Scope and safety

Security automation targets only UrbanFlow environments explicitly allowlisted for testing. No production targets, destructive payloads, persistence mechanisms, denial-of-service techniques, credential stuffing, or third-party systems are permitted. Active ZAP scanning requires `SECURITY_ACTIVE_SCAN=true` plus a local/CI hostname allowlist.

## Coverage

- Authentication: missing, malformed, expired, wrong-audience, wrong-issuer, and tampered JWTs.
- Authorization: role boundaries, ownership, IDOR, administrative operations, and feature flags.
- Input: benign injection probes, malformed JSON, mass assignment, path/query boundaries.
- Transport: security headers, CORS, content types, cache directives, and error disclosure.
- Abuse: rate limits, duplicate requests, upload size/type/signature, and filename handling.
- Data: sensitive response/log/evidence exposure and secret scanning.
- Supply chain: OWASP Dependency-Check, Dependabot, pinned actions/images, and artifact inventory.

## ZAP

The baseline scan runs against the local demo deployment and publishes HTML/JSON reports. Authenticated context uses a synthetic least-privilege account. Alert suppression requires rule ID, route scope, evidence, risk owner, expiry date, and justification.

## Dependency vulnerability gate

OWASP Dependency-Check runs in CI with a CVSS 7 failure threshold and publishes HTML/JSON reports.
The `security-scan.yml` workflow fails closed unless the repository secret `NVD_API_KEY` is
configured. The key is passed by environment-variable name, so it is not exposed in Maven command
arguments or committed configuration.

For local execution, populate `NVD_API_KEY` through the workstation's secret-management mechanism,
then run:

```powershell
if (-not $env:NVD_API_KEY) { throw 'NVD_API_KEY is required' }
.\mvnw.cmd org.owasp:dependency-check-maven:check `
  -DnvdApiKeyEnvironmentVariable=NVD_API_KEY `
  -DfailBuildOnCVSS=7 `
  -Dformats=HTML,JSON
```

The key requirement and secure Maven parameters are documented by the
[Dependency-Check Maven plugin](https://jeremylong.github.io/DependencyCheck/dependency-check-maven/check-mojo.html).
Version 13.0.0 currently rejects an empty key during the NVD update; the behavior is tracked in the
[upstream issue](https://github.com/dependency-check/DependencyCheck/issues/8715).

## Finding policy

Confirmed critical vulnerabilities block immediately. High findings block when reachable or when exploitability cannot be determined within the PR. Medium/low findings are triaged with severity, reachability, affected build, owner, and remediation date. False positives retain reproducible evidence and expiring suppression.

## Evidence handling

Reports are masked, access-controlled by CI retention, and must not store raw bearer tokens, cookies, passwords, personal data, or uploaded file bodies.
