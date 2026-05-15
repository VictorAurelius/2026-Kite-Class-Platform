-- GAP-040 (Wave 79 Bucket F-bis): admin "View as tenant" impersonation audit log.
--
-- Sister-table to admin_audit_log (V36) but specialized for impersonation
-- sessions: tracks 30-second-bounded support workflows where a platform admin
-- assumes the identity of a tenant for troubleshooting.
--
-- Ordering note (per concurrent-production-mutation-ops.md + plan §3): V47
-- applies STRICTLY after V46 rbac_roles (Bucket B). Flyway enforces version
-- order — this filename is V47 so even if Bucket B's V46 has not yet landed
-- at deploy time, Flyway will block until V46 is present.
--
-- Schema mirrors documents/01-business/kitehub/support/impersonation/api-contract.md
-- (created same-PR per contract-first-for-cross-layer.md, though this bucket is
-- BE-only so contract is informational).
--
-- Columns:
--   id              — BIGSERIAL primary key
--   admin_user_id   — UUID of the impersonating PLATFORM_ADMIN (FK conceptual)
--   tenant_id       — UUID of the target tenant being viewed
--   tenant_slug     — denormalized for log readability + tenant rename safety
--   started_at      — TIMESTAMPTZ when impersonation JWT was issued
--   ended_at        — TIMESTAMPTZ when admin clicked "Thoát ra" OR session
--                     auto-expired at +30s; NULL while session still active
--   ended_reason    — VARCHAR(32) enum: MANUAL_EXIT | AUTO_TIMEOUT | NEVER (NULL when active)
--   request_ip      — VARCHAR(45) IPv6-safe inet of admin's browser
--   user_agent      — VARCHAR(512) admin's UA string
--   created_at      — audit-log default
--
-- Retention: 7 years per .claude/rules/logs-format-standard.md §4 (security/audit).
-- Indexes optimized for: per-admin history queries + active-session lookup.

CREATE TABLE IF NOT EXISTS impersonation_audit_log (
    id              BIGSERIAL PRIMARY KEY,
    admin_user_id   UUID         NOT NULL,
    tenant_id       UUID         NOT NULL,
    tenant_slug     VARCHAR(100) NOT NULL,
    started_at      TIMESTAMPTZ  NOT NULL,
    ended_at        TIMESTAMPTZ  NULL,
    ended_reason    VARCHAR(32)  NULL,
    request_ip      VARCHAR(45)  NULL,
    user_agent      VARCHAR(512) NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_imp_ended_reason CHECK (
        ended_reason IS NULL
        OR ended_reason IN ('MANUAL_EXIT', 'AUTO_TIMEOUT', 'NEVER')
    )
);

CREATE INDEX IF NOT EXISTS idx_imp_admin_user ON impersonation_audit_log (admin_user_id, started_at DESC);
CREATE INDEX IF NOT EXISTS idx_imp_tenant     ON impersonation_audit_log (tenant_id, started_at DESC);
CREATE INDEX IF NOT EXISTS idx_imp_active     ON impersonation_audit_log (started_at DESC) WHERE ended_at IS NULL;

COMMENT ON TABLE  impersonation_audit_log IS 'GAP-040 Wave 79 F-bis: admin "View as tenant" impersonation audit log (30s sessions).';
COMMENT ON COLUMN impersonation_audit_log.ended_reason IS 'MANUAL_EXIT=admin click Exit; AUTO_TIMEOUT=30s expiry; NULL=still active.';
