-- GAP-353b (Wave 25 Bucket A): server-side consent record store for PDPL Phase 2.
--
-- Backs `BR-PDPL-CONSENT-001..004` from `documents/01-business/kitehub/marketing/rules.md`.
-- Wave 23 BC shipped LocalStorage-only persistence (PDPL Art 11+13 read OK for MVP).
-- This table adds server-side audit trail so:
--   - Cross-device consent sync becomes possible (visitor_id + LocalStorage cooperate)
--   - DR-03 36-month retention pipeline has rows to retain (cron runs daily 3am)
--   - Consent revocation produces a tamper-evident server record (PDPL Art 13(1))
--
-- Pseudonymous design: visitor_id is a UUID v4 generated client-side and persisted in
-- LocalStorage. user_id + tenant_id stay nullable for marketing-surface visitors who
-- haven't signed up; they can be back-filled after login if downstream wave needs.

CREATE TABLE consent_record (
    id BIGSERIAL PRIMARY KEY,
    visitor_id UUID NOT NULL,
    user_id BIGINT NULL,
    tenant_id UUID NULL,
    essential_consented BOOLEAN NOT NULL DEFAULT TRUE,
    analytics_consented BOOLEAN NOT NULL DEFAULT FALSE,
    marketing_consented BOOLEAN NOT NULL DEFAULT FALSE,
    consent_version INTEGER NOT NULL DEFAULT 1,
    ip_address VARCHAR(45) NULL,
    user_agent TEXT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '12 months'),
    revoked_at TIMESTAMP WITH TIME ZONE NULL
);

CREATE INDEX idx_consent_record_visitor ON consent_record(visitor_id);
CREATE INDEX idx_consent_record_user ON consent_record(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_consent_record_expires ON consent_record(expires_at);

COMMENT ON TABLE consent_record IS
    'Server-side PDPL consent audit trail (GAP-353b Wave 25). visitor_id is pseudonymous; user_id back-filled after login; expires_at = created_at + 12 months per BR-PDPL-CONSENT-002 re-prompt cadence; DR-03 cron deletes rows older than 36 months.';

COMMENT ON COLUMN consent_record.visitor_id IS
    'Pseudonymous UUID v4 generated client-side, persisted in LocalStorage `kite_visitor_id`.';
COMMENT ON COLUMN consent_record.ip_address IS
    'IPv4 or IPv6 textual form (max 45 chars per RFC 4291). Stored as VARCHAR for portability across H2 (test) and Postgres (prod) — Postgres-native INET type avoided to keep test ddl-auto schema parity.';
COMMENT ON COLUMN consent_record.expires_at IS
    'Re-prompt deadline: 12 months from creation per BR-PDPL-CONSENT-002. Distinct from DR-03 36-month retention which is enforced by `ConsentRetentionCron`.';
