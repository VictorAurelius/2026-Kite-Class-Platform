# GAP-1333: ImpersonationController 3 endpoint chưa có api-contract.md

**Status:** 🟢 DONE
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-14 (API-contract full audit, AUDIT-2026-06-14-api-contract-full)
**Affects:** `ImpersonationController` (kitehub-subscription) + `documents/01-business/kitehub/admin-audit/api-contract.md`

## Problem

3 endpoint admin impersonation chưa được document ở mức endpoint (path/method/request/response):

- `POST /api/v1/admin/impersonate/{tenantSlug}` (`ImpersonationController.java:64`)
- `POST /api/v1/admin/impersonate/end` (`ImpersonationController.java:83`)
- `GET /api/v1/admin/impersonate/audit-log` (`ImpersonationController.java:92`)

`admin-audit/api-contract.md:173` chỉ nhắc `IMPERSONATE` như một audit action-type trong bảng audit-log, KHÔNG document endpoint contract. Vi phạm Cat 1.1 — admin operator + compliance team không có request/response contract cho impersonation flow (scoped read-only token issuance + end + audit query).

## Proposed Fix

Thêm section impersonation vào `admin-audit/api-contract.md` (hoặc tạo `documents/01-business/kitehub/impersonation/`): document 3 endpoint với scoped-JWT response shape, RBAC gate (PLATFORM_ADMIN), audit-log entry contract, TTL của impersonation session.

## Acceptance Criteria

- [ ] 3 endpoint impersonate documented (path/method/request/response/error-codes)
- [ ] RBAC + scoped read-only token semantics documented
- [ ] Cross-ref audit-log capture (IMPERSONATE action row)

## Resolution

🟢 DONE (2026-06-15, branch `fix/audit-fixC-apidocs-2026-06-14`). Thêm section "Impersonation endpoints (GAP-1333)" vào `documents/01-business/kitehub/admin-audit/api-contract.md`: document 3 endpoint `POST /api/v1/admin/impersonate/{tenantSlug}` + `POST /end` + `GET /audit-log` (path/method/request/response/error 403/404/400). Đã ghi: RBAC `@PreAuthorize(hasRole('PLATFORM_ADMIN'))` + gateway `X-User-Id`/`X-User-Roles`, scoped read-only JWT 30s TTL (claim `tenant_id` + `impersonated_by`), response shapes (`ImpersonationStartResponse`/`EndResponse`/`AuditEntryDto`), cross-ref `IMPERSONATE` audit-row (sensitive-actions table).

AC: cả 3 ✅.

## Related

- Discovered in: `documents/04-quality/audits/api-contract/2026-06-14-api-contract-full-audit.md` B2
- Related: GAP-967 (admin view-as-tenant UI), GAP-589 (impersonate-read-only debug path), GAP-1029 (admin audit completeness)
