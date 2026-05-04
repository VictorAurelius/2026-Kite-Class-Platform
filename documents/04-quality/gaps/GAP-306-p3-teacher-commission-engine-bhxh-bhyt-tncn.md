# GAP-306: Teacher Commission Engine + Payroll BHXH/BHYT/BHTN/TNCN

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 — blocks GA cho P3 (Medium Center) + P5 (K-12 School)
**Domain:** Backend (kiteclass-core) + Frontend (kế toán + teacher dashboards)
**Found:** 2026-05-04 (Persona Review Round 1 — P3 Bucket C)
**Affects:** 11 ACs across 4 personas — see "Linked ACs" below
**Tier impact:** PREMIUM (P3 default tier — kế toán bắt buộc fall back to Excel mà không có engine này)

---

## Problem

P3 archetype (Trung tâm 250 HS, 12 GV, 30 lớp) có 12 giáo viên với commission % varied per teacher × per class (50% Beg / 60% Inter / 70% Adv là phổ biến). Kế toán mỗi tháng phải:

1. Compute commission per teacher × per class × tuition collected
2. Apply BHXH 8% + BHYT 1.5% + BHTN 1% + thuế TNCN bậc thang (per Luật Thuế TNCN 2007/2012)
3. Generate payslip PDF với từng deduction breakdown rõ ràng
4. Generate bank transfer file format MT940 (Vietcombank/BIDV)
5. Generate báo cáo C12-TS BHXH + Mẫu 02/KK-TNCN cho TNCN
6. Allow teacher xem real-time earnings dashboard (transparency yêu cầu)
7. Handle commission dispute workflow + audit log + 30-day SLA
8. Pro-rata commission khi teacher nghỉ ốm hoặc resignation mid-month

**Without this engine, kế toán phải dùng Excel + MISA — center churns sau 3 tháng** (real-world feedback baseline 2026-04-26).

## Root Cause

KHÔNG có module commission/payroll trong codebase:
- `grep -ri "commission|payroll|TNCN|BHXH|BHYT" kiteclass/kiteclass-core/src/main/java` → chỉ match `Permission.java` (false positive trên word "permission")
- `find kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module -maxdepth 1 -type d` → no `payroll`, no `commission`, no `taxstatement`
- `kitehub-subscription/src/main/java/com/kitehub/subscription/scheduler/` chỉ handle SaaS billing cho tenant, KHÔNG phải teacher payroll

## Current State (verified 2026-05-04)

| Component | Path | State |
|-----------|------|-------|
| Teacher entity | `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/teacher/` | ✅ exists (no commission/contract fields) |
| Commission entity | — | ❌ missing |
| Payroll entity | — | ❌ missing |
| BHXH/BHYT/BHTN/TNCN computation service | — | ❌ missing |
| Payslip generator | — | ❌ missing |
| Bank file MT940 export | — | ❌ missing |
| Báo cáo C12-TS / Mẫu 02 export | — | ❌ missing |
| Teacher earnings dashboard FE | — | ❌ missing (no `(dashboard)/teacher/earnings` route) |
| Commission dispute workflow | — | ❌ missing |
| Tax presets VN 2026 (BHXH 8% / BHYT 1.5% / BHTN 1% / TNCN bậc thang) | — | ❌ missing |

## Proposed Fix

**Phase 1 — Domain model + computation (Wave 18-A):**
1. Entities: `TeacherContract` (full-time/part-time + base salary), `CommissionRule` (per teacher × per class × %), `Payroll` (monthly aggregate), `PayslipLine` (per deduction), `TaxConfig` (BHXH/BHYT/BHTN rates + TNCN bậc thang).
2. Service: `CommissionCalculatorService` (real-time per-class earnings), `PayrollRunService` (monthly batch), `TaxComputationService` (deduction calc per Luật Thuế TNCN 2007/2012).
3. Migration: `V70__teacher_commission_payroll.sql` schema.
4. Business rules.md per `business-logic-review.md` §2 5-attribute documentation cho mỗi rate (Source = Luật cụ thể, Compliance = "Compliant", Reviewer = solo-dev acting Tax-scout, Review cadence = Annual + event-driven on regulator change).

