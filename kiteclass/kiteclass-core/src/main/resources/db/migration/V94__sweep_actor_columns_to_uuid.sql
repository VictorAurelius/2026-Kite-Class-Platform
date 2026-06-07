-- GAP-877 (KC portion): sweep the SAFE actor user-id columns BIGINT/VARCHAR -> UUID.
--
-- ROOT CAUSE
-- ----------
-- Gateway forwards X-User-Id = JWT `sub` claim = a UUID (GAP-795). V73 already
-- converted created_by / updated_by + a few hardcoded actor columns (classes.teacher_id,
-- classes.rescheduled_by_user_id, parent_invitations.invited_by_user_id). Many other
-- actor-identity columns were left as BIGINT (or VARCHAR for user_roles.assigned_by).
-- A UUID actor written into a BIGINT column fails to bind, so the column can never
-- carry the real actor identity.
--
-- SCOPE OF THIS MIGRATION (the SAFE subset — see "DEFERRED" note below)
-- --------------------------------------------------------------------
-- Only columns that are EITHER (a) not mapped/bound by any JPA entity, OR (b) mapped by
-- an entity field that no writer ever populates. These convert with zero behavioral risk
-- because no live write path binds a value into them today. Each entry below is verified
-- against the codebase (grep: no @Column binding for payments.received_by/payer_id; no
-- JPA entity for reward_redemptions; UserRole.assignedBy / ModerationQueue.assignedReviewerId
-- / Incident.assignedOfficerUserId have no setter/builder writer). The companion entity-field
-- sync (same PR) retypes the three unset entity fields Long/String -> UUID so the column and
-- entity stay aligned per design-patterns.md §3.12 (Entity-Migration triad).
--
--   payments.received_by            (no entity field binding)
--   payments.payer_id               (no entity field binding)
--   reward_redemptions.approved_by  (no JPA entity at all)
--   user_roles.assigned_by          VARCHAR(100) -> uuid (entity field unset)
--   moderation_queue.assigned_reviewer_id (entity field unset)
--
-- DATA-LOSS-FREE GUARANTEE (pre-launch)
-- -------------------------------------
-- Phase 1 BETA has no production data in these tables. Converting BIGINT/VARCHAR -> uuid
-- with `USING NULL::uuid` discards nothing — these columns are NULL / never written today.
--
-- EXPLICITLY OUT OF SCOPE (verified by migration grep + code trace before authoring):
--   * grades.graded_by — DROPPED in V85 (column no longer exists).
--   * payment_idempotency_keys.user_id — written via native JdbcTemplate bound to the
--     numeric parent reference id (ParentPaymentController passes resolvedParentId Long,
--     NOT the actor UUID). Requires a semantic decision (actor UUID vs parent domain id).
--   * 13 actor columns that ARE threaded with a numeric domain/reference id by live
--     service code and so cannot be retyped without per-module actor re-wiring
--     (e.g. attendance.marked_by is set from the numeric teacherId used in
--     findByTeacherIdAndClassId authz; incidents.reporter_user_id / rebrand_approvals.*
--     are NOT NULL Long params threaded from controllers; audit_log.actor_user_id is fed
--     by Long actor params across retention/legal/childprotection). Converting these
--     mechanically would store a UUID where authz/domain logic still expects a Long, or
--     fail the NOT NULL constraint when UserContext is unset. Deferred to a focused
--     follow-up gap (GAP-877 KC remainder) that re-wires each writer to
--     UserContext.getCurrentUser() (UUID) while keeping numeric domain ids separate.
--     Columns deferred: attendance.marked_by, grades.finalized_by, submissions.graded_by,
--     subject_grades.reviewed_by, attendance_period.recorded_by, payment_records.recorded_by,
--     vettings.decided_by_user_id, audit_log.actor_user_id,
--     dmca_takedown_requests.reviewer_user_id, deletion_requests.user_id,
--     incidents.reporter_user_id, incidents.assigned_officer_user_id,
--     child_protection_audit_log.actor_id,
--     rebrand_approvals.initiator_user_id, rebrand_approvals.approver_user_id.

-- =============================================================================
-- Sweep: each (table, column) -> uuid via dynamic loop. Drops any FK on the
-- column first (mirrors V73 classes.teacher_id handling — actor UUID can never
-- reference a BIGINT PK). Guarded by information_schema existence + non-uuid type
-- check, so the migration is idempotent and skips silently if a column is absent
-- or already uuid.
-- =============================================================================
DO $$
DECLARE
    fk_name TEXT;
    targets CONSTANT TEXT[][] := ARRAY[
        ['payments',          'received_by'],
        ['payments',          'payer_id'],
        ['reward_redemptions','approved_by'],
        ['user_roles',        'assigned_by'],
        ['moderation_queue',  'assigned_reviewer_id']
    ];
    i   INT;
    tbl TEXT;
    col TEXT;
BEGIN
    FOR i IN 1 .. array_length(targets, 1) LOOP
        tbl := targets[i][1];
        col := targets[i][2];

        IF EXISTS (
            SELECT 1 FROM information_schema.columns c
            JOIN information_schema.tables t
              ON t.table_schema = c.table_schema AND t.table_name = c.table_name
            WHERE c.table_schema = 'public'
              AND t.table_type   = 'BASE TABLE'
              AND c.table_name   = tbl
              AND c.column_name  = col
              AND c.data_type   <> 'uuid'
        ) THEN
            -- Drop any FK on the column before retyping (actor UUID is never a domain FK).
            FOR fk_name IN
                SELECT tc.constraint_name
                FROM information_schema.table_constraints tc
                JOIN information_schema.key_column_usage kcu
                  ON tc.constraint_name = kcu.constraint_name
                 AND tc.table_schema    = kcu.table_schema
                WHERE tc.table_schema    = 'public'
                  AND tc.table_name      = tbl
                  AND tc.constraint_type = 'FOREIGN KEY'
                  AND kcu.column_name    = col
            LOOP
                EXECUTE format('ALTER TABLE %I DROP CONSTRAINT %I', tbl, fk_name);
            END LOOP;

            EXECUTE format(
                'ALTER TABLE %I ALTER COLUMN %I TYPE uuid USING NULL::uuid',
                tbl, col
            );
        END IF;
    END LOOP;
END $$;
