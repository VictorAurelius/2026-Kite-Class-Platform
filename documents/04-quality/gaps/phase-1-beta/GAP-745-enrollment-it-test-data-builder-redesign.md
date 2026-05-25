# GAP-745 — EnrollmentIT test-data redesign (real root-cause GAP-735 residual)

**Status:** OPEN
**Priority:** P1
**Domain:** Backend (test infrastructure)
**Phase:** phase-1-beta
**Completion:** 0%
**Found:** 2026-05-25
**Updated:** 2026-05-25

## Problem

Wave meta-1 Bucket A (PR #1813) shipped `@Transactional` + `@Rollback(true)` annotations on 3 IT classes to fix GAP-735 cross-test DB pollution. Outcome:

- `CourseSecurityTest` + `InvoiceFlowIntegrationTest` — likely fixed (didn't surface in CI failures)
- `EnrollmentIntegrationTest.enrollStudent_shouldIsolate_multiTenantData` — **STILL FAILS** với `uk_invoices_instance_number` constraint violation on `INV-2026-000001`

Retry #2 với `@DirtiesContext(AFTER_EACH_TEST_METHOD)` (per GAP-735 Option B fallback) made it WORSE: 1 failure → 13 errors (Testcontainer connection torn down between methods). Retry budget hit per `release-fix-retry-budget.md` §3.

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

### Option A — Make `InvoiceTestDataBuilder` generate unique invoice numbers (preferred)

```java
public static InvoiceBuilder defaultInvoice() {
    return Invoice.builder()
        .invoiceNumber("INV-2026-" + String.format("%06d", uniqueCounter.incrementAndGet()))
        // ...
}
```

Replace hardcoded `INV-2026-000001` with sequence-based generator. Update all 7 affected test files to use builder default (already do via `.invoiceNumber("INV-2026-000001")` — remove explicit override).

### Option B — Explicit @AfterEach cleanup in EnrollmentIT

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

- [ ] `EnrollmentIntegrationTest.enrollStudent_shouldIsolate_multiTenantData` 14/14 PASS in full suite CI (3 consecutive runs)
- [ ] Other 6 hardcoded `INV-2026-000001` references update với unique number OR explicit isolation
- [ ] `./mvnw verify -P strict-warnings` clean
- [ ] Removes need for `AUDIT_OVERRIDE: GAP-735` trailer (consolidate GAP-735 closure)

## Related

- GAP-735 PARTIAL — Bucket A residual flake
- Wave meta-1 PR #1813 closure note (PARTIAL)
- `release-fix-retry-budget.md` §3 retry budget cap hit retry #2
- `pre-handoff-self-test-completeness.md` §2.4 verify discipline
