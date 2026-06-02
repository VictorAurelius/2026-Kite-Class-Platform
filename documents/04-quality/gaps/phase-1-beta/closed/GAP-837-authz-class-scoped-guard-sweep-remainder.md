# GAP-837: Per-resource authz guard sweep — remaining class-scoped + id-resolution endpoints

**Status:** 🟢 DONE (100% — 14/14 OWNED endpoints guarded incl. final `getEnrollmentsByStudent`; 2 SHARED documented; 0 DEFER outstanding)
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

- [x] ClassController OWNED `{classId}` lifecycle/write endpoints guarded (8 endpoints: update / delete / start / complete / cancel / generateClassCode / createSchedule / generateFromRecurrence) — Wave local-doable-5 Bucket C
- [x] ClassController SHARED READ endpoints documented (getClass / listClasses / listSessions tenant-filter only — student/parent flows depend on reading class info) + createClass(courseId) tenant-filter + service-side teacher binding (`courseId` ownership helper deferred future)
- [x] AssignmentController class-scoped endpoints classified + guarded: `getAssignmentsByClass` (teacher full view) + `getPendingGradingByClass` guarded; `getPublishedAssignmentsByClass` documented SHARED (student-view by design)
- [x] EnrollmentController id-resolution endpoints: new `@authz.hasAccessToEnrollment(Long)` helper (resolves enrollment.classId → reuses `hasAccessToClass`) + guards on `getEnrollment`/`updateEnrollmentStatus`/`withdrawStudent` (3 endpoints)
- [x] IT cross-user (same-tenant) per OWNED endpoint: 6 new tests A01-U07..U12 (ClassController PATCH/DELETE/cancel + AssignmentController list + EnrollmentController GET/withdraw) — 12/12 PASS Testcontainers Postgres
- [x] New `@authz` helper fail-closed (deny when enrollment not found / soft-deleted; null guard; admin bypass) + JPA NoResultException handled
- [~] GAP-746 tenant-filter concern: `hasAccessToEnrollment` uses native query with `deleted = false`; TenantFilterInterceptor enforces tenant scope on existing entity queries — new helper relies on same chain. Live verify deferred (per GAP-612 AWS restore + GAP-746 multi-tenant repo audit)
- [x] `EnrollmentController.getEnrollmentsByStudent(studentId)` — DONE Wave local-doable-6 Bucket G: new `@authz.hasAccessToStudent(Long)` helper (native join enrollments → classes; admit teacher who owns ≥1 of student's enrolled classes; admin bypass; fail-closed on null/no-enrollment) + `@PreAuthorize` guard on endpoint + IT A01-U13 (Teacher-2 non-owner → 403)

### Out-of-scope

- Production exploit verification — gated GAP-612 AWS restore
- SHARED-resource endpoints (course catalog, public reads) — tenant-filter sufficient

## Priority Rationale (P1)

Same as GAP-729: intra-tenant lateral movement (lower attack surface than cross-tenant); beta cohort small. P1, not P0. Upgrade if quality audit detects actual exploit path on these endpoints.

## Log

- **2026-06-02** (Wave local-doable-6 Bucket G) — DONE closure 100%. Final residual `EnrollmentController.getEnrollmentsByStudent(studentId)` guarded via new `AuthorizationBean.hasAccessToStudent(Long)` helper: native `SELECT COUNT(*) FROM enrollments e JOIN classes c ON c.id = e.class_id WHERE e.student_id = :studentId AND e.deleted = false AND c.deleted = false AND c.teacher_id = :userId` — admits teacher who owns at least one of student's enrolled classes, admin bypass via `isAdmin()`, fail-closed on null userId / no-enrollment. New IT `A01-U13` in `CrossUserAuthzTest`: Teacher-2 GET `/api/v1/enrollments/student/{studentId}` (student enrolled only in Teacher-1's class) → **403** ✅. Full suite `CrossUserAuthzTest` 13/13 PASS Testcontainers Postgres (62.5s). `mvnw compile -P strict-warnings` clean (0 warnings). Total OWNED endpoints guarded across GAP-729 + GAP-837: 16 (Enrollment 4 + Class 8 + Assignment 2 + Attendance 1 + Grade 1 pre-existing). Branch `wave/local-doable-6-bucket-g-authz-residual`. Cross-flow sweep per `cross-flow-bug-class-sweep.md` §3: 1 new FIX site (`getEnrollmentsByStudent`), 0 sister sites remaining.

- **2026-06-02** (Wave local-doable-5 Bucket C) — PARTIAL closure 85%. Shipped 13 guards + 1 new authz helper + 6 IT tests. Sites: (a) ClassController 8 OWNED write/lifecycle guards + 3 SHARED READ documented + createClass classification, (b) AssignmentController 2 OWNED guards (getAssignmentsByClass + getPendingGradingByClass) + getPublishedAssignmentsByClass documented SHARED, (c) EnrollmentController new `@authz.hasAccessToEnrollment` helper + 3 id-scoped guards (getEnrollment + updateEnrollmentStatus + withdrawStudent). Tests: 6 new A01-U07..U12 in `CrossUserAuthzTest` — 12/12 PASS on Testcontainers Postgres (44s). `getEnrollmentsByStudent(studentId)` deferred (different scope — needs `hasAccessToStudent` helper resolving multi-class student membership; tracked as residual P2). Branch `wave/local-doable-5-bucket-c`. Cross-flow sweep evidence in PR body §3 per `cross-flow-bug-class-sweep.md`.

## Related

- GAP-729 (parent — 2 sites fixed; this gap tracks the swept remainder)
- GAP-727 (teacher-class guard fix — provides `@authz.hasAccessToClass`)
- GAP-728 (test method-security enabler)
- GAP-746 (multi-tenant `findByIdAndDeletedFalse` tenant-filter concern — affects new helpers)
- `cross-flow-bug-class-sweep.md` §3 (rule that surfaced this scope)
- OWASP Top 10 2021 A01 Broken Access Control
