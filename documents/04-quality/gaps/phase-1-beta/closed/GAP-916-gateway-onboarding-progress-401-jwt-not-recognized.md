# GAP-916: Gateway → 401 cho `/api/v1/onboarding-progress` dù JWT hợp lệ

**Status:** 🟢 DONE
**Closed:** 2026-06-03 (Wave flow-kh2 Bucket B fix)
**Priority:** 🔴 P0
**Domain:** Backend (Gateway)
**Found:** 2026-06-03 (Wave flow-kh2 walk Bucket A)
**Affects:** `kitehub-gateway/src/main/java/com/kitehub/gateway/filter/JwtAuthenticationGatewayFilter.java` + route `kitehub-onboarding-progress` trong `application.yml:615-630`

## Problem

Walk KH-2 sub-step S5 phát hiện: GET `/api/v1/onboarding-progress` via gateway (port 9000) với valid `Authorization: Bearer <JWT>` → HTTP 401 + `X-Gateway-Version: 1.0`. Subscription log KHÔNG có log của request → 401 do gateway trả về.

Evidence (Wave flow-kh2 walk 2026-06-03):
- Token signature verified hợp lệ với `JWT_SECRET` shared cả gateway + subscription (Python HMAC-SHA512 match=True)
- Token có claim đúng: `sub=<userId UUID>`, `role=OWNER`, `tenantId=<UUID>`, `type=access`, `alg=HS512`
- TenantResolverGatewayFilterFactory log SUCCESS: `Resolved tenant from JWT claim: <tenantId>` → `Routing to instance: <subdomain>` — chứng tỏ JWT verify OK ở filter này
- Cùng token + cùng `X-User-Id` + `X-User-Roles: OWNER` + `X-Tenant-Id` forged → direct subscription port 8081 → HTTP 200 (subscription auth filter chấp nhận khi headers present)
- Gateway → JWT → 401 trước khi forward to subscription

Possible root cause:
- `JwtAuthenticationGatewayFilter` order=`-100` chạy TRƯỚC route `default-filters: RemoveRequestHeader=X-User-Id,X-Tenant-Id` → filter add headers từ JWT, default-filter strip ngay sau → downstream nhận empty headers
- HOẶC parse exception trong filter (line 130 catch) trả 401 dù signature đúng (JJWT version mismatch?)
- Cần enable `logging.level.com.kitehub.gateway.filter.JwtAuthenticationGatewayFilter=DEBUG` để pin-point

Affects production-parity: end-user FE qua gateway sẽ không vào được onboarding dashboard sau login → trải nghiệm KH-2 vỡ tại UI level (G2 + G3 fail).

## Proposed Fix

Investigate:
1. Enable DEBUG logging trên `JwtAuthenticationGatewayFilter`
2. Confirm filter chain order: GlobalFilter(-100) vs route `default-filters` RemoveRequestHeader
3. Nếu ordering bug → di chuyển header re-injection vào filter AFTER strip (post route default-filter), HOẶC explicitly add headers vào route filter chain riêng cho `kitehub-onboarding-progress` route
4. Nếu JJWT parse exception → log full stacktrace + fix dependency hoặc key configuration

## Acceptance Criteria

- [x] Root cause identified + documented trong gap §Resolution
- [x] GET `/api/v1/onboarding-progress` via gateway với valid Bearer JWT → HTTP 200 + correct response body (verified Wave flow-kh2 walk 2026-06-03)
- [x] PUT `/api/v1/onboarding-progress` via gateway với valid Bearer JWT + body → HTTP 200 + state update (completionPercent 0→20 verified)
- [ ] Regression test: integration test mỗi onboarding endpoint qua gateway image, KHÔNG chỉ direct subscription — defer Wave flow-kh2 fix-bundle
- [ ] Cross-flow sweep per `cross-flow-bug-class-sweep.md`: kiểm tra other tenant-scoped routes (`/api/v1/staff-invitations`, `/api/v1/notifications`, `/api/v1/dsar`) có cùng class bug không — defer (filed sister gap nếu cần khi walk subsequent flows)

## Resolution (2026-06-03)

### Root cause

**Filter ordering race với Spring Cloud Gateway default-filters.** Stack diagnostic chain:
1. `JwtAuthenticationGatewayFilter` Order `-100` (chạy SỚM) → inject `X-User-Id` + `X-User-Roles` + `X-User-Email` từ JWT verified
2. `TenantHeaderGuardFilter` Order `-99` (chạy ngay sau) → inject `X-Tenant-Id` từ JWT claim
3. Route-level `default-filters: RemoveRequestHeader=X-User-Id,X-Tenant-Id` (Order ~0, chạy SAU global filters trên) → **strip headers vừa inject**
4. `NettyRoutingFilter` (LOWEST_PRECEDENCE) forward request → subscription nhận empty `X-User-*` headers → `XUserRolesHeaderFilter` không tạo Spring Security context → endpoint `@PreAuthorize` reject với 401

