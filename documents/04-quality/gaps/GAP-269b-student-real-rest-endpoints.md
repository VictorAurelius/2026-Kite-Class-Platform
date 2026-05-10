# GAP-269b: kc-student real REST endpoints (today/grades/payments/notifications)

**Status:** 🔵 OPEN
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

- [ ] 5 REST endpoints documented in api-contract.md + use-cases.md
- [ ] Backend integration tests pass per controller
- [ ] FE student pages render real data from API (no mock fixtures in production code path)
- [ ] Loading + empty + error states handled per existing kit design
- [ ] Pagination on notifications feed (cursor-based)
- [ ] GAP-269 parent gap "Real REST endpoints" AC ✅ verifiable

## Related

- Parent: GAP-269
- Sibling: GAP-269a (social login backend — independent concern)
- Sibling: GAP-269c (E2E spec — exercises these endpoints)

## Log

- **2026-05-10**: Filed at Wave 49 closure as named follow-up promised in GAP-269 Log entry §"Deferred (explicit) → follow-up sub-gaps".
