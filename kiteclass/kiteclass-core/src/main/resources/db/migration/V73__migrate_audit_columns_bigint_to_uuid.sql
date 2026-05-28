-- GAP-795: Migrate created_by / updated_by audit columns from BIGINT to UUID.
--
-- ROOT CAUSE
-- ----------
-- Gateway forwards X-User-Id = JWT `sub` claim = a UUID (e.g. b9fa3522-64e4-...).
-- There is NO numeric user id anywhere in the JWT — the UUID is the only user identity.
-- kiteclass-core was built for a legacy numeric model:
--   * UserContext.CURRENT_USER was ThreadLocal<Long>
--   * TenantFilterInterceptor did Long.parseLong(X-User-Id) → threw on every UUID
--   * The throw was caught → UserContext left null → AuditorAware<Long> returned empty
--   * => created_by / updated_by were NEVER populated (NULL across all tables).
--
-- DATA-LOSS-FREE GUARANTEE
-- ------------------------
-- Because the bug meant the columns were never written, every created_by / updated_by
-- value in production is NULL today. Converting BIGINT → UUID with `USING NULL::uuid`
-- therefore discards no data — there is nothing to discard.
--
-- COMPANION CHANGES (same PR)
-- ---------------------------
--   * BaseEntity.createdBy / updatedBy : Long  → UUID
--   * UserContext.CURRENT_USER          : Long  → UUID
--   * JpaConfig.AuditorAware<Long>       → AuditorAware<UUID>
--   * TenantFilterInterceptor            : Long.parseLong → UUID.fromString
--   * Class.teacherId / rescheduledByUserId (actor identity columns) → UUID (see below)
--
-- IDEMPOTENCY
-- -----------
-- DO block detects current column type before altering. Re-running on an already-UUID
-- column is a no-op. Scans information_schema so it covers every table carrying these
-- columns (V1 core schema BIGINT set + V26 additions + V28..V44 VARCHAR set that V46
-- aligned to BIGINT). No hard-coded table list to drift.

-- =============================================================================
-- 1. created_by / updated_by audit columns → UUID (all tables, all current types)
-- =============================================================================
DO $$
DECLARE
    target RECORD;
BEGIN
    FOR target IN
        SELECT c.table_name, c.column_name
        FROM information_schema.columns c
        JOIN information_schema.tables t
          ON t.table_schema = c.table_schema AND t.table_name = c.table_name
        WHERE c.table_schema = 'public'
          AND t.table_type = 'BASE TABLE'
          AND c.column_name IN ('created_by', 'updated_by')
          AND c.data_type <> 'uuid'   -- skip columns already migrated (idempotent)
    LOOP
        EXECUTE format(
            'ALTER TABLE %I ALTER COLUMN %I TYPE UUID USING NULL::uuid',
            target.table_name, target.column_name
        );
    END LOOP;
END $$;

-- =============================================================================
-- 2. classes.teacher_id → UUID (actor identity, NOT a numeric teacher PK)
-- =============================================================================
-- teacher_id is set from UserContext.getCurrentUser() (the caller's X-User-Id UUID)
-- in ClassServiceImpl.createClass (per GAP-727). It was declared in V1 as
-- `teacher_id BIGINT REFERENCES teachers(id)` — but the value written is the actor
-- UUID, never a teachers.id. With Long.parseLong throwing on every UUID, teacher_id
-- was in practice never set (all NULL). Convert to UUID so the AuthorizationBean
-- ownership check (c.teacher_id = :userId) compares UUID == UUID consistently.
--
-- The teachers(id) FK is dropped: it never held (actor UUID ≠ teachers.id bigint) and
-- a UUID column cannot reference a BIGINT PK.
DO $$
DECLARE
    fk_name TEXT;
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'classes'
          AND column_name = 'teacher_id' AND data_type <> 'uuid'
    ) THEN
        -- Drop any FK constraint on classes.teacher_id before retyping.
        FOR fk_name IN
            SELECT tc.constraint_name
            FROM information_schema.table_constraints tc
            JOIN information_schema.key_column_usage kcu
              ON tc.constraint_name = kcu.constraint_name
             AND tc.table_schema = kcu.table_schema
            WHERE tc.table_schema = 'public'
              AND tc.table_name = 'classes'
              AND tc.constraint_type = 'FOREIGN KEY'
              AND kcu.column_name = 'teacher_id'
        LOOP
            EXECUTE format('ALTER TABLE classes DROP CONSTRAINT %I', fk_name);
        END LOOP;

        ALTER TABLE classes ALTER COLUMN teacher_id TYPE UUID USING NULL::uuid;
    END IF;
END $$;

-- =============================================================================
-- 3. classes.rescheduled_by_user_id → UUID (actor identity audit column)
-- =============================================================================
-- Written from UserContext.getCurrentUser() (V68 GAP-291). Same actor-UUID semantics.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'classes'
          AND column_name = 'rescheduled_by_user_id' AND data_type <> 'uuid'
    ) THEN
        ALTER TABLE classes
            ALTER COLUMN rescheduled_by_user_id TYPE UUID USING NULL::uuid;
    END IF;
END $$;

-- =============================================================================
-- 4. parent_invitations.invited_by_user_id → UUID (actor identity attribution)
-- =============================================================================
-- "Gateway user id of the inviter" — pure actor attribution, never compared to a
-- domain PK. Written from UserContext.getCurrentUser() via ParentInvitationController.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'parent_invitations'
          AND column_name = 'invited_by_user_id' AND data_type <> 'uuid'
    ) THEN
        ALTER TABLE parent_invitations
            ALTER COLUMN invited_by_user_id TYPE UUID USING NULL::uuid;
    END IF;
END $$;
-- NOTE: payments.created_by is already covered by section 1 (created_by/updated_by sweep).
