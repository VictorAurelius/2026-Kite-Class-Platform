# GAP-1023: Domain endpoints cross-tenant IDOR (ownership binding chưa enforce)

**Status:** 🟡 PARTIAL
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-06-06 (KH-7 custom domain G1 walk)
**Affects:** `DomainController` + `DomainService` + gateway (kitehub-subscription, kitehub-gateway)

## Problem

KH-7 G1 walk: domain endpoints `/api/instances/{id}/domain` dùng `{id}` (instanceId) làm path variable mà **không bind ownership** với caller. Walk evidence (trước fix): owner.test GET domain của instance khác → 200; **DELETE domain của instance khác → 204** (xoá cross-tenant, destructive). `DomainController` ban đầu có **ZERO @PreAuthorize** — bất kỳ authenticated user (mọi role) thao tác được mọi instance's domain. Tệ hơn KH-5 (GAP-1015) / KH-6 (GAP-1019) vốn ít nhất có role gate.

## Current State (verified 2026-06-06)

- **PARTIAL FIX (this wave):** thêm `@PreAuthorize(OWNER_AUTHZ)` cho cả 4 endpoint (initiate/verify/delete/get) — chặn non-owner role (STAFF/TEACHER/...). Defense-in-depth.
- **CÒN OPEN:** cross-tenant ownership binding — Owner A vẫn thao tác được domain của Owner B (cùng class GAP-1015/1019, cần gateway tenant-identity propagation). `DomainService` load instance theo `{id}` không check caller tenant.

## Root Cause

Gateway không forward/validate caller tenant (JWT `tenantId`) cho `/api/instances/**`; service tin path `{id}` không verify ownership. Systemic — chung root với GAP-1015 (subscription) + GAP-1019 (branding).

## Proposed Fix

1. Gateway forward JWT `tenantId` thành trusted header (shared fix GAP-1015/1019).
2. `DomainService` verify `{id}` instance thuộc caller tenant (bypass PLATFORM_ADMIN/ADMIN).

## Acceptance Criteria

- [x] Non-owner role → 403 (defense-in-depth, DONE this wave)
- [ ] Owner A GET/set/delete domain của Owner B instance → 403
- [ ] PLATFORM_ADMIN/ADMIN vẫn thao tác mọi instance
- [ ] IT cover cross-tenant 403 trên 4 domain endpoint

## Related

- Discovered in: KH-7 G1 walk — `documents/04-quality/audits/persona-review/2026-06-06-pre-walk-kh7-domain-management.md` (FM-1)
- Sister (cùng gateway-tenant-bind root): GAP-1015 (subscription IDOR), GAP-1019 (branding IDOR)
