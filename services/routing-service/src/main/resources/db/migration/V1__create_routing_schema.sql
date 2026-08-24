CREATE TABLE couriers (
    id UUID PRIMARY KEY,
    display_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    maximum_assignments INTEGER NOT NULL,
    active_assignments INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_couriers_status CHECK (status IN ('AVAILABLE', 'OFF_DUTY', 'SUSPENDED')),
    CONSTRAINT ck_couriers_capacity CHECK (
        maximum_assignments BETWEEN 1 AND 100
        AND active_assignments BETWEEN 0 AND maximum_assignments
    ),
    CONSTRAINT ck_couriers_version CHECK (version >= 0)
);

CREATE INDEX idx_couriers_assignment_candidates
    ON couriers (active_assignments, id)
    WHERE status = 'AVAILABLE';

CREATE TABLE delivery_routes (
    id UUID PRIMARY KEY,
    shipment_id UUID NOT NULL,
    courier_id UUID NOT NULL REFERENCES couriers(id),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_delivery_routes_shipment UNIQUE (shipment_id),
    CONSTRAINT ck_delivery_routes_status CHECK (status IN ('PLANNED', 'STARTED', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT ck_delivery_routes_timestamps CHECK (
        (started_at IS NULL OR started_at >= created_at)
        AND (completed_at IS NULL OR completed_at >= started_at)
    ),
    CONSTRAINT ck_delivery_routes_version CHECK (version >= 0)
);

CREATE INDEX idx_delivery_routes_courier_created
    ON delivery_routes (courier_id, created_at);

CREATE TABLE processed_shipment_events (
    id UUID PRIMARY KEY,
    shipment_id UUID NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_processed_shipment_events_shipment
    ON processed_shipment_events (shipment_id);

CREATE TABLE routing_outbox_events (
    id UUID PRIMARY KEY,
    shipment_id UUID NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    payload JSONB NOT NULL,
    correlation_id VARCHAR(64) NOT NULL,
    trace_id CHAR(32) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_routing_outbox_attempts CHECK (attempts >= 0),
    CONSTRAINT ck_routing_outbox_trace CHECK (trace_id ~ '^[0-9a-f]{32}$')
);

CREATE INDEX idx_routing_outbox_due
    ON routing_outbox_events (next_attempt_at, occurred_at)
    WHERE published_at IS NULL;