**Lý do filter inject + strip cùng pipeline:** `default-filters` thiết kế để chống client-spoofed headers trên public routes (vd `/api/auth/**` không validate JWT). Strip stripped CLIENT-supplied headers. JWT filter định inject AFTER strip — comment `application.yml:691-696` claim "JwtAuthenticationGatewayFilter (GAP-604) then re-injects X-User-Id from verified JWT" — nhưng implementation cũ Order=-100 → strip Order=~0 sau cùng cancel inject.

### Fix

Bump 2 GlobalFilter orders:
- `JwtAuthenticationGatewayFilter.ORDER = Ordered.LOWEST_PRECEDENCE - 2` (was `-100`) — runs SAU default-filter strip + route filters, NGAY TRƯỚC NettyRoutingFilter
- `TenantHeaderGuardFilter.ORDER = Ordered.LOWEST_PRECEDENCE - 1` (was `-99`) — runs ngay sau JWT filter, vẫn trước Netty

Filter chain (post-fix):
```
[Order ascending]
~0..10000   Route-level (TenantResolver, CircuitBreaker, RemoveRequestHeader strip, RateLimiter)
MAX-2       JwtAuthenticationGatewayFilter   inject X-User-Id/Roles/Email
MAX-1       TenantHeaderGuardFilter           inject X-Tenant-Id
MAX         NettyRoutingFilter                forward to subscription (headers preserved)
```

Result: subscription nhận đầy đủ `X-User-*` + `X-Tenant-Id` → `XUserRolesHeaderFilter` tạo Spring Security context → endpoint authenticated → 200.

### Verified behavior (Wave flow-kh2 walk continuation 2026-06-03 + re-walk fix-v3 1780538335)

- GET `/api/v1/onboarding-progress` via gateway:9000 với valid Bearer JWT → HTTP 200 + body `{tenantId: ..., completionPercent: 0, totalSteps: 5, steps: [PROFILE_SETUP, INVITE_TEAM, IMPORT_DATA, CREATE_FIRST_CLASS, EXPLORE_FEATURES]}`
- PUT `/api/v1/onboarding-progress` `{stepId: PROFILE_SETUP, completed: true}` via gateway → HTTP 200 + body `{completionPercent: 20, completedSteps: 1, steps[0].completed: true, completedAt: <timestamp>}`
- Gateway log trace: `JwtAuthenticationGatewayFilter Injected X-User-Id=... X-User-Roles=... path=/api/v1/onboarding-progress` → subscription handler receives + processes

### Investigation phase per `release-fix-retry-budget.md` §3.5

- Hypothesis 1 (JWT signature/key mismatch): REJECTED — Python HMAC-SHA512 verify confirms signature valid + both gateway+subscription have identical JWT_SECRET (84 bytes, HS512)
- Hypothesis 2 (default-filter strip race): CONFIRMED — diagnostic logs verified inject success but subscription log empty (request reached subscription without headers); fix Order=LOWEST_PRECEDENCE-2 verified post-mutate header set + downstream subscription log received headers + response 200
- 5 rebuild cycles + 1 `--no-cache` final to bypass Docker layer cache (earlier rebuilds with normal cache failed to pick up Order change → false-negative results)

### Cross-flow sweep deferred per scope

Other tenant-scoped routes (staff-invitations, notifications, dsar) — same filter chain → same bug class likely affects them too. Will surface naturally when walking KH-1/KH-2/KC flows. File sister gaps inline if discovered (per `cross-flow-bug-class-sweep.md`).

## Related

- Discovered in: Wave flow-kh2 walk (`documents/03-planning/waves/wave-2026-06-03-flow-kh2-auth-onboarding.md` §6.1 Blocker #5)
- Sister gap: GAP-604 (Wave 89 Bucket A — initial GAP-604 JwtAuthenticationGatewayFilter scaffold)
- Sister gap: GAP-714 (Wave 104.5 — kitehub-onboarding-progress route was falling through, fixed routing but maybe not header injection)
- Sister gap: GAP-790 (TenantResolver sweep — added TenantResolver filter to this route)
- Blocks: campaign flow KH-2 G2 (human test cần FE qua gateway hoạt động) + G3 (production parity)
