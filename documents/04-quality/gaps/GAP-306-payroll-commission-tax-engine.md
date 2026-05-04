# GAP-306: Payroll + Commission + BHXH/BHYT/TNCN Tax Engine + Bank File MT940

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 (business-logic tier — blocks P3/P5 GA viability; teacher compensation core)
**Domain:** Backend / Financial / Compliance
**Found:** 2026-05-04 (Wave 17 Bucket C P3 persona review — round 1)
**Affects:** P3 Medium Center (12 teachers), P5 K-12 School (45 teachers), P2 Small Center (2 teachers, simpler scope)

## Problem

P3 Medium Center cần payroll engine compute monthly per-teacher: base salary + commission per class × varied % + (-) BHXH 8% + (-) BHYT 1.5% + (-) BHTN 1% + (-) thuế TNCN bậc thang per Luật Thuế TNCN 2007/2012, output payslip PDF + bank transfer file MT940.

State-check 2026-05-04 confirms ZERO implementation:
- `find kiteclass kitehub -type d -iname "*payroll*"` → 0 results
- `find kiteclass kitehub -type d -iname "*commission*"` → 0 results
- `grep -rln "BHXH\|TNCN\|MT940" kiteclass/kiteclass-core/src/main/java` → 0 functional results

Affects ACs (P3 review): AC-FIN-003, AC-FIN-004, AC-FIN-005 (tenant), AC-FIN-001, AC-FIN-003 (admin), AC-FIN-001, AC-FIN-002, AC-FIN-003 (teacher), AC-EDGE-002 (commission dispute), AC-EXIT-001/002 (settlement) → ~12 ACs blocked.

Existing GAP-057 claims OPEN but state-check shows nothing implemented; this gap supersedes / re-scopes GAP-057 with explicit phasing.

## Root Cause

Wave 1-16 prioritized core enrollment + invoice + branding. Payroll explicitly deferred per existing GAP-057. This gap re-frames with 4 phases mapped to current state-check evidence.

## Proposed Fix

4-phase delivery:

**Phase 1 — Commission engine** (Wave 18)
- `kiteclass-core/module/payroll/` module foundation
- `TeacherCommissionRule` entity (per-teacher × per-class % varied)
- `CommissionCalculator` service computing earnings real-time
- API: `GET /api/v1/payroll/teachers/{id}/earnings` (real-time per teacher)
- Outbox events for tuition-paid → commission-accrued

**Phase 2 — Tax computation** (Wave 19)
- `BhxhBhytBhtnCalculator` (BHXH 8% / BHYT 1.5% / BHTN 1% with mức đóng caps)
- `TncnProgressiveCalculator` (bậc thang per Luật Thuế TNCN 2007/2012)
- Tax rate config preset for VN 2026 (admin override allowed)
- Mẫu 02/KK-TNCN annual export (Tổng cục Thuế format)

**Phase 3 — Payslip + bank file** (Wave 19-20)
- Payslip PDF generator (HSM signature optional)
- MT940 bank file generator (Vietcombank / BIDV format)
- Báo cáo C12-TS BHXH XML for cổng giao dịch BHXH

**Phase 4 — Dispute + offboard** (Wave 20)
- Commission dispute workflow + 30-day SLA + audit log
- Teacher offboard wizard (handover lớp + final commission pro-rata)
- Mid-semester contract change wizard

## Acceptance Criteria

- [ ] Phase 1: `kiteclass-core/module/payroll/` module exists with `TeacherCommissionRule` entity + `CommissionCalculator` service + REST endpoint
- [ ] Phase 1: Real-time commission earnings dashboard endpoint returns per-class breakdown
- [ ] Phase 2: `BhxhBhytBhtnCalculator` + `TncnProgressiveCalculator` services with VN 2026 preset config
- [ ] Phase 2: Mẫu 02/KK-TNCN PDF + XML export endpoint for annual statement
- [ ] Phase 3: Monthly payroll batch endpoint generates 12 payslips + MT940 bank file in ≤2 minutes
- [ ] Phase 3: Báo cáo C12-TS BHXH XML matches cổng giao dịch BHXH schema
- [ ] Phase 4: Commission dispute workflow with 30-day SLA enforced + audit log
- [ ] Phase 4: Teacher offboard wizard preserves historical records 5+ years (Tax law)
- [ ] Each phase: `documents/01-business/kiteclass/payroll/{rules,use-cases,api-contract}.md` ships with code per Living Docs rule

## Related

- Audit report: `documents/00-brd/persona-reviews/P3-medium-center-round-1-2026-05-04.md` §Critical Findings #1
- Existing gap (re-scope): GAP-057 (payroll-teacher-commission)
- Wave plan: `documents/03-planning/waves/wave-2026-05-04-persona-review-round-1.md`
- Compliance: Luật Thuế TNCN 2007/2012, Luật Bảo hiểm Xã hội 2014, Luật Quản lý Thuế 2019, NĐ 123/2020/NĐ-CP
- Persona AC: P3-medium-center.md AC-FIN-003..005, admin-in-P3.md AC-FIN-001..003, teacher-employee-in-P3.md AC-FIN-001..003
