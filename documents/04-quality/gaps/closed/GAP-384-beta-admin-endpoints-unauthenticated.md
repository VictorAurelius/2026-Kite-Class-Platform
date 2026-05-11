# GAP-384: Beta admin endpoints (`/api/v1/admin/beta-requests/*/approve|reject`) thiếu @PreAuthorize

**Status:** 🟢 DONE 2026-05-08 (Wave 35 Bucket A)
**Priority:** 🔴 P0 BLOCKING — Phase 1 launch chặn cho đến khi resolved
**Domain:** Backend / Security
**Found:** 2026-05-07 (Security /100 audit Wave 33 — agent a24fe574)
**Affects:** `kitehub-subscription` BetaAccessController approve/reject/list — production beta tenant invite flow

## Problem

`/api/v1/admin/beta-requests/{id}/approve` + `/reject` + `GET /admin/beta-requests` — 3 admin endpoints **không có authentication guard nào**. Controller javadoc nói "Admin endpoints expect a coordinator role guarded at the gateway / Spring Security configuration level" nhưng:

1. Gateway routing scope mismatch: `/api/v1/admin/**` (controller path) vs `/api/platform/admin/**` (gateway routes to kitehub-admin). Beta controller ở kitehub-subscription, KHÔNG match gateway admin route.
2. KHÔNG có `@PreAuthorize` / `SecurityFilterChain` rule nào trên các endpoints này trong kitehub-subscription.

**Impact:** Bất kỳ unauth user nào có thể `POST /api/v1/admin/beta-requests/123/approve` → cấp invite token → bypass beta tenant gating hoàn toàn.

## Root Cause

Wave 33 Bucket C (PR #?) ship controller mà chỉ rely on javadoc convention "guarded at gateway level" — nhưng gateway route mismatch + Spring Security không enforce ở subscription module. `DsarController` reference trong javadoc đang hoạt động vì nó được serve qua kitehub-admin module.

## Proposed Fix

**Option A (preferred, 1h):** Add `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` directly trên 3 admin endpoints. Yêu cầu kitehub-subscription enable Spring Security method-level annotations.

**Option B (more invasive):** Move admin endpoints sang kitehub-admin module để align với gateway routing convention.

Recommend Option A cho tốc độ + isolation.

## Acceptance Criteria

- [x] Add `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` trên `BetaAccessController.listRequests` + `approve` + `reject`
- [x] Enable `@EnableMethodSecurity` trong kitehub-subscription Spring config
- [x] Integration test: unauth POST to `/admin/beta-requests/1/approve` → 401/403
- [x] Integration test: PLATFORM_ADMIN role → 200
- [x] Update controller javadoc: "Guarded at controller level via @PreAuthorize" (replace gateway-only assertion)
- [x] Update `documents/01-business/kitehub/beta-access/api-contract.md` (nếu tồn tại) với auth requirement — verified: file documents `Bearer + role PLATFORM_ADMIN` + `@PreAuthorize` cross-ref + 401/403 error codes for all 3 admin endpoints (lines 124/146/163)

## Related

- Source audit: `documents/04-quality/audits/security/2026-05-07-wave-33-beta-deploy.md` (Finding #1)
- Parent gap: GAP-372 (beta tenant invite mechanism — Wave 33)
- Pattern: GAP-308 P3 RBAC unauthorized-403 (recurring "endpoints shipped before guards")
- Memory: `feedback_release_1_first_session_priority.md` — Phase 1 trigger gates require 0 P0 incidents

## Log

- **2026-05-08** (Wave 35 Bucket A — PR pending) Closed by adding `spring-boot-starter-security` to `kitehub-subscription`, new `SecurityConfig` (`@EnableMethodSecurity`, `@EnableWebSecurity`, profile-aware filter chains, `XUserRolesHeaderFilter` translating gateway-forwarded `X-User-Id` + `X-User-Roles` headers into Spring authorities, `HttpStatusEntryPoint(401)` for anonymous-on-protected paths), `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` on `BetaAccessController#listRequests/approve/reject`, controller javadoc updated, `GlobalExceptionHandler` extended with explicit `AuthorizationDeniedException`/`AuthenticationException` mappings (so 403/401 don't fall through to the catch-all 500). Tests rewritten to `@WebMvcTest + @Import(SecurityConfig.class)` with `@WithMockUser(roles="PLATFORM_ADMIN")` + `@WithAnonymousUser` covering 401 (anon list), 401 (anon approve), 403 (TENANT_USER reject) plus PLATFORM_ADMIN happy-paths. `InstanceApiContractTest` extended `@Import(SecurityConfig.class)` so existing `/api/platform/instances/**` permitAll path stays green. Verification artifact: `mvn -pl kitehub-subscription verify` → 431 tests, 0 failures.
- **2026-05-07** Filed from Security /100 audit Wave 33. State-check: 0 existing gaps cover this finding (grep `PreAuthorize|admin.*beta` returned 5 unrelated files). Confirmed hardcoded path mismatch via `BetaAccessController.java:120-148` read.
