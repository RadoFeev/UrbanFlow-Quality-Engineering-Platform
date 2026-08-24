# Quality Gates

## Pull request blocking gates

| Gate | Threshold | Evidence |
|---|---|---|
| Maven reactor | Compile and package success | Maven log and artifact manifest |
| Unit/component | 100% pass | JUnit XML and Allure |
| Smoke | 100% pass, no quarantine used to satisfy gate | JUnit XML |
| Contracts | All changed consumers compatible with providers | Pact verification report |
| Checkstyle/Spotless | Zero violations or diff | Plugin reports |
| SpotBugs | Zero high-priority findings | XML/HTML report |
| Dependency policy | No snapshots/pre-releases | Maven Enforcer |
| Security | No critical exploitable dependency or application finding | Dependency-Check/ZAP/security tests |
| Accessibility | No critical axe violations on gated pages | axe/Allure report |
| Report generation | JUnit XML and Allure results produced | Artifact check |

During Phase 2 each implemented product vertical slice has a 40% JaCoCo line-coverage floor. Identity currently has 76.08%, shipment 71.19%, and tracking 90.57% line coverage. The floor rises as API/component coverage arrives; generated models and configuration-only exclusions must be explicit and reviewed.

An unavailable required gate fails closed. Waivers are time-boxed, risk-owned, linked to a defect, and cannot be embedded as permanent ignore rules without review.

## Nightly monitored gates

- Regression pass rate ≥ 98% excluding documented quarantine.
- Flaky rate < 2% across the last 20 comparable runs.
- Execution duration regression ≤ 20% against the rolling median unless test scope changed.
- Visual differences require review; gating is enabled only by explicit workflow input.
- Performance p95/p99, error rate, throughput, and response maximums meet scenario thresholds.
- No new high/critical security finding; existing accepted findings do not increase in severity or reachability.

## Initial performance thresholds

| Operation | p95 | p99 | Max | Error rate |
|---|---:|---:|---:|---:|
| Login | 400 ms | 800 ms | 2 s | < 0.5% |
| Create shipment | 700 ms | 1.2 s | 3 s | < 1% |
| Get/track shipment | 300 ms | 600 ms | 1.5 s | < 0.5% |
| Update status | 500 ms | 900 ms | 2 s | < 1% |
| List assignments | 400 ms | 800 ms | 2 s | < 0.5% |

Thresholds are calibrated after a stable baseline; relaxing a threshold requires a versioned performance decision with capacity evidence.

## Branch protection

`main` requires review, current branch checks, resolved conversations, signed or verified commit policy where available, and the named blocking workflows. Direct pushes and force pushes are disabled for normal contributors.
