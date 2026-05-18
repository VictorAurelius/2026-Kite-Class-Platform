# GAP-317: Staff Offboard Wizard (Admin + Teacher) with Handover + Access Revocation

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend (kiteclass-core role + teacher modules) + Frontend (offboard wizard)
**Found:** 2026-05-04 (Persona Review Round 1 — P3 Bucket C)
**Affects:** 3 ACs across teacher + admin + tenant

---

## Problem

When admin staff hoặc teacher resigns, P3 cần wizard:
1. Reassign open work (inquiries, complaints, classes, gradebook)
2. Revoke account access at official last day (≤24h)
3. Final commission settlement (teacher) hoặc final payslip
4. Export Mẫu 02/KK-TNCN cho tax (teacher freelance)
5. Historical records preserved (10-year Tax law for financial; 5-year LĐ for contracts)
6. Audit log marked "former staff" (not deleted — break compliance trail nếu xóa)

Without wizard, manual reassign loses data, latent access risk.

## Root Cause

Không có offboard wizard. Account deletion clears history (compliance break).

## Current State (verified 2026-05-04)

| Component | Path | State |
|-----------|------|-------|
| Offboard wizard backend service | — | ❌ missing |
| Reassignment logic (open inquiries / complaints / classes) | — | ❌ missing |
| Final settlement integration | — | ❌ missing (depends GAP-306) |
| Historical record preservation | — | ⚠️ unknown (verify if soft-delete vs hard-delete) |
| Audit log mark "former staff" | — | ❌ missing |
| Frontend wizard UI | — | ❌ missing |

## Proposed Fix

1. `OffboardingService.offboard(staffId, lastDay)` orchestrates 6-step wizard
2. Reassign open work to specified successor with handover note
3. Schedule access revocation at lastDay 23:59
4. Trigger final settlement (depends GAP-306)
5. Soft-delete account (mark "former staff", preserve history)
6. Frontend wizard with progress indicator + confirmation

## Acceptance Criteria

- [ ] Offboard wizard 6 steps with progress UI
- [ ] Open work auto-listed by category (inquiries / complaints / classes / pending invoices)
- [ ] Successor assignment per category with handover note text field
- [ ] Access revocation scheduled (cron or scheduled task)
- [ ] Final settlement triggered (depends GAP-306)
- [ ] Audit log entry: "Staff X offboarded by Y on Z; reassignments: ..."
- [ ] Historical records remain queryable; account marked `former_staff = true`

## Linked ACs

| AC ID | Persona | Doc |
|-------|---------|-----|
| AC-EDGE-001 | Tenant Director | `P3-medium-center.md` |
| AC-EXIT-001 | Admin | `secondary/admin-in-P3.md` |
| AC-EXIT-001 | Teacher Employee | `secondary/teacher-employee-in-P3.md` |

## Related

- Existing: GAP-058 (role hierarchy + offboard scope), GAP-184 (data retention)
- Depends on: GAP-306 (final settlement)
- Persona review: §2 (Tenant AC-EDGE-001), §4 (Admin AC-EXIT-001), §5 (Teacher AC-EXIT-001)

## Log

- **2026-05-04** Created from Persona Review Round 1 P3 Bucket C.
