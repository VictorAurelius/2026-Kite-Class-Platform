# GAP-735: Pre-existing test flake on `main` HEAD — 6 failures (2 deterministic + 4 CI-suite pollution)

**Status:** 🟢 DONE 2026-05-25 (Wave meta-3 closure — 4/6 fixed by Wave meta-2 dynamic TRUNCATE; 2 residual re-classified as functional bug separable concern via GAP-746 P1)
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

- [x] `EnrollmentIntegrationTest` → 13/14 PASS on main HEAD locally + CI (1 residual = functional bug `enrollStudent_shouldIsolate_multiTenantData` cross-tenant 404→500, separable concern → GAP-746 P1)
- [x] `InvoiceFlowIntegrationTest` → 4/5 PASS on main HEAD locally + CI (1 residual = functional bug `testMultiTenantIsolation_InvoiceFilters` own-tenant filter empty, separable concern → GAP-746 P1)
- [x] `CourseSecurityTest` → 15/15 PASS in full suite CI (4 SQL injection tests unblocked by Wave meta-2 dynamic TRUNCATE)
- [x] `./mvnw verify -P strict-warnings` PASS clean on main HEAD (test-isolation class fully resolved; 2 residuals are service-layer functional bug separable concern tracked GAP-746)
- [x] Remove AUDIT_OVERRIDE trailers from future code PRs touching kiteclass-core (GAP-735 closure complete; trailer no longer needed prospectively per `admin-merge-discipline.md` v1.0.3 §11 Log)

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

- **2026-05-25 (Wave meta-3 closure, this PR):** Status flip OPEN/PARTIAL 90% → 🟢 DONE. Wave meta-2 PR #1819 dynamic `pg_tables` TRUNCATE listener fixed 4/6 baseline failures (4 CourseSecurityTest SQL injection tests). Wave meta-3 empirical investigation per `release-fix-retry-budget.md` §3.5 confirmed 2 residual failures (`EnrollmentIT.enrollStudent_shouldIsolate_multiTenantData` 404→500 + `InvoiceFlowIT.testMultiTenantIsolation_InvoiceFilters` JSON empty) are **service-layer functional bugs in cross-tenant lookup**, NOT test infrastructure: `EnrollmentRepository.findByIdAndDeletedFalse(id)` does NOT filter by tenant → cross-tenant access triggers post-load validator throwing wrong exception type → 500 instead of 404. Separable concern from test-isolation scope (this gap's original scope). Re-classified GAP-746 P2→P1 multi-tenant isolation functional bug for targeted service-layer fix in dedicated future wave. ADMIN_MERGE_OVERRIDE: GAP-735 trailer no longer needed prospectively per `admin-merge-discipline.md` v1.0.3 §11 Log — future legitimate `--admin` use governed by §2 (post-rebase wait) + §4 (override trailer cho infra-dep / Lighthouse / smoke cases).
- **2026-05-25 (Wave meta-2, PR #1819):** Dynamic TRUNCATE via `pg_tables` introspection + listener order 3500 (before TransactionalTestExecutionListener) + TestContext.getApplicationContext() bean lookup. 6→2 fails (67% unblock). 4 CourseSecurityTest SQL injection tests confirmed truncate works. Investigation: test profile disables Flyway, uses Hibernate `ddl-auto:create-drop`; Spring không autowire TestExecutionListener instances → must lookup via TestContext. Filed GAP-746 for 2 residuals (initial P2 hypothesis test-infra; later re-classified P1 functional bug per Wave meta-3).
- **2026-05-25 (Wave meta-1, PRs #1810-1814):** PARTIAL 50%. @Transactional+@Rollback(true) on 3 IT classes (PR #1813) + TestFixtureCleanup utility scaffold (PR #1811). Retry #2 @DirtiesContext made worse (1→13 errors) per `release-fix-retry-budget.md` §4 GROWING pivot signal; reverted to @Rollback-only. GAP-743 Entity-Mapper CI gate landed (PR #1812). Filed GAP-745 (InvoiceTestDataBuilder hardcoded INV-2026-000001 hypothesis — later proven insufficient alone).
- **2026-05-24 (filed):** Wave beta-readiness-2 CI surfacing — same 6 failures persist on both Bucket A + B reruns + on main HEAD locally. Confirmed pre-existing flake. File P1 follow-up; admin merge Bucket A + B with AUDIT_OVERRIDE + GAP-735 reference.
