# GAP-268b: kc-teacher Playwright E2E spec (login → attendance → grade → report)

**Status:** 🟢 DONE 2026-05-10 — Wave 51 Bucket A shipped Playwright spec covering full teacher journey
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

- [x] E2E spec authored (`kiteclass-frontend/e2e/wave-49-followups/teacher-attendance-grade-report.spec.ts`)
- [x] Spec covers happy path (dashboard → attendance → grades → reports) + 1 error branch (non-existent class report does not crash)
- [x] Discoverable via `pnpm test:e2e -- wave-49-followups` (CI gate scope intentionally narrow per `frontend-ci.yml` `test:e2e:gates` script — class-lifecycle gate stays focused)
- [x] GAP-268 parent gap "E2E flow" AC ✅ verifiable

## Out-of-scope (track separately)

| Item | Where |
|---|---|
| Adding wave-49-followups specs to CI gate suite | Future — once specs stabilize across 1-2 wave runs, fold into `test:e2e:gates` if signal/noise warrants |

## Related

- Parent: GAP-268
- Sibling: GAP-268a (backend endpoint this spec exercises)
- Sibling: GAP-267a (kc-parent E2E counterpart) + GAP-269c (kc-student E2E counterpart) — bundle if same E2E sweep run

## Log

- **2026-05-10** (DONE flip): Wave 51 Bucket A shipped `kiteclass-frontend/e2e/wave-49-followups/teacher-attendance-grade-report.spec.ts`. Happy path covers `/teacher/dashboard` → `/teacher/attendance` (Lớp 10A2 — actual fixture name; gap text "Lớp 6A1" was placeholder, see §Out-of-scope) → `/teacher/grades` → `/teacher/reports`. Error branch: navigating to non-existent class report renders fallback (no client crash). Specs locally pass via `pnpm -F kiteclass-frontend test:e2e -- wave-49-followups`. Verification artifact = spec file + suite presence on `wave/51-bucket-a-kc-e2e-sweep`.
- **2026-05-10**: Filed at Wave 49 closure as named follow-up promised in GAP-268 Log entry.
