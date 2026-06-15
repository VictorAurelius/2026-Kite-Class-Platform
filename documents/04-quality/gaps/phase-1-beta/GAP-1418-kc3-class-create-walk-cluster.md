# GAP-1418: KC-3 class-create walk cluster (recurrence contract drift + LMS-DRAFT 400 + discoverability)

**Status:** 🟡 PARTIAL
**Priority:** 🟠 P1
**Domain:** Frontend
**Found:** 2026-06-15 (KC-3 academic G2★ walk — owner create course → class → schedule)
**Affects:** `kiteclass-frontend` class-create + course-detail flow

## Problem

Owner KC-3 walk (tạo khóa → lớp → xếp lịch) surfaced 3 bugs:

1. **Recurrence 400 — FE↔BE field-name contract drift (P1, walk-blocker).** `POST /api/v1/classes/{id}/sessions/generate-from-recurrence` rejected `by_day`/`start_time`/`end_time` as null. BE `RecurrenceRuleDto` binds **snake_case** via explicit `@JsonProperty("by_day"/"start_time"/"end_time"/"exclude_dates")`; FE `classesApi.generateSessionsFromRecurrence` POSTed the camelCase `RecurrenceRule` object → BE saw null → `MethodArgumentNotValidException` 400. The auto-gen-sessions step (core KC-3) was fully broken. A stale FE comment falsely claimed "Jackson maps byDay ↔ by_day automatically".
2. **LMS modules 400 on DRAFT course (P2).** Course-detail "Nội dung" tab mounted `CourseContentManager` unconditionally → `GET /api/v1/lms/courses/{id}/modules` → BE `COURSE_NOT_PUBLISHED` 400 for a freshly-created DRAFT course (console error).
3. **Class-create discoverability (P2).** No link from the course row (👁 only → detail) nor the course-detail page to class management; class-create reachable only via top-level "Lớp học" nav + course re-selection.

## Fix (this PR — all 3)

1. `classesApi.generateSessionsFromRecurrence` maps camelCase → snake_case (`by_day`/`start_time`/`end_time`/`exclude_dates`) before POST. Unit test pins the wire contract.
2. Course-detail gates `CourseContentManager` on `isPublished`; DRAFT shows guidance ("Xuất bản khóa học để thêm nội dung") instead of firing the 400.
3. Added "Thêm lớp học" button on course-detail → `/courses/{id}/classes/new`.

## Acceptance Criteria

- [x] Recurrence POST sends snake_case; generate-sessions returns 200 (unit test `classes-recurrence.test.ts`).
- [x] DRAFT course content tab shows guidance, no 400.
- [x] Course-detail has discoverable "Thêm lớp học" button.
- [ ] G2★ re-walk on rebuilt FE: create course → class with recurrence → sessions auto-gen verified live.

## Related

- Found in: KC-3 academic G2★ walk 2026-06-15
- Contract-drift class sweep → GAP-1419 (other FE→BE POST bodies vs @JsonProperty snake_case DTOs)
- BE design question (defer): should LMS authoring be allowed pre-publish? (currently modules-list gated on PUBLISHED)
