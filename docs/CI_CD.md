# CI/CD

## Workflow inventory

| Workflow | Trigger | Blocking purpose |
|---|---|---|
| `pull-request-checks.yml` | Pull request | Reactor, unit/component, style/static checks |
| `smoke-tests.yml` | Push/manual | Cross-layer smoke |
| `api-tests.yml` | PR/push/manual | Sharded API coverage |
| `ui-tests.yml` | PR/push/manual | Chrome/Firefox matrix and evidence |
| `contract-tests.yml` | PR/push | Pact compatibility gate |
| `integration-tests.yml` | PR/push/manual | PostgreSQL/Kafka/Redis integrations |
| `e2e-tests.yml` | Push/manual | Critical full flows |
| `accessibility-tests.yml` | PR/push | WCAG 2.2 AA critical gate |
| `visual-tests.yml` | Manual/nightly | Baseline comparison; opt-in block |
| `performance-tests.yml` | Manual/nightly/release | k6 thresholds and trends |
| `security-scan.yml` | PR/schedule/manual | Dependency and ZAP/security checks |
| `nightly-regression.yml` | Schedule/manual | Full sharded regression and flaky metrics |
| `dependency-update.yml` | Schedule/manual | Version report and safe update validation |
| `release.yml` | Version tag/manual | Semantic artifacts and release notes |
| `publish-allure.yml` | Workflow completion/manual | Allure history and Pages publication |

## Standard job pattern

Each job uses least-privilege permissions, concurrency cancellation for superseded branch work, wrapper validation, JDK 25, Maven cache keyed by POM/wrapper files, explicit timeout, environment capability check, deterministic run ID, and always-run artifact upload. Secrets are unavailable to untrusted fork code.

The security workflow requires an `NVD_API_KEY` repository secret. Dependency-Check receives only
the environment-variable name and fails closed if the secret is absent; vulnerability reports are
still uploaded by the always-run evidence step when analysis completes or fails.

All 15 named workflows are implemented under `.github/workflows`; a private reusable workflow keeps
the JDK/cache/evidence pattern identical. UI uses a Chrome/Firefox and customer/operations-courier
matrix. Integration and browser lanes may retry once only through the allowlisted infrastructure
classifier, which retains both attempt logs.

Recommended `main` branch protection requires the pull-request quality job plus API, contract,
integration, accessibility, and all four UI matrix checks. Require the branch to be current, reject
force pushes, require one review and CODEOWNERS review, dismiss stale approvals, and prevent
administrators from bypassing critical security/contract gates.

## Sharding

Tests receive stable shards from a historical-duration manifest. New tests use class-name hashing until enough history exists. A merge job checks that all discovered tests appear in exactly one shard and combines JUnit/Allure results without double-counting retries.

## Infrastructure retry

Workflow reruns are not automatic for assertion failures. A single job retry is permitted only when provisioning/runner/Grid/container signatures match an allowlisted infrastructure classifier. The original attempt is retained and contributes to flaky/infrastructure metrics.

## Artifact retention

JUnit XML, Allure results, screenshots, logs, page sources, network captures, visual diffs, k6 summaries, ZAP reports, dependency reports, Pact results, Compose logs, and environment manifests are uploaded under the run ID. Sensitive values are masked before persistence.

## Release

Semantic tags `vMAJOR.MINOR.PATCH` create immutable artifacts, SBOM/dependency reports, changelog excerpt, and release notes. A release references the exact container digests, Helm chart version, contract versions, and verification run.
