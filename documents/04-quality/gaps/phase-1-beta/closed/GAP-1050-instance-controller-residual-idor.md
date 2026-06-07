# GAP-1050: InstanceController residual cross-tenant IDOR — update + owner-enumeration endpoints

**Status:** 🟢 DONE
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-06-07 (G3 P2 review — residual của GAP-1025)
**Closed:** 2026-06-07 (G3 runtime walk via :9000 — cross-tenant PUT 403, cross-user GET /owner 403, self/admin 200)
**Affects:** `InstanceController` (kitehub-subscription) — `PUT/PATCH /{id}`, `GET /owner/{ownerId}`

## Problem

GAP-1025 (Wave security-2, DONE) đã gate list/delete/purge/extend-trial của `InstanceController` thành admin-only, NHƯNG bỏ SÓT 3 endpoint — vẫn **ZERO `@PreAuthorize` + ZERO ownership binding** (confirmed dòng ~163-192 pre-fix):

- **`PUT /{id}` + `PATCH /{id}` (`updateInstance`)** — cross-tenant MUTATION write: bất kỳ authenticated user có thể sửa instance của tenant khác (organizationName / tier / customDomain / notification prefs) bằng cách đoán instance id. OWASP A01 + integrity violation.
- **`GET /owner/{ownerId}` (`getInstancesByOwner`)** — cross-user enumeration: liệt kê toàn bộ instance theo `ownerId` tùy ý → kẻ tấn công enumerate instances của user khác.

3 endpoint này gateway route `platform-instances` không có role predicate → chỉ rơi vào `anyRequest().authenticated()`. Severity = **P0** vì PUT/PATCH là cross-tenant write (mutation), không chỉ read.

## Root Cause

GAP-1025 scope chỉ cover enumeration-list + destructive (delete/purge/extend-trial), bỏ sót update mutation + owner-enumeration. `SecurityConfig` có `@EnableMethodSecurity` nhưng 3 method này thiếu annotation; không có gì bind path id/ownerId vào caller.

## Fix (shipped this PR — code layer)

1. **`PUT/PATCH /{id}`**: `@PreAuthorize("hasAnyRole('OWNER','PLATFORM_ADMIN','ADMIN')")` + `TenantOwnershipGuard.requireOwnership(id, X-Tenant-Id)` — path `{id}` (instance id) bound vào gateway-trusted `X-Tenant-Id` (caller's own instance). Owner self-service (mirror `DomainController` GAP-1023 canonical pattern); admin bypass. Cross-tenant → 403.
2. **`GET /owner/{ownerId}`**: `@PreAuthorize(OWNER_AUTHZ)` + new `TenantOwnershipGuard.requireSelfOrAdmin(ownerId, X-User-Id)` — path `{ownerId}` (user id) bound vào gateway-trusted `X-User-Id` (caller's own user id). Axis khác `requireOwnership` (user id, không phải instance id). Cross-user → 403; admin bypass.
3. `X-Tenant-Id` / `X-User-Id` là gateway-injected từ verified JWT (GAP-814 `TenantHeaderGuardFilter`, default-filter `RemoveRequestHeader` strip client-sent value first).

## Acceptance Criteria

- [x] OWNER PUT/PATCH instance của tenant khác → 403 (IT `shouldRejectCrossTenantUpdate` + victim untouched verified)
- [x] OWNER PUT/PATCH instance của chính mình (X-Tenant-Id == id) → 200 (IT `shouldUpdateOwnInstance`)
- [x] PLATFORM_ADMIN PATCH bất kỳ instance (no tenant header) → 200 (IT `shouldAllowAdminPatchAnyInstance`)
- [x] OWNER GET /owner/{otherUser} → 403 (IT `shouldRejectCrossUserEnumeration`)
- [x] OWNER GET /owner/{self} (X-User-Id == ownerId) → 200 (IT `shouldListOwnInstancesByOwner`)
- [x] PLATFORM_ADMIN GET /owner/{any} → 200 (IT `shouldAllowAdminEnumerateAnyOwner`)
- [x] `TenantOwnershipGuard.requireSelfOrAdmin` unit tests (self/cross-user/missing/malformed/admin-bypass) — 4 cases PASS
- [x] `InstanceControllerIntegrationTest` 12/12 + `TenantOwnershipGuardTest` 11/11 + `InstanceApiContractTest` 10/10 PASS local (Testcontainers Postgres, strict-warnings)
- [x] G3 runtime walk on live stack — 2026-06-07 via gateway :9000 (minted HS512 JWT): ownerB PUT instance A cross-tenant → **403**; ownerB GET /owner/{userA} cross-user → **403**; ownerB GET /owner/{self} → **200**; PLATFORM_ADMIN GET /owner/{any} → **200**. Evidence: `documents/04-quality/audits/persona-review/2026-06-07-g3-security-cluster-idor-walk.md` + this closure.

DONE: code-layer fix + tests shipped + verified local; G3 runtime walk (production-equivalent live verify per `pre-handoff-self-test-completeness.md` §3) PASS 2026-06-07.

## Related

- Residual của: GAP-1025 (InstanceController missing @PreAuthorize, DONE)
- Sister: GAP-1023 (DomainController cross-tenant IDOR — same `TenantOwnershipGuard.requireOwnership` pattern), GAP-814 (gateway tenant header strip + inject)
- Branch: `feature/g3-p2-instance-controller-idor`
