-- V46: Wave 79 Bucket B (GAP-562) — RBAC OWNER/STAFF role separation.
--
-- Persona P3 Manager + P2 Center Owner distinction:
--   - OWNER  = tenant owner; full access to billing, branding, AI Branding,
--              staff invitation, instance management.
--   - STAFF  = invited staff; scoped access to operational features; CANNOT
--              touch billing / branding / AI Branding / staff management.
--
-- Schema source-of-truth: documents/01-business/roles/rules.md +
-- documents/01-business/roles/use-cases.md (Wave 79 Bucket 0 Foundation).
--
-- Migration ordering: V46 (this) MUST run AFTER V45__create_staff_invitations.sql
-- per .claude/rules/concurrent-production-mutation-ops.md (V45 creates the
-- staff_invitations table that this role split depends on for ownership).
--
-- Backward compatibility (30-day window, cutoff 2026-06-14 — Wave 81 cleanup):
--   - Existing 'PLATFORM_ADMIN' role values continue to work (mapped to OWNER
--     at security-filter level via alias).
--   - Existing 'ADMIN' role values continue to work (mapped to OWNER).
--   - No existing rows mutated by this migration; runtime authorities resolver
--     handles aliasing. Wave 81 follow-up will UPDATE rows to canonical OWNER
--     and DROP alias support.

-- 1. Add allowed_roles enumeration constraint to users.role column (idempotent).
--    Constraint applied as CHECK to avoid breaking existing PLATFORM_ADMIN /
--    ADMIN values during 30-day backward-compat window.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'users'
          AND constraint_name = 'ck_users_role_v46'
    ) THEN
        ALTER TABLE users
            ADD CONSTRAINT ck_users_role_v46
            CHECK (role IN (
                'OWNER',            -- Wave 79 canonical
                'STAFF',            -- Wave 79 canonical
                'PLATFORM_ADMIN',   -- backward-compat alias for OWNER (Wave 81 cleanup)
                'ADMIN'             -- backward-compat alias for OWNER (Wave 81 cleanup)
            ));
    END IF;
END $$;

-- 2. Reverse-link from users to staff_invitations (per V45) for accepted invites.
--    Allows audit trail: "which staff was invited by which owner, on what date".
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'invited_by_user_id'
    ) THEN
        ALTER TABLE users ADD COLUMN invited_by_user_id UUID;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'tenant_id'
    ) THEN
        ALTER TABLE users ADD COLUMN tenant_id UUID;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_users_tenant_role
    ON users (tenant_id, role)
    WHERE tenant_id IS NOT NULL;

COMMENT ON COLUMN users.role IS
    'Wave 79 GAP-562 — Canonical values: OWNER, STAFF. Backward-compat aliases PLATFORM_ADMIN, ADMIN map to OWNER until 2026-06-14 (Wave 81 cleanup wave).';

COMMENT ON COLUMN users.invited_by_user_id IS
    'Wave 79 GAP-562 — For STAFF users, references the OWNER user.id who issued the invitation. NULL for OWNER users (self-registered).';

COMMENT ON COLUMN users.tenant_id IS
    'Wave 79 GAP-562 — For STAFF users, the tenant they belong to (matches staff_invitations.tenant_id at accept time). OWNER users may have tenant_id = their owned tenant.';