**Phase 2 — Generators + reports (Wave 18-B):**
1. Payslip PDF generator (header trung tâm, breakdown per deduction, bank ref).
2. Bank file MT940 export cho Vietcombank/BIDV.
3. Báo cáo C12-TS BHXH + Mẫu 02/KK-TNCN export with XML format chuẩn TCT/BHXH portals.

**Phase 3 — UI (Wave 18-C):**
1. Kế toán: financial dashboard widget "Payroll due this month" + "Run payroll" wizard preview-then-confirm.
2. Teacher: real-time earnings dashboard with per-class drill-down + projected end-of-month.
3. Teacher: payslip history view + Mẫu 02 annual download.
4. Commission dispute workflow with audit log + 30-day SLA timer.

## Acceptance Criteria

- [ ] Commission entity supports per-teacher × per-class × % with effective date ranges (handle mid-semester contract changes)
- [ ] BHXH 8% + BHYT 1.5% + BHTN 1% + thuế TNCN bậc thang (5/10/15/20/25/30/35%) computed correctly cho 5 sample teacher profiles (full-time + part-time + commission-only)
- [ ] Monthly payroll batch cho 12 teachers completes in ≤30s
- [ ] Payslip PDF includes header + 8+ line items breakdown + bank ref + footer disclaimer
- [ ] Bank file MT940 valid format (validated với Vietcombank sample)
- [ ] Báo cáo C12-TS XML pass schema validation cho cổng giaodichdientu.bhxh.gov.vn
- [ ] Mẫu 02/KK-TNCN PDF pre-filled với MST + tổng thu nhập + thuế đã khấu trừ
- [ ] Teacher earnings dashboard real-time (≤5s refresh) cho per-class breakdown
- [ ] Commission dispute workflow with audit log + 30-day SLA + email notification
- [ ] All business rules in `documents/01-business/kiteclass/payroll/rules.md` follow `business-logic-review.md` §2 5-attribute standard
- [ ] Unit tests ≥80% coverage cho `TaxComputationService` (5 sample profiles × full year)
- [ ] Integration test: full payroll run → payslip → bank file → tax report end-to-end

## Linked ACs

| AC ID | Persona | Doc |
|-------|---------|-----|
| AC-FIN-003 | Tenant Director | `P3-medium-center.md` |
| AC-FIN-004 | Tenant Director | `P3-medium-center.md` |
| AC-EXIT-002 | Tenant Director | `P3-medium-center.md` |
| AC-ONBOARD-003 | Admin (kế toán) | `secondary/admin-in-P3.md` |
| AC-OPS-004 | Admin (kế toán) | `secondary/admin-in-P3.md` |
| AC-FIN-003 | Admin (kế toán) | `secondary/admin-in-P3.md` |
| AC-FIN-001 | Teacher Employee | `secondary/teacher-employee-in-P3.md` |
| AC-FIN-002 | Teacher Employee | `secondary/teacher-employee-in-P3.md` |
| AC-FIN-003 | Teacher Employee | `secondary/teacher-employee-in-P3.md` |
| AC-EDGE-002 | Teacher Employee | `secondary/teacher-employee-in-P3.md` (commission dispute) |
| AC-EDGE-003 | Teacher Employee | `secondary/teacher-employee-in-P3.md` (mid-semester contract change) |
| AC-EXIT-001 | Teacher Employee | `secondary/teacher-employee-in-P3.md` |

## Related

- Existing: GAP-057 (payroll commission — this gap is the concrete implementation), GAP-062 (payroll bank integration — included in Phase 2)
- Persona review: [`documents/00-brd/persona-reviews/P3-medium-center-round-1-2026-05-04.md`](../../00-brd/persona-reviews/P3-medium-center-round-1-2026-05-04.md) §Finding 1
- Compliance laws: Luật Thuế TNCN 2007/2012; Luật BHXH 2014; NĐ 123/2020/NĐ-CP (e-invoice); Bộ luật Lao động 2019
- Business doc target: `documents/01-business/kiteclass/payroll/{rules.md,use-cases.md,api-contract.md}` (3-layer per CLAUDE.md)

## Log

- **2026-05-04** Created from Persona Review Round 1 P3 Bucket C — 11 ACs across 4 personas blocked. State-check confirmed no commission/payroll module exists. Filed per `audit-to-gap-pipeline.md` Step 2.5 (Code state = Nothing exists → 🔵 OPEN).
