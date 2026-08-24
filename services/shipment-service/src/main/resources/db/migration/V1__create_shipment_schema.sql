CREATE TABLE shipments (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    tracking_number VARCHAR(24) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL,
    delivery_type VARCHAR(24) NOT NULL,
    pickup_contact_name VARCHAR(120) NOT NULL,
    pickup_line_1 VARCHAR(160) NOT NULL,
    pickup_line_2 VARCHAR(160),
    pickup_city VARCHAR(100) NOT NULL,
    pickup_postal_code VARCHAR(20) NOT NULL,
    pickup_country_code VARCHAR(2) NOT NULL,
    pickup_phone VARCHAR(16) NOT NULL,
    delivery_contact_name VARCHAR(120) NOT NULL,
    delivery_line_1 VARCHAR(160) NOT NULL,
    delivery_line_2 VARCHAR(160),
    delivery_city VARCHAR(100) NOT NULL,
    delivery_postal_code VARCHAR(20) NOT NULL,
    delivery_country_code VARCHAR(2) NOT NULL,
    delivery_phone VARCHAR(16) NOT NULL,
    weight_grams INTEGER NOT NULL CHECK (weight_grams BETWEEN 1 AND 50000),
    length_cm INTEGER NOT NULL CHECK (length_cm BETWEEN 1 AND 200),
    width_cm INTEGER NOT NULL CHECK (width_cm BETWEEN 1 AND 200),
    height_cm INTEGER NOT NULL CHECK (height_cm BETWEEN 1 AND 200),
    description VARCHAR(240) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT ck_shipments_status CHECK (
        status IN (
            'CREATED', 'CONFIRMED', 'COURIER_ASSIGNED', 'PICKUP_IN_PROGRESS',
            'PICKED_UP', 'AT_ORIGIN_HUB', 'IN_TRANSIT', 'AT_DESTINATION_HUB',
            'OUT_FOR_DELIVERY', 'DELIVERY_ATTEMPTED', 'DELIVERED', 'CANCELLED',
            'RETURN_IN_PROGRESS', 'RETURNED'
        )
    ),
    CONSTRAINT ck_shipments_delivery_type CHECK (
        delivery_type IN ('STANDARD', 'EXPRESS', 'SAME_DAY')
    ),
    CONSTRAINT ck_shipments_tracking_number CHECK (
        tracking_number ~ '^UF[0-9A-F]{18}$'
    )
);

CREATE INDEX idx_shipments_customer_created
    ON shipments (customer_id, created_at DESC)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_shipments_status_updated
    ON shipments (status, updated_at DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE shipment_status_history (
    id UUID PRIMARY KEY,
    shipment_id UUID NOT NULL REFERENCES shipments (id),
    previous_status VARCHAR(32),
    new_status VARCHAR(32) NOT NULL,
    actor VARCHAR(32) NOT NULL,
    reason VARCHAR(500),
    delivery_proof_id UUID,
    occurred_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_shipment_history_actor CHECK (
        actor IN (
            'CUSTOMER', 'COURIER', 'WAREHOUSE_OPERATOR',
            'ROUTING_SERVICE', 'SYSTEM', 'OPERATIONS'
        )
    )
);

CREATE INDEX idx_shipment_history_timeline
    ON shipment_status_history (shipment_id, occurred_at, id);

CREATE TABLE idempotency_records (
    idempotency_key VARCHAR(200) PRIMARY KEY,
    request_fingerprint VARCHAR(64) NOT NULL,
    shipment_id UUID NOT NULL REFERENCES shipments (id),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_idempotency_records_created_at
    ON idempotency_records (created_at);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL REFERENCES shipments (id),
    aggregate_version BIGINT NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    schema_version VARCHAR(16) NOT NULL,
    payload JSONB NOT NULL,
    correlation_id VARCHAR(64) NOT NULL,
    trace_id VARCHAR(32) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    CONSTRAINT uq_outbox_aggregate_version_type
        UNIQUE (aggregate_id, aggregate_version, event_type)
);

CREATE INDEX idx_outbox_events_unpublished
    ON outbox_events (occurred_at, id)
    WHERE published_at IS NULL;
