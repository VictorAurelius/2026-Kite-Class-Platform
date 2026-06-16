# GAP-1459: KC-2 STAFF login lands owner-centric /dashboard, widget owner-scoped 403

**Status:** 🔵 OPEN
**Priority:** 🔴 P2
**Domain:** Frontend
**Found:** 2026-06-16 (Flow Verification Campaign — KC-1/2/3/8 browser re-walk)
**Affects:** Frontend

## Problem

KC-2 walk FM-3: STAFF login → /dashboard render nhưng widget gọi GET /api/platform/instances/owner/{id} → 403 (STAFF không own instance). Cần STAFF dashboard scope riêng hoặc ẩn owner-only widgets. Phase 2.

## Acceptance Criteria

- [ ] Fix/verify per Problem
- [ ] Browser re-walk confirm

## Related

- Discovered in: 2026-06-16 browser walk batch (KC-1/2/3/8)
