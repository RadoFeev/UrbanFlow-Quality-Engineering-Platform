# Coverage Matrix

This matrix is expanded with exact class names as suites are implemented. Current entries define the accepted traceability contract; unimplemented rows are marked `Planned` and do not claim coverage.

| Requirement | Risk | Test level | Automated suite | Planned test class | CI workflow | State |
|---|---|---|---|---|---|---|
| Authentication and session lifecycle | R-05, R-06 | Unit/Component/API/UI/Security | identity, api, ui, security | `AuthServiceTest`, `AccessTokenServiceTest`, `CustomerAuthenticationUiTest` | api-tests, ui-tests, security-scan | Unit, API, and 22 browser scenarios implemented; security level planned |
| Role and permission enforcement | R-05 | Unit/API/UI/Security | identity, api, ui | `RoleTest`, `BearerTokenFilterTest`, `OperationsDashboardUiTest` | pull-request-checks, security-scan | Role mapping, bearer-authority, and operations RBAC browser behavior implemented |
| Create shipment and validate parcel | R-01, R-02 | Unit/API/UI/E2E | shipment, api, ui, e2e | `CreateShipment*Test`, `CustomerShipmentUiTest` | smoke-tests, api-tests, ui-tests | Unit, API, and data-driven UI boundaries implemented; E2E planned |
| Shipment transition policy | R-01, R-07 | Unit/API/Event/E2E | shipment, api, event, e2e | `ShipmentTransition*Test` | pull-request-checks, e2e-tests | Unit implemented; other levels planned |
| Idempotent writes | R-02 | Unit/API/DB/Event | shipment, api, database, event | `ShipmentApplicationServiceTest`, `ShipmentIdempotency*Test` | api-tests, integration-tests | Unit orchestration implemented; other levels planned |
| Optimistic locking | R-07 | API/DB/UI | api, database, ui | `OptimisticLocking*Test`, `OperationsDashboardUiTest` | integration-tests, ui-tests | Database and integration scenarios implemented; operations conflict UI verified |
| Transactional outbox | R-03 | Unit/DB/Integration/Event | shipment, database, integration, event | `OutboxEventTest`, `OutboxPublisherTest`, `OutboxStateServiceTest`, `OutboxPublication*Test` | integration-tests | Lease/retry/envelope unit behavior implemented; DB/Kafka levels planned |
| Tracking ordering and projection | R-04, R-08 | Unit/Event/API/UI/E2E | tracking, event, api, ui, e2e | `TrackingProjectionServiceTest`, `CustomerShipmentUiTest` | api-tests, e2e-tests | Unit projection, API contract, and public timeline UI implemented; event/E2E planned |
| Redis cache/session/rate limits | R-08 | Unit/Integration/API | tracking, integration, api | `RedisTrackingTimelineCacheTest`, `Redis*Test` | integration-tests | Cache fallback/TTL unit behavior implemented; real Redis level planned |
| Courier offline synchronization | R-01, R-10 | Component/UI/E2E | component, ui, e2e | `CourierPortalUiTest` | ui-tests, e2e-tests | Offline queue and reconnect synchronization browser behavior implemented |
| Courier assignment and route capacity | R-01, R-02, R-07 | Unit/Event/API/E2E | routing, event, api, e2e | `RoutingAssignmentServiceTest`, `RouteLifecycleServiceTest`, `CourierAssignment*Test` | integration-tests, e2e-tests | Domain/event publication unit behavior implemented; cross-boundary levels planned |
| Proof-of-delivery upload | R-06, R-17 | API/UI/Security | api, ui, security | `CourierPortalUiTest` | ui-tests, security-scan | Required proof and MIME allowlist UI behavior implemented; security scan planned |
| Notification retry and DLT | R-09 | Component/Event | component, event | `NotificationRetry*Test` | integration-tests | Planned |
| Consumer contracts | R-15 | Contract | contract | `*PactTest`, `*ProviderTest` | contract-tests | Planned |
| Database migrations/integrity | R-16 | Database | database | `Migration*Test`, `Constraint*Test` | integration-tests | Schema implemented; container verification planned |
| WCAG 2.2 AA critical pages | R-11 | Accessibility | accessibility | `*AccessibilityTest` | accessibility-tests | Planned |
| Responsive visual stability | R-12 | Visual | visual | `*VisualRegressionTest` | visual-tests | Planned |
| Latency, throughput, saturation | R-14 | Performance | performance | `*.js` scenario manifest | performance-tests | Planned |
| IDOR/injection/JWT/CORS/headers | R-05, R-06 | Security/API | security | `*SecurityTest` | security-scan | Planned |

## Coverage reporting rules

Functional coverage requires at least one assertion at the owning boundary and, for critical risks, one cross-boundary scenario. Code coverage is diagnostic and cannot substitute for requirement/risk coverage. JaCoCo thresholds apply to product domain code, excluding generated models and configuration-only classes with documented exclusions.
