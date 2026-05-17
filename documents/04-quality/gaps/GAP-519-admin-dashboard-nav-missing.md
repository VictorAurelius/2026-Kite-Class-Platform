# GAP-519: Admin dashboard nav-bar missing links to subpages

**Status:** 🟡 PARTIAL — FE sidebar nav shipped (Wave 72a Bucket C); live admin click-through pending
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

- [x] Sidebar visible after login as admin (existing AdminLayout already renders `<Sidebar variant="admin" />` — verified via component test)
- [x] 4 links visible: Beta Requests, Instances, Payments, Revenue (with data-testid attrs per task spec)
- [ ] Click each → navigates without re-login (live verify pending — code shipped, hrefs assert via test)

## Related

- Rule: `pre-handoff-self-test-completeness.md` §2.4(e)
- Parent: GAP-518
- Wave 72a Bucket C — FE sidebar nav implementation (this PR)

## Log

- **2026-05-17 (Wave 87 Bucket D)** — Verify-at-spawn state-check per `audit-to-gap-pipeline.md` §2.8: `Sidebar.tsx` adminNav verified with 4 `data-testid` attrs (admin-nav-beta-requests/instances/payments/revenue); AdminLayout test suite shipped Wave 72a Bucket C. **Code-side fix complete; remaining live click-through is live verify step gated on prod admin session.** Wave 87 Bucket D NO-OP for this gap — Status PARTIAL 80% unchanged pending live walkthrough.
- **2026-05-14 (Wave 72a Bucket C)** — FE sidebar nav shipped. `Sidebar.tsx` adminNav extended: added `Beta Requests` (icon ClipboardList) as first link → `/admin/beta-requests`; renamed labels to English (Payments/Revenue) for consistency; added `data-testid` attribute on each Link (admin-nav-beta-requests / admin-nav-instances / admin-nav-payments / admin-nav-revenue). AdminLayout.tsx already wired `<Sidebar variant="admin" />` from prior work — no new shell needed. Tests: new `AdminLayout.test.tsx` GAP-519 suite asserts all 4 testids present + correct href targeting. Local verify GREEN: 664/664 tests + lint + build. Status PARTIAL pending live click-through; sidebar nav scope (Sidebar.tsx) is FE-only and full code paths exercised in unit tests.
