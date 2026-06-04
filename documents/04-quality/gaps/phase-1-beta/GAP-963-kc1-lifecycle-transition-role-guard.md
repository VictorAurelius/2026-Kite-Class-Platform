# GAP-963: Lifecycle transition TRIAL→SUSPENDED no service-level role guard + settings per-row guard

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-04 (Wave flow-kh3 KC-1 pre-walk audit — 3-agent outside-in consensus)
**Affects:** KC-1 (Lifecycle + settings) — auth/permission cross-flow
**Defer-to:** After Wave flow-kh3 finish

## Problem

(1) `SubscriptionRenewalService:177` flip `instance.setStatus(InstanceStatus.SUSPENDED)` — caller authentication không enforced tại service level (only at controller). Nếu non-admin endpoint indirectly calls (vd webhook callback), suspension bypass possible. (2) Per-row guard cho settings edit: even with role=OWNER, must verify `tenant.ownerId == auth.userId`. Nếu endpoint chỉ check role không check row scope → owner edit ANY tenant via tenantId path param. Surfaced: matrix A7×E1×EC6 + A6×E4×EC6.

## Proposed Fix

Add service-level guard `@PreAuthorize("hasRole('PLATFORM_ADMIN') or hasRole('OWNER') and #instance.ownerId == authentication.principal.userId")` cho mọi lifecycle transition + settings edit. Audit each caller path từ webhook/cron → admin guard chain.

## Acceptance Criteria

- [ ] `grep -B5 -A10 "setStatus.SUSPENDED" kitehub-subscription/.../service/` mọi caller path có admin/owner guard
- [ ] POST settings với tenantId=other-tenant → 403 (not 200 silent)
- [ ] Webhook callback path requires HMAC signature check + admin role assertion

## Related

- Discovered in: 3-agent outside-in audit 2026-06-04
- Audit artifact: persona-review/2026-06-04-pre-walk-kc1-failure-mode-matrix.md A7×E1×EC6 + A6×E4×EC6
- Sister: GAP-814 (X-Tenant-Id spoofing) — same per-row guard pattern
- Flow Verification Campaign §4 row KC-1
