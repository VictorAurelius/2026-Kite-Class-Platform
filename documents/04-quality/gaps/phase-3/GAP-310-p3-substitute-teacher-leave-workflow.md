# GAP-310: Substitute Teacher Matcher + Leave Request Workflow

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend (kiteclass-core teacher module) + Frontend (teacher + ops admin views)
**Found:** 2026-05-04 (Persona Review Round 1 — P3 Bucket C)
**Affects:** 4 ACs across teacher + admin + tenant + student personas

---

## Problem

P3 với 12 teachers thường xuyên có sick leave / personal leave. Quy trình hiện tại = gọi điện đồng nghiệp, manual reassign — không scale.

Workflow needed:
1. Teacher submit leave request với date range + lý do (optional medical cert upload)
2. System suggest 3 peer teachers qualified (cùng môn + free slot + same level)
3. Quản lý học vụ approve in ≤30 phút
4. Substitute nhận class assignment + roster + lesson plan access
5. Students/parents nhận notification "Lớp X do cô Y đứng lớp"
6. Original teacher commission pro-rata (depends GAP-306)
7. Substitute commission cho ngày cover (depends GAP-306)

## Root Cause

Không có module substitute / leave request:
- `find -iname "substitute*"` → 0 results
- `find -iname "*leave*"` (in modules) → 0 module-level matches

## Current State (verified 2026-05-04)

| Component | Path | State |
|-----------|------|-------|
| LeaveRequest entity | — | ❌ missing |
| SubstituteAssignment entity | — | ❌ missing |
| SubstituteMatchingService (qualified + free + same level) | — | ❌ missing |
| Approval workflow | — | ❌ missing |
| Frontend teacher "Báo nghỉ" wizard | — | ❌ missing |
| Frontend ops admin approval queue | — | ❌ missing |

## Proposed Fix

1. Entities: `LeaveRequest`, `SubstituteAssignment`, `Qualification` (link teacher × subject × level).
2. Service: `SubstituteMatchingService.findCandidates(originalTeacher, slot)` returns top 3 qualified + free.
3. Workflow: `LeaveRequestService.submit() → notifyManager() → approve() → assignSubstitute() → notifyStakeholders()`.
4. Frontend: teacher "Báo nghỉ" wizard, ops admin approval queue, substitute "My substitute classes" widget.

## Acceptance Criteria

- [ ] Teacher submits leave request with date range + reason + optional medical cert upload
- [ ] System suggests top 3 qualified-free-same-level peers within ≤2s
- [ ] Approval flow: manager email + dashboard alert; SLA 30 phút
- [ ] Substitute receives class details + roster + lesson plan access for cover period only
- [ ] Notifications fire to students/parents (depends GAP-063)
- [ ] Commission pro-rata applied (depends GAP-306)
- [ ] Audit log captures full leave/substitute lifecycle

## Linked ACs

| AC ID | Persona | Doc |
|-------|---------|-----|
| AC-OPS-005 | Tenant Director | `P3-medium-center.md` |
| AC-OPS-005 | Teacher Employee | `secondary/teacher-employee-in-P3.md` |
| AC-EDGE-001 | Teacher Employee | `secondary/teacher-employee-in-P3.md` (sick leave 3-5 ngày coverage) |
| AC-OPS-001 (indirect) | Student | `secondary/student-in-P3.md` (notification when teacher swap) |

## Related

- Depends on: GAP-058 (qualification model), GAP-063 (Zalo notification), GAP-306 (commission pro-rata)
- Persona review: §2 (Tenant AC-OPS-005), §5 (Teacher AC-OPS-005, AC-EDGE-001)

## Log

- **2026-05-04** Created from Persona Review Round 1 P3 Bucket C.
