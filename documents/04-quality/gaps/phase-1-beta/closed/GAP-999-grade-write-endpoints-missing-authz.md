# GAP-999: grade write/calculate/read endpoints thiếu @PreAuthorize (OWASP A01)

**Status:** 🟢 DONE (Wave flow-kc6 G1 walk PASS, 2026-06-05)
**Priority:** 🟠 P1
**Domain:** Backend (security — KC-6)
**Found:** 2026-06-05 (Wave flow-kc6 pre-walk, HIGH #3)
**Affects:** `GradeController` — calculate/finalize/unfinalize/components/getGradeById/getGradesByStudent/transcript

## Problem

Chỉ `initialize`/`getStudentGrade`/`getGradesByClass`/`statistics` có `@PreAuthorize @authz.hasAccessToClass`. **KHÔNG có authz**: `getGradeById`, `getGradesByStudent`, `addOrUpdateComponent`, `updateComponent`, `calculateFinalScore`, `finalizeGrade`, `unfinalizeGrade`, `generateTranscript`, transcript GET. → Bất kỳ authenticated tenant user (kể cả không phải teacher lớp) sửa/tính/finalize/**unfinalize** điểm bất kỳ. OWASP A01. `unfinalize` đặc biệt nguy hiểm (mở khoá grade đã chốt → sửa). Cross-flow sweep miss của GAP-729/991 (giống single-mark KC-5).

## Proposed Fix

Thêm helper `hasAccessToGrade(gradeId)` + `hasAccessToGradeComponent(componentId)` vào `AuthorizationBean` (resolve grade→class_id / component→grade→class_id → `hasAccessToClass`). Thêm `@PreAuthorize` cho các endpoint trên (grade-id path → `hasAccessToGrade(#id)`; component → `hasAccessToGradeComponent(#id)`; student/transcript → `hasAccessToStudent(#studentId)`).

## Acceptance Criteria
- [x] calculate/unfinalize by non-teacher/no-user → 403 (W6 calculate no-user → 403; W6b unfinalize wrong-user → 403)
- [x] by class teacher → 200 (W1-W5 happy path all OK)
- [x] tests green (76 run 0 fail; IT method-security OFF → no-op, authz verified via live walk)

## Related
- Cross-flow sweep GAP-729 (bulk) + GAP-991 (KC-5 single-mark); `cross-flow-bug-class-sweep.md`
- Discovered in: Wave flow-kc6 pre-walk 2026-06-05 (FM #3)

## Log

- **2026-06-05 (Wave flow-kc6 — DONE):** hasAccessToGrade/hasAccessToGradeComponent helpers + @PreAuthorize on 11 endpoints. G1 walk: calculate no-user → 403, unfinalize wrong-user → 403, teacher happy → 200. 76 tests green.
