# GAP-197: Attendance Calendar-Mode UI Variant

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (feature tier — UX enhancement, backend exists)
**Domain:** Frontend / KiteClass
**Found:** 2026-04-20 (action-1 §14.1 + §15.H)
**Wave:** Wave 11 (kiteclass feature enhancement)
**Affects:** Teacher attendance workflow, student/parent attendance visibility

## Problem

Current attendance UI is period-based (list per class session). User asked (action-1 line 421): "điểm danh đang design thế nào, có cần làm mode kiểu calender không?"

Calendar-mode (month / week grid view) is a well-known education-app pattern that gives:
- Teacher at-a-glance view of absence patterns
- Parent one-click view of child's weekly attendance
- Student visibility into own trend over time

Backend for period attendance exists via GAP-060 (done). Only UI layer missing.

## Context

Feature-P2: nice-to-have. Should wait until BL-P0 + BL-P1 + meta gaps are closed (per meta-gap-priority tiers).

## Proposed Fix

1. **UX spec**
   - Month view: 7×N grid, each cell = one student-day, color-coded (present/absent/late/excused)
   - Week view: more detail (per-period dots within each day cell)
   - Teacher: filter by class, bulk-edit mode
   - Parent: read-only, single student, month picker
   - Student: read-only, own record
2. **Shared component** — `AttendanceCalendar` under `kiteclass-frontend/src/components/attendance/`
3. **Data fetching** — range query endpoint (may require BE endpoint addition, verify GAP-060 contract)
4. **Mode toggle** — existing period view + new calendar view (default to calendar for parents, period for teachers)
5. **Accessibility** — keyboard nav, ARIA grid role, screen reader announces cell state

## Acceptance Criteria

- [ ] Figma mock for month + week view (3 persona variants)
- [ ] Component implemented + unit tests
- [ ] E2E test: teacher views class month, marks absence, saves, refresh confirms
- [ ] Accessibility audit passes (WCAG AA)
- [ ] UI review /128 run on updated attendance screens
- [ ] Mode toggle preference persists per user

## Related

- action-1 §14.1 + §15.H
- GAP-060 structured attendance (backend — DONE)
- `.claude/skills/quality/ui-review/SKILL.md`
- Rule: `.claude/rules/meta-gap-priority.md` §3 (Feature P2)

## Log

- 2026-04-20 — Created from action-1 §15.H.
