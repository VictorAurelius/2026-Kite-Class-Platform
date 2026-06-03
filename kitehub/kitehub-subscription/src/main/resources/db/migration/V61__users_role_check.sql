-- V61: Wave 14 Bucket C-KH — users.role data migration + CHECK coverage (GAP-893).
--
-- GAP-893 (P1): users.role drift — seed admin (V9) inserted role='ADMIN', but V37 TOTP
--   backfill used `WHERE role='PLATFORM_ADMIN'` → the seed admin never got totp_required=TRUE.
--   Legacy 'ADMIN' is a platform-admin alias (NOT a tenant OWNER); the seed user
--   00000000-0000-0000-0000-000000000099 is genuinely a platform administrator.
--
-- State-check (V9 + V37 + V46 + V58):
--   * V9   seed: role='ADMIN' for id ...000099 (admin@kitehub.com), no totp.
--   * V37  TOTP backfill: UPDATE ... WHERE role='PLATFORM_ADMIN' → MISSED the 'ADMIN' seed.
--   * V46  ALREADY added CHECK ck_users_role_v46 allowing OWNER/STAFF/PLATFORM_ADMIN/ADMIN
--          (backward-compat 30-day window). So GAP-893 "no CHECK constraint" is STALE — a
--          CHECK exists. This migration does the DATA migration + TOTP sync that V46 did NOT.
--
-- ANOMALY/CONFLICT reported: V46 roadmap (Wave 81 cleanup, line 22-27) plans to UPDATE legacy
--   'ADMIN'/'PLATFORM_ADMIN' rows → canonical 'OWNER'. GAP-893 wants 'ADMIN' → 'PLATFORM_ADMIN'.
--   These disagree on intent for tenant-OWNER aliases. RESOLUTION: this migration migrates ONLY
--   the seed platform-admin row (genuinely a PLATFORM_ADMIN, not a tenant owner). It does NOT
--   touch any other 'ADMIN'/'PLATFORM_ADMIN' rows, leaving the V46/Wave-81 OWNER-canonicalization
--   decision intact for real tenant-owner rows. CHECK stays as V46 defined (allows all 4 values).
--
-- Forward-only + idempotent. No money fields, no KC files.

-- ===== GAP-893 (a): data migration — seed platform admin 'ADMIN' -> 'PLATFORM_ADMIN' =====
-- Scope strictly to the V9 seed admin id to avoid colliding with V46/Wave-81 tenant-owner
-- OWNER-canonicalization. (V46 keeps 'ADMIN' as a valid value during the compat window, so
-- this is a targeted normalization of the platform-admin seed only.)
UPDATE users
SET role = 'PLATFORM_ADMIN'
WHERE id = '00000000-0000-0000-0000-000000000099'
  AND role = 'ADMIN';

-- ===== GAP-893 (b): sync TOTP requirement for platform admins (the V37 miss) =====
-- Now the seed admin (and any PLATFORM_ADMIN missed earlier) gets totp_required=TRUE so first
-- login forces enrollment. Idempotent — only flips FALSE -> TRUE.
UPDATE users
SET totp_required = TRUE
WHERE role = 'PLATFORM_ADMIN'
  AND totp_required = FALSE;

-- ===== GAP-893 (c): CHECK coverage reaffirmation =====
-- V46 already created ck_users_role_v46 allowing OWNER/STAFF/PLATFORM_ADMIN/ADMIN. The CHECK
-- already permits every value present after the UPDATE above (PLATFORM_ADMIN ∈ allowed set),
-- so no constraint change is needed. Idempotent guard documents the dependency + ensures the
-- constraint exists even if replay order ever diverges.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_schema = 'public'
          AND table_name = 'users'
          AND constraint_name = 'ck_users_role_v46'
    ) THEN
        ALTER TABLE users
            ADD CONSTRAINT ck_users_role_v46
            CHECK (role IN ('OWNER', 'STAFF', 'PLATFORM_ADMIN', 'ADMIN'));
    END IF;
END $$;
