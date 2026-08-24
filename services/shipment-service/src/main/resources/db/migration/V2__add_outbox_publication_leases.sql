ALTER TABLE outbox_events
    ADD COLUMN publication_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN claimed_at TIMESTAMPTZ,
    ADD COLUMN claimed_by VARCHAR(100),
    ADD COLUMN last_error VARCHAR(1000),
    ADD CONSTRAINT ck_outbox_publication_attempts_non_negative
        CHECK (publication_attempts >= 0),
    ADD CONSTRAINT ck_outbox_claim_complete
        CHECK (
            (claimed_at IS NULL AND claimed_by IS NULL)
            OR (claimed_at IS NOT NULL AND claimed_by IS NOT NULL)
        );

CREATE INDEX idx_outbox_events_publishable
    ON outbox_events (next_attempt_at, occurred_at, id)
    WHERE published_at IS NULL;
