# GAP-197: Attendance Calendar-Mode UI — Verify + Extend

**Status:** 🟡 PARTIAL (component shipped in PR 3.8.1; verification + variants outstanding)
**Priority:** 🟡 P2 (feature tier — UX enhancement, most core work done)
**Domain:** Frontend / KiteClass
**Found:** 2026-04-20 (action-1 §14.1 + §15.H); scope revised 2026-04-20 post state-check
**Wave:** Wave 11 (kiteclass feature enhancement)
**Affects:** Teacher attendance workflow, parent/student attendance visibility

## Current State (verified 2026-04-20)

Much of the calendar UX already shipped:

| Piece | File | Status |
|-------|------|--------|
| Enhanced calendar component | `kiteclass-frontend/src/components/attendance/enhanced-attendance-calendar.tsx` (315 LOC, PR 3.8.1) | ✅ implemented with month grid, filters, tooltips, present/absent/late/excused counts |
| Teacher integration (class report) | `app/(dashboard)/attendance/reports/page.tsx` | ✅ wired |
| Student integration (student detail) | `app/(dashboard)/students/[id]/attendance/page.tsx` | ✅ wired |
| Base `Calendar` primitive | `components/ui/calendar.tsx` | ✅ used |

User asked "có cần làm mode kiểu calendar không?" (action-1 line 421) without knowing a calendar already exists — the gap was filed speculatively.

## Problem — Remaining Gaps

1. **Parent-facing variant not wired** — parent portal (GAP-052 IN_PROGRESS) has no calendar integration yet
2. **Student-self variant** — student viewing OWN attendance flow not explicit; may reuse teacher page with RBAC filtering
3. **Accessibility audit absent** — ARIA grid role, keyboard navigation, screen-reader day-cell announcements not verified
4. **Week-view variant** — only month view exists; week view useful for dense schedules
5. **Mode toggle persistence** — user preference (list vs calendar) not persisted
6. **UI review /128** — component shipped before the UI audit skill upgrade; never formally scored
7. **E2E test coverage** — no Playwright flow covering: teacher opens class → calendar → marks absence → refreshes → sees update

## Context

Component quality appears good (315 LOC, strict TypeScript, tooltips, filters). Scope narrows from "design from scratch" to "verify + extend + fill variant gaps".

## Proposed Fix

1. **Integrate into parent portal** when GAP-052 completes — read-only month view, single child
2. **Student-self flow** — add explicit route `/attendance` under student RBAC, reuses component with `readonly` prop
3. **Accessibility pass** — add ARIA roles, keyboard nav (arrow keys → day, enter → detail), axe-core test
4. **Week-view variant** — new `variant: 'week' | 'month'` prop
5. **Preference persistence** — store mode in localStorage or user settings API
6. **UI review** — run `/ui-review` on updated attendance pages and capture baseline
7. **E2E test** — Playwright scenario covering marking + reload

## Acceptance Criteria

- [ ] Parent portal calendar (after GAP-052 done) — read-only, single-child
- [ ] Student-self route + RBAC
- [ ] axe-core test: zero violations on calendar
- [ ] Keyboard nav works (arrow keys + enter)
- [ ] Week-view variant shipped
- [ ] Mode toggle persists
- [ ] UI review /128 score recorded (target ≥ 100)
- [ ] E2E test passes CI

## Out of Scope

- Backend data model (GAP-060 DONE)
- List-mode attendance page (exists, untouched)

## Related

- action-1 §14.1 + §15.H
- GAP-060 structured attendance (backend — DONE)
- GAP-052 parent portal (IN_PROGRESS — blocks parent-facing variant)
- PR 3.8.1 (introduced enhanced calendar)
- `.claude/skills/quality/ui-review/SKILL.md`
- Rule: `.claude/rules/meta-gap-priority.md` §3 (Feature P2)
- Rule: `.claude/rules/audit-to-gap-pipeline.md` §2 (dedupe — state verified)

## Log

- 2026-04-20 — Created from action-1 §15.H.
- 2026-04-20 — **Scope revised** after state-check. Found `enhanced-attendance-calendar.tsx` shipped in PR 3.8.1 with month grid + filters + tooltips + 4 status counts; integrated in 2 pages. Gap narrowed to parent/student variants + a11y + week view + UI review + E2E.
