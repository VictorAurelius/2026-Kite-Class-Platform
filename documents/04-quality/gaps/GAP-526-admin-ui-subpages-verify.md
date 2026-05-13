# GAP-526: Verify all admin UI subpages reach correct backends (extends GAP-519)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (post GAP-518 + GAP-519 cleanup; not blocking GAP-518)
**Domain:** Frontend + Backend contract verify
**Found:** 2026-05-13 (Wave 71c-meta-Phase-2 — coverage audit)
**Affects:** /admin/instances, /admin/payments, /admin/revenue (3 admin subpages besides /admin/beta-requests)

## Problem

Wave 71b verified `/admin/beta-requests` reaches kitehub-subscription (correct). But 3 sibling admin subpages — `instances`, `payments`, `revenue` — exist per filesystem (`(admin)/admin/{instances,payments,revenue}/`) and were NOT explicitly verified during Wave 71b deploy.

Bucket A agent during Wave 71b found that `AdminController` (kitehub-admin) owns `/api/platform/admin/instances` lifecycle endpoints, while `AdminMigrationController` (kitehub-subscription) owns only 2 specific instance migration sub-paths. Routing was scoped to match.

But the FE subpages have NOT been clicked → verified end-to-end. Could be:
- 404 (controller doesn't exist on right service)
- 401 (auth works but data load fails)
- Blank page (FE crash on data shape mismatch)
- Wrong-service routing for some endpoints called by these pages

## Proposed Fix

Post GAP-518 fix, log in as admin → click through each subpage → for each:
1. Open browser DevTools network tab
2. Note all API requests fired by the page
3. Verify each lands on correct backend (kitehub-admin vs kitehub-subscription vs kitehub-branding)
4. Verify response shape matches FE expectations (no console errors)
5. File sub-gap if any subpage broken

Or automate: extend `scripts/audit-gateway-routes.sh` to test live API endpoints (curl + auth) per controller path.

## Acceptance Criteria

- [ ] /admin/instances renders + lists instances + buttons (suspend/activate/force-convert/rollback) reach correct backends
- [ ] /admin/payments renders without crash (may be empty if no payment data — that's OK)
- [ ] /admin/revenue renders without crash
- [ ] Pattern coverage: `pre-handoff-self-test-completeness.md` §2.4 admin-flow checklist applied to all 4 admin subpages

## Related

- Parent: GAP-519 (admin nav sidebar)
- Sibling: GAP-512 (gateway routing scope extension — closed)
- Rule: `pre-handoff-self-test-completeness.md` §2.4
