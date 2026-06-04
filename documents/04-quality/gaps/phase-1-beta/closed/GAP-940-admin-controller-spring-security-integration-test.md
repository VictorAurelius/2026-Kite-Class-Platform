# GAP-940: Admin controller `@PreAuthorize` MockMvc + Spring Security integration tests

**Status:** 🟢 DONE
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-04 (Wave flow-kh3 G1 walk follow-up — PR #2152 GAP-938 self-flagged)
**Affects:** `kitehub-subscription` 3 admin controllers (AdminEmailController + AdminPaymentController + AdminMigrationController)

## Problem

PR #2152 (GAP-938 fix, merged 8ae3dfe2) replaced legacy `AdminApiKeyInterceptor` (X-Admin-Key header) với Spring Security `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` annotations trên 3 admin controllers:

- `AdminEmailController` — class-level `@PreAuthorize`
- `AdminPaymentController` — 3 methods (confirm/reject/listPending) per-method `@PreAuthorize`
- `AdminMigrationController` — 2 methods (forceConvert/rollback) per-method `@PreAuthorize`

Existing pure Mockito tests instantiate controllers via `@InjectMocks` → bypass Spring AOP proxy → `@PreAuthorize` annotation chỉ fire khi bean go through proxy. Tests PASS dù auth gate có thể broken in production (Wave flow-kh3 G1 walk verified gate hoạt động trực tiếp curl, nhưng CI test layer KHÔNG lock invariant).

Wave flow-kh3 G1 walk verified live stack: direct curl to `subscription:8081/api/platform/admin/payments/<id>/confirm` without `X-User-Roles` → 401; với `X-User-Roles: PLATFORM_ADMIN` → 200. Cần CI-layer test lock invariant để regression post-PR-#2152 catch sớm.

## Proposed Fix

Thêm 3 IT classes `@WebMvcTest` + `@Import(SecurityConfig.class)` + `@MockitoBean` cho downstream services. Pattern theo `RoleGuardMatrixIT` + `PaymentControllerSecurityTest`. Mỗi endpoint × 3 RBAC cases (anonymous 401, non-admin 403, PLATFORM_ADMIN 200/201/202).

## Acceptance Criteria

- [x] `AdminEmailControllerIntegrationTest.java` covers ≥4 endpoints × 3 cases (anonymous/non-admin/PLATFORM_ADMIN)
- [x] `AdminPaymentControllerIntegrationTest.java` covers 3 endpoints × 3 cases (UC-SUB-07)
- [x] `AdminMigrationControllerIntegrationTest.java` covers 2 endpoints × 3+ cases (UC-T2P-02 + UC-T2P-05)
- [x] All tests load `SecurityConfig` via `@Import` để exercise real Spring Security filter chain + AOP advice
- [x] `cd kitehub && ./mvnw -pl kitehub-subscription test -Dtest='Admin*IntegrationTest' -P strict-warnings` PASS (29/29 tests)

## Walk evidence (per feature-ship-runtime-walk-mandate.md §3)

Out-of-scope: tests are not user-facing feature ship; auth invariant verified by `@PreAuthorize` annotation `hasRole('PLATFORM_ADMIN')` matching live `X-User-Roles` header via `XUserRolesHeaderFilter` (per `SecurityConfig`). Wave flow-kh3 G1 walk separately verified live gate on production-equivalent stack (PR #2152 closure evidence).

## Local self-test evidence

```
$ cd kitehub && ./mvnw -pl kitehub-subscription test -Dtest='Admin*IntegrationTest' -P strict-warnings
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0 -- AdminEmailControllerIntegrationTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0 -- AdminPaymentControllerIntegrationTest
[INFO] Tests run:  7, Failures: 0, Errors: 0, Skipped: 0 -- AdminMigrationControllerIntegrationTest
[INFO] Tests run: 29, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## Related

- **Origin PR**: #2152 (GAP-938 admin auth pattern doc-vs-code mismatch fix)
- **Sister precedent**: `RoleGuardMatrixIT.java` (Wave 98 Bucket B7) + `PaymentControllerSecurityTest.java` (Wave 80 Bucket C GAP-562b)
- **Wave**: flow-kh3 (Flow Verification Campaign — KH-3 subscription)
- **Walk source**: Wave flow-kh3 G1 walk live curl verification 2026-06-04

## Log

- **2026-06-04**: Gap filed + closed same PR. 3 IT test classes shipped (29 tests PASS) locking `@PreAuthorize` invariant via Spring AOP proxy (test infrastructure scope, không phải user-facing feature). Per `gap-done-discipline.md` §2 — all 5 AC items verified empirically with `./mvnw test` output; no banned phrases; closing PR contains complete fix. `ADMIN_MERGE_OVERRIDE: GAP-941` trailer cited cho kitehub-admin Spring context-load preexisting still blocks Test Admin Service job (separate scope, GAP-941 covers).
