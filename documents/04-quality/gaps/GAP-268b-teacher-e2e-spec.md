# GAP-268b: kc-teacher Playwright E2E spec (login → attendance → grade → report)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (smoke test passes; full E2E coverage adds regression safety)
**Domain:** Frontend testing
**Found:** 2026-05-10 (Wave 49 Bucket B PARTIAL exit-ramp per `gap-done-discipline.md` §3)
**Parent:** [GAP-268](GAP-268-track-2-port-kiteclass-teacher.md)
**Affects:** `kiteclass-frontend/e2e/**` Playwright spec layer

## Problem

Wave 49 Bucket B (PR #1094) shipped 24 screens → 11 routes under canonical `(teacher)/teacher/*`; routes navigate end-to-end manually + smoke test passes (`teacher-shell.test.tsx` 4 PASS). Full Playwright E2E flow covering login → mark attendance Lớp 6A1 → enter grades → see report deferred to keep Wave 49 wall-clock under cap.

## Current State (verified 2026-05-10)

| Artifact | Status |
|---|---|
| 11 production routes | ✅ shipped Wave 49 Bucket B |
| Component smoke test (`teacher-shell.test.tsx`) | ✅ 4 PASS |
| Playwright spec for full teacher journey | ❌ not authored |

## Proposed Fix

1. Author `kiteclass-frontend/e2e/teacher-attendance-grade-report.spec.ts`:
   - Login as teacher → land on `/teacher/dashboard` → navigate to `/teacher/attendance` → select Lớp 6A1 → mark attendance for current period → save → confirm "Đã lưu" toast
   - Navigate to `/teacher/grades` → select subject → enter grade for student → finalize → confirm finalized state
   - Navigate to `/teacher/reports` → select class → see report (G3 GradebookEntryGrid renders + PaymentStatusTimeline if applicable)
2. Use existing MSW handlers + mock teacher fixture data (5 students, 6 subjects, 3 periods)
3. Add to `frontend-ci.yml` E2E job manifest

## Acceptance Criteria

- [ ] E2E spec passes locally + CI
- [ ] Spec covers happy path + at least 1 error branch (e.g., conflict on schedule entry)
- [ ] Wired into `frontend-ci.yml` E2E job
- [ ] GAP-268 parent gap "E2E flow" AC ✅ verifiable

## Related

- Parent: GAP-268
- Sibling: GAP-268a (backend endpoint this spec exercises)
- Sibling: GAP-267a (kc-parent E2E counterpart) + GAP-269c (kc-student E2E counterpart) — bundle if same E2E sweep run

## Log

- **2026-05-10**: Filed at Wave 49 closure as named follow-up promised in GAP-268 Log entry.
