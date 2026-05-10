# GAP-269c: kc-student E2E spec + Lighthouse PWA ≥90 verification

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (verification of Wave 49 Bucket C AC; not blocking new feature ship)
**Domain:** Frontend testing + PWA deployment
**Found:** 2026-05-10 (Wave 49 Bucket C PARTIAL exit-ramp per `gap-done-discipline.md` §3)
**Parent:** [GAP-269](GAP-269-track-2-port-kiteclass-student.md)
**Affects:** `kiteclass-frontend/e2e/**` + Vercel preview deploy

## Problem

Wave 49 Bucket C (PR #1093) shipped 11 student screens with offline assignment retry queue (page-context module — clean separation from sw.js). Two AC items defer to dedicated verification:

1. **Lighthouse PWA score ≥90** — requires HTTPS deploy
2. **Playwright E2E spec for offline-online sync flow** — unit test covers queue contract; full E2E (login → today → submit assignment offline → see synced when online) deferred

## Current State (verified 2026-05-10)

| Artifact | Status |
|---|---|
| 11 production routes + components + offline queue module | ✅ shipped Wave 49 Bucket C |
| Unit test `student-assignment-queue.test.ts` (queue contract: enqueue/remove/flush/corrupt-tolerance) | ✅ 6 cases pass |
| Playwright spec covering full offline → online sync flow | ❌ not authored |
| Lighthouse PWA ≥90 measurement on staging URL | ❌ not measured |

## Proposed Fix

1. Author Playwright E2E spec at `kiteclass-frontend/e2e/student-offline-sync.spec.ts`:
   - Login as student → land on `(dashboard)/student/today` → navigate to `student/assignments/[id]` → simulate network offline → submit assignment → verify queue holds the submission (localStorage check) → simulate network online → verify queue auto-flushes → verify success state
   - Use Playwright `context.setOffline(true/false)` API; mock `/api/assignments/:id/submit` endpoint
2. Run Lighthouse on Vercel preview URL post-merge of any kc-student PR; capture PWA + performance scores in `documents/04-quality/audits/ui/lighthouse-kc-student-YYYY-MM-DD.md`
3. Update parent gap GAP-269 AC checkboxes + Log entry

## Acceptance Criteria

- [ ] Playwright E2E spec passes locally + in `frontend-ci.yml` E2E job
- [ ] Lighthouse PWA ≥90 + Performance ≥85 on Vercel preview (audit report committed)
- [ ] GAP-269 parent gap "E2E flow" + "Lighthouse PWA ≥90" AC ✅ verifiable

## Related

- Parent: GAP-269
- Sibling: GAP-269a (social login backend) + GAP-269b (real REST endpoints) — E2E may exercise these once shipped
- Cross-cut: GAP-267a (kc-parent E2E counterpart) — bundle in same Lighthouse audit run + Playwright sweep
- Wave 49 Bucket C PR #1093

## Log

- **2026-05-10**: Filed at Wave 49 closure as named follow-up promised in GAP-269 Log entry §"Deferred (explicit) → follow-up sub-gaps". Per `audit-to-gap-pipeline.md` §3 + `gap-done-discipline.md` §3, deferred items get real gap files.
