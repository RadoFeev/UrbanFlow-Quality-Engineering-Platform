CREATE TABLE tracking_events (
    id UUID PRIMARY KEY,
    shipment_id UUID NOT NULL,
    tracking_number VARCHAR(24) NOT NULL,
    status VARCHAR(32) NOT NULL,
    aggregate_version BIGINT NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    correlation_id VARCHAR(64) NOT NULL,
    trace_id VARCHAR(32) NOT NULL,
    CONSTRAINT ck_tracking_aggregate_version_non_negative CHECK (aggregate_version >= 0),
    CONSTRAINT ck_tracking_status CHECK (
        status IN (
            'CREATED', 'CONFIRMED', 'COURIER_ASSIGNED', 'PICKUP_IN_PROGRESS',
            'PICKED_UP', 'AT_ORIGIN_HUB', 'IN_TRANSIT', 'AT_DESTINATION_HUB',
            'OUT_FOR_DELIVERY', 'DELIVERY_ATTEMPTED', 'DELIVERED', 'CANCELLED',
            'RETURN_IN_PROGRESS', 'RETURNED'
        )
    ),
    CONSTRAINT uq_tracking_shipment_version UNIQUE (shipment_id, aggregate_version)
);

CREATE INDEX idx_tracking_number_timeline
    ON tracking_events (tracking_number, aggregate_version, occurred_at, id);

CREATE INDEX idx_tracking_shipment_latest
    ON tracking_events (shipment_id, aggregate_version DESC, occurred_at DESC);
