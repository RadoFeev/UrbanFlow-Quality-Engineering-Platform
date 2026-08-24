# Routing Service

Routing Service consumes confirmed shipment events, selects the least-loaded available courier,
reserves versioned courier capacity, creates a unique delivery route, and stages a
`CourierAssigned` event in the same PostgreSQL transaction.

Duplicate event IDs and duplicate shipment confirmations are acknowledged without creating a
second route. Courier and route optimistic versions protect concurrent assignment and lifecycle
updates. Broker-acknowledged outbox publication uses the shipment ID as Kafka ordering key and
retains failed events for capped exponential retry.

The service requires `ROUTING_DATABASE_USER` and `ROUTING_DATABASE_PASSWORD`. Kafka and database
locations are configurable through the environment variables documented in `application.yml`.
