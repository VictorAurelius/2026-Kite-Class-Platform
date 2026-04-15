# GAP-065: Migration Chain Not Fresh-Deploy Safe

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend / DevOps
**Detected:** 2026-04-14 (during GAP-007/009 IT test work)
**Discovered in:** PR #280

## Problem

`V25__add_theme_config_to_branding.sql` runs `ALTER TABLE branding ADD COLUMN IF NOT EXISTS theme_config_json`. The `branding` table is **not** created by any kiteclass-core migration V1..V24 — it is provisioned by `kitehub-branding` service.

On a **fresh, empty** Postgres, Flyway runs V1..V25 and V25 fails with:
```
ERROR: relation "branding" does not exist
```

Production tolerates this via `spring.flyway.baseline-on-migrate=true`: existing environments had `branding` already present (from kitehub-branding migrations), so Flyway baselined past the issue. New/fresh deploys will hit it.

## Evidence

- Wave02MigrationsTest (PR #280) caught this when running `Flyway.migrate()` against a fresh Testcontainers Postgres from V1 — had to baseline at V27 to proceed.
- `grep "CREATE TABLE branding"` in `kiteclass/kiteclass-core/src/main/resources/db/migration` → no match in V1..V24.
- Production `application.yml` has `baseline-on-migrate: true` — masks the issue.

## Impact

- **Fresh DR restore** from bare Postgres backup may fail.
- **New environment bootstrap** (staging rebuild, developer onboarding with empty DB) fails.
- **CI migration testing** blocked without workarounds.

## Proposed Fix

Option A — **Move `branding` table create into kiteclass-core** (if branding belongs here conceptually):
- New migration `V33__create_branding_table.sql` with the DDL kitehub-branding currently ships.
- Keep V25 as-is (idempotent `ADD COLUMN IF NOT EXISTS` still safe).

Option B — **Make V25 defensive**:
- Wrap V25 in `DO $$ IF EXISTS ... END $$` block so it skips gracefully when `branding` missing.
- Document that `branding` is provisioned externally.

Option C — **Split migration paths**:
- `spring.flyway.locations=classpath:db/migration/core,classpath:db/migration/shared`
- Put `branding`-related migrations in `shared` and only activate when appropriate profile.

**Recommended:** Option A — single source of ownership avoids latent fragility.

## Acceptance Criteria

- [ ] Fresh empty Postgres + `./mvnw test` + Flyway migrate from V1 succeeds without baseline workaround
- [ ] Wave02MigrationsTest removes `baselineVersion("27")` hack
- [ ] Migration plan doc updated with resolution
- [ ] Drift check added to CI (migration-dry-run on fresh Postgres)

## Log

- 2026-04-14 — Detected while adding Wave02MigrationsTest; workaround (baselineVersion=27) shipped in PR #280
