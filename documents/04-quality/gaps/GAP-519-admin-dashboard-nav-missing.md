# GAP-519: Admin dashboard nav-bar missing links to subpages

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (UX blocker after GAP-518 fix)
**Domain:** Frontend
**Found:** 2026-05-13 (Wave 71c per `pre-handoff-self-test-completeness.md` §2.4(e))
**Affects:** PLATFORM_ADMIN can't reach `/admin/beta-requests` `/admin/instances` `/admin/payments` `/admin/revenue` without typing URL

## Problem

After GAP-518 fix, admin lands at `/admin` dashboard. Need verify there's nav-bar/sidebar with links to 4 subpages. Currently unverified.

## Proposed Fix

1. Inspect `AdminLayout.tsx` sidebar — if no nav links, add Sidebar component listing 4 sections
2. Add `data-testid="admin-nav-beta-requests"` etc. for E2E test
3. Each link points to correct route

## Acceptance Criteria

- [ ] Sidebar visible after login as admin
- [ ] 4 links visible: Beta Requests, Instances, Payments, Revenue
- [ ] Click each → navigates without re-login

## Related

- Rule: `pre-handoff-self-test-completeness.md` §2.4(e)
- Parent: GAP-518
