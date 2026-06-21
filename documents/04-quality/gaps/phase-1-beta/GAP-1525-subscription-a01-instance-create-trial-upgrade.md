# GAP-1525: kitehub-subscription A01 — unguarded instance-create + trial→paid upgrade (GAP-1491 residual)

**Status:** 🟡 PARTIAL
**Priority:** 🔴 P0
**Domain:** Backend (security)
**Found:** 2026-06-22 (loop security audit — OWASP A01 sweep continuing GAP-1491)
**Affects:** `kitehub/kitehub-subscription` — 2 endpoints

## Problem

OWASP A01 (Broken Access Control). The GAP-1491 cluster (kiteclass-core financial/admin controllers) did not cover kitehub-subscription. Two endpoints were missing method-level guards:

| Sev | Endpoint | Issue |
|---|---|---|
| 🔴 P0 | `POST /api/platform/instances/{id}/upgrade` (`TrialToPaidController.upgrade`) | Zero `@PreAuthorize` + no ownership check → any authenticated user could drive **any tenant's** trial→paid upgrade (tier + billing mutation). |
| 🟠 P1 | `POST /api/platform/instances` (`InstanceController.createInstance`) | No guard while every sibling (`GET` list, `DELETE`, `extend-trial`, `purge`) has `hasAnyRole('PLATFORM_ADMIN','ADMIN')` → any authenticated user could create trial instances. |

The platform-admin self-service path `POST /api/platform/instances/register` is intentionally public (signup) — left unguarded by design.

## Root Cause

GAP-1491 scoped its A01 sweep to kiteclass-core controllers only. kitehub-subscription's `SecurityConfig` already carries `@EnableMethodSecurity`, so endpoints lacking `@PreAuthorize` were enforced as "any authenticated user" rather than role/owner-scoped.

## Fix shipped

- `InstanceController.createInstance` → `@PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ADMIN')")` (matches siblings).
- `TrialToPaidController.upgrade` → `@PreAuthorize("hasAnyRole('OWNER','PLATFORM_ADMIN','ADMIN')")` + `@RequestHeader X-Tenant-Id` + `TenantOwnershipGuard.requireOwnership(id, tenantHeader)` — an OWNER may upgrade only their own instance; platform admins bypass; cross-tenant → 403.
- New `TrialAndInstanceAuthzSecurityTest` (`@WebMvcTest` + real `SecurityConfig`) — 9 cases: STAFF/OWNER → 403 on create, PLATFORM_ADMIN → 201; wrong-role → 403, OWNER-own-instance → 202, OWNER-cross-tenant → 403 (IDOR), PLATFORM_ADMIN bypass → 202, anonymous → 401.

## Acceptance Criteria

- [x] Both endpoints role-guarded; upgrade additionally ownership-checked
- [x] Authz test (allow-right-role + deny-wrong-role + deny-cross-tenant + anonymous-401)
- [ ] CI green on PR + runtime G2 walk (per `feature-ship-runtime-walk-mandate.md`) — *PR pending*

## Related

- **Parent:** GAP-1491 (DONE) — kiteclass-core A01 cluster; this is the kitehub-subscription residual
- Sister (same audit): GAP-1526 (kitehub-branding A01), GAP-1527 (kiteclass-core A01 residual)
- Discovered during GAP-1523/GAP-1524 loop session; per `discovery-to-gap-inline-filing.md`

## Log

- 2026-06-22 — Filed + fixed (→ PARTIAL). Loop security audit OWASP A01 sweep. Guards + ownership check + `TrialAndInstanceAuthzSecurityTest` shipped. PARTIAL until PR CI green.
