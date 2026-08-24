# Coverage Matrix

This matrix is expanded with exact class names as suites are implemented. Current entries define the accepted traceability contract; unimplemented rows are marked `Planned` and do not claim coverage.

| Requirement | Risk | Test level | Automated suite | Planned test class | CI workflow | State |
|---|---|---|---|---|---|---|
| Authentication and session lifecycle | R-05, R-06 | Component/API/UI/Security | identity, api, ui, security | `Authentication*Test` | api-tests, ui-tests, security-scan | Planned |
| Role and permission enforcement | R-05 | Unit/API/UI/Security | identity, api, ui | `RoleBasedAccess*Test` | pull-request-checks, security-scan | Planned |
| Create shipment and validate parcel | R-01, R-02 | Unit/API/UI/E2E | shipment, api, ui, e2e | `CreateShipment*Test` | smoke-tests, api-tests, ui-tests | Planned |
| Shipment transition policy | R-01, R-07 | Unit/API/Event/E2E | shipment, api, event, e2e | `ShipmentTransition*Test` | pull-request-checks, e2e-tests | Unit implemented; other levels planned |
| Idempotent writes | R-02 | Unit/API/DB/Event | shipment, api, database, event | `ShipmentApplicationServiceTest`, `ShipmentIdempotency*Test` | api-tests, integration-tests | Unit orchestration implemented; other levels planned |
| Optimistic locking | R-07 | API/DB/UI | api, database, ui | `OptimisticLocking*Test` | integration-tests, ui-tests | Planned |
| Transactional outbox | R-03 | DB/Integration/Event | database, integration, event | `OutboxPublication*Test` | integration-tests | Planned |
| Tracking ordering and projection | R-04, R-08 | Event/API/UI/E2E | event, api, ui, e2e | `TrackingTimeline*Test` | api-tests, e2e-tests | Planned |
| Redis cache/session/rate limits | R-08 | Integration/API | integration, api | `Redis*Test` | integration-tests | Planned |
| Courier offline synchronization | R-01, R-10 | Component/UI/E2E | component, ui, e2e | `OfflineDeliverySync*Test` | ui-tests, e2e-tests | Planned |
| Proof-of-delivery upload | R-06, R-17 | API/UI/Security | api, ui, security | `DeliveryProofUpload*Test` | ui-tests, security-scan | Planned |
| Notification retry and DLT | R-09 | Component/Event | component, event | `NotificationRetry*Test` | integration-tests | Planned |
| Consumer contracts | R-15 | Contract | contract | `*PactTest`, `*ProviderTest` | contract-tests | Planned |
| Database migrations/integrity | R-16 | Database | database | `Migration*Test`, `Constraint*Test` | integration-tests | Schema implemented; container verification planned |
| WCAG 2.2 AA critical pages | R-11 | Accessibility | accessibility | `*AccessibilityTest` | accessibility-tests | Planned |
| Responsive visual stability | R-12 | Visual | visual | `*VisualRegressionTest` | visual-tests | Planned |
| Latency, throughput, saturation | R-14 | Performance | performance | `*.js` scenario manifest | performance-tests | Planned |
| IDOR/injection/JWT/CORS/headers | R-05, R-06 | Security/API | security | `*SecurityTest` | security-scan | Planned |

## Coverage reporting rules

Functional coverage requires at least one assertion at the owning boundary and, for critical risks, one cross-boundary scenario. Code coverage is diagnostic and cannot substitute for requirement/risk coverage. JaCoCo thresholds apply to product domain code, excluding generated models and configuration-only classes with documented exclusions.
