# GAP-1413: Nil-UUID tenant resolver stubs in kiteclass-core parent module (multi-tenant isolation risk)

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-06-15 (hardcode-mock state-check, BE agent — top functional finding)
**Affects:** `kiteclass-core` parent notification + payment modules

## Problem

Two tenant resolvers return a hardcoded **nil-UUID** `00000000-0000-0000-0000-000000000000` instead of the real tenant (stubbed "Wave 105 stub / Wave 106 wiring" TODO never completed):
- `kiteclass-core/.../parent/notification/impl/ZaloOaNotificationServiceImpl.java:140` — `resolveTenantId()` → nil-UUID → outbox event tenant scope broken
- `kiteclass-core/.../parent/payment/ParentPaymentController.java:208` — `currentTenantId()` → nil-UUID

This is "stub-not-wired + hardcoded-constant" intersection (mock-nature + hardcode-form). **Multi-tenant correctness risk:** events/payments scoped to nil tenant → RLS isolation hole / cross-tenant leak potential. P0 because it affects tenant isolation across parent flows (KC-8 notification + payment).

## Proposed Fix

Wire `resolveTenantId()` / `currentTenantId()` to real tenant context (gateway-injected `X-Tenant-Id` / TenantContext, per the Host-based resolver other controllers use). Verify outbox events + parent payments scope to correct tenant. Add multi-tenant isolation test (parent A event/payment NOT visible to tenant B).

## Acceptance Criteria

- [ ] Both resolvers return real tenant UUID from request context (no nil-UUID stub)
- [ ] Outbox events from ZaloOa notification scoped to correct tenant_id
- [ ] Parent payment scoped to correct tenant; cross-tenant isolation test PASS
- [ ] Grep `00000000-0000-0000-0000-000000000000` in kiteclass-core parent module = 0 (excl test)

## Related

- Umbrella: GAP-1410 · Audit: `2026-06-15-hardcode-mock-state-check.md`
- KC-8 parent portal (campaign §4); RLS isolation (sister GAP-983 KC-3 leak class); `g1-browser-walk-before-flip` tenant-resolution (GAP-1068 class)
