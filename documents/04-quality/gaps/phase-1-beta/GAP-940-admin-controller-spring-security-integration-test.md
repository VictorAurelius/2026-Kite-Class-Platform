# GAP-940: Admin controller Spring Security integration test (@PreAuthorize bypass in Mockito-only tests)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-04 (PR #2152 GAP-938 fix author self-flagged)
**Affects:** `kitehub-subscription` admin controllers — `AdminEmailController`, `AdminPaymentController`, `AdminMigrationController`

## Problem

PR #2152 (GAP-938 fix) thay legacy `AdminApiKeyInterceptor` (X-Admin-Key) bằng Spring Security `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` ở 3 admin controller classes. Author tự flag trong PR body:

> Tests pure Mockito instantiate controller trực tiếp → @PreAuthorize bypass. Cần MockMvc + Spring Security integration test layer verify auth round-trip (defer Phase 2 hoặc dedicated wave).

Hậu quả: pure Mockito test (vd `AdminPaymentControllerTest`, `AdminMigrationControllerTest`) construct controller bằng `@InjectMocks` thay vì Spring context → `@PreAuthorize` annotation không được Spring AOP wrap → tests PASS dù auth thật sự broken trong production. Đây là **trust-pass anti-pattern** mà `.claude/rules/feature-ship-runtime-walk-mandate.md` warn.

## Root Cause

Mockito unit test instantiate controller object trực tiếp, bypass Spring proxy layer mà `@PreAuthorize` cần để intercept. Spring Security `@EnableMethodSecurity` chỉ apply khi:
1. Bean được tạo qua Spring context (không phải `new Controller()` hay `@InjectMocks`)
2. Spring Security filter chain active trong test context
3. AOP proxy wrap method call

Mockito-only test thiếu cả 3 → annotation = inert metadata, không enforce gì.

## Proposed Fix

Add `@SpringBootTest` + `@AutoConfigureMockMvc` + `@WithMockUser(roles="PLATFORM_ADMIN")` / `@WithMockUser(roles="OWNER")` integration test cho mỗi admin controller, verify:

1. Anonymous request → HTTP 401 (no JWT)
2. Authenticated user without PLATFORM_ADMIN role → HTTP 403
3. PLATFORM_ADMIN role → HTTP 200/201 + correct response shape
4. Role-guard accepts seeded `ROLE_PLATFORM_ADMIN` literal (verify BE seed + FE guard match per `pre-handoff-self-test-completeness.md` §2.4)

Reference pattern: `kitehub-subscription` đã có `MagicLinkCacheControlIntegrationTest` dùng MockMvc — copy infrastructure setup.

Effort estimate: ~2-3h (3 controllers × ~5 test cases each + 1 fixture setup).

## Acceptance Criteria

- [ ] `AdminEmailControllerIntegrationTest` mới: anonymous 401 / non-admin 403 / admin 200 cho ≥2 endpoints
- [ ] `AdminPaymentControllerIntegrationTest` mới: same 3 cases cho confirm/reject/getPending
- [ ] `AdminMigrationControllerIntegrationTest` mới: same 3 cases cho 2 admin endpoints
- [ ] CI job "Test KiteHub Subscription Service" PASS without ADMIN_MERGE_OVERRIDE trailer (depends on GAP-937 closure first)
- [ ] Update `api-contract.md` Admin endpoints section nếu phát hiện auth shape drift

## Related

- Triggered by: PR #2152 (GAP-938 admin auth migration) author self-flag
- Blocks: full confidence in admin auth coverage (current state = annotation present but untested)
- Depends on: GAP-937 closure (else CI fails on unrelated tests, masking new test signal)
- Rule cite: `feature-ship-runtime-walk-mandate.md` §3 walk evidence per AC
- Rule cite: `pre-handoff-self-test-completeness.md` §2.4 admin-flow checklist (a) role match BE seed + FE guard
- Sister precedent: `MagicLinkCacheControlIntegrationTest` pattern in same module
