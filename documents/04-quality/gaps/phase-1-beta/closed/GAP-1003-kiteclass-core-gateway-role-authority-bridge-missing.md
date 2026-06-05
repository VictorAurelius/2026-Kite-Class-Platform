# GAP-1003: kiteclass-core thiếu gateway X-User-Roles → Spring authority bridge (24 endpoints dead-deny)

**Status:** 🟢 DONE
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-06-05 (KC-7 invoice→payment G1 walk, production-equivalent stack)
**Affects:** kiteclass-core — mọi `@PreAuthorize("hasRole(...)")` / `hasAnyRole(...)` (24 endpoints / 10 controller) + `AuthorizationBean.isAdmin()`

## Problem

KC-7 G1 walk (record payment trên invoice 28, tenant sky) bắt P0: `POST /api/v1/invoices/{id}/record-payment` (`@PreAuthorize("hasAnyRole('TEACHER','ADMIN','OWNER','PLATFORM_ADMIN')")`) trả **HTTP 403 ACCESS_DENIED** với MỌI format header `X-User-Roles` (OWNER / ROLE_OWNER / TEACHER / "OWNER,ADMIN"). Đối chứng: `GET /api/v1/invoices/{id}` (KHÔNG @PreAuthorize) trả 200.

**Root cause:** kiteclass-core KHÔNG có filter convert gateway header `X-User-Roles` → Spring Security `GrantedAuthority`. `SecurityConfig` = `.anyRequest().permitAll()` + `@EnableMethodSecurity`, nhưng `SecurityContextHolder` luôn rỗng (grep toàn core main: 0 site dựng `Authentication`). Gateway `JwtAuthenticationGatewayFilter` chỉ forward header thô; core không đọc cho Spring auth → mọi `hasRole`/`hasAnyRole` deny.

**Blast radius:** 24 `hasRole`/`hasAnyRole` @PreAuthorize trên 10 controller dead-deny (payment-record, marketing Lead/Contact/Landing, document-gen, report, payroll, settings/BrandingVersion, parent/ParentConsentAdmin). PLUS `AuthorizationBean.isAdmin()` (đọc `SecurityContextHolder` authorities) dead → trên KC-5 attendance / KC-6 grade đường admin/owner override gãy thầm lặng (chỉ teacher-ownership DB path chạy — lý do 2 walk PASS với persona TEACHER).

**Tại sao IT mù:** `SecurityConfig` `@Profile("!test")`; test dùng `TestSecurityConfig` + `@WithMockUser` (set authorities sẵn) → @PreAuthorize PASS trong IT, 403 trên production gateway-headers. Cùng class bài học KC-5/KC-6 (IT mù schema-drift); lần này auth-context-drift.

**Sister precedent:** kitehub-subscription đã có `XUserRolesHeaderFilter` (GAP-706 / GAP-783) làm đúng việc này — kiteclass-core chưa từng được cấp filter tương đương.

## Root Cause

Missing pre-authentication filter trong kiteclass-core. Gateway resolves identity (JWT) + forwards `X-User-Id`/`X-User-Roles`/`X-User-Email`; downstream service phải tự dựng `Authentication` từ header để method-security hoạt động. Subscription có; core không.

## Proposed Fix

Thêm `GatewayHeaderAuthenticationFilter extends OncePerRequestFilter`: đọc `X-User-Roles` → split comma → map `ROLE_<role>` (uppercase, không double-prefix) → `UsernamePasswordAuthenticationToken(principal=X-User-Id, authorities)` vào `SecurityContextHolder`. Wire `http.addFilterBefore(..., UsernamePasswordAuthenticationFilter.class)` trong `SecurityConfig`. Mirror sister `XUserRolesHeaderFilter`.

## Acceptance Criteria

- [x] Filter dựng authorities từ `X-User-Roles` (unit test 7 case: single/comma/prefixed/lowercase/no-header/blank/missing-userId) — PASS
- [x] `SecurityConfig` wire filter trước `UsernamePasswordAuthenticationFilter`
- [x] Live re-walk (kiteclass-core rebuilt, kiteclass_shared DB): `POST /invoices/28/record-payment` với `X-User-Roles: OWNER` → **HTTP 201** (was 403)
- [x] Re-walk regression: `GET /invoices/28` vẫn **200**; cross-tenant `GET /invoices/1` (other tenant) → **404** (RLS giữ nguyên)

## Log

- **2026-06-05** Wave flow-kc7 — `GatewayHeaderAuthenticationFilter` shipped (mirror subscription `XUserRolesHeaderFilter`) + wired `SecurityConfig.addFilterBefore(..., UsernamePasswordAuthenticationFilter.class)` + 7-case unit test PASS. Rebuild kiteclass-core (image kiteclass-core:latest, profile full, kiteclass_shared DB). Live re-walk: record-payment OWNER 500k → 201 (status SENT→PARTIAL), +1M → 201 (PARTIAL→PAID, balance 0); GET invoice 200; cross-tenant GET → 404. Verified on production-equivalent stack. DONE.

## Related

- Discovered in: KC-7 G1 walk session 2026-06-05; artifact `documents/04-quality/audits/persona-review/2026-06-05-pre-walk-kc7-invoice-payment.md` §G1
- Sister: GAP-706 + GAP-783 (kitehub-subscription XUserRolesHeaderFilter), GAP-728 (TestSecurityConfig @PreAuthorize NO-OP)
- Unblocks: KC-7 functional walk (verified PASS) + 23 other dead-deny endpoints (marketing/document/report/payroll/branding-version/parent-consent-admin)
- Follow-up findings (KC-7 walk): GAP-1004 over-payment no-clamp, GAP-1005 InvoiceController missing @PreAuthorize
