# GAP-916: Gateway → 401 cho `/api/v1/onboarding-progress` dù JWT hợp lệ

**Status:** 🔵 OPEN
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

- [ ] Root cause identified + documented trong gap §Resolution
- [ ] GET `/api/v1/onboarding-progress` via gateway với valid Bearer JWT → HTTP 200 + correct response body
- [ ] PUT `/api/v1/onboarding-progress` via gateway với valid Bearer JWT + body → HTTP 200 + state update
- [ ] Regression test: integration test mỗi onboarding endpoint qua gateway image, KHÔNG chỉ direct subscription
- [ ] Cross-flow sweep per `cross-flow-bug-class-sweep.md`: kiểm tra other tenant-scoped routes (`/api/v1/staff-invitations`, `/api/v1/notifications`, `/api/v1/dsar`) có cùng class bug không

## Related

- Discovered in: Wave flow-kh2 walk (`documents/03-planning/waves/wave-2026-06-03-flow-kh2-auth-onboarding.md` §6.1 Blocker #5)
- Sister gap: GAP-604 (Wave 89 Bucket A — initial GAP-604 JwtAuthenticationGatewayFilter scaffold)
- Sister gap: GAP-714 (Wave 104.5 — kitehub-onboarding-progress route was falling through, fixed routing but maybe not header injection)
- Sister gap: GAP-790 (TenantResolver sweep — added TenantResolver filter to this route)
- Blocks: campaign flow KH-2 G2 (human test cần FE qua gateway hoạt động) + G3 (production parity)
