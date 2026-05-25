-- GAP-735 + GAP-745 — truncate all user tables before each test method
-- Per Wave meta-1 retry-budget pivot: @Transactional@Rollback insufficient for tests using
-- @Async / REQUIRES_NEW / event listeners that escape test transaction scope.
-- This script truncates all user tables (excluding Flyway migration tracking) in a single
-- transaction, restoring identity sequences so subsequent test assertions on row counts/ids
-- start from a clean slate.

DO $$
DECLARE
    r RECORD;
BEGIN
    -- Disable triggers temporarily (some FK constraints have triggers)
    SET session_replication_role = 'replica';

    FOR r IN
        SELECT tablename
        FROM pg_tables
        WHERE schemaname = 'public'
          AND tablename NOT IN ('flyway_schema_history')
    LOOP
        EXECUTE 'TRUNCATE TABLE ' || quote_ident(r.tablename) || ' RESTART IDENTITY CASCADE';
    END LOOP;

    -- Re-enable triggers
    SET session_replication_role = 'origin';
END $$;
