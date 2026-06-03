-- =========================================================================
-- V83: audit_log append-only DB-level enforcement (GAP-889)
-- =========================================================================
-- Context: GAP-889 (Wave 13 cluster docs writing — KC compliance). P1.
-- audit_log (V35, Wave 4) is semantically append-only (AuditLogWriter javadoc:
-- "Direct repository.save is discouraged") but has NO DB-level UPDATE/DELETE
-- block. Compared to admin_audit_logs (V60 RLS UPDATE/DELETE=false) and
-- child_protection_audit_log (V54 REVOKE DELETE), audit_log v1 is the loosest
-- generation — test/migration/raw SQL can mutate rows. Compliance drift.
--
-- State-check (2026-06-03):
--   - audit_log already has instance_id UUID (V35:15) + version DEFAULT 0
--     (V35:31) → NOT in GAP-884 scope.
--   - audit_log ALREADY has RLS enabled + FORCED + tenant_isolation policy
--     (V58/V59 — table is in both tenant_tables arrays). That policy is a
--     PERMISSIVE FOR ALL policy covering SELECT/INSERT/UPDATE/DELETE with the
--     tenant predicate.
--   - To block UPDATE/DELETE we CANNOT add a permissive USING(false) policy:
--     permissive policies for the same command are OR'd, so the existing
--     tenant predicate would still permit. We use RESTRICTIVE policies
--     (AND'd → false always wins) PLUS REVOKE (V54 belt-and-suspenders).
--
-- Append-only model (keeps INSERT + SELECT via existing tenant_isolation):
--   1. RESTRICTIVE FOR UPDATE USING(false) WITH CHECK(false) → no row updatable
--   2. RESTRICTIVE FOR DELETE USING(false)                   → no row deletable
--   3. REVOKE UPDATE/DELETE/TRUNCATE from app role (V54 DO-block pattern) when
--      migration runs as kiteclass_app; no-op when run as superuser (dev/test).
--
-- Test profile bypass: Testcontainers + local dev run migrations as Postgres
-- superuser. Superusers bypass RLS unless FORCE is set; FORCE is set, so even
-- superuser is subject to the RESTRICTIVE block. Tests that must purge audit
-- rows (cleanup) should use `SET LOCAL row_security = off` (break-glass, V58
-- note) OR TRUNCATE via a superuser session where REVOKE wasn't applied.
-- App-path tests never UPDATE/DELETE audit_log (AuditLogWriter is insert-only).
--
-- Breaking change: NO new columns. Adds restrictive policies + REVOKE.
-- Idempotent (DROP POLICY IF EXISTS before CREATE; REVOKE is idempotent).
-- =========================================================================

ALTER TABLE audit_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_log FORCE ROW LEVEL SECURITY;

-- 1. Block UPDATE — RESTRICTIVE so it AND's with the permissive tenant_isolation
--    policy; USING(false) → zero rows visible for UPDATE → nothing updatable.
DROP POLICY IF EXISTS audit_log_no_update ON audit_log;
CREATE POLICY audit_log_no_update ON audit_log
    AS RESTRICTIVE
    FOR UPDATE
    USING (false)
    WITH CHECK (false);

-- 2. Block DELETE — RESTRICTIVE USING(false) → nothing deletable.
DROP POLICY IF EXISTS audit_log_no_delete ON audit_log;
CREATE POLICY audit_log_no_delete ON audit_log
    AS RESTRICTIVE
    FOR DELETE
    USING (false);

-- 3. REVOKE UPDATE/DELETE/TRUNCATE from app role (defense-in-depth, V54 pattern).
DO $$
DECLARE
    app_role TEXT;
BEGIN
    SELECT rolname INTO app_role
    FROM pg_roles
    WHERE rolname IN ('kiteclass_app', 'kiteclass', 'kite_app')
    LIMIT 1;

    IF app_role IS NOT NULL THEN
        EXECUTE format('REVOKE UPDATE ON audit_log FROM %I', app_role);
        EXECUTE format('REVOKE DELETE ON audit_log FROM %I', app_role);
        EXECUTE format('REVOKE TRUNCATE ON audit_log FROM %I', app_role);
    END IF;
END $$;

COMMENT ON TABLE audit_log IS
    'Append-only audit trail (V35 Wave 4). DB-level immutability via V83 '
    '(GAP-889): RESTRICTIVE RLS policies block UPDATE/DELETE + REVOKE on app '
    'role. INSERT/SELECT remain tenant-scoped via V58/V59 tenant_isolation. '
    'Compliance parity with admin_audit_logs (V60) + child_protection_audit_log (V54).';
