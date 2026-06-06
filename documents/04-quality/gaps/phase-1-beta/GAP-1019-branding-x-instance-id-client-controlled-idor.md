# GAP-1019: Branding X-Instance-Id client-controlled → cross-tenant IDOR

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-06-06 (KH-6 AI Branding wizard G1 walk)
**Affects:** `BrandingJobController` + `AIBrandingController` + gateway (kitehub-branding, kitehub-gateway)

## Problem

KH-6 G1 walk: branding endpoints xác định tenant scope qua header `X-Instance-Id` do **client tự gửi**, gateway KHÔNG inject/strip/validate header này (chỉ xử lý `X-Tenant-Id` + `X-User-Id/Roles`). `BrandingJobController.createJob` dùng `@RequestHeader("X-Instance-Id") UUID instanceId` trực tiếp + `@PreAuthorize(OWNER_AUTHZ)` chỉ check ROLE, không bind ownership.

Hệ quả: Owner A gửi `X-Instance-Id: <instance của Owner B>` → tạo/đọc branding job + assets cho tenant khác. Cross-tenant IDOR (OWASP A01). Cùng class với GAP-1015 (subscription lifecycle IDOR) — gateway chưa enforce tenant ownership cho platform routes.

Walk evidence: createJob 201 với X-Instance-Id tùy ý owner gửi (không verify khớp JWT tenantId). Affects mọi branding endpoint nhận X-Instance-Id (jobs create/get/assets, ai/generate-*, regenerate).

## Root Cause

Gateway `JwtAuthenticationGatewayFilter` forward `X-User-Id/Roles/Email` nhưng KHÔNG forward/validate `X-Instance-Id`; branding tin tưởng client header. `TenantHeaderGuardFilter` chỉ re-inject `X-Tenant-Id` từ JWT cho subdomain routes, không cho `/api/platform/branding/**`.

## Proposed Fix

1. Gateway: forward JWT `tenantId` (= instance id cho owner) thành trusted header + strip client-sent `X-Instance-Id` (giống `RemoveRequestHeader=X-Tenant-Id` pattern).
2. Branding: verify `X-Instance-Id` khớp gateway-trusted tenant (bypass PLATFORM_ADMIN/ADMIN). Hoặc derive instanceId từ trusted tenant header thay vì client.
3. Shared fix với GAP-1015 — cùng gateway tenant-identity propagation mechanism.

## Acceptance Criteria

- [ ] Owner A gửi X-Instance-Id của Owner B → 403 (không tạo/đọc được)
- [ ] PLATFORM_ADMIN/ADMIN vẫn thao tác mọi instance
- [ ] Gateway strip client-sent X-Instance-Id, derive từ JWT
- [ ] IT cover cross-tenant 403 trên branding job create/get/assets

## Related

- Discovered in: KH-6 G1 walk — `documents/04-quality/audits/persona-review/2026-06-06-pre-walk-kh6-ai-branding-wizard.md` (FM-1)
- Sister: GAP-1015 (subscription lifecycle IDOR — cùng gateway tenant-bind root); GAP-1007 (KC-8 IDOR)
