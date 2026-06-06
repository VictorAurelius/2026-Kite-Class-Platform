# GAP-1015: Subscription lifecycle endpoints thiếu ownership binding — IDOR cross-tenant

**Status:** 🟢 DONE
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-06-06 (KH-5 subscription downgrade/cancel/renew G1 walk)
**Closed:** 2026-06-06 (Wave security-2 Bucket B — `TenantOwnershipGuard` ownership binding)
**Affects:** `SubscriptionController` + `SubscriptionService` + `SubscriptionRenewalService` (kitehub-subscription)

## Problem

KH-5 G1 walk (live, gateway :9000) phát hiện **cross-tenant IDOR** trên toàn bộ subscription lifecycle endpoints. `@PreAuthorize(OWNER_AUTHZ)` = `hasAnyRole('OWNER','PLATFORM_ADMIN','ADMIN')` chỉ check ROLE, KHÔNG check ownership — controller nhận đúng `UUID id` của subscription, không bao giờ verify subscription đó thuộc về tenant/instance của caller.

Walk evidence: Owner `owner.test@test.vn` (JWT `tenantId=22003e3c…`) thao tác trên subscription thuộc instance `ad0fa96e…` (instance KHÁC):
- `GET /api/platform/subscriptions/{other-sub}` → **HTTP 200** (đọc được)
- `DELETE /api/platform/subscriptions/{other-sub}?immediate=true` → **HTTP 204** (huỷ được)
- Log xác nhận: `[tenant=22003e3c-…] Cancelled subscription ed63ef19-…` (sub thuộc instance ad0fa96e)

Bất kỳ OWNER nào có thể GET / downgrade / cancel / renew subscription của tenant khác chỉ bằng cách đoán/biết subscription UUID. OWASP A01 Broken Access Control, cross-tenant — P0.

Cùng class: `POST /api/platform/subscriptions` create cũng nhận `instanceId` trong body mà không verify caller sở hữu instance đó (walk tạo được sub cho instance không thuộc owner.test → HTTP 201).

## Root Cause

Endpoints under `/api/platform/subscriptions/**` chỉ được gateway inject `X-User-Id` + `X-User-Roles` (KHÔNG có `X-Tenant-Id` — filter `TenantResolverGatewayFilterFactory` chỉ chạy trên subdomain-route, không trên platform-route). JWT có claim `tenantId` (= instance id) nhưng `JwtAuthenticationGatewayFilter` hiện chỉ forward `X-User-Id`/`X-User-Roles`/`X-User-Email`/`referenceId`, KHÔNG forward `tenantId`. Service-layer không có ownership check.

## Proposed Fix

1. Gateway `JwtAuthenticationGatewayFilter`: forward JWT `tenantId` claim thành header (vd `X-User-Tenant-Id`), mirror pattern hiện có cho `X-User-Id`.
2. `SubscriptionController` / `SubscriptionService`: với GET/downgrade/cancel/renew/upgrade + create, verify `subscription.getInstanceId()` (hoặc `request.getInstanceId()` cho create) khớp với caller tenant từ header. Bypass cho `PLATFORM_ADMIN`/`ADMIN` (quản lý mọi instance).
3. Pattern tham khảo: `StaffInvitationController` đã scope bằng `@RequestHeader("X-Tenant-Id")` — dùng cùng cơ chế.
4. Áp dụng cho cả `upgradeSubscription` (KH-4 đã ship cùng `OWNER_AUTHZ`, cùng lỗ hổng).

## Acceptance Criteria

- [x] Owner A GET/downgrade/cancel/renew subscription của Owner B → 403 (không phải 200/204) — `TenantOwnershipGuard.requireOwnership` bind subscription.instanceId vs trusted X-Tenant-Id (controller pre-loads via getSubscription để guard TRƯỚC mutation)
- [x] PLATFORM_ADMIN/ADMIN vẫn thao tác được mọi subscription — admin bypass via SecurityContext authority
- [x] create subscription cho instance không thuộc caller → 403 — bind request.getInstanceId() vs X-Tenant-Id
- [x] Cross-tenant 403 + same-tenant 200 + admin-bypass tested — `SubscriptionTenantOwnershipTest` @WebMvcTest (real SecurityConfig method-security chain) + `TenantOwnershipGuardTest` unit. NOTE: guard là controller-layer (so sánh header, không query DB) → @WebMvcTest với real security chain là test layer đúng; Testcontainers Postgres không thêm coverage cho header-compare logic.

## Resolution (Wave security-2 Bucket B, 2026-06-06)

**Fix-time state-check (per `audit-to-gap-pipeline.md` §2.8):** Root Cause "gateway không forward tenantId" outdated — gateway `TenantHeaderGuardFilter` ĐÃ inject trusted `X-Tenant-Id` từ JWT `tenantId` claim trên mọi non-public route + strip client-sent value. **KHÔNG cần gateway change.** Fix = service-layer ownership binding (`TenantOwnershipGuard`) dùng header sẵn có, theo precedent StaffInvitationController.

Also restricted global `GET /expiring` (returned ALL tenants' expiring subs to any OWNER — list-all leak, sister of this IDOR class per `cross-flow-bug-class-sweep.md`) → `@PreAuthorize` admin-only. FE không dùng endpoint này (verified grep).

## Related

- Discovered in: KH-5 G1 walk — `documents/04-quality/audits/persona-review/2026-06-06-pre-walk-kh5-subscription-lifecycle.md` (FM-1)
- Sister authz gaps: GAP-1005 (InvoiceController authz, KC-7), GAP-1007 (parent role-collision IDOR, KC-8)
- Same authority-bridge family: GAP-1003 (gateway X-User-Roles→Spring authority, KC-7 DONE)
