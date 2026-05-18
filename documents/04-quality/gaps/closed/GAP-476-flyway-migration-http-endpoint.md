# GAP-476: Flyway migration HTTP endpoint for smoke-test verification

**Status:** 🟢 DONE (Wave 64 Bucket C SHIPPED 2026-05-11 — PR #1195)
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-05-11 (Wave 62 Bucket A state-check)
**Affects:** Sub-5 of GAP-475 (smoke-test migration head verify)

## Problem

Wave 62 Bucket A (PR #1183) added `check_migration_head` function to `scripts/smoke-test.sh` (Sub-5 of GAP-475). Function probes `/api/platform/admin/migrations` + `/actuator/flyway` fallback. **Neither endpoint exists in the codebase as of 2026-05-11** — state-check via `grep -rn "flyway_schema_history|@GetMapping.*migration" kitehub/ kiteclass/` confirmed only psql access via `scripts/verify-restore.sh`.

Function ships with graceful SKIP path: when both probe URLs return 404, log `[SKIP]` and exit 0 instead of failing. Sub-5 marked PARTIAL deferred-to-this-gap.

## Proposed Fix

Add admin-authenticated HTTP endpoint exposing Flyway schema head version. Options:

**Option A — Enable Spring Boot Actuator Flyway endpoint:**
- Add to gateway/admin `application.yml`: `management.endpoints.web.exposure.include: flyway`
- Add admin auth filter to `/actuator/flyway` (production must not be public)
- Endpoint returns `{contexts: {<dataSource>: {flywayBeans: [{migrations: [{version, description, state}]}]}}}`

**Option B — Custom admin controller:**
- New `MigrationStatusController` in `kitehub-admin` or `kiteclass-core` admin module
- `GET /api/platform/admin/migrations` returns `{current_version, last_applied, count}`
- Query `flyway_schema_history` directly via injected `Flyway` bean OR `JdbcTemplate`
- Admin auth via existing JWT filter

Option A simpler; Option B more controlled output. Pick during implementation.

## Acceptance Criteria

- [ ] HTTP endpoint exposed at either `/actuator/flyway` (Option A) or `/api/platform/admin/migrations` (Option B), admin-auth-gated
- [ ] Returns max-version field in stable JSON shape
- [ ] `scripts/smoke-test.sh check_migration_head` updated to use real endpoint, removes graceful SKIP path
- [ ] Integration test verifies admin token required (401 without, 200 with)
- [ ] Documented in `api-contract.md` per project convention

## Related

- **Parent:** GAP-475 Sub-5 (P1+P2 cluster, Wave 62)
- **Sibling:** GAP-477 (rollback.yml workflow absent, also Wave 62 deferral)
- **References:**
  - `scripts/smoke-test.sh` `check_migration_head` function (Wave 62 Bucket A)
  - `scripts/verify-restore.sh` (current psql-only access path)

## Log

- **2026-05-11:** Filed as Wave 62 Bucket A deferral. Sub-5 of GAP-475 PARTIAL pending this endpoint.
- **2026-05-11 (Wave 64 Bucket C SHIPPED):** PR #1195 ships Option A (Spring Actuator Flyway expose) + custom `FlywayEndpointAuthFilter` (gateway-trust pattern matching existing `VettingController` precedent — Spring Security permitAll because gateway is auth boundary forwarding `X-User-Roles`). 6 unit tests PASS. smoke-test.sh `check_migration_head` rewired to `${KC_URL}/kiteclass/actuator/flyway`, graceful SKIP removed → probe failures now FAIL. kiteclass-core canonical Flyway owner (58 V*.sql migrations). Status OPEN → DONE.
