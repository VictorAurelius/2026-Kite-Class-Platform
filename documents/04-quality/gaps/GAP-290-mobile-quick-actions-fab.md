# GAP-290: Mobile quick-action FAB + tap-target sizes

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Frontend (kiteclass-frontend) + UX (mobile)
**Found:** 2026-05-04 (Wave 17 Bucket A — P1 Solo Teacher Round 1 review)
**Affects:** P1 Solo Teacher (mobile 70%+ usage), P2 Owner mobile-on-the-go scenarios

## Problem

P1 AC §0 critical concern #2: "Mobile-friendly: phải dùng được hoàn toàn qua phone (chỉnh schedule, mark attendance, send Zalo)". P1 AC-OPS-001 requires "schedule 1 lesson session trong ≤5 clicks trên mobile". P1 AC-OPS-003 requires "mark attendance per session cho 5-10 students trong ≤2 phút trên mobile".

Current state:
- Class create flow at `kiteclass/kiteclass-frontend/src/app/(dashboard)/courses/[id]/classes/new/page.tsx` requires nav: Dashboard → Courses → Course detail → "+Class" = ≥3 navigations BEFORE the create form (≥6 total clicks).
- No floating-action-button (FAB) on dashboard for "+Class" / "+Student" / "+Payment" quick actions.
- Attendance UI uses `Select` dropdowns per student (`attendance/page.tsx:26-30`) — slower than tap-tap status icons; tap-target sizes unverified for thumb-reach on mobile.

## Root Cause

UX patterns inherited from desktop layout. Mobile-first redesign was not in v1 scope. Tap-target sizes (44×44 px Apple HIG / 48×48 dp Material) not enforced.

## Proposed Fix

1. Add FAB component to `DashboardLayout` — visible on mobile breakpoint, hidden ≥md. FAB expands into 3-4 quick actions (Class, Student, Payment, Attendance Today).
2. Quick-action routes that pre-fill context (e.g., "+ Class" defaults to most-recent course; teacher can change after).
3. Replace `Select` dropdown in attendance with tap-icon array (5 icons: P/A/L/E/M) — bigger tap-targets.
4. Audit all tap-targets on mobile; enforce 48×48 minimum via Tailwind utility.
5. E2E (Playwright mobile viewport): solo flow ≤5 clicks for class create, ≤2 min for 10-student attendance.

## Acceptance Criteria

- [ ] FAB component in mobile dashboard
- [ ] ≥4 quick actions accessible from FAB
- [ ] "+ Class" pre-fills course context — ≤5 clicks total
- [ ] Attendance UI uses tap-icons not Select dropdown — measured ≤2 min for 10 students in E2E
- [ ] All tap-targets ≥48×48 dp on mobile
- [ ] AC-OPS-001 + AC-OPS-003 PASS in re-test

## Related

- Review: [`documents/00-brd/persona-reviews/P1-solo-teacher-round-1-2026-05-04.md`](../../00-brd/persona-reviews/P1-solo-teacher-round-1-2026-05-04.md) §2
- AC: AC-OPS-001, AC-OPS-003
- Sibling: GAP-295 (PWA + offline — also mobile UX investment)

## Log

- 2026-05-04 — Created from Wave 17 Bucket A.
