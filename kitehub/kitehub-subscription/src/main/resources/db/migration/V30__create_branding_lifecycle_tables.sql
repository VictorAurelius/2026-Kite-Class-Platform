-- V30: Branding instance lifecycle state + audit events (GAP-272l, Wave 34 Bucket C)
-- Reference: ai-branding-guidelines.md §6 InstanceLifecycle state machine
-- Bucket C uses V30 (Bucket A is expected to claim V29; coordinator notified in PR body).

CREATE TABLE branding_instance_state (
    instance_id UUID PRIMARY KEY,
    state VARCHAR(32) NOT NULL,
    branding_version INTEGER NOT NULL DEFAULT 0,
    regenerate_count INTEGER NOT NULL DEFAULT 0,
    last_failure_reason VARCHAR(1000),
    row_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_branding_instance_state_state
        CHECK (state IN ('NOT_STARTED', 'INITIALIZING', 'GENERATING',
                         'DEPLOYED', 'REGENERATING', 'FAILED'))
);

COMMENT ON TABLE branding_instance_state
    IS 'Current branding lifecycle state per tenant instance. Updated only via InstanceLifecycleService.';

CREATE TABLE branding_lifecycle_events (
    id UUID PRIMARY KEY,
    instance_id UUID NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    from_state VARCHAR(32),
    to_state VARCHAR(32),
    actor_kind VARCHAR(16) NOT NULL,
    actor_id VARCHAR(128),
    metadata_json TEXT,
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_branding_lifecycle_events_actor_kind
        CHECK (actor_kind IN ('user', 'system', 'admin')),
    CONSTRAINT chk_branding_lifecycle_events_from_state
        CHECK (from_state IS NULL OR from_state IN
            ('NOT_STARTED', 'INITIALIZING', 'GENERATING',
             'DEPLOYED', 'REGENERATING', 'FAILED')),
    CONSTRAINT chk_branding_lifecycle_events_to_state
        CHECK (to_state IS NULL OR to_state IN
            ('NOT_STARTED', 'INITIALIZING', 'GENERATING',
             'DEPLOYED', 'REGENERATING', 'FAILED'))
);

CREATE INDEX idx_branding_lifecycle_events_instance_ts
    ON branding_lifecycle_events (instance_id, occurred_at DESC);

COMMENT ON TABLE branding_lifecycle_events
    IS 'Append-only history of branding lifecycle transitions + audit markers. Doubles as audit trail.';
