-- Wave 85 Bucket B — Immutable admin_audit_logs table (B-AC2 + B-AC7)
--
-- PDPL Art 11 compliance: every platform-admin action that bypasses tenant isolation
-- (via GUC `app.is_platform_admin=true` set by PlatformAdminAuditAspect) MUST emit an
-- immutable audit row. This table is append-only — RLS policy chặn UPDATE + DELETE
-- cho mọi role kể cả admin (defense-in-depth even if app layer compromised).
--
-- Schema:
--   id                UUID — surrogate primary key
--   admin_id          UUID — the platform admin user_id (references kh-subscription.users)
--   admin_email       VARCHAR — denormalized snapshot for forensics (users.email may change)
--   action            VARCHAR — semantic action token (READ_TENANT, IMPERSONATE_USER,
--                              EXPORT_AUDIT_LOG, MUTATE_TENANT_CONFIG, etc.)
--   target_tenant_id  UUID — the tenant whose data was accessed (nullable for system-wide ops)
--   target_resource   VARCHAR — fully-qualified resource id (e.g., students/UUID, audit/UUID)
--   payload_jsonb     JSONB — request context (query params, body summary, NOT raw response data)
--   client_ip         VARCHAR — admin source IP (forensic)
--   user_agent        TEXT — admin source UA
--   created_at        TIMESTAMP — server time, immutable
--
-- Indexes optimize forensic queries: by admin, by tenant, by action, by time-range.

CREATE TABLE IF NOT EXISTS admin_audit_logs (
    id UUID PRIMARY KEY,
    admin_id UUID NOT NULL,
    admin_email VARCHAR(255) NOT NULL,
    action VARCHAR(64) NOT NULL,
    target_tenant_id UUID,
    target_resource VARCHAR(512),
    payload_jsonb JSONB,
    client_ip VARCHAR(64),
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_admin ON admin_audit_logs(admin_id);
CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_target_tenant ON admin_audit_logs(target_tenant_id);
CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_action ON admin_audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_created_at ON admin_audit_logs(created_at DESC);

COMMENT ON TABLE admin_audit_logs IS
    'PDPL Art 11 — immutable platform admin action audit log (Wave 85 Bucket B B-AC2/B-AC7). '
    'Append-only — UPDATE/DELETE chặn bởi RLS policy.';

-- Enable RLS + immutability policies
--
-- READ policy: ALL roles can SELECT (admin self-audit, compliance review)
--   B-AC7: admin-bypass NOT applied here — admin_audit_logs is metadata, not tenant data.
--   Trade-off: SELECT exposed to any DB user; mitigated by app-layer authorization
--   (only PLATFORM_ADMIN role calls the AdminAuditLogService.findAll endpoint).
--
-- INSERT policy: ALL roles can INSERT (writes are immutable per row, no harm in allowing).
--   In practice only PlatformAdminAuditAspect emits rows.
--
-- UPDATE policy: BLOCK — predicate `false` for all rows.
-- DELETE policy: BLOCK — predicate `false` for all rows.

ALTER TABLE admin_audit_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE admin_audit_logs FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS admin_audit_select ON admin_audit_logs;
CREATE POLICY admin_audit_select ON admin_audit_logs
    FOR SELECT
    USING (true);

DROP POLICY IF EXISTS admin_audit_insert ON admin_audit_logs;
CREATE POLICY admin_audit_insert ON admin_audit_logs
    FOR INSERT
    WITH CHECK (true);

-- UPDATE blocked: USING predicate false → no row visible for UPDATE → no row updated.
DROP POLICY IF EXISTS admin_audit_no_update ON admin_audit_logs;
CREATE POLICY admin_audit_no_update ON admin_audit_logs
    FOR UPDATE
    USING (false)
    WITH CHECK (false);

-- DELETE blocked: USING predicate false → no row visible for DELETE → no row deleted.
DROP POLICY IF EXISTS admin_audit_no_delete ON admin_audit_logs;
CREATE POLICY admin_audit_no_delete ON admin_audit_logs
    FOR DELETE
    USING (false);
