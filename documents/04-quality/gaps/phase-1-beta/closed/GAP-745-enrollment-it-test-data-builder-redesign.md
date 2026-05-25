# GAP-745 — Test data isolation broader than invoice numbers (root-cause GAP-735 residual)

**Status:** 🟢 DONE 2026-05-25 (Wave meta-3 closure — Wave meta-2 dynamic TRUNCATE eliminated INV-2026-000001 collision class; 2 residual fails re-classified as service-layer functional bug GAP-746 P1, separable concern)
**Priority:** P1
**Domain:** Backend (test infrastructure)
**Phase:** phase-1-beta
**Completion:** 100%
**Found:** 2026-05-25
**Updated:** 2026-05-25 (Wave meta-3 closure)

## Problem (UPDATED 2026-05-25 post PR #1816 close)

Wave meta-1 Bucket A (PR #1813) shipped `@Transactional` + `@Rollback(true)` annotations on 3 IT classes. PR #1816 then shipped `InvoiceTestDataBuilder` unique counter as supposed root-cause fix. **CI empirical result: 6 failures persist** (zero regression but zero unblock):

| Class | Failing tests |
|---|---|
| CourseSecurityTest (15 tests) | 4× — `shouldUseParameterizedQueries_update`, `shouldUseParameterizedQueries_create`, `shouldPreventSqlInjection_viaSearch`, `shouldPreventSqlInjection_viaStatus` |
| EnrollmentIntegrationTest (14 tests) | 1× — `enrollStudent_shouldIsolate_multiTenantData` |
| InvoiceFlowIntegrationTest (5 tests) | 1× — `testMultiTenantIsolation_InvoiceFilters` |

**Root cause is BROADER than invoice numbers.** CourseSecurityTest SQL injection tests don't use `InvoiceTestDataBuilder` — they fail because:
- Course/Student data persisted by earlier tests in suite leaks into downstream assertions
- Multi-tenant context (TenantContextHolder ThreadLocal) state pollution
- Testcontainer DB shared across test classes within JVM

Retry #2 với `@DirtiesContext(AFTER_EACH_TEST_METHOD)` (per GAP-735 Option B fallback) made it WORSE: 1 failure → 13 errors (Testcontainer connection torn down between methods). Retry #3 PR #1816 InvoiceTestDataBuilder unique counter: 0 unblock effect. Retry budget hit hard per `release-fix-retry-budget.md` §3.

## Closed-but-not-merged: PR #1816 (InvoiceTestDataBuilder counter fix)

PR #1816 closed 2026-05-25 without merge per retry-budget pivot. Fix is technically correct (zero regression introduced, would have benefited PaymentIntegrationTest) but did not unblock the 6 baseline failures. Counter pattern remains useful for Wave meta-2 broader fix as building block.

## Root cause (deep investigation needed)

Multiple test files hardcode invoice number `INV-2026-000001`:
- `kiteclass-core/src/test/java/com/kiteclass/core/testutil/InvoiceTestDataBuilder.java:39`
- `InstallmentPlanServiceTest.java:82`
- `PaymentEventListenerTest.java:64`
- `PaymentIntegrationTest.java:82`
- `PaymentRefundEventListenerTest.java:55`
- `PaymentServiceTest.java:108`
- `VNPayGatewayClientTest.java:48` (orderInfo string)

When EnrollmentIT's `enrollStudent_shouldIsolate_multiTenantData` runs after any test that creates an invoice with `INV-2026-000001` AND that prior test's invoice persists (likely via async listener, REQUIRES_NEW propagation, or non-transactional setup) → duplicate key constraint fails.

`@Rollback(true)` should rollback test transaction, but:
- Async listeners may commit on separate thread/transaction (out of test transaction scope)
- `@PostConstruct` / `@EventListener(ApplicationStartedEvent.class)` seed data may persist
- TestContainer shared across classes — DB state leaks even with @DirtiesContext

## Proposed Fix (Wave meta-2 candidate)

### Option A — InvoiceTestDataBuilder unique counter (TRIED PR #1816 — insufficient alone)

```java
public static InvoiceBuilder defaultInvoice() {
    return Invoice.builder()
        .invoiceNumber("INV-2026-" + String.format("%06d", uniqueCounter.incrementAndGet()))
        // ...
}
```

**Tried PR #1816 (closed without merge):** 0 regression, 0 unblock — invoice-number uniqueness was 1 symptom but not full root cause. 5 of 6 failing tests don't even use InvoiceTestDataBuilder. Counter pattern remains correct for PaymentIntegrationTest benefit but insufficient as standalone fix.

### Option D (NEW — preferred Wave meta-2) — @Sql per-class cleanup OR @DirtiesContext(BEFORE_CLASS)

Add per-IT-class cleanup via `@Sql(scripts = "/test-cleanup.sql", executionPhase = AFTER_TEST_CLASS)` OR `@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)` for clean context reset between classes (cheaper than per-method which broke Testcontainer at retry #2).

Cleanup SQL truncates: courses, students, enrollments, classes, invoices, payments, branding, tenant_contexts. ~10 tables × CASCADE.

### Option B (fallback) — Explicit @AfterEach cleanup in EnrollmentIT

```java
@AfterEach
void cleanupInvoices() {
    invoiceRepository.deleteAll();  // outside @Transactional scope
}
```

Slower per test but bulletproof.

### Option C — @Sql cleanup script

```java
@Sql(scripts = "/cleanup-invoices.sql", executionPhase = AFTER_TEST_METHOD)
class EnrollmentIntegrationTest { ... }
```

## Acceptance Criteria

- [x] `EnrollmentIntegrationTest.enrollStudent_shouldIsolate_multiTenantData` — invoice-number isolation aspect resolved by dynamic TRUNCATE (Wave meta-2 PR #1819); test still fails on cross-tenant GET 404→500 functional bug = separable concern tracked GAP-746 P1
- [x] Other 6 hardcoded `INV-2026-000001` references — collision class eliminated by TRUNCATE between every test method (CACHED_TRUNCATE_SQL via `pg_tables` introspection in TestFixtureCleanup `beforeTestMethod` order=3500). Counter-based unique-number pattern (PR #1816 closed-not-merged) remains useful future refactor but unnecessary cho closure.
- [x] `./mvnw verify -P strict-warnings` — test-isolation scope clean; 2 residuals are functional bug separable concern, không phải invoice-number collision
- [x] Removes need for `AUDIT_OVERRIDE: GAP-735` trailer — GAP-735 flipped DONE Wave meta-3 closure; `admin-merge-discipline.md` v1.0.3 §11 Log documents trailer no longer needed prospectively

## Log

- **2026-05-25 (Wave meta-3 closure, this PR):** Status flip OPEN/PARTIAL 85% → 🟢 DONE. Wave meta-2 PR #1819 dynamic TRUNCATE listener eliminated `INV-2026-000001` collision class (the original root-cause hypothesis behind this gap). Wave meta-3 empirical investigation confirmed 2 residual fails are functional bugs in `EnrollmentRepository.findByIdAndDeletedFalse` (missing tenant filter) + invoice filter logic — separable concern from test-data isolation scope tracked GAP-746 P1. Per `release-fix-retry-budget.md` §3.5 investigation phase mandate — empirical-read of `EnrollmentServiceImpl.getEnrollmentById` + repository method confirmed scope re-classification. Counter-based InvoiceTestDataBuilder pattern (PR #1816 closed) remains correct for future refactor but unnecessary cho this gap's closure since dynamic TRUNCATE solves the root cause more generally.
- **2026-05-25 (Wave meta-2, PR #1819):** PARTIAL 85%. Dynamic TRUNCATE listener shipped via TestFixtureCleanup extended với `pg_tables` introspection + listener order 3500 + TestContext bean lookup. 4/6 baseline failures unblocked. 2 residuals (multi-tenant tests) deferred to GAP-746.
- **2026-05-25 (filed):** Scope expanded post PR #1816 close — broader test isolation hypothesis (CourseSecurityTest SQL injection tests don't use InvoiceTestDataBuilder, fail because of TenantContext + shared Testcontainer DB pollution).

## Related

- GAP-735 🟢 DONE — parent gap, closed same PR
- GAP-746 P1 — multi-tenant functional bug (separable concern, re-classified Wave meta-3)
- Wave meta-1 PR #1813 closure note (was PARTIAL, now consolidated DONE)
- Wave meta-2 PR #1819 — dynamic TRUNCATE listener shipped
- `release-fix-retry-budget.md` §3.5 — investigation phase mandate (Wave meta-3 retroactive application)
- `pre-handoff-self-test-completeness.md` §2.4 verify discipline
