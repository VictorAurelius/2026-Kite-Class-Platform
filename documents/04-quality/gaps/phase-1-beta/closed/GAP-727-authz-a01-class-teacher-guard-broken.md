# GAP-727: `hasAccessToClass` guard broken — Class entity không map `teacher_id` → teacher full lock-out

**Status:** 🟢 DONE
**Priority:** 🔴 P0
**Domain:** Backend (kiteclass-core authz)
**Detected:** 2026-05-24 (Wave beta-readiness-1 Bucket D audit, PR #1763)
**Affects:** Mọi teacher login + class-scope operation (attendance / grade / class view)

## Problem

Wave beta-readiness-1 Bucket D audit phát hiện `hasAccessToClass` guard BROKEN:
- `Class` entity (kiteclass-core) KHÔNG map `teacher_id` column từ DB
- `ClassServiceImpl` KHÔNG set `teacher_id` khi create/update class
- → `hasAccessToClass(teacherId, classId)` query luôn trả `false` cho mọi teacher
- → Production behavior: TEACHER login OK NHƯNG mọi `/api/teacher/classes/**` → 403 Forbidden (full lock-out, NOT just IDOR)

KHÔNG phải IDOR vulnerability — teacher KHÔNG thể access OWN class chứ chưa nói access other teacher's class.

## Evidence (PR #1763 Bucket D)

`CrossUserAuthzTest.java` 2 tests `@Disabled`:
- A01-U01 (teacher own class) → SHOULD 200 but guard returns false → 403
- A01-U03 (teacher other class IDOR) → SHOULD 403 but already 403 do guard broken (vacuous PASS)

Both tests document the broken guard:
```java
@Disabled("hasAccessToClass guard always false — classes.teacher_id absent from
test schema (Flyway disabled) + Class entity doesn't map teacher_id +
ClassServiceImpl never sets it in production code path. Audit finding A01-CLASS-01.")
void testTeacherAccessOwnClass() { ... }
```

## Root Cause

3 layer issue:
1. **Schema:** `classes.teacher_id` column MAY exist (per migration history) BUT not in test schema
2. **Entity mapping:** `Class.java` entity missing `@Column(name = "teacher_id") private Long teacherId;` field
3. **Service layer:** `ClassServiceImpl.create()` never sets `teacherId` — đầu vào DTO có nhưng không persist

Guard `hasAccessToClass(teacherId, classId)` → JPA query `findByIdAndTeacherId` → always empty (teacher_id field absent or null) → guard returns false.

## Proposed Fix (Wave beta-readiness-2 Bucket E suggested)

1. **Verify schema:** check production DB `\d classes` xem `teacher_id` column existed (Flyway migration history)
2. **Entity:** add `@Column(name = "teacher_id") private Long teacherId;` to `Class.java`
3. **Service:** update `ClassServiceImpl.create()` + `update()` set `teacherId` từ DTO
4. **Test schema:** ensure Flyway enabled trong test profile OR test fixture creates `teacher_id` column
5. **Re-enable** 2 `@Disabled` tests trong `CrossUserAuthzTest.java`
6. **Verify** teacher login → class operation → 200 OK (not 403)

## Acceptance Criteria

- [x] `Class.java` entity maps `teacher_id` — `@Column(name = "teacher_id") private UUID teacherId;` (UUID not Long, per GAP-795 + V73 `bigint→uuid` migration; actor identity is JWT `sub` UUID)
- [x] `ClassServiceImpl.createClass()` persists `teacherId` from `UserContext.getCurrentUser()` (line 118). `updateClass()` intentionally does NOT reassign owner (ownership set at creation; mutating it from `UpdateClassRequest` would be an ownership-transfer feature + security concern — out of scope)
- [x] `hasAccessToClass(classId)` returns true for owner, false for non-owner — **proven by IT** (`CrossUserAuthzTest` A01-U03 owner→200, A01-U01 non-owner→403, Testcontainers Postgres)
- [x] 2 `@Disabled` tests in `CrossUserAuthzTest.java` re-enabled + PASS (A01-U01 IDOR-deny + A01-U03 owner-allow) — GAP-732 DONE 2026-06-01; Tests run: 4, Failures: 0, Errors: 0
- [x] Guard correctness empirically verified on real Postgres (Testcontainers) — `AuthorizationBeanHasAccessToClassIT` 7/7 PASS (owner-allow + IDOR-deny + 5 edge: non-existent class / null teacher_id / soft-deleted / null classId / no UserContext) + `CrossUserAuthzTest` 4/4 PASS (A01-U01 IDOR→403, A01-U02 parent IDOR, A01-U03 owner→200, A01-U04 parent guard). Run 2026-06-02, kiteclass-core local Testcontainers. **IT layer = acceptable DONE evidence per autonomous-gap-campaign mandate (guard fix proven; teacher access restored).**
- [~] Local browser test: teacher login → class operation → 200 OK — **deferred, AWS stack stopped** (`FEATURE_SHIP_WALK_DEFER` per `feature-ship-runtime-walk-mandate.md` §5; gated GAP-612 AWS restore — superseded as DONE gate by IT-layer empirical proof above)

### Out-of-scope (per `gap-done-discipline.md` §3 Option B)

- Live verify production teacher login — gated GAP-612 AWS restore (file follow-up post-restore)
- Migration data fix nếu existing production rows có `teacher_id = NULL` — separate gap

## Priority Rationale (P0)

CHẶN beta invite cho P3 Manager + Teacher personas — teacher KHÔNG operate được hệ thống. Phase 1 BETA gate prerequisite.

## Related

- PR #1763 Wave beta-readiness-1 Bucket D audit finding A01-CLASS-01
- `CrossUserAuthzTest.java` `@Disabled` 2 tests document
- Wave beta-readiness-2 Bucket B (production defect fix — entity field + service setter)
- Wave beta-prep-1 Bucket D (this PR — IT test re-enable via dedicated repository fixture)
- V2 audit `2026-05-24-outside-in-phase-1-closure-failure-mode-matrix-v2-state-checked.md` A5 partial
- Follow-up: GAP-732 (CrossUserAuthzTest @Disabled body re-enable via controller-level fixture)

## Log

- **2026-06-02 (DONE 95→100% — autonomous gap campaign, IT-layer empirical proof):** State-check per `audit-to-gap-pipeline.md` §2.8 confirmed production defect already FIXED (Class entity maps `teacher_id` UUID lines 82-83; `ClassServiceImpl.createClass()` sets `teacherId` from `UserContext.getCurrentUser()` lines 116-118; `AuthorizationBean.hasAccessToClass()` native query `WHERE c.id = :classId AND c.teacher_id = :userId AND c.deleted = false` lines 88-93 — UUID==UUID comparison + admin bypass + null-guard + soft-delete filter). **Ran both IT on local Testcontainers Postgres 2026-06-02:** `AuthorizationBeanHasAccessToClassIT` Tests run 7, Failures 0, Errors 0 (19.86s) + `CrossUserAuthzTest` Tests run 4, Failures 0, Errors 0 (6.07s) — guard correctness + multi-tenant scoping empirically proven (owner→200, non-owner IDOR→403, soft-delete deny, null teacher_id deny, anonymous deny). Cross-flow sweep per `cross-flow-bug-class-sweep.md`: sibling guard `hasAccessToChild` EXEMPT (different mechanism — repository method on already-mapped `parent_student_links` relation via `users.reference_id` bridge GAP-798, no missing-column bug class; IT-covered A01-U02/U04). All `@authz.hasAccessToClass` controller usages (Grade/Attendance/AttendancePeriod/AttendanceClassBatch/Class) share the same fixed guard bean → covered transitively. Live browser walk (last AC) stays deferred (AWS stopped, GAP-612) — superseded as DONE gate by IT-layer proof per autonomous-gap-campaign mandate ("a passing IT proving teacher access is restored is acceptable DONE evidence"). `FEATURE_SHIP_WALK_DEFER: GAP-727 — AWS stack stopped (GAP-612); IT layer (11 cases, 0 fail) proves functional correctness`. Flipped 🟡 PARTIAL → 🟢 DONE. No code change needed — verification-only closure.

- **2026-06-01 (PARTIAL 80→95% — guard verified via IT):** Fix-time state-check per `audit-to-gap-pipeline.md` §2.8 confirmed production defect already FIXED (entity maps `teacher_id` UUID + `ClassServiceImpl.createClass()` sets it line 118 + `classes.teacher_id` present V1/V73). Remaining drift = 2 `@Disabled` IT tests (GAP-732). Shipped both test bodies in `CrossUserAuthzTest` (A01-U01 IDOR→403 + A01-U03 owner→200) — guard correctness now empirically proven on Testcontainers Postgres (Tests run: 4, Failures: 0, Errors: 0). GAP-732 flipped DONE same PR. Only live browser walk remains (AWS-gated GAP-612) → stays PARTIAL 95% per `feature-ship-runtime-walk-mandate.md` §5 EC2-OFFLINE override. Cross-flow note: `hasAccessToClass` is now enforced + tested; other class-scoped guarded controllers (Grade/AttendancePeriod/AttendanceClassBatch) share the same guard bean → covered by this fix.
- **2026-05-26 (Wave beta-prep-1 Bucket D — PARTIAL):** Investigation phase per
  `release-fix-retry-budget.md` v1.2.0 §3.5 + `audit-to-gap-pipeline.md` §2.8 fix-time
  state-check revealed production defect ĐÃ FIXED tại Wave beta-readiness-2 Bucket B:
  - `Class.java` lines 65-80: `@Column(name = "teacher_id") private Long teacherId;` mapped ✅
  - `ClassServiceImpl.createClass()` lines 112-118: persist `teacherId` từ
    `UserContext.getCurrentUser()` ✅
  - `AuthorizationBean.hasAccessToClass()` lines 66-90: native JPQL query
    `WHERE c.id = :classId AND c.teacher_id = :userId` ✅
  - `V1__create_core_schema.sql` lines 158, 190: column + index tồn tại trong schema ✅

  This PR ships dedicated IT test `AuthorizationBeanHasAccessToClassIT.java` (Testcontainers
  + ClassRepository fixture) covering 6 multi-tenant boundary cases (POSITIVE owner / NEGATIVE
  IDOR cross-teacher / EDGE null classId + null teacher_id + soft-delete + no UserContext) —
  empirically verifies production defect FIXED on real Postgres binding (per
  `postgres-specific-type-testcontainers.md` — H2 + Mockito insufficient).

  Status flipped 🔵 OPEN → 🟡 PARTIAL (per `gap-done-discipline.md` §3 PARTIAL exit ramp):
  - ✅ Class entity maps teacher_id (AC 1)
  - ✅ ClassServiceImpl persists teacherId (AC 2)
  - ✅ Guard returns true/false correctly (AC 3 — verified by new IT)
  - ⚠️ 2 `@Disabled` tests trong `CrossUserAuthzTest` STILL DISABLED — re-enable cần
    controller-level fixture (mockMvc POST `/api/v1/courses/{id}/classes`) deferred GAP-732
  - ⚠️ Live verify production teacher login → 200 OK — gated GAP-612 AWS account restore

  Bucket D's mandate satisfied by new dedicated IT (cleaner, isolated, faster) thay vì
  re-enable original `@Disabled` tests yêu cầu full controller fixture stack. GAP-732 vẫn
  track remaining `CrossUserAuthzTest` re-enable work cho cross-cutting controller scope.
