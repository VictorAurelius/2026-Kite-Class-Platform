# GAP-544: kitehub-subscription integration tests require Postgres :5433 (testcontainers flakiness)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** DevOps
**Found:** 2026-05-14 (Wave 78 Bucket B + F CI fail on shared infra)
**Affects:** `kitehub-subscription` + `kitehub-admin` CI builds in `kitehub-ci.yml`

## Problem

`Test KiteHub Subscription Service` + `Test KiteHub Admin Service` CI jobs fail with:

```
pg_dump failed with exit code 1: pg_dump: error: connection to server at "localhost" (::1), port 5433 failed: Connection refused
```

Triggered by `DatabaseBackupServiceTest.BackupInstance.shouldAcceptValidDatabaseName` + `InstanceController Integration Tests` (6 errors).

Tests assume a running Postgres on `localhost:5433` (Docker Compose dev stack) but CI runner does not provision Postgres for these jobs. Local `mvn verify` passes when dev stack is up.

Surfaced on Wave 78 Bucket B (PR #1356) + Bucket F (PR #1355) because both touched `kitehub-subscription` module (new packages `onboarding.*` + `feedback.*` added to JPA entity scan → triggered full ApplicationContext load).

Bucket F agent identified as pre-existing: "Pre-existing `InstanceControllerIntegrationTest` ApplicationContext failures unrelated (testcontainers Postgres, present on main)."

## Root Cause

Two test classes hardcode `localhost:5433` connection:
1. `DatabaseBackupService` — invokes real `pg_dump` against running Postgres (should mock OR use Testcontainers)
2. `InstanceControllerIntegrationTest` — Spring Boot integration test with full DataSource bean (should use Testcontainers or H2 profile)

## Proposed Fix

- Migrate `DatabaseBackupServiceTest` to mock `ProcessBuilder` invocation OR add Testcontainers dependency
- `InstanceControllerIntegrationTest` either: (a) use Testcontainers `@PostgreSQLContainer`, (b) fallback to H2 with profile, or (c) move to a separate `@Tag("docker")` group + skip in CI without Postgres

## Acceptance Criteria

- [ ] Both test classes pass on a runner WITHOUT pre-running Postgres :5433
- [ ] CI job `Test KiteHub Subscription Service` green on PRs touching subscription module
- [ ] Local `mvn verify` continues to pass with or without dev stack up

## Related

- Wave 78 Bucket B PR #1356 + Bucket F PR #1355 — admin-merged with `ADMIN_MERGE_OVERRIDE` citing this gap
- Class similar to GAP-244 dev-stack issue
