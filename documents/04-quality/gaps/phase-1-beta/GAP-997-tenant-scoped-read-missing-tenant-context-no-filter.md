# GAP-997: tenant-scoped GET-by-id trả data khi TenantContext chưa set (no X-Tenant-Id → no filter)

**Status:** 🔵 OPEN
**Priority:** 🟢 P3
**Domain:** Backend (security defense-in-depth — cross-cutting)
**Found:** 2026-06-05 (Wave flow-kc5 G1 walk — W10c)
**Affects:** Mọi tenant-scoped GET-by-id (attendance, enrollment, ...) — không chỉ KC-5

## Problem

GET `/api/v1/attendance/{id}` **không có** `X-Tenant-Id` header → `TenantContext` chưa set → Hibernate `tenantFilter` + RLS GUC KHÔNG enable (chỉ active khi `TenantContext.isSet()` per GAP-983 design) → query chạy KHÔNG có tenant filter → trả row của tenant bất kỳ (HTTP 200). Walk W10c: GET attendance id=6 (sky) không tenant header → 200 (đáng lẽ 404).

**Mức độ:** Production gateway LUÔN set `X-Tenant-Id` cho mọi request → core không bao giờ nhận request thiếu tenant. State này chỉ đạt được qua **direct core access bypass gateway** (internal trust boundary). KHÔNG phải production-reachable vuln, nhưng vi phạm defense-in-depth (core nên reject tenant-scoped read khi thiếu tenant context thay vì trả data). Đây là limitation đã biết của GAP-983 (fix chỉ enable filter khi TenantContext.isSet()).

**Verified cùng walk:** cross-tenant read VỚI tenant context (khanh GET sky id) → 404 ✅ (GAP-983 fix holds đúng cho production invariant).

## Proposed Fix

Defer (cross-cutting, low priority). Options: (a) `TenantFilterInterceptor` reject request thiếu `X-Tenant-Id` cho tenant-scoped routes (403/400) thay vì pass-through không filter; (b) document gateway-trust boundary invariant rõ ràng (core chỉ chạy sau gateway). Cân nhắc Phase 1.5+ security hardening.

## Acceptance Criteria
- [ ] (defer) GET tenant-scoped không tenant context → 400/403 thay vì 200

## Related
- Known limitation of GAP-983 (tenantFilter requires TenantContext.isSet())
- Discovered in: Wave flow-kc5 G1 walk 2026-06-05 (W10c)

## Log

- **2026-06-05 (Wave flow-kc5):** Filed — defense-in-depth, gateway-trust boundary; not production-reachable (gateway always sets tenant). Defer Phase 1.5+ hardening.
