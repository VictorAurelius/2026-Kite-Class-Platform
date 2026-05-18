# GAP-362: TenantIsolationIT.shouldIsolateCourseDataBetweenTenants pre-existing flake

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (test-isolation correctness — security-adjacent test, untracked)
**Domain:** Backend Testing / Multi-tenancy
**Found:** 2026-05-05 (orphan audit during Wave 19 closure — flagged inline in GAP-347 Log "out of scope, pre-existing" but no dedicated gap)
**Affects:** `mvn verify` on kiteclass-core; CI Test Core Service may flake; tenant-isolation regression detection signal degraded

## Problem

`TenantIsolationIT.shouldIsolateCourseDataBetweenTenants` (line 148 of `kiteclass/kiteclass-core/src/test/java/com/kiteclass/core/integration/TenantIsolationIT.java`) fails sporadically when running `./mvnw verify`. Surfaced multiple times as "pre-existing, unrelated to current scope":

- 2026-05-04 GAP-347 closure (PR #775 merged): closing PR Log explicitly noted "Pre-existing `TenantIsolationIT.shouldIsolateCourseDataBetweenTenants` failure surfaced by `mvn verify` is unrelated test-data-isolation cleanup (out of this config-only PR's scope per task mandate)."
- 2026-05-05 Wave 19 Bucket A closure (PR #793): salvage agent's report quoted same failure, again "untouched since Wave 14" — orphan persists.

**Governance violation:** per `audit-to-gap-pipeline.md` Step 1-3 + `gap-done-discipline.md` §2 anti-pattern row "Verified locally on my machine after I worked around X", every pre-existing failure unrelated to current scope MUST get its own gap file. Inline "out of scope, pre-existing" Log mention without paired gap = orphaned debt that nobody tracks.

This gap closes that orphan.

## Current State (verified 2026-05-05)

| Artifact | State |
|---|---|
| `kiteclass-core/.../integration/TenantIsolationIT.java:46` | ✅ exists (class) |
| `shouldIsolateCourseDataBetweenTenants` test method (line 148) | ✅ exists |
| Untouched since Wave 14 (per Bucket A closure agent report) | needs git blame verify |
| Existing dedicated gap | ❌ not filed (this gap = the filing) |
| Mentioned in | GAP-347 Log (PR #775 closure) + GAP-322c PR #793 closure report |

## Root Cause (preliminary — needs investigation)

Hypothesis (not yet verified):
- Test-data isolation cleanup race between concurrent `@Transactional` rollback + Hibernate session caching
- OR shared instance/tenant fixture ID across IT classes leaking state
- OR Postgres testcontainer fixture issue with multi-tenant row-level filter (PostgreSQL RLS or per-tenant schema)

Reproduction protocol (suggested for fix PR):
1. `./mvnw -pl kiteclass-core verify -Dtest=TenantIsolationIT` ≥10 runs → record pass rate
2. If <100% → flake confirmed; capture stack from failing run
3. If 100% locally → CI-only flake → check CI's parallelism/test-order

## Proposed Fix (defer to dedicated PR — NOT this filing)

### Phase 1 — Investigation
1. Run flake repro ≥20× locally, document pass/fail rate
2. Identify race condition or fixture leak via `--debug` + thread dump
3. ADR if architectural change needed (e.g., switch tenant isolation from app-layer filter → Postgres RLS)

### Phase 2 — Fix
- Either fixture isolation hardening (`@DirtiesContext` per test method) OR domain-layer fix
- Add CI flake-detection if root cause is timing/race-condition

### Phase 3 — Lock-in
- Re-run `mvn verify` ≥10× post-fix, confirm 100% pass
- Optional: add `@RetryingTest(3)` shield only if root cause is environmental + truly intermittent

## Acceptance Criteria

- [ ] Reproduction protocol documented + run results captured
- [ ] Root cause identified (fixture leak / race / RLS / etc.)
- [ ] Fix shipped (could be test-fix only OR domain code fix depending on root cause)
- [ ] `./mvnw -pl kiteclass-core verify -Dtest=TenantIsolationIT` 10/10 pass post-fix
- [ ] CI Test Core Service green for ≥3 consecutive runs of changed branch
- [ ] If domain fix touched: business docs update per `business-logic-review.md` §2 5-attribute frontmatter (tenant-isolation BR-* might exist)

## Related

- GAP-347 (DONE 2026-05-04, PR #775) — first inline mention of this flake
- Wave 19 Bucket A closure report (PR #793) — second inline mention
- `kiteclass-core/.../integration/TenantIsolationIT.java:148` — failing test
- `audit-to-gap-pipeline.md` Step 1-3 — gap-filing discipline
- `gap-done-discipline.md` §2 — anti-pattern banning "verified locally after I worked around X"

## Wave-eligibility

❌ NOT during Wave 19. ✅ Post-Wave-19 P1 (test-isolation correctness, security-adjacent → higher than typical tech-debt).

## Out of scope

- Refactor to Postgres RLS (deferred separate gap if Phase 1 investigation recommends)
- Performance audit of multi-tenant queries (separate concern)

## Estimated Effort

~1-2 days:
- Phase 1: 0.5 day investigation + repro
- Phase 2: 0.5-1 day fix
- Phase 3: 0.5 day verification

## Log

- **2026-05-05** Filed during Wave 19 closure orphan audit. User flagged: "TenantIsolationIT flake là miss của previous closure agent — flag inline trong GAP-347 'out of scope' nhưng không file gap riêng → orphaned. Đáng lẽ phải có GAP riêng cho mỗi pre-existing failure unrelated to current scope." Orphan audit found ONE such case (this gap); rule confirmed via `audit-to-gap-pipeline.md` Step 1-3 + `gap-done-discipline.md` §2 anti-pattern row. Filed at GAP-362 to avoid collision with reserved GAP-359/360/361 (Bucket A/B/C salvage agents' Phase 1C remainder gaps in flight).
