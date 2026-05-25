# GAP-746 — Multi-tenant isolation service-layer functional bugs (re-classified Wave meta-3, was test infra hypothesis)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (re-classified Wave meta-3 from P2 test-infra — empirical investigation per `release-fix-retry-budget.md` §3.5 revealed real root cause = service-layer multi-tenant isolation bug, not test infrastructure)
**Domain:** Backend (service layer — multi-tenant isolation)
**Phase:** phase-1-beta
**Completion:** 0%
**Found:** 2026-05-25 (initial test-infra hypothesis)
**Updated:** 2026-05-25 (Wave meta-3 — scope revised to service-layer functional bug)

## Problem

2 integration tests fail with `multiTenantIsolation` semantic — empirical investigation Wave meta-3 (per `release-fix-retry-budget.md` §3.5 investigation phase mandate) revealed the failure mode is **service-layer functional bug in cross-tenant lookup**, NOT test infrastructure. Wave meta-2 PR #1819 dynamic TRUNCATE listener fully resolved the test-isolation class (4/6 baseline unblock); 2 residuals are separable functional-bug class.

| Test | Failure mode | Root cause (Wave meta-3 empirical finding) |
|---|---|---|
| `EnrollmentIT.enrollStudent_shouldIsolate_multiTenantData:479` | `Status expected:<404> but was:<500>` on cross-tenant GET | `EnrollmentRepository.findByIdAndDeletedFalse(Long id)` ([repo line 34](../../../../kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/enrollment/repository/EnrollmentRepository.java#L34)) does NOT filter by tenant. `EnrollmentServiceImpl.getEnrollmentById` ([service line 113-121](../../../../kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/enrollment/service/EnrollmentServiceImpl.java#L113)) calls it directly. Cross-tenant GET returns the entity → post-load `EntityPersistenceListener` (or Hibernate `@Filter`) likely throws `IllegalStateException` / `AccessDeniedException` instead of `EntityNotFoundException` → 500 instead of 404. |
| `InvoiceFlowIT.testMultiTenantIsolation_InvoiceFilters` | `No matching value at JSON path "$.data.content[*].studentId"` on own-tenant GET | Tenant1's own GET unpaid invoices returns empty `data.content[]` when at least 1 should exist. Likely: invoice creation via `@Async` listener commits outside test transaction window, OR invoice tenant filter logic in `InvoiceService.getUnpaidInvoices` excludes own-tenant data. Needs targeted read of `InvoiceServiceImpl` + invoice event listener. |

## Why P1 (was P2 originally)

Multi-tenant isolation = **Phase 1 BETA quality gate**. A repository method that doesn't enforce tenant scope means:
- Cross-tenant data leak risk (tenant B can theoretically access tenant A's enrollment by ID enumeration)
- Wrong HTTP status codes (500 vs 404) confuse clients + leak existence information
- Test catches a real production-relevant gap, not just test isolation

P2 was initial hypothesis (test-infra residual). Empirical investigation re-classified P1.

## Proposed Fix (dedicated future wave — not Wave meta-3 scope)

### Path A — Repository tenant filter

Add `@Where(clause = "tenant_id = current_tenant()")` Hibernate filter OR change repository method:

```java
// Before
Optional<Enrollment> findByIdAndDeletedFalse(Long id);

// After (option 1: explicit tenant param)
Optional<Enrollment> findByIdAndTenantIdAndDeletedFalse(Long id, UUID tenantId);

// Option 2: Hibernate @Filter enabled per-session via TenantContext interceptor
@Filter(name = "tenantFilter", condition = "tenant_id = :currentTenant")
```

Update `EnrollmentServiceImpl.getEnrollmentById` to pass `TenantContext.getCurrentTenant()` (option 1) OR ensure Hibernate session filter enabled (option 2).

### Path B — Exception mapper

Even if repository returns cross-tenant entity, fix the post-load validator to throw `EntityNotFoundException` (mapped to 404) instead of `IllegalStateException` (mapped to 500). Defense-in-depth alongside Path A.

### Path C — InvoiceFlow investigation

Read `InvoiceServiceImpl.getUnpaidInvoices` + invoice creation event listener. Verify:
- Invoice creation committed before GET (transaction propagation)
- Tenant filter logic in unpaid-invoice query

## Acceptance Criteria

- [ ] `EnrollmentRepository` enforces tenant filter (Path A) — cross-tenant `findById` returns empty Optional → service throws `EntityNotFoundException` → controller returns 404
- [ ] `EnrollmentIT.enrollStudent_shouldIsolate_multiTenantData` PASS
- [ ] `InvoiceFlowIT.testMultiTenantIsolation_InvoiceFilters` PASS (after Path C investigation)
- [ ] No regression in other ~1480 tests
- [ ] `./mvnw verify -P strict-warnings` clean
- [ ] Audit sweep: grep other `findByIdAndDeletedFalse` patterns trong kiteclass-core for same cross-tenant leak class (Course, Student, Class, Invoice, Payment) — file follow-up gap nếu found

## Out-of-scope

- Broader Hibernate `@Filter` session-wide adoption (separate refactor wave nếu Path A option 2 chosen)
- Test infrastructure changes — Wave meta-2 TestFixtureCleanup already correct

## Related

- GAP-735 🟢 DONE — test-isolation parent, closed Wave meta-3
- GAP-745 🟢 DONE — invoice number isolation parent, closed Wave meta-3
- Wave meta-2 PR #1819 — dynamic TRUNCATE listener (resolved test-infra class, surfaced this functional bug)
- Wave meta-3 closure PR (this Wave) — re-classified scope; empirical investigation per `release-fix-retry-budget.md` §3.5
- `release-fix-retry-budget.md` §3.5 — investigation phase mandate (applied retroactively to refine this gap's scope)
- `audit-to-gap-pipeline.md` §2.8 — fix-time state-check (applied at Wave meta-3 pickup)
- `pre-handoff-self-test-completeness.md` §2.4 — verify discipline for cross-tenant flows

## Log

- **2026-05-25 (Wave meta-3 closure, this PR):** Re-classified from P2 test-infra hypothesis → P1 service-layer functional bug. Empirical investigation per `release-fix-retry-budget.md` §3.5: read `EnrollmentServiceImpl.getEnrollmentById:113-121` + `EnrollmentRepository.findByIdAndDeletedFalse:34` + EnrollmentIT test body line 430-483 + InvoiceFlowIT test body line 485-526. Confirmed test-isolation hypothesis (FK cascade / @BeforeAll / TenantContext propagation timing) WRONG; root cause = repository missing tenant filter + likely wrong exception mapping in post-load validator. Scope title revised. Path A/B/C proposed. Defer dedicated future wave (not Wave meta-3 scope since meta-3 is closure wave; functional bug fix needs targeted impl + audit sweep).
- **2026-05-25 (filed Wave meta-2):** Initial P2 hypothesis "test infrastructure residual fail post-truncate" — assumed FK cascade / @BeforeAll / TenantContext timing issue. Re-classified Wave meta-3 after empirical investigation.
