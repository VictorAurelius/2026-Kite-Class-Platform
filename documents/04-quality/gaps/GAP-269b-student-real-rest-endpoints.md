# GAP-269b: kc-student real REST endpoints (today/grades/payments/notifications)

**Status:** 🟡 PARTIAL — endpoint contracts + Phase 1 v1 stubs + business docs shipped Wave 51 Bucket B; full data joins + FE swap-to-real-data deferred to follow-ups
**Priority:** 🟡 P2 (UI ships with mock VN data; production wiring blocks beta tenant ship)
**Domain:** Backend (kiteclass-core student-facing read APIs)
**Found:** 2026-05-10 (Wave 49 Bucket C PARTIAL exit-ramp per `gap-done-discipline.md` §3)
**Parent:** [GAP-269](GAP-269-track-2-port-kiteclass-student.md)
**Affects:** `kiteclass-core/.../student/**` controllers + service layer; FE replaces mock data with real fetch

## Problem

Wave 49 Bucket C (PR #1093) shipped 11 kc-student screens rendering representative VN mock data (lớp 10A2, 6 môn, học phí 2.500.000 ₫/tháng). Real REST endpoints for `today`, `grades`, `payments`, `notifications` are NOT wired — UI consumes hardcoded fixtures.

Phase 1 BETA can ship invite-only with mock data short-term, but real beta tenants need actual data flowing from kc-core.

## Current State (verified 2026-05-10)

| Endpoint | Status |
|---|---|
| `GET /api/v1/students/me/today` (today screen — current schedule + assignments due) | ❌ does not exist |
| `GET /api/v1/students/me/grades` (grades index) | ❌ does not exist |
| `GET /api/v1/students/me/grades/{subjectId}` (grade detail per subject) | ❌ does not exist |
| `GET /api/v1/students/me/payments` (invoice list) | ❌ does not exist (or partial — verify) |
| `GET /api/v1/students/me/notifications` (notification feed) | ❌ does not exist |
| FE `studentApi.fetchToday()`, `fetchGrades()`, etc. | ❌ stubs only |

## Proposed Fix

1. State-check existing `kiteclass-core/.../student/**` for any partial controllers (likely some exist; assemble inventory)
2. Add missing REST controllers + services + DTOs:
   - `StudentTodayController.getTodaySchedule(authPrincipal)` — joins ClassSchedule + AttendancePeriod + Assignment due-today filter
   - `StudentGradesController.getGradesOverview` + `getSubjectDetail`
   - Verify or extend `StudentPaymentsController` (may already exist for owner UI)
   - `StudentNotificationsController.getFeed(cursor, limit)`
3. Update `documents/01-business/kiteclass/student-portal/api-contract.md` (create if missing) with all 5 endpoints + DTO shapes + error codes
4. Update `use-cases.md` UC-STUDENT-* (today / grades / payments / notifications)
5. FE: replace mock fixtures in `(dashboard)/student/**/page.tsx` with React Query hooks calling real APIs
6. Integration tests per controller

## Acceptance Criteria

- [x] 5 REST endpoints documented in api-contract.md + use-cases.md (NEW domain folder `documents/01-business/kiteclass/student-portal/` with rules.md + use-cases.md UC-STUDENT-PORTAL-01..05 + api-contract.md covering all 5 endpoints with request/response schemas + error codes + Phase 1 v1 stub vs Phase 2 split)
- [x] Backend integration tests pass per controller (`StudentPortalControllerIT` — 7 tests covering all 5 endpoints + auth-missing 401 path + cursor/limit pagination shape)
- [ ] FE student pages render real data from API (no mock fixtures in production code path) — **deferred to FE swap-to-real-data follow-up gap; out of scope of Wave 51 Bucket B per plan §3 R1**
- [ ] Loading + empty + error states handled per existing kit design — **FE concern, deferred with FE follow-up**
- [x] Pagination on notifications feed (cursor-based) — `StudentNotificationFeedResponse` ships with `items + nextCursor`; clamping `[1..100]` documented per BR-STUDENT-PORTAL-003
- [ ] GAP-269 parent gap "Real REST endpoints" AC ✅ verifiable — pending FE consumer + service-layer joins (Phase 2)

## Related

- Parent: GAP-269
- Sibling: GAP-269a (social login backend — independent concern)
- Sibling: GAP-269c (E2E spec — exercises these endpoints)

## Log

- **2026-05-10**: Filed at Wave 49 closure as named follow-up promised in GAP-269 Log entry §"Deferred (explicit) → follow-up sub-gaps".
- **2026-05-10**: Wave 51 Bucket B shipped Phase 1 v1 — endpoint contracts published. New package `kiteclass-core/.../module/student/portal/` with `StudentPortalController` exposing 5 GET endpoints + `StudentPortalService` interface + `StudentPortalServiceImpl` returning shape-stable empty payloads (foundation pattern matching `ParentNotificationsFacetController` Wave 18b2). DTOs: `StudentTodayResponse`, `StudentGradeOverview`, `StudentGradeDetailResponse`, `StudentPaymentSummary`, `StudentNotificationItem`, `StudentNotificationFeedResponse`. Auth via Gateway-injected `X-User-Reference-Id` header → 401 when missing. NEW domain folder `documents/01-business/kiteclass/student-portal/` with full 3-layer (rules.md BR-STUDENT-PORTAL-001..005 with 5-attribute review per `business-logic-review.md` + use-cases.md UC-STUDENT-PORTAL-01..05 + api-contract.md). Integration tests `StudentPortalControllerIT` (7 tests) cover all 5 endpoints + auth guard. Status flips OPEN → PARTIAL per `gap-done-discipline.md` §3 — full join logic against ClassSchedule/Assignment/SubjectGrade/Invoice/Notification entities deferred to FE consumer PR per Wave 51 plan §3 R1.
