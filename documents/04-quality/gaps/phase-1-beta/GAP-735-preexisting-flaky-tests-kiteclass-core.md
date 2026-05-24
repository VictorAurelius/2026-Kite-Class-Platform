# GAP-735: Pre-existing test flake on `main` HEAD — 6 failures (2 deterministic + 4 CI-suite pollution)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend (test infrastructure)
**Detected:** 2026-05-24 (Wave beta-readiness-2 Bucket A + B CI surface — same 6 failures across both PRs + on local main HEAD)
**Affects:** `kiteclass-core` test suite `Test Core Service` job — blocks every code PR touching `kiteclass-core` từ admin merge override

## Problem

Wave beta-readiness-2 Bucket A (PR #1769) + Bucket B (PR #1768) both surface SAME 6 test failures trong CI `Test Core Service` job:

| Test class | Failures | Local repro (main HEAD) | CI repro |
|---|---|---|---|
| `EnrollmentIntegrationTest` | 1 | ✅ FAIL (deterministic) | ✅ FAIL |
| `InvoiceFlowIntegrationTest` | 1 | ✅ FAIL (deterministic) | ✅ FAIL |
| `CourseSecurityTest` | 4 | ❌ PASS 15/15 isolated | ✅ FAIL (only in full suite — test pollution) |

Evidence:
- Local main HEAD `./mvnw test -Dtest=EnrollmentIntegrationTest,CourseSecurityTest,InvoiceFlowIntegrationTest` → 32/34 PASS (2 deterministic failures)
- Local main HEAD `./mvnw test -Dtest=CourseSecurityTest` (isolated) → 15/15 PASS
- Bucket A Agent A (Opus) confirmed via `git stash` + rerun on pristine main → identical 2 failures
- 3 separate CI runs trên cùng commits → same 6 failures (not order-dependent flake — deterministic in suite)

## Root cause hypothesis

1. **EnrollmentIntegrationTest fail (1):** `InvalidDataAccessApiUsageException: Cannot update entity from different tenant. Entity belongs to tenant: A, Current tenant: B` — tenant context bleed across test methods
2. **InvoiceFlowIntegrationTest fail (1):** Multi-tenant isolation test fixture brittle — confirmed by Agent A `git stash` test
3. **CourseSecurityTest 4 fails:** Only fail trong full suite, PASS isolated → suite test pollution (shared Testcontainer DB state carry-over)

All 3 patterns share: insufficient test data isolation between IT classes that share Testcontainer DB instance.

## Impact

- Blocks every `kiteclass-core` PR từ Test Core Service gate
- Forces `AUDIT_OVERRIDE` trailer + admin merge → degrades pre-merge quality signal
- Wave beta-readiness-2 Bucket A + B both required override (admin merge with reference to this gap)

## Proposed Fix (P1 dedicated wave candidate)

### Option A — Per-class @Transactional rollback (preferred)
Add `@Transactional` + `@Rollback(true)` to each IT class → Spring rolls back DB state after each test → no cross-test pollution.

### Option B — Per-class @DirtiesContext
Force Spring context reload per test class → expensive (~20-30s per class) but guaranteed isolation.

### Option C — Explicit test data cleanup (least preferred)
Add `@AfterEach` cleanup methods — labor-intensive, error-prone.

## Acceptance Criteria

- [ ] `EnrollmentIntegrationTest` → 14/14 PASS on main HEAD locally + CI
- [ ] `InvoiceFlowIntegrationTest` → 5/5 PASS on main HEAD locally + CI
- [ ] `CourseSecurityTest` → 15/15 PASS in full suite CI (not just isolated)
- [ ] `./mvnw verify -P strict-warnings` PASS clean on main HEAD
- [ ] Remove AUDIT_OVERRIDE trailers from future code PRs touching kiteclass-core

## Out-of-scope

- Migrate ALL IT classes to per-class isolation pattern — track Wave 110+ standalone hygiene wave nếu Option A adoption proves cleaner
- Testcontainer optimization (sharing DB across modules) — separate concern

## Priority Rationale (P1)

Test gate brittleness blocks every code PR + dirties merge process với override trailers. Not P0 because production is unaffected (test infrastructure only); not P2 because it compounds across every wave touching kiteclass-core.

## Related

- Wave beta-readiness-2 Bucket A PR #1769 (admin merge with AUDIT_OVERRIDE)
- Wave beta-readiness-2 Bucket B PR #1768 (admin merge with AUDIT_OVERRIDE)
- Agent A Opus state-check report — confirmed 2 deterministic failures pre-existing
- `release-fix-retry-budget.md` §4 pivot matrix — STOP fixing pre-existing flake in unrelated wave
- `admin-merge-discipline.md` — override discipline for genuine pre-existing CI failures

## Log

- **2026-05-24 (filed):** Wave beta-readiness-2 CI surfacing — same 6 failures persist on both Bucket A + B reruns + on main HEAD locally. Confirmed pre-existing flake. File P1 follow-up; admin merge Bucket A + B with AUDIT_OVERRIDE + GAP-735 reference.
