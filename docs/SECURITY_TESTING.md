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

## Finding policy

Confirmed critical vulnerabilities block immediately. High findings block when reachable or when exploitability cannot be determined within the PR. Medium/low findings are triaged with severity, reachability, affected build, owner, and remediation date. False positives retain reproducible evidence and expiring suppression.

## Evidence handling

Reports are masked, access-controlled by CI retention, and must not store raw bearer tokens, cookies, passwords, personal data, or uploaded file bodies.
