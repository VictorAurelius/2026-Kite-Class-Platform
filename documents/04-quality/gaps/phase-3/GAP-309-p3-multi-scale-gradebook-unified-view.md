# GAP-309: Multi-Scale Gradebook with Auto-Convert + Unified Multi-Teacher View

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend (kiteclass-core grade module) + Frontend
**Found:** 2026-05-04 (Persona Review Round 1 — P3 Bucket C)
**Affects:** 3 ACs across student + teacher + tenant personas

---

## Problem

P3 Trung tâm Anh ngữ thường có lớp Beg/Inter dùng scale VN 1-10, lớp Adv theo CEFR / international curriculum dùng A-F. Teacher cần:

1. Chọn scale per assessment (1-10 hoặc A-F)
2. Auto-convert sang 10-scale cho parent report card view (parent quen thuộc với 10-scale VN)
3. Không phải duplicate work cho 2 scales

Student cần **multi-teacher unified gradebook** view across 3-5 môn (không navigate per teacher để xem điểm).

## Root Cause

`module/grade` exists nhưng:
- Không có scale enum + conversion service
- Không có "all subjects" aggregated view cho student
- Weighted avg computation chưa verify
- Teacher attribution metadata chưa kiểm tra

## Current State (verified 2026-05-04)

| Component | Path | State |
|-----------|------|-------|
| Grade entity | `kiteclass-core/.../module/grade/` | ✅ exists |
| Multi-scale support (10/100/A-F enum) | — | ❌ missing |
| ScaleConversionService | — | ❌ missing |
| Student unified multi-teacher view FE | — | ❌ missing |
| Weighted avg computation | — | ⚠️ unknown — needs verification |
| Teacher attribution per grade entry | — | ⚠️ unknown |
| RBAC scope teacher to own classes only | — | ⚠️ unknown — depends GAP-058/308 |

## Proposed Fix

1. Add `GradeScale` enum (TEN_POINT, HUNDRED_POINT, A_F_LETTER) on Grade entity
2. `ScaleConversionService` with conversion table
3. Weighted average computation per assessment type
4. Frontend student view: "All subjects" aggregated; per-subject drill-down
5. Frontend teacher view: scoped to own classes (RBAC enforcement via GAP-058/308)

## Acceptance Criteria

- [ ] Grade entity supports 3 scales with conversion table documented
- [ ] Teacher creates assessment with scale choice — works for both 1-10 and A-F
- [ ] Student parent report shows 10-scale auto-converted (configurable per tenant)
- [ ] Student "All subjects" view loads ≤2s with weighted avg per subject
- [ ] Teacher access to grade module of OTHER classes returns 403 (RBAC scope)
- [ ] Each grade entry stores teacher_id for attribution; visible in student view
- [ ] Unit test: 5 conversion scenarios (1-10 ↔ 100, 1-10 ↔ A-F)

## Linked ACs

| AC ID | Persona | Doc |
|-------|---------|-----|
| AC-OPS-003 | Tenant Director | `P3-medium-center.md` |
| AC-OPS-003 | Student | `secondary/student-in-P3.md` |
| AC-OPS-003 | Teacher Employee | `secondary/teacher-employee-in-P3.md` |

## Related

- Persona review: §3 (Student §3.2), §5 (Teacher §5.2)
- Depends on: GAP-058 + GAP-308 for teacher scope enforcement

## Log

- **2026-05-04** Created from Persona Review Round 1 P3 Bucket C.
