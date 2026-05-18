# GAP-321b.1 — Fees facet instalment-plan join + payment-history projection

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (parent-portal v2 enrichment — Đ.83 K2 fee-detail completeness)
**Domain:** Backend (kiteclass-core parent + invoice + payment)
**Detected:** 2026-05-04 (Wave 18b3 Bucket C scope split)
**Affects:** Pa. Parent (P5 K-12) — fees facet shows totals + balance due but not instalment breakdown / payment history

## Context

Wave 18b3 Bucket C shipped fees facet date-range narrowing + `@EntityGraph(items, adjustments)` + N+1 protection (BR-PARENT-FACET-FEES-002). The facet returns `ParentFeeFacetResponse` with `invoiceNumber`, `status`, `totalAmount`, `balanceDue`, `dueDate` — sufficient for "what do I owe and when". This sub-gap covers the v2 enrichment: instalment plan breakdown (when applicable) + payment history join.

## Problem

Parents on instalment plans (FE drilling into "tháng 4 đóng bao nhiêu, đã đóng bao nhiêu") need the per-instalment status + paid-amount-to-date. Currently the parent portal can show the parent invoice totals but not the instalment-by-instalment view a school cashier or parent would expect for school fees.

## Root Cause

Phase 1B remainder consciously narrowed scope to date-range + items + adjustments only; instalment + payment-history join was bracketed out per Wave 18b3 plan §3 Bucket C ("instalment-plan join + payment-history projection — incremental v2 work"). The relevant entities (`Installment`, `InstallmentPlan`, `Payment`) all exist in `kiteclass-core` and are wired by `InstallmentPlanRepository` + `PaymentRepository`.

## Proposed Fix

### Phase 1 — DTO design
- Decide: extend `ParentFeeFacetResponse` (breaking) OR add a new `ParentFeeDetailResponse` (non-breaking, exposed via `GET /api/v1/parent/children/{id}/fees/{invoiceId}`).
- Decision: non-breaking — new detail endpoint.

### Phase 2 — query
- Add JPQL on `InvoiceRepository` (or a dedicated parent-facet repo): join `Invoice` + `Installment` + `Payment` filtered by `invoiceId` + parent's link guard.
- `@EntityGraph` for `installmentPlan.instalments` + `payments`.
- assertSelectCount ≤4 (1 invoice + 1 instalments + 1 payments + 1 coalesce).

### Phase 3 — endpoint + service
- `GET /api/v1/parent/children/{childId}/fees/{invoiceId}` returns `ParentFeeDetailResponse` with `[{installmentNo, dueDate, amount, status, paidAt, paidAmount}]` array.
- Reuse scope-guard pattern (BR-PARENT-FACET-FEES-001) + audit row pattern (BR-PARENT-AUDIT-001).
- New BR rule BR-PARENT-FACET-FEES-003 documenting the instalment + payment-history scope.

### Phase 4 — FE drill-down
- Add a deeper drill-down route: `/parent/fees/[childId]/[invoiceId]/page.tsx` showing per-instalment table.
- Out-of-scope here — track in FE follow-up gap.

## Acceptance Criteria

- [ ] New `ParentFeeDetailResponse` DTO with instalment + payment-history fields
- [ ] New endpoint `GET /api/v1/parent/children/{childId}/fees/{invoiceId}` with scope guard + audit log
- [ ] JPQL `findByIdWithInstallmentsAndPayments` with `@EntityGraph`
- [ ] BR-PARENT-FACET-FEES-003 authored in parent-portal/rules.md with 5-attribute frontmatter
- [ ] Unit tests + IT for new endpoint (≥4 unit + ≥1 IT)
- [ ] N+1 protection: assertSelectCount ≤4 prepared statements
- [ ] All 4 layers covered per `design-layer-coverage.md` §2.1

## Out of Scope

- FE drill-down route (separate FE follow-up gap)
- Per-instalment write actions (defer to GAP-321c)
- Refund timeline projection

## Estimated Effort

~2-3 days (DTO + query + endpoint + tests + docs)

## Related

- **Parent gap:** GAP-321b (Phase 1B umbrella)
- **Sister sub-gaps:** GAP-321b.1-conduct-incident-visibility, GAP-321b.1-notifications-engine-wiring
- **Wave plan:** `documents/03-planning/waves/wave-2026-05-04-18b3-k12-legal-phase-1b-remainder.md` §3 Bucket C
- **Source code (when filed):** `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/parent/service/impl/ParentFeesFacetServiceImpl.java`, `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/invoice/repository/InvoiceRepository.java`, `documents/01-business/kiteclass/parent-portal/rules.md` §13.4

## Log

- **2026-05-04** Filed by Wave 18b3 Bucket C agent. Bucket C consciously narrowed fees scope to date-range + items + adjustments (BR-PARENT-FACET-FEES-002 only); instalment + payment-history v2 enrichment deferred per `gap-done-discipline.md` §3 — ship narrow value (date-range + N+1 protection) now, file follow-up gap for v2 enrichment.
