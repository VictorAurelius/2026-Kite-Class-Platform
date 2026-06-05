# GAP-1000: finalize teacherId self-asserted (body, spoofable) + ADMIN bị chặn finalize (BR-GRD-007)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend (security/business — KC-6)
**Found:** 2026-06-05 (Wave flow-kc6 pre-walk, HIGH #4)
**Affects:** `GradeServiceImpl.finalizeGrade` + `validateTeacherPermission` + `FinalizeGradeRequest`

## Problem

`finalizeGrade` lấy teacherId từ `request.getTeacherId()` (**body, self-asserted**), không qua JWT/SecurityContext. Attacker biết teacherId của MAIN_TEACHER lớp → gửi `{"teacherId":<main_teacher_id>}` → finalize thành công dù không phải người đó (IDOR/privilege spoof). Đồng thời ADMIN không có TeacherClass row → `findByTeacherIdAndClassId` empty → `PermissionDeniedException("TEACHER_NOT_IN_CLASS")` → 403, vi phạm BR-GRD-007 (ADMIN full access). `deleteComponent` cũng dùng `X-Teacher-Id` header self-asserted — không nhất quán.

**Note:** GAP-999 thêm `@PreAuthorize hasAccessToGrade` cho finalize → đã chặn cross-tenant/non-teacher ở layer authz. GAP-1000 còn lại: (a) teacherId từ JWT thay body cho MAIN_TEACHER-specific check + audit "ai finalize"; (b) ADMIN bypass.

## Proposed Fix

Derive teacherId từ `UserContext`/JWT (X-User-Id) thay vì body; ADMIN (isAdmin()) bypass TeacherClass MAIN_TEACHER check. Áp dụng nhất quán cho finalize + deleteComponent.

## Acceptance Criteria
- [ ] finalize teacherId resolved từ JWT (body teacherId ignored/removed)
- [ ] ADMIN finalize → 200 (BR-GRD-007)
- [ ] spoof teacherId người khác → 403

## Related
- Pairs với GAP-999 (authz layer); mirror class KC-5 ADMIN-blocked-PATCH
- Discovered in: Wave flow-kc6 pre-walk 2026-06-05 (FM #4)

## Log

- **2026-06-05 (Wave flow-kc6):** Filed — defer (GAP-999 @PreAuthorize covers cross-tenant/non-teacher; teacherId-from-JWT redesign + ADMIN bypass = follow-up).
