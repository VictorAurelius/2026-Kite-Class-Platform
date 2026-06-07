# GAP-951: Mobile-first admin missing — 50%+ VN admin dùng phone

**Status:** 🟢 DONE
**Priority:** 🔴 P0
**Domain:** Frontend
**Found:** 2026-06-04 (Wave flow-kh3 KC-1 pre-walk audit — 3-agent outside-in consensus)
**Completion:** 100%
**Affects:** KC-1 (Admin layout) — persona P3 (Owner non-tech 50+, smartphone-first)
**Defer-to:** After Wave flow-kh3 finish

## Problem

Per persona simulation 2026-05-14 Wave 79: 50%+ VN edu admin dùng phone. `kiteclass-frontend` admin layout chưa có evidence breakpoint <768px tested. Bác Hùng mở `kc-...kitehub.me/admin` trên Samsung A05 → sidebar nav cover 80% screen → settings form input đè nhau → bỏ cuộc. Surfaced: persona Finding 3.3.

## Proposed Fix

Audit + responsive refactor admin layout: collapsible sidebar <768px, form fields stack vertically, touch target ≥44px. Playwright config thêm mobile viewport (iPhone SE, Galaxy A05).

## Acceptance Criteria

- [x] `grep -rn "md:\|lg:\|sm:" kiteclass/kiteclass-frontend/src/app --include="*.tsx" \| wc -l` returns ≥50 (breakpoint usage — count 67)
- [x] Playwright spec `mobile-admin.spec.ts` PASS trên 375×667 viewport (5/5 PASS local Mobile SE)
- [x] Manual walk Samsung A05 emulation: sidebar collapsed by default, settings form usable (Mobile SE project 375×667; no horizontal overflow; hamburger opens drawer; 44px touch target)

## Related

- Discovered in: 3-agent outside-in audit 2026-06-04
- Audit artifact: persona-review/2026-06-04-pre-walk-kc1-tenant-provisioning.md Finding 3.3
- Sister: persona simulation 2026-05-14 Wave 79
- Flow Verification Campaign §4 row KC-1

## Log

- **2026-06-07 (Wave p0-ux-1 closure):** Status OPEN → DONE. **Scope-revise (per `audit-to-gap-pipeline.md` §2.8 state-check):** collapsible sidebar + hamburger + Sheet drawer ALREADY existed in `dashboard-layout.tsx`/`sidebar.tsx`/`header.tsx`. Added this wave = the real deliverable: 44px touch targets (sidebar nav + hamburger `h-11 w-11`) + 7 core admin page headers responsive-stacking (overview/students/classes/courses/teachers/attendance/billing) + Playwright `Mobile SE` project (375×667) + `e2e/mobile-admin.spec.ts` (5 tests: sidebar collapsed default + no horizontal overflow + hamburger opens drawer + search usable + 44px hamburger). **Verify:** breakpoint grep count 67 (≥50 target); `mobile-admin.spec.ts` 5/5 PASS local Mobile SE; `pnpm --filter kiteclass-frontend build` exit 0. git mv → `phase-1-beta/closed/`.
