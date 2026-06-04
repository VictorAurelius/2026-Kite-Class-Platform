# GAP-918: Gateway `authCircuitBreaker` mở state lúc startup → register 503 fallback HTML

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend (Gateway)
**Found:** 2026-06-03 (Wave flow-kh2 walk Bucket A)
**Affects:** `kitehub-gateway/src/main/resources/application.yml:31-46` route `auth-register` + `kitehub-gateway/src/main/java/com/kitehub/gateway/controller/FallbackController.java`

## Problem

Walk KH-2 S1 sub-step phát hiện: ngay sau `bash kitehub/scripts/up.sh`, request đầu tiên POST `/api/auth/register` qua gateway → HTTP 503 + HTML fallback page "Dịch vụ tạm ngưng — Auth service hiện không khả dụng" (FallbackController).

Cause:
- Gateway startup nhanh hơn subscription readiness (subscription healthcheck 60s ~)
- Authority circuit breaker đếm fail trong startup window → trigger OPEN state
- Mọi request /api/auth/register subsequent rơi vào fallback HTML cho đến khi circuit chuyển HALF_OPEN (~30-60s)
- Sau warmup, register lại nhận response 201 bình thường

Evidence:
- Đầu tiên try qua gateway → 503 HTML fallback (Wave flow-kh2 walk timing 19:50)
- Sau ~30s sleep + retry → HTTP 201 success
- Subscription log không có entry cho 503 requests (gateway short-circuited)

Impact:
- Beta tenant đầu tiên đăng ký ngay sau deploy → có thể nhận trang "Dịch vụ tạm ngưng" → bounce
- Local dev confusing: contributor mở browser ngay sau `up.sh` → thấy fallback → mất 30s không hiểu

Severity P2 vì transient (self-healing), nhưng UX trên prod chuyển user-facing impact.

## Proposed Fix

Options:
1. **Wait-for-healthy chain trong up.sh** — block stack-up return cho đến khi gateway nhận response 200 từ subscription `/actuator/health` (rule out startup race)
2. **Circuit breaker startup grace period** — config Resilience4j `waitDurationInOpenState=10s`, `slidingWindowType=COUNT_BASED, slidingWindowSize=10`, `minimumNumberOfCalls=5` để giảm sensitivity trong startup
3. **Fallback content tone** — message "Đang khởi động, vui lòng thử lại sau vài giây" thay vì "Dịch vụ tạm ngưng" (less alarming)
4. **Health-check gating** — gateway readiness probe block trafic cho đến khi subscription `/actuator/health` UP

Recommend combo 1+2 (root-fix + sensitivity reduction).

## Acceptance Criteria

- [ ] Ngay sau `bash kitehub/scripts/up.sh` complete, request đầu tiên qua gateway → 201/200 (no fallback HTML)
- [ ] Circuit breaker config audit: minimum 5 fail trong 10-call window mới mở (giảm startup-race false positive)
- [ ] Wait-for-healthy script verify cả gateway lẫn subscription before exit 0
- [ ] Documentation `documents/05-guides/deploy/local-dev-setup.md` reference cold-start warmup notes
- [ ] Production parity check: prod deploy script integrate health-gate cho zero-downtime expectation

## Related

- Discovered in: Wave flow-kh2 walk (Blocker #1, downgraded from initially-suspected P0)
- Affects production-parity G3 cho KH-2 flow
- Sister gap: GAP-916 (gateway onboarding 401 — different layer)
- Wave flow-kh2 plan: `documents/03-planning/waves/wave-2026-06-03-flow-kh2-auth-onboarding.md` §6.1
