CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    event_type VARCHAR NOT NULL,
    aggregate_id UUID NOT NULL,
    payload jsonb NOT NULL,
    create_at TIMESTAMP DEFAULT now(),
    processed BOOLEAN DEFAULT FALSE,
);

CREATE INDEX ix_outbox_events_processed ON outbox_events(processed) WHERE processed = false