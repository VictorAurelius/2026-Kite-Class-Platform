# GAP-1059: Dedicated landing_pages Flyway-real RLS IT (regression guard)

**Status:** 🔵 OPEN
**Priority:** 🟢 P3
**Domain:** Backend / DB / Test
**Found:** 2026-06-08 (GAP-885 closure — state-check follow-up)
**Affects:** `kiteclass-core` test coverage — `landing_pages` V78 RLS policy

## Problem

GAP-885 closed AC#3 (landing_pages public-read security model) via design documentation + evidence chain. The V78 `tenant_isolation` policy on landing_pages is byte-identical in shape to `oauth_attempts` V66 (`is_platform_admin OR instance_id = current_tenant_id GUC`), which `OauthAttemptsRlsPostgresIT` already proves on Flyway-real Postgres. So the mechanism is proven repo-wide.

A DEDICATED `LandingPageRlsPostgresIT` would be a direct regression guard for the landing_pages-specific policy, but is non-trivial: kiteclass-core test profile runs Flyway OFF + `ddl-auto=create-drop` (KC-5 masking lesson) → testing the real V78 policy needs a forced-Flyway pattern (clone `OauthAttemptsRlsPostgresIT` `@DynamicPropertySource` flyway.enabled=true + ddl-auto=validate) with bean-wiring for full kiteclass-core @SpringBootTest boot (messaging/Redis mocks).

Not blocking: the identical-shape oauth_attempts IT + `RLSEnforcementIT` (GUC mechanism) + V78 migration confirmation cover the risk.

## Proposed Fix

Add `LandingPageRlsPostgresIT` (kiteclass-core) cloning `OauthAttemptsRlsPostgresIT` self-contained pattern:
- Own `PostgreSQLContainer` + forced Flyway (`spring.flyway.enabled=true`, `ddl-auto=validate`)
- Mock messaging beans as needed for kiteclass-core boot
- Assert: RLS enabled on landing_pages; tenant_isolation policy present; no-GUC → 0 rows (fail-safe); tenant-A GUC → only A's landing page; platform-admin GUC → cross-tenant

## Acceptance Criteria

- [ ] `LandingPageRlsPostgresIT` runs Flyway-real (V78 applies, not entity-derived schema)
- [ ] 4 cases pass: RLS enabled + policy present + tenant-scope isolation + admin-bypass
- [ ] `./mvnw test -Dtest=LandingPageRlsPostgresIT` green

## Related

- Parent: GAP-885 (closed 2026-06-08 — AC#3 design documented; this IT = beyond-AC regression guard)
- Pattern: `OauthAttemptsRlsPostgresIT` (kitehub-subscription — identical policy shape, Flyway-real)
- Sister: `RLSEnforcementIT` (kiteclass-core — GUC mechanism, manual policy)
- Standard: `postgres-specific-type-testcontainers.md` + KC-5 ddl-auto-masks-drift lesson
- Discovered in: GAP-885 closure state-check 2026-06-08
