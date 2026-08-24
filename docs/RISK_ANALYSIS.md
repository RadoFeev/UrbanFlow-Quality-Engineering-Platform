# Risk Analysis

Scores use probability (P) and impact (I) from 1 to 5; exposure is `P × I`. Scores 15–25 are critical, 8–14 high, 4–7 medium, and 1–3 low. Residual risk is reviewed after each milestone.

| ID | Risk | P | I | Exposure | Primary controls | Automated evidence |
|---|---|---:|---:|---:|---|---|
| R-01 | Illegal shipment transition corrupts lifecycle | 4 | 5 | 20 | Central state machine, actor permissions, version checks | Unit, API, event, DB, E2E transition suites |
| R-02 | Duplicate write or event creates duplicate shipment/action | 4 | 5 | 20 | Idempotency keys, request hashes, unique constraints, processed-event ledger | API concurrency, event duplicate, DB unique tests |
| R-03 | DB commit succeeds but event is lost | 3 | 5 | 15 | Transactional outbox, publication metrics, replay | Integration, restart, eventual-consistency E2E |
| R-04 | Out-of-order events regress tracking status | 4 | 4 | 16 | Aggregate version and Kafka key ordering, monotonic projection | Event ordering and delayed-delivery tests |
| R-05 | Unauthorized operations or IDOR expose shipments | 4 | 5 | 20 | Ownership checks, RBAC, audit, negative security suite | API/UI authorization, IDOR, audit tests |
| R-06 | Sensitive data leaks through logs or evidence | 3 | 5 | 15 | Central masker, attachment filters, synthetic data | Masking unit tests, security log scans |
| R-07 | Optimistic updates silently overwrite changes | 3 | 5 | 15 | Version column and conflict response | DB, API concurrency, operations UI conflict tests |
| R-08 | Redis stale cache contradicts source of truth | 4 | 4 | 16 | Event-driven invalidation, TTL, read-through fallback | Cache hit/miss/TTL/invalidation/failure tests |
| R-09 | Notification retry produces repeated customer messages | 3 | 4 | 12 | Idempotent provider key and delivery-attempt ledger | Component and event retry/DLT tests |
| R-10 | Offline courier sync applies invalid stale actions | 3 | 5 | 15 | Ordered local queue, versions, transition revalidation, conflict UI | Offline sync UI and E2E conflict tests |
| R-11 | Accessibility barriers block core journeys | 3 | 4 | 12 | Semantic components, keyboard design, WCAG 2.2 AA gate | axe-core plus manual checklist |
| R-12 | Browser timing creates false failures | 4 | 3 | 12 | Explicit condition waits, stable selectors, isolated data | Framework unit tests, cross-browser history |
| R-13 | Test retries conceal a product defect | 3 | 5 | 15 | Infra-only classifier, one controlled retry, original evidence | Retry classification tests and flaky report |
| R-14 | Load causes saturation and cascading failure | 3 | 5 | 15 | Timeouts, pools, backpressure, SLO thresholds | k6 load/stress/spike/soak and metrics |
| R-15 | Contract drift breaks a consumer after deployment | 3 | 5 | 15 | Pact verification and OpenAPI compatibility gate | Consumer/provider workflows |
| R-16 | Migration fails or damages existing data | 2 | 5 | 10 | Immutable Flyway migrations and production-like upgrade fixtures | Clean/upgrade/rollback-safety database tests |
| R-17 | File upload accepts unsafe or excessive content | 3 | 5 | 15 | MIME/signature/size validation, isolated storage | Security and API boundary tests |
| R-18 | Environment drift makes CI/non-CI results disagree | 4 | 4 | 16 | Pinned wrapper/images, environment manifest, health gates | Clean clone and Compose/Kubernetes validation |

## Review cadence

Critical and high risks are reviewed at every milestone exit and release. New production behavior cannot merge without a requirement/risk mapping. A failed gate creates or updates a risk entry when it reveals a missing control rather than a one-off implementation defect.
