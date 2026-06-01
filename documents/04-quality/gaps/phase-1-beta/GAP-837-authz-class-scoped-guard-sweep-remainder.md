# GAP-837: Per-resource authz guard sweep — remaining class-scoped + id-resolution endpoints

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend (kiteclass-core)
**Detected:** 2026-06-02 (cross-flow sweep during GAP-729 fix, per `cross-flow-bug-class-sweep.md` §3)
**Affects:** ~14 class-scoped endpoints + enrollment/student id-resolution helpers — OWASP A01 IDOR (same bug class as GAP-729)

## Problem

Trong khi fix GAP-729 (thêm `@authz.hasAccessToClass` cho `EnrollmentController.getEnrollmentsByClass` + `AttendanceController.getClassStats`), cross-flow sweep theo `cross-flow-bug-class-sweep.md` phát hiện bug-class signature "class-scoped endpoint thiếu `@PreAuthorize("@authz.hasAccessToClass(#classId)")`" còn xuất hiện ở nhiều controller khác — vượt phạm vi 11 controller GAP-729 đặt tên ban đầu (codebase đã tăng lên 50+ controller).

Đây là OWNED resource (chỉ teacher sở hữu class mới được thao tác) nhưng chỉ có tenant-level Hibernate filter, KHÔNG có per-resource ownership check.

## Cross-flow sweep evidence (per cross-flow-bug-class-sweep.md §3)

**Bug class signature:** class-scoped endpoint (`{classId}` path var, OWNED resource) không gắn `@PreAuthorize("@authz.hasAccessToClass(#classId)")`.

**Grep command:**
```bash
grep -rn '{classId}' kiteclass/kiteclass-core/src/main/java --include="*Controller.java"
# cross-reference với @PreAuthorize proximity
```

**Sites found + verdict:**

| # | Controller : endpoint | Verdict | Reason |
|---|---|---|---|
| 1 | `EnrollmentController` GET `/class/{classId}` | **FIXED (GAP-729)** | Class roster OWNED — guard added + IT A01-U05/U06 |
| 2 | `AttendanceController` GET `/stats/class/{classId}` | **FIXED (GAP-729)** | Lone unguarded sibling next to 2 guarded — consistency fix |
| 3 | `ClassController` PATCH/DELETE `/classes/{classId}` | **DEFER** | OWNED class lifecycle; needs guard + IT |
| 4 | `ClassController` POST `/classes/{classId}/{start,complete,cancel,generate-code,schedule,sessions/generate-from-recurrence}` | **DEFER** | 6 OWNED lifecycle mutations unguarded |
| 5 | `ClassController` GET `/classes/{classId}` + `/classes/{classId}/sessions` | **DEFER** | 2 OWNED reads unguarded |
| 6 | `ClassController` POST `/classes/{classId}/reschedule` | **EXEMPT** | Already guarded (line 197) |
| 7 | `AssignmentController` GET `/class/{classId}` + `/published` + `/pending-grading` | **DEFER** | 3 class-scoped reads; `/published` may be student-view SHARED — classify per endpoint |
| 8 | `EnrollmentController` GET `/{id}`, `/student/{studentId}`, PUT `/{id}/status`, `/{id}/withdraw` | **DEFER** | Needs NEW `@authz` helper: enrollment-id → owning class resolution + student-id ownership; not class-scoped directly |
| 9 | `AttendancePeriodController`, `AttendanceClassBatchController` | **EXEMPT** | Already guarded |
| 10 | `GradeController` (4 endpoints) | **EXEMPT** | Already guarded |

**Decision:**
- Sites FIXED in GAP-729 PR: 2 (Enrollment class roster + Attendance class stats)
- Sites DEFERRED to this gap: ~14 (ClassController 10 + Assignment 2-3 + Enrollment id-resolution 4)
- Sites EXEMPT (already guarded): 4 controllers

## Root Cause

Same as GAP-729: dev adds tenant-level Hibernate filter but forgets per-resource `@PreAuthorize`. Class-scoped endpoints proliferated faster than the guard pattern was applied. The `@authz.hasAccessToClass` helper exists and works (verified GAP-727/GAP-729) but is applied inconsistently.

## Proposed Fix (Wave beta-readiness-N+)

1. **ClassController** — add `@PreAuthorize("@authz.hasAccessToClass(#classId)")` to all 10 OWNED `{classId}` lifecycle + read endpoints (reschedule already done). GET `/classes/{classId}` + `/sessions` are reads but still OWNED (teacher's own class detail).
2. **AssignmentController** — classify each `/class/{classId}` endpoint: `getAssignmentsByClass` + `pending-grading` = teacher OWNED → guard; `/published` (student view) → needs student-enrollment check (different helper) OR document SHARED-within-class.
3. **EnrollmentController** id-resolution endpoints —需要 NEW `@authz` helper `hasAccessToEnrollment(Long enrollmentId)` (resolve enrollment → class → teacher ownership) + `hasAccessToStudent(Long studentId)` for `/student/{studentId}`. Beware multi-tenant `findByIdAndDeletedFalse` tenant-filter concern (GAP-746).
4. IT per OWNED endpoint: non-owner → 403, owner → 200 (mirror `CrossUserAuthzTest` A01-U05/U06 pattern).

## Acceptance Criteria

- [ ] ClassController 10 OWNED `{classId}` endpoints guarded with `@authz.hasAccessToClass(#classId)`
- [ ] AssignmentController class-scoped endpoints classified + guarded (or SHARED-documented)
- [ ] EnrollmentController id-resolution endpoints: new `@authz` helper(s) + guards
- [ ] IT cross-user (same-tenant) per OWNED endpoint: non-owner 403, owner 200
- [ ] New `@authz` helpers fail-closed (deny non-admin when ownership unresolvable) + admin bypass
- [ ] GAP-746 tenant-filter concern verified for any new repository query

### Out-of-scope

- Production exploit verification — gated GAP-612 AWS restore
- SHARED-resource endpoints (course catalog, public reads) — tenant-filter sufficient

## Priority Rationale (P1)

Same as GAP-729: intra-tenant lateral movement (lower attack surface than cross-tenant); beta cohort small. P1, not P0. Upgrade if quality audit detects actual exploit path on these endpoints.

## Related

- GAP-729 (parent — 2 sites fixed; this gap tracks the swept remainder)
- GAP-727 (teacher-class guard fix — provides `@authz.hasAccessToClass`)
- GAP-728 (test method-security enabler)
- GAP-746 (multi-tenant `findByIdAndDeletedFalse` tenant-filter concern — affects new helpers)
- `cross-flow-bug-class-sweep.md` §3 (rule that surfaced this scope)
- OWASP Top 10 2021 A01 Broken Access Control
