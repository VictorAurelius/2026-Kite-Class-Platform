# GAP-128: InstallmentPlan lookup does full-table scan with nested N+1

**Status:** 🟢 DONE
**Priority:** 🔴 P0
**Domain:** Backend / Performance
**Detected:** 2026-04-19 (performance baseline audit)
**Affects:** `kiteclass-core` `InstallmentPlanServiceImpl.recordInstallmentPayment(Long installmentId, BigDecimal amount)`
**Related Docs:** `documents/04-quality/audits/performance/performance-audit-2026-04-19.md`

## Problem

```java
InstallmentPlan plan = installmentPlanRepository.findAll().stream()
    .filter(p -> p.getInstallments().stream()
        .anyMatch(i -> i.getId().equals(installmentId)))
    .findFirst()
    .orElseThrow(...);
```

To find the plan that owns a single installment, the code loads **every InstallmentPlan in the database**, then for each plan loads its `installments` lazy collection (N+1), then scans each collection linearly. O(plans × installments) per payment.

Worst-case: tenant has 1000 students × 12-month installment plans = 12000 plans, 144000 installments loaded per single payment call.

## Context

Wave 2 / invoice module introduced InstallmentPlan + Installment entities. The payment webhook path calls `recordInstallmentPayment` every time an installment is paid → every webhook triggers the scan.

## Evidence

- `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/invoice/service/InstallmentPlanServiceImpl.java:147`
- Performance audit §1

## Proposed Fix

Use the indexed reverse lookup:

```java
Installment installment = installmentRepository.findById(installmentId)
    .orElseThrow(() -> new EntityNotFoundException("INSTALLMENT_NOT_FOUND", installmentId));
InstallmentPlan plan = installment.getPlan();
```

Ensure `Installment` entity has `@ManyToOne InstallmentPlan plan` (already present per the scan pattern), and that `plan_id` has an index (verify in migration V12).

Alternative (if circular navigation undesired): add `@Query("select p from InstallmentPlan p join p.installments i where i.id = :installmentId")` on the repository with `@EntityGraph(attributePaths = "installments")`.

## Acceptance Criteria

- [ ] `recordInstallmentPayment(...)` executes ≤2 SQL queries (select installment + select plan, or one JOIN)
- [ ] Unit test asserts SQL query count via `datasource-proxy` or `Statistics`
- [ ] Verify `installments.plan_id` has an index (check V12 migration, add if missing)

## Related

- Audit: performance-audit-2026-04-19.md §1
- Migration: `V12__create_invoice_extended_tables.sql`

## Log

- 2026-04-19 — Gap created from performance baseline audit
- 2026-04-20 — Fixed in feature/partb-perf-batch: new `InstallmentRepository` + `recordInstallmentPayment` now does `installmentRepository.findById()` → `installment.getPlan()` (PK lookup, ~2 queries vs. O(plans × installments) before). Regression guard test asserts `installmentPlanRepository.findAll()` is NEVER invoked. `installments.plan_id` index `idx_installments_plan` already present in V12 — no new migration needed.
