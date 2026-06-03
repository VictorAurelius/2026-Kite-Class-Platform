-- Wave 14 Bucket C: audit actor UUID sweep after V73.
--
-- V73 intentionally covered BaseEntity audit columns plus a small set of known
-- actor columns. Wave 13 cluster audit found remaining actor-attribution columns
-- that still used BIGINT/VARCHAR even though Gateway X-User-Id is a JWT subject
-- UUID.
--
-- Boundary call: production may contain legacy BIGINT actor ids and KiteClass has
-- no local `users` table that can map those numeric ids to JWT subject UUIDs.
-- Direct `col::uuid` would fail for non-castable values, and `NULL::uuid` would
-- break NOT NULL actor columns such as `attendance_period.recorded_by`. Preserve
-- row distinctness with deterministic UUIDs derived from the legacy literal:
--   md5('kiteclass:audit-actor:' || legacy_value) -> UUID text form.
-- This keeps migrations replayable and non-castable values queryable as UUIDs;
-- future writes should store real Gateway X-User-Id UUIDs.

CREATE OR REPLACE FUNCTION _kc_audit_actor_to_uuid(raw_value TEXT)
RETURNS UUID
LANGUAGE SQL
IMMUTABLE
AS $$
    SELECT CASE
        WHEN raw_value IS NULL OR btrim(raw_value) = '' THEN NULL
        WHEN btrim(raw_value) ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
            THEN btrim(raw_value)::uuid
        ELSE (
            substr(md5('kiteclass:audit-actor:' || btrim(raw_value)), 1, 8) || '-' ||
            substr(md5('kiteclass:audit-actor:' || btrim(raw_value)), 9, 4) || '-' ||
            substr(md5('kiteclass:audit-actor:' || btrim(raw_value)), 13, 4) || '-' ||
            substr(md5('kiteclass:audit-actor:' || btrim(raw_value)), 17, 4) || '-' ||
            substr(md5('kiteclass:audit-actor:' || btrim(raw_value)), 21, 12)
        )::uuid
    END;
$$;

DO $$
DECLARE
    target RECORD;
BEGIN
    FOR target IN
        SELECT c.table_schema, c.table_name, c.column_name
        FROM information_schema.columns c
        JOIN information_schema.tables t
          ON t.table_schema = c.table_schema
         AND t.table_name = c.table_name
        WHERE c.table_schema = 'public'
          AND t.table_type = 'BASE TABLE'
          AND c.data_type <> 'uuid'
          AND (
              c.column_name LIKE '%\_by' ESCAPE '\'
              OR (c.table_name = 'payments' AND c.column_name IN ('payer_id', 'received_by'))
              OR (c.table_name = 'payment_idempotency_keys' AND c.column_name = 'user_id')
              OR (c.table_name = 'rebrand_approvals' AND c.column_name IN ('initiator_user_id', 'approver_user_id'))
              OR (c.table_name = 'audit_log' AND c.column_name = 'actor_user_id')
              OR (c.table_name = 'parent_invitations' AND c.column_name = 'invited_by_user_id')
              OR (c.table_name = 'staff_invitations' AND c.column_name IN ('invited_by_user_id', 'accepted_user_id'))
              OR (c.table_name = 'incidents' AND c.column_name IN ('reporter_user_id', 'assigned_officer_user_id'))
              OR (c.table_name = 'dmca_takedown_requests' AND c.column_name = 'reviewer_user_id')
              OR (c.table_name = 'vettings' AND c.column_name = 'decided_by_user_id')
          )
        ORDER BY c.table_name, c.column_name
    LOOP
        EXECUTE format(
            'ALTER TABLE %I.%I ALTER COLUMN %I TYPE UUID USING _kc_audit_actor_to_uuid(%I::text)',
            target.table_schema,
            target.table_name,
            target.column_name,
            target.column_name
        );

        RAISE NOTICE 'Converted %.% to UUID audit actor column',
            target.table_name,
            target.column_name;
    END LOOP;
END $$;

DROP FUNCTION _kc_audit_actor_to_uuid(TEXT);
