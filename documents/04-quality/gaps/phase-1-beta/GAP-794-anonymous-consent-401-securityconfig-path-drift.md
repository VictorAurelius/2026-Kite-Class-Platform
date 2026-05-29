# GAP-794 — Anonymous PDPL consent endpoints trả 401 do SecurityConfig path drift

**Status:** PARTIAL
**Priority:** P1
**Type:** Backend (security / PDPL compliance)
**Phase:** phase-1-beta
**Created:** 2026-05-28
**Last-verified:** 2026-05-28
**Wave:** Wave Phase 2 Beta Wave A
**Service:** kitehub-subscription

---

## Problem

Các endpoint consent ẩn danh (anonymous) phục vụ PDPL cookie consent banner trả về HTTP **401 Unauthorized** thay vì cho phép visitor chưa đăng nhập gọi. Xác nhận bằng curl trực tiếp tới `kitehub-subscription` (bypass gateway) → vẫn 401, chứng minh lỗi nằm ở **SecurityConfig của service**, không phải gateway.

Đây là lỗi PDPL-compliance đã được coordinator confirm. Banner-stage visitor không có JWT, nên 401 chặn hoàn toàn luồng ghi nhận đồng ý cookie → vi phạm yêu cầu thu thập consent của PDPL 2023.

## Root Cause

`kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/config/SecurityConfig.java` (dòng 112-113 trước fix) khai báo:

```java
.requestMatchers("/api/v1/consent/cookie").permitAll()
.requestMatchers("/api/v1/consent/cookie/**").permitAll()
```

Nhưng `/api/v1/consent/cookie` là **endpoint KHÔNG TỒN TẠI**. `ConsentController` (`@RequestMapping("/api/v1/consent")`, Wave 25 GAP-353b, javadoc ghi rõ "Auth: unauthenticated by design — visitor_id is pseudonymous and banner-stage visitors aren't logged in") thực tế route:

- `POST /api/v1/consent/record` — idempotent upsert theo visitorId
- `GET  /api/v1/consent/{visitorId}` — query state (visitorId là UUID PathVariable)
- `POST /api/v1/consent/{visitorId}/revoke` — revoke flow

Không endpoint nào khớp `/consent/cookie` permitAll → cả ba rơi xuống `anyRequest().authenticated()` default-deny (GAP-552) → **401 cho anonymous visitor**.

Đây là **path-naming drift**: GAP-558 (Wave 79) thêm permitAll cho path `"cookie"` nhưng controller dùng `"record"` / `"{visitorId}"`. Bug class = "SecurityConfig permitAll matcher trỏ vào path KHÔNG có controller endpoint tương ứng" (dead allowlist) + chiều ngược "endpoint public-by-design KHÔNG được phủ bởi permitAll → 401".

## Proposed Fix

Thay 2 matcher chết bằng các matcher HttpMethod-specific (least-privilege) trỏ vào endpoint thật:

```java
.requestMatchers(HttpMethod.POST, "/api/v1/consent/record").permitAll()
.requestMatchers(HttpMethod.GET,  "/api/v1/consent/*").permitAll()       // GET /{visitorId}
.requestMatchers(HttpMethod.POST, "/api/v1/consent/*/revoke").permitAll()
```

- Loại bỏ `/consent/cookie` + `/consent/cookie/**` (đã grep toàn codebase — không có handler nào dùng `/consent/cookie`).
- KHÔNG mở `/api/v1/consent/v2/**` cho anonymous: `ImmutableConsentController` (`/api/v1/consent/v2`) keyed theo `userId` = consent của authenticated user theo design, mỗi method có `@PreAuthorize("@consentAuthz.canAccessUser(...)")`. Matcher `GET /consent/*` (1 segment) KHÔNG khớp `/consent/v2/{userId}` (2 segment) → v2 vẫn default-deny + method-level guard (defense in depth).

### Secondary hardening (đã bundle — clean)

`GET /api/v1/consent/{visitorId}` trả HTTP 500 (không phải 400) khi visitorId không phải UUID hợp lệ (`MethodArgumentTypeMismatchException` chưa được handle). Đã thêm `@ExceptionHandler(MethodArgumentTypeMismatchException.class)` → 400 vào `GlobalExceptionHandler` (RFC 7807 ProblemDetail). Áp dụng cho mọi controller trong service.

## Acceptance Criteria

- [x] SecurityConfig: 3 matcher HttpMethod-specific cho real consent endpoints; dead `/consent/cookie` matchers đã xóa
- [x] `/api/v1/consent/v2/**` vẫn authenticated (không bị mở nhầm)
- [x] SecurityConfigTest mirror chain + assertions: anonymous `POST /consent/record`, `POST /consent/{id}/revoke`, `GET /consent/{id}` đều NOT 401; `GET /consent/v2/{id}` vẫn 401 (25/25 tests PASS)
- [x] Secondary: `MethodArgumentTypeMismatchException` → 400 (không còn 500) cho non-UUID path var
- [x] Module unit/MockMvc tests PASS (SecurityConfigTest 25/25 + GlobalExceptionHandlerDataIntegrityTest 2/2)
- [ ] **flow-5 RST walk verification** trên shared stack (coordinator) — live anonymous POST/GET với Postgres + RLS để xác minh end-to-end (ConsentControllerIT cần Testcontainers/Postgres; H2 thiếu `set_config` RLS function nên IT không chạy được local)

## Log

- **2026-05-28:** Gap filed + fix shipped (PARTIAL). SecurityConfig path-drift fixed (dead `/consent/cookie` → 3 real HttpMethod matchers). Cross-flow sweep thực hiện (xem PR body §Cross-flow sweep): tìm thấy 2 dead matcher sister (`/api/v1/public-config/**`, `/api/v1/payments/webhook` — controller thực ở `/api/platform/**`) → DEFER follow-up (xem dưới). Secondary 500→400 nit bundled. Tests: SecurityConfigTest 25/25 PASS, GlobalExceptionHandlerDataIntegrityTest 2/2 PASS. KHÔNG flip DONE — flow-5 RST walk live trên shared stack do coordinator thực hiện (per `feature-ship-runtime-walk-mandate.md` + `pre-handoff-self-test-completeness.md` §2.2 anonymous flow).

### Follow-up (DEFER từ cross-flow sweep)

2 dead allowlist matcher cùng bug class, scope khác (platform-namespace routing, KHÔNG phải v1-consent) — DEFER, cần gap riêng:
- `/api/v1/public-config/**` permitAll nhưng controller là `@RequestMapping("/api/platform/config")` (`PublicConfigController.getPublicConfig` = `/api/platform/config/public`).
- `/api/v1/payments/webhook` + `/webhook/**` permitAll nhưng controller là `@RequestMapping("/api/platform/webhooks/payment")` (`PaymentWebhookController`).

→ `/api/platform/**` không có permitAll entry nào trong SecurityConfig; nếu các path này phục vụ trực tiếp (không qua gateway rewrite) thì cũng default-deny. Cần điều tra gateway routing trước khi fix (có thể gateway rewrite `/api/v1/...` → `/api/platform/...`). Out-of-scope cho consent PR này.
