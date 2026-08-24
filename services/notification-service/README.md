# Notification Service

Notification Service consumes allowlisted shipment events and creates exactly one durable customer
message per source event. Templates include only public tracking information; contact details do not
cross the Kafka boundary.

The delivery worker calls the configured internal HTTPS communications gateway with the
notification ID as idempotency key. Provider acknowledgment marks delivery. Failures use capped
exponential retry and transition to `DEAD_LETTER` after the configured attempt limit. The customer
feed is paginated, newest-first, and does not expose provider IDs or internal errors.

The service requires `NOTIFICATION_DATABASE_USER` and `NOTIFICATION_DATABASE_PASSWORD`.
