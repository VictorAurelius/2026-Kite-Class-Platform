-- Wave 14 Bucket C: KiteHub BaseEntity audit columns String -> UUID.
--
-- Boundary call: existing `created_by` / `updated_by` values may include legacy
-- emails or service labels, not UUID literals. There is no reliable cross-service
-- lookup from those labels to `users.id`. Direct casts would fail. Preserve
-- distinct legacy attribution with deterministic UUIDs derived from the literal:
--   md5('kitehub:audit-actor:' || legacy_value) -> UUID text form.
-- New JPA writes use BaseEntity.createdBy / updatedBy as UUID.

CREATE OR REPLACE FUNCTION _kh_audit_actor_to_uuid(raw_value TEXT)
RETURNS UUID
LANGUAGE SQL
IMMUTABLE
AS $$
    SELECT CASE
        WHEN raw_value IS NULL OR btrim(raw_value) = '' THEN NULL
        WHEN btrim(raw_value) ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
            THEN btrim(raw_value)::uuid
        ELSE (
            substr(md5('kitehub:audit-actor:' || btrim(raw_value)), 1, 8) || '-' ||
            substr(md5('kitehub:audit-actor:' || btrim(raw_value)), 9, 4) || '-' ||
            substr(md5('kitehub:audit-actor:' || btrim(raw_value)), 13, 4) || '-' ||
            substr(md5('kitehub:audit-actor:' || btrim(raw_value)), 17, 4) || '-' ||
            substr(md5('kitehub:audit-actor:' || btrim(raw_value)), 21, 12)
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
          AND c.column_name IN ('created_by', 'updated_by')
        ORDER BY c.table_name, c.column_name
    LOOP
        EXECUTE format(
            'ALTER TABLE %I.%I ALTER COLUMN %I TYPE UUID USING _kh_audit_actor_to_uuid(%I::text)',
            target.table_schema,
            target.table_name,
            target.column_name,
            target.column_name
        );

        RAISE NOTICE 'Converted %.% to UUID BaseEntity audit column',
            target.table_name,
            target.column_name;
    END LOOP;
END $$;

DROP FUNCTION _kh_audit_actor_to_uuid(TEXT);
