-- Wave beta-readiness-4 Bucket B (GAP-353b) — Immutable consent record với hash chain.
--
-- Bổ sung table consent_record_immutable cho PDPL Decree 13/2023 Art 11+14
-- (đồng ý + rút lại đồng ý). Khác với consent_record (Wave 25 Bucket A — pseudonymous
-- visitor_id, idempotent upsert) ở 3 điểm:
--
--   1) IMMUTABLE: RLS policy chặn UPDATE + DELETE — append-only audit trail.
--      Withdraw = INSERT new row with granted={analytics:false,marketing:false}
--      (NOT flip existing). Hash chain preserves tamper-evidence.
--
--   2) HASH CHAIN: mỗi row chứa current_hash = SHA-256(prev_hash || canonical_row_json).
--      Chain validation phát hiện bất kỳ row nào bị sửa/xoá thủ công qua DB superuser.
--      Đầu chuỗi (row đầu tiên cho user_id) có prev_hash = NULL.
--
--   3) GRANTED JSONB: linh hoạt (categories có thể mở rộng: essential/analytics/
--      marketing/personalization/...) thay vì N cột boolean cố định. Schema evolution
--      không cần migration cho category mới.
--
-- Service: kitehub-subscription (LOCKED per wave plan §3.6 — NOT new service).
-- Reference: documents/04-quality/compliance/pdpl-pre-launch-checklist.md.

CREATE TABLE consent_record_immutable (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NULL,                  -- soft reference; FK omitted per multi-tenant arch
    tenant_id BIGINT NULL,                -- soft reference; null cho marketing-surface visitor
    granted JSONB NOT NULL,               -- {essential:true, analytics:bool, marketing:bool, ...}
    prev_hash VARCHAR(64) NULL,           -- SHA-256 hex of previous row; NULL = chain head
    current_hash VARCHAR(64) NOT NULL,    -- SHA-256(prev_hash || canonical(row))
    ip_address INET NOT NULL,             -- INET native validation per Postgres
    user_agent TEXT NOT NULL,
    signed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_consent_record_immutable_user_id_signed_at
    ON consent_record_immutable (user_id, signed_at);

CREATE INDEX idx_consent_record_immutable_tenant_id_signed_at
    ON consent_record_immutable (tenant_id, signed_at)
    WHERE tenant_id IS NOT NULL;

COMMENT ON TABLE consent_record_immutable IS
    'Immutable PDPL consent audit trail (Wave br-4 Bucket B GAP-353b). Append-only; '
    'withdraw = new row not flip. Hash chain SHA-256(prev_hash || canonical_row) for tamper-evidence. '
    'Co-exists với consent_record (Wave 25 Bucket A visitor_id-based path) for pre-login banner stage; '
    'this table is for post-login authenticated consent capture per PDPL Art 11 informed-consent + Art 14 withdrawal.';

COMMENT ON COLUMN consent_record_immutable.user_id IS
    'Soft reference (no FK). FK omitted per multi-tenant architecture decision (Wave br-4 §3.6 decision 3): '
    'consent_record_immutable may outlive user account deletion (PDPL Art 11 retention >= consent expiry); '
    'FK cascade would violate audit-trail immutability.';

COMMENT ON COLUMN consent_record_immutable.tenant_id IS
    'Soft reference (no FK). Nullable cho marketing-surface visitor pre-tenant (cùng lý do user_id).';

COMMENT ON COLUMN consent_record_immutable.granted IS
    'JSONB consent categories: {"essential":true,"analytics":bool,"marketing":bool[,"personalization":bool,...]}. '
    'Essential coerced to true server-side per BR-PDPL-CONSENT-001. Schema-flex for category evolution.';

COMMENT ON COLUMN consent_record_immutable.prev_hash IS
    'SHA-256 hex (64 chars) của row trước đó trong chain cho user_id. NULL = chain head.';

COMMENT ON COLUMN consent_record_immutable.current_hash IS
    'SHA-256(COALESCE(prev_hash,"") || canonical(user_id|tenant_id|granted|ip|ua|signed_at)). '
    'Chain validation: ConsentService.verifyChainIntegrity() chạy SHA-256 lại + so sánh.';

COMMENT ON COLUMN consent_record_immutable.ip_address IS
    'IPv4/IPv6 INET native — Postgres validates format. Per audit RCA 2026-05-16 (LoginAuditLog.ip),'
    ' Testcontainers IT mandatory cho INET binding (ConsentRecordImmutablePostgresIT).';

-- Enable RLS + define immutability policies (per Wave br-4 §3.6 decision 1).
ALTER TABLE consent_record_immutable ENABLE ROW LEVEL SECURITY;

-- Allow INSERT for app role (default permissive when no INSERT policy denies).
CREATE POLICY consent_record_immutable_insert
    ON consent_record_immutable
    FOR INSERT
    WITH CHECK (true);

-- Allow SELECT for app role (audit history GET endpoint).
CREATE POLICY consent_record_immutable_select
    ON consent_record_immutable
    FOR SELECT
    USING (true);

-- BANNED: UPDATE + DELETE — immutability enforcement.
-- Withdraw flow MUST INSERT new row with granted={..,analytics:false,marketing:false}.
CREATE POLICY consent_record_immutable_no_update
    ON consent_record_immutable
    FOR UPDATE
    USING (false)
    WITH CHECK (false);

CREATE POLICY consent_record_immutable_no_delete
    ON consent_record_immutable
    FOR DELETE
    USING (false);

COMMENT ON POLICY consent_record_immutable_no_update ON consent_record_immutable IS
    'RLS PDPL immutability: UPDATE blocked per Wave br-4 Bucket B. Withdraw = INSERT new row.';

COMMENT ON POLICY consent_record_immutable_no_delete ON consent_record_immutable IS
    'RLS PDPL immutability: DELETE blocked. Retention purge (PDPL Art 11 36-month) must use '
    'superuser bypass + paired audit row; out of app scope.';
