-- GAP-1265 (wave-kitehub-biz-100): persistent in-app notification (persistent-banner) store.
-- Durable fallback channel for the NotificationChannel seam — written for every dispatched owner
-- notification so the owner always has a record even when email delivery is disabled/fails.
--
-- NOTE (multi-session coordination): version V74 (kitehub-subscription Flyway sequence; prior max
-- was V72) reserved by the BE-4 notification bucket of wave-kitehub-biz-100. If a sibling bucket
-- also claimed V74, renumber to the next free version at wave merge (Flyway orders by version;
-- gaps such as the unused V73 slot are allowed).
--
-- Tenant isolation: enforced at the app layer (TenantOwnershipGuard + instance_id filter) AND
-- at the DB layer via RLS. The RLS policy below follows the established KH convention
-- (V58__rls_sweep_kh.sql / V66__oauth_attempts_rls.sql): non-forced RLS + tenant_isolation policy
-- with an admin-bypass clause (`app.is_platform_admin`) + NULL force-fail. The admin-bypass clause
-- is exactly what lets the dispatch path (which writes notifications in admin/system context where
-- the per-request `app.current_tenant_id` GUC is not the owner's tenant) still INSERT rows: the
-- dispatcher runs with `app.is_platform_admin=true`. Owner-facing reads scope to their tenant via
-- the `app.current_tenant_id` GUC. Non-forced means the Spring Boot HikariCP table-owner workload
-- bypasses per-row policy for the app itself; the policy guards FORCE-RLS / non-owner / cross-service
-- connections (consistent with every other KH tenant-scoped table — payments, branding_outbox, etc.).

CREATE TABLE IF NOT EXISTS in_app_notifications (
    id                UUID PRIMARY KEY,
    instance_id       UUID NOT NULL,
    notification_type VARCHAR(50)  NOT NULL,
    title             VARCHAR(200) NOT NULL,
    body              VARCHAR(1000) NOT NULL,
    action_url        VARCHAR(500),
    is_read           BOOLEAN NOT NULL DEFAULT FALSE,
    read_at           TIMESTAMP,
    created_at        TIMESTAMP NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP NOT NULL DEFAULT now(),
    created_by        VARCHAR(100),
    updated_by        VARCHAR(100),
    deleted           BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_in_app_notifications_instance_unread
    ON in_app_notifications (instance_id, is_read)
    WHERE deleted = FALSE;

COMMENT ON TABLE in_app_notifications IS
    'GAP-1265 persistent-banner fallback channel for owner notifications (payment-confirmed, win-back, ...).';

-- Row-Level Security (KH convention per V58/V66): non-forced + admin-bypass + NULL force-fail.
-- instance_id-keyed tenant isolation; admin/system dispatch bypasses via app.is_platform_admin GUC.
ALTER TABLE in_app_notifications ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation ON in_app_notifications;
CREATE POLICY tenant_isolation ON in_app_notifications
    USING (
        COALESCE(current_setting('app.is_platform_admin', true)::boolean, false)
        OR instance_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid
    )
    WITH CHECK (
        COALESCE(current_setting('app.is_platform_admin', true)::boolean, false)
        OR instance_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid
    );
