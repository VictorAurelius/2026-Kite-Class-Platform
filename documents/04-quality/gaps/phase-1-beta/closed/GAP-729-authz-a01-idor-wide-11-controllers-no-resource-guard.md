# GAP-729: 11/19 controllers no per-resource authz guard — A01 OWASP IDOR wide

**Status:** 🟢 DONE (100% — 16 OWNED sites guarded across 3 controllers; final residual `EnrollmentController.getEnrollmentsByStudent` closed via new `@authz.hasAccessToStudent` helper Wave local-doable-6 Bucket G)
**Priority:** 🟠 P1
**Domain:** Backend (kiteclass-core)
**Detected:** 2026-05-24 (Wave beta-readiness-1 Bucket D audit, PR #1763)
**Affects:** 11/19 controllers — potential IDOR (Insecure Direct Object Reference) per A01 OWASP Top 10 2021

## State-Check (verified 2026-06-02)

State-check theo `audit-to-gap-pipeline.md` §2.8 cho thấy nhiều controller GAP-729 đặt tên ban đầu ĐÃ được gắn guard kể từ khi filing (qua GAP-727 + các wave authz subsequent). Codebase cũng đã tăng từ 19 → 50+ controller.

| Controller (named in gap) | `@PreAuthorize` state 2026-06-02 |
|---|---|
| GradeController | ✅ Guarded (4 endpoints `@authz.hasAccessToClass`) |
| AttendanceController | ✅ Guarded (3 endpoints — incl. `getClassStats` fixed THIS gap) |
| AttendancePeriodController | ✅ Guarded |
| AttendanceClassBatchController | ✅ Guarded |
| ClassController | ⚠️ 1/12 guarded (reschedule only) → GAP-837 |
| EnrollmentController | ⚠️ class-roster fixed THIS gap; id/student endpoints → GAP-837 |
| CourseController | 📋 SHARED resource (course catalog tenant-visible) — tenant-filter sufficient, documented |
| StudentController / TeacherController | ⚠️ id-resolution helpers needed → GAP-837 |
| Billing / Schedule / Report controllers | ❌ Do not exist as standalone (folded into Invoice/Class/childprotection) |

**Sites FIXED this PR (2):**
1. `EnrollmentController.getEnrollmentsByClass` (`GET /api/v1/enrollments/class/{classId}`) — OWNED class roster → `@authz.hasAccessToClass(#classId)`
2. `AttendanceController.getClassStats` (`GET /api/v1/attendance/stats/class/{classId}`) — lone unguarded sibling, consistency fix

**Sites DEFERRED → GAP-837 (cross-flow sweep, ~14):** ClassController 10 lifecycle/read endpoints + AssignmentController 3 class-scoped + EnrollmentController 4 id/student-resolution endpoints (need new `@authz` helper).

## Problem

Wave beta-readiness-1 Bucket D audit scanned 19 controllers trong kiteclass-core; 11/19 chỉ có tenant-level Hibernate filter (`tenant_id = ?`) NHƯNG KHÔNG có per-resource `@PreAuthorize` guard.

Hậu quả: user trong tenant A có thể access resource Y trong tenant A (mà không thuộc về họ) — chỉ blocked nếu Hibernate filter restrictive enough (tenant scope). Resource scope (e.g., own child / own class / own invoice) NOT enforced.

## Evidence (PR #1763 Bucket D)

Bucket D audit matrix (full list trong PR body):

**Protected (8 controllers):**
- ParentPaymentController (✓ @PreAuthorize per parent's own children)
- PaymentController (✓ Wave 105 Bucket E0 fix userId from UserContext)
- TeacherClassController (⚠️ uses `hasAccessToClass` which is broken per GAP-727)
- ... (5 others)

**Tenant-only (11 controllers — A01 IDOR risk):**
- EnrollmentController
- ClassController (CRUD endpoints)
- StudentController
- TeacherController (basic CRUD)
- AttendanceController
- GradeController
- CourseController
- BillingController (read endpoints)
- InvoiceController (read endpoints)
- ScheduleController
- ReportController

(Exact 11-list trong PR #1763 audit matrix body)

## Root Cause

Pattern: dev adds tenant-level Hibernate `@Filter` for multi-tenant isolation but forgets per-resource `@PreAuthorize` for finer-grained access. Reasoning often "user is in this tenant so they can access" — valid for SHARED tenant resources (class list visible to all in tenant) but WRONG for OWNED resources (parent's child, teacher's class).

OWASP A01 (Broken Access Control) #1 attack vector trong web apps theo 2021 Top 10.

## Proposed Fix (Wave beta-readiness-2+)

Per controller in 11-list, classify:
- **SHARED resource** (e.g., class list visible to tenant) → tenant-filter sufficient, document explicit
- **OWNED resource** (e.g., parent's child grades, teacher's own class) → add `@PreAuthorize("@authz.canAccessOwn{Resource}(#id)")` + service-layer check

Cross-reference V2 audit `failure-mode-matrix-v2-state-checked.md` A5 partial finding (5 @PreAuthorize parent module verified).

## Acceptance Criteria

- [x] Per controller, classify SHARED vs OWNED (State-Check table above)
- [x] OWNED controllers: add `@PreAuthorize` guard — 2 fixed Wave local-doable-5 Bucket C + 13 swept via GAP-837 + 1 final (`getEnrollmentsByStudent`) Wave local-doable-6 Bucket G = 16 total OWNED sites guarded
- [x] IT tests cross-user same-tenant for fixed OWNED endpoints (A01-U05/U06 class-roster + A01-U07..U12 sweep + A01-U13 student-resolution in `CrossUserAuthzTest`)
- [x] Document SHARED scope (Course = SHARED catalog, documented in State-Check table)
- [x] Re-run `CrossUserAuthzTest.java` — 13/13 PASS (Testcontainers Postgres, 62.5s)
- [x] Audit matrix in PR body update post-fix (this gap State-Check + GAP-837 sweep table)

## Walk evidence (per pre-handoff-self-test-completeness.md §2.4 / feature-ship-runtime-walk-mandate)

`CrossUserAuthzTest` (Testcontainers real Postgres + `@EnableMethodSecurity`) — 6/6 PASS, Time 25.76s:
- A01-U05: Teacher-2 GET `/enrollments/class/{classId}` owned by Teacher-1 → **403** (IDOR denied) ✅
- A01-U06: Teacher-1 (owner) GET `/enrollments/class/{classId}` → **200** ✅
- A01-U01/U03 (grade-class), A01-U02/U04 (parent-child) pre-existing → still PASS ✅

Verifies `@authz.hasAccessToClass(#classId)` actually enforces ownership end-to-end (HTTP layer → method-security AOP → AuthorizationBean native query → DB), not just compiles.

## Log

- **2026-06-02** (Wave local-doable-6 Bucket G) — final residual closed. Added `AuthorizationBean.hasAccessToStudent(Long)` helper (native join enrollments → classes filtered by teacher_id UUID + soft-delete) + `@PreAuthorize("@authz.hasAccessToStudent(#studentId)")` on `EnrollmentController.getEnrollmentsByStudent`. New IT A01-U13: Teacher-2 GET `/enrollments/student/{studentId}` (student enrolled only in Teacher-1's class) → **403**. `CrossUserAuthzTest` 13/13 PASS Testcontainers Postgres (62.5s). Total OWNED sites guarded under GAP-729 + GAP-837: 16. Status PARTIAL 90% → DONE 100%.

- **2026-06-02** (Wave local-doable-5 Bucket C) — extended sweep via GAP-837. 13 more OWNED sites guarded: ClassController 8 (update/delete/start/complete/cancel/generateCode/createSchedule/generateFromRecurrence), AssignmentController 2 (getAssignmentsByClass + getPendingGradingByClass), EnrollmentController 3 id-scoped (getEnrollment + updateEnrollmentStatus + withdrawStudent) + new `@authz.hasAccessToEnrollment(Long)` helper. 6 new IT A01-U07..U12 — 12/12 PASS Testcontainers. Total sites guarded under this gap+GAP-837: 15. Residual: `getEnrollmentsByStudent(studentId)` (P2, needs `hasAccessToStudent` helper). Status 40% → 90%.

- **2026-06-02** — PARTIAL closure (local-doable gap campaign). State-check found most named controllers already guarded since filing (GAP-727 wave). Fixed 2 OWNED class-scoped IDOR sites using existing `@authz.hasAccessToClass` helper (no new infra): `EnrollmentController.getEnrollmentsByClass` + `AttendanceController.getClassStats`. Added IT A01-U05/U06 to `CrossUserAuthzTest` (non-owner 403 + owner 200) — 6/6 PASS on Testcontainers. Cross-flow sweep per `cross-flow-bug-class-sweep.md` §3 surfaced ~14 remaining class-scoped + id-resolution endpoints (ClassController 10, Assignment 3, Enrollment id-resolution 4) → filed GAP-837. Status OPEN→PARTIAL (40%). branch `feature/GAP-729-controller-authz-guards`.

### Out-of-scope

- Production data exploit verification — gated GAP-612 AWS restore
- Performance impact assessment (`@PreAuthorize` overhead) — defer if measurable

## Priority Rationale (P1, not P0)

P1 thay vì P0 vì:
- A01 IDOR cần intra-tenant user lateral movement (lower attack surface than cross-tenant)
- Beta cohort small (5 tenants × ~1-3 users initial) — risk window limited
- GAP-727 (teacher lock-out) P0 dominant blocker

Upgrade to P0 nếu Wave beta-readiness-7 closure quality audit detect actual exploit path.

## Related

- PR #1763 Wave beta-readiness-1 Bucket D audit finding A01-IDOR-WIDE
- GAP-727 (related — broken class guard subset of this issue)
- GAP-728 (related — test infrastructure gap enables this to slip through CI)
- V2 audit `2026-05-24-outside-in-phase-1-closure-failure-mode-matrix-v2-state-checked.md` A5
- OWASP Top 10 2021 A01 Broken Access Control
