# Coverage Matrix

This matrix maps each material requirement and risk to its implemented automated boundary and owning
pipeline. Container/deployed rows remain implemented when local execution is capability-skipped; the
state column distinguishes that from an executed pass.

| Requirement | Risk | Test level | Automated suite | Principal test or scenario | CI workflow | State |
|---|---|---|---|---|---|---|
| Authentication and session lifecycle | R-05, R-06 | Unit/API/UI/Security/E2E | identity, api, ui, security, e2e | `AuthServiceTest`, `IdentityHttpAdapterApiTest`, `CustomerAuthenticationUiTest`, `ProductionSecurityControlTest` | api-tests, ui-tests, security-scan, e2e-tests | Implemented and locally verified; deployed E2E is opt-in |
| Role and permission enforcement | R-05 | Unit/API/UI/Security | identity, api, ui, security | `RoleTest`, `BearerTokenFilterTest`, `OperationsDashboardUiTest` | pull-request-checks, ui-tests, security-scan | Implemented and locally verified |
| Create shipment and validate parcel | R-01, R-02 | Unit/API/UI/E2E | shipment, api, ui, e2e | `ShipmentApplicationServiceTest`, `OpenApiOperationContractTest`, `CustomerShipmentUiTest`, `DistributedDeliveryJourneyTest` | api-tests, ui-tests, e2e-tests | Implemented; local layers verified and deployed journey opt-in |
| Shipment transition policy | R-01, R-07 | Unit/API/Event/UI | shipment, api, event, ui | `ShipmentTransitionPolicyTest`, `OpenApiOperationContractTest`, `EventConsumerSemanticsTest` | pull-request-checks, api-tests, integration-tests, ui-tests | Implemented and locally verified |
| Idempotent writes | R-02 | Unit/API/DB/Event/E2E | shipment, api, database, event, e2e | `ShipmentApplicationServiceTest`, `PostgresPersistenceTest`, `EventConsumerSemanticsTest`, `DistributedDeliveryJourneyTest` | api-tests, integration-tests, e2e-tests | Implemented; PostgreSQL/E2E execution capability-dependent |
| Optimistic locking | R-07 | Unit/API/DB/UI | services, api, database, ui | `EntityTagVersionTest`, `PostgresPersistenceTest`, `OperationsDashboardUiTest` | pull-request-checks, integration-tests, ui-tests | Implemented; browser verified and PostgreSQL capability-dependent |
| Transactional outbox | R-03 | Unit/DB/Integration/Event | shipment, database, integration, event | `OutboxEventTest`, `OutboxPublisherTest`, `PostgresPersistenceTest`, `KafkaContainerRoundTripTest` | integration-tests | Implemented; unit/event verified and real infrastructure capability-dependent |
| Tracking ordering and projection | R-04, R-08 | Unit/Event/API/UI/E2E | tracking, event, api, ui, e2e | `TrackingProjectionServiceTest`, `EventConsumerSemanticsTest`, `CustomerShipmentUiTest`, `DistributedDeliveryJourneyTest` | api-tests, ui-tests, e2e-tests | Implemented and local boundaries verified |
| Redis tracking cache | R-08 | Unit/Integration/API | tracking, integration, api | `RedisTrackingTimelineCacheTest`, `ShipmentPersistenceIntegrationTest` | integration-tests | Cache TTL/fallback verified; deployed Redis lane capability-dependent |
| Courier offline synchronization | R-01, R-10 | UI | ui | `CourierPortalUiTest` | ui-tests | Implemented and verified in browser |
| Courier assignment and route capacity | R-01, R-02, R-07 | Unit/Event/API | routing, event, api | `RoutingAssignmentServiceTest`, `RouteLifecycleServiceTest`, `KafkaEnvelopeContractTest` | integration-tests, api-tests | Implemented and local boundaries verified |
| Proof-of-delivery upload | R-06, R-17 | UI/Security | ui, security | `CourierPortalUiTest`, `ProductionSecurityControlTest` | ui-tests, security-scan | MIME/size policy implemented and verified |
| Notification retry and DLT | R-09 | Component/Event | component, event | `NotificationGatewayComponentTest`, `EventConsumerSemanticsTest` | integration-tests | Implemented and locally verified |
| Consumer contracts | R-15 | Contract/OpenAPI | contract | five `*PactTest` classes plus compatibility gate | contract-tests | 20 Pact interactions implemented and verified |
| Database migrations/integrity | R-16 | Database/Integration | database, integration | `PostgresPersistenceTest`, `ShipmentPersistenceIntegrationTest` | integration-tests | 54 scenarios implemented; Docker-dependent locally |
| WCAG critical pages | R-11 | Accessibility | accessibility | `PortalAccessibilityTest` | accessibility-tests | 15 axe-core scans implemented and verified |
| Responsive visual stability | R-12 | Visual | visual | `PortalVisualRegressionTest` | visual-tests | 15 committed baselines implemented and verified |
| Latency, throughput, saturation | R-14 | Performance | performance | `urbanflow.js`, `K6ScenarioContractTest` | performance-tests | 10 thresholded scenarios implemented; bundle verified |
| JWT/CORS/headers/input controls | R-05, R-06 | Security/API | security, api | `ProductionSecurityControlTest`, `IdentityHttpAdapterApiTest` | security-scan, api-tests | 20 production controls implemented and verified |

## Coverage reporting rules

Functional coverage requires at least one assertion at the owning boundary and, for critical risks, one cross-boundary scenario. Code coverage is diagnostic and cannot substitute for requirement/risk coverage. JaCoCo thresholds apply to product domain code, excluding generated models and configuration-only classes with documented exclusions.
