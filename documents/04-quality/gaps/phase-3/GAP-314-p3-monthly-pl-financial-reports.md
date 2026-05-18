# GAP-314: Monthly P&L per Branch + Teacher-Level Financial Breakdown

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend (kiteclass-core financial reports module new) + Frontend
**Found:** 2026-05-04 (Persona Review Round 1 — P3 Bucket C)
**Affects:** 2 ACs — tenant + admin (giám đốc + kế toán)

---

## Problem

Giám đốc cần monthly P&L per branch:
- Revenue (tuition + extras)
- Costs broken down (payroll + rent + utilities + marketing + ops)
- Profit
- Drill-down per teacher: doanh thu generated vs commission paid

Output: PDF/Excel matching MISA/SAP format if center imports.

## Root Cause

`module/reportcard` chỉ student academic reports. Không có module financial reports.

## Current State (verified 2026-05-04)

| Component | Path | State |
|-----------|------|-------|
| FinancialReport entity | — | ❌ missing |
| Cost categories config (rent/utilities/marketing) | — | ❌ missing |
| P&L computation service | — | ❌ missing |
| Frontend P&L view | — | ❌ missing |
| Export PDF/Excel matched MISA format | — | ❌ missing |

## Proposed Fix

1. `CostCategory` entity + per-tenant cost entries
2. `PLComputationService.computePL(tenantId, monthYear, branch)` aggregates revenue (from invoices) + costs (from payroll + cost entries) → P&L
3. Frontend report view with charts + drill-down per teacher
4. Export PDF/Excel matched MISA/SAP-compatible format

## Acceptance Criteria

- [ ] CostCategory CRUD (rent / utilities / marketing / ops / other)
- [ ] P&L computed for sample month with 250 invoices + 12 teacher payroll + 5 cost categories
- [ ] Drill-down per teacher: revenue generated, commission paid, contribution margin
- [ ] Export Excel matched MISA template
- [ ] RBAC: only kế toán + giám đốc + owner can access

## Linked ACs

| AC ID | Persona | Doc |
|-------|---------|-----|
| AC-FIN-005 | Tenant Director | `P3-medium-center.md` |
| AC-FIN-001 | Admin (kế toán) | `secondary/admin-in-P3.md` |

## Related

- Depends on: GAP-306 (commission/payroll for cost component), GAP-185 (invoicing for revenue)
- Persona review: §2 (Tenant AC-FIN-005), §4 (Admin AC-FIN-001)

## Log

- **2026-05-04** Created from Persona Review Round 1 P3 Bucket C.
