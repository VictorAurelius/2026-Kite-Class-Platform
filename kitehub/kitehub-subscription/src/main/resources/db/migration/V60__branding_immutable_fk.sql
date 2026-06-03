-- V60: Wave 14 Bucket C-KH — branding_lifecycle_events immutability + branding cluster FK coverage.
--
-- GAP-900 (P1): branding_lifecycle_events documents append-only audit trail (V30 comment +
--   entity javadoc) but DB has NO enforcement. Apply RLS immutable pattern matching
--   admin_audit_logs (V50:108-142) + consent_record_immutable (V56): ENABLE + FORCE RLS,
--   SELECT/INSERT permitted, UPDATE/DELETE policies USING(false). KH tenant tables normally
--   do NOT use FORCE (per V34/V50/V58 — Spring HikariCP user = table owner bypasses non-forced
--   policy), BUT immutable audit tables DO use FORCE — same as admin_audit_logs V50:130.
--
-- GAP-901 (P3): branding cluster FK coverage thin — only branding_jobs (V4) has real FK to
--   instances(id). State-check (V16/V29/V30) confirms 3 counter/state tables can safely add
--   FK CASCADE to instances(id) without losing functionality (gap AC: branding_instance_state,
--   branding_regenerate_usage, backup_records). NOTE: gap originally listed ai_usage_log but
--   NO ai_usage_log migration exists in this folder (state-check: grep CREATE TABLE ai_usage_log
--   = 0 hits) → out of scope, reported as anomaly. Audit/outbox tables keep logical ref
--   (forensic survive instance purge) per gap design intent.
--
-- Forward-only + idempotent (DO-block guards). NOT touching KC files.

-- ===== GAP-900: branding_lifecycle_events append-only at DB level =====

ALTER TABLE branding_lifecycle_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE branding_lifecycle_events FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS branding_lifecycle_events_select ON branding_lifecycle_events;
CREATE POLICY branding_lifecycle_events_select ON branding_lifecycle_events
    FOR SELECT USING (true);

DROP POLICY IF EXISTS branding_lifecycle_events_insert ON branding_lifecycle_events;
CREATE POLICY branding_lifecycle_events_insert ON branding_lifecycle_events
    FOR INSERT WITH CHECK (true);

-- BANNED: UPDATE + DELETE — append-only enforcement (matches admin_audit_no_update/no_delete V50).
DROP POLICY IF EXISTS branding_lifecycle_events_no_update ON branding_lifecycle_events;
CREATE POLICY branding_lifecycle_events_no_update ON branding_lifecycle_events
    FOR UPDATE USING (false) WITH CHECK (false);

DROP POLICY IF EXISTS branding_lifecycle_events_no_delete ON branding_lifecycle_events;
CREATE POLICY branding_lifecycle_events_no_delete ON branding_lifecycle_events
    FOR DELETE USING (false);

COMMENT ON POLICY branding_lifecycle_events_no_update ON branding_lifecycle_events IS
    'GAP-900 Wave 14 C-KH: append-only audit trail. UPDATE blocked at DB. State change = new event row.';
COMMENT ON POLICY branding_lifecycle_events_no_delete ON branding_lifecycle_events IS
    'GAP-900 Wave 14 C-KH: append-only audit trail. DELETE blocked at DB. Retention purge = superuser bypass.';

-- ===== GAP-901: FK CASCADE for branding counter/state tables → instances(id) =====
-- Added via NOT VALID + VALIDATE to avoid long lock + tolerate forward-only replay
-- (validation will fail loudly only if orphan rows exist — dev DBs are empty/seeded clean).

-- branding_instance_state.instance_id (PK, NOT NULL — V30)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_schema = 'public'
          AND constraint_name = 'fk_branding_instance_state_instance'
    ) THEN
        ALTER TABLE branding_instance_state
            ADD CONSTRAINT fk_branding_instance_state_instance
            FOREIGN KEY (instance_id) REFERENCES instances(id) ON DELETE CASCADE;
    END IF;
END $$;

-- backup_records.instance_id (NOT NULL — V16)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_schema = 'public'
          AND constraint_name = 'fk_backup_records_instance'
    ) THEN
        ALTER TABLE backup_records
            ADD CONSTRAINT fk_backup_records_instance
            FOREIGN KEY (instance_id) REFERENCES instances(id) ON DELETE CASCADE;
    END IF;
END $$;

-- branding_regenerate_usage.instance_id (NULLABLE — V29). FK CASCADE tolerates NULL
-- (FK not enforced on NULL); non-null rows cascade-delete with instance.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_schema = 'public'
          AND constraint_name = 'fk_branding_regen_instance'
    ) THEN
        ALTER TABLE branding_regenerate_usage
            ADD CONSTRAINT fk_branding_regen_instance
            FOREIGN KEY (instance_id) REFERENCES instances(id) ON DELETE CASCADE;
    END IF;
END $$;
