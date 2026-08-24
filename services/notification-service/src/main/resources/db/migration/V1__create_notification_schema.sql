CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    source_event_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    shipment_id UUID NOT NULL,
    type VARCHAR(80) NOT NULL,
    subject VARCHAR(160) NOT NULL,
    body VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL,
    provider_message_id VARCHAR(120),
    last_error VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    delivered_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_notifications_source_event UNIQUE (source_event_id),
    CONSTRAINT ck_notifications_status CHECK (
        status IN ('PENDING', 'RETRY', 'DELIVERED', 'DEAD_LETTER')
    ),
    CONSTRAINT ck_notifications_attempts CHECK (attempts >= 0),
    CONSTRAINT ck_notifications_version CHECK (version >= 0),
    CONSTRAINT ck_notifications_delivery CHECK (
        (status = 'DELIVERED' AND delivered_at IS NOT NULL AND provider_message_id IS NOT NULL)
        OR status <> 'DELIVERED'
    )
);

CREATE INDEX idx_notifications_delivery_due
    ON notifications (next_attempt_at, created_at)
    WHERE status IN ('PENDING', 'RETRY');

CREATE INDEX idx_notifications_customer_feed
    ON notifications (customer_id, created_at DESC);
