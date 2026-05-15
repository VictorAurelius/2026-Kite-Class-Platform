# GAP-551: Feedback endpoint missing gateway rate-limit + tenant context always null

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Backend (Gateway + Subscription)
**Found:** 2026-05-14 (Wave 78 post-wave Security /100 audit — `documents/04-quality/audits/security/2026-05-14-post-wave-78.md` P0-1)
**Affects:** `kitehub/kitehub-gateway/src/main/resources/application.yml`, `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/feedback/controller/FeedbackController.java`
**Phase:** Phase 1 BETA pre-launch (BLOCKING `v1.0.0-rc`)
**Standards:** OWASP A01 (Broken Access Control) + A04 (Insecure Design) + `pre-launch-auth-hardening-checklist.md` §2.1 spirit

## Problem

Wave 78 Bucket F shipped `POST /api/v1/feedback` (GAP-542) public endpoint cho in-app feedback widget. Two security gaps phát hiện ở post-wave audit:

1. **Gateway rate-limit MISSING.** API contract (`documents/01-business/kitehub/feedback/api-contract.md`) ghi rõ "10 req/min/IP" rate-limit, NHƯNG `kitehub-gateway/src/main/resources/application.yml` KHÔNG có route definition cho `/api/v1/feedback`. Hậu quả: request đi qua catch-all `instance-apis` route (`id: instance-apis`, path `/api/v1/**`) → forward sang `kiteclass-core` (SAI service, kitehub-subscription mới là owner) → KHÔNG rate-limit, KHÔNG circuit breaker.

2. **`currentTenantId()` hard-coded `return null;`** (`FeedbackController.java:74-79`). Comment ghi "Gateway forwards X-Tenant-Id via SecurityContext extension; absent for anonymous submits. For now return null — auth filter populates when JWT carries tenantId claim (Wave 33+ infrastructure)." Nhưng Wave 33 infra ĐÃ có `XUserRolesHeaderFilter` (`SecurityConfig.java:103-126`) extract `X-User-Id` từ header — chỉ thiếu `X-Tenant-Id` parsing.

## Root Cause

- Bucket F coordinator focus vào FE widget + service layer + DTO validation; gateway routing được giả định "đi qua catch-all OK" → không cross-check với gateway YAML actual route definitions.
- TenantId TODO bỏ qua vì "Wave 33+ infrastructure" hiểu nhầm — infrastructure đã sẵn, chỉ cần wire field thứ 2.

## Impact

**P0 BLOCKING `v1.0.0-rc`:**
- Spam vector: anonymous endpoint không rate-limit → bot mass-submit feedback / abusive content / DoS via 10k+ /min POST
- Misrouting: requests hiện forward sai service (kiteclass-core không có `/api/v1/feedback` handler) → 404 trên production hoặc unintended routing nếu kiteclass-core add handler tương lai
- Audit trail gap: tenant-scoped feedback abuse không trace được tenantId → không thể ban abusive tenant
- Persona analytics bị skew: feedback từ authenticated users không bind tenant

## Proposed Fix

### Step 1: Add gateway route (kitehub-gateway/application.yml)

Thêm route definition BEFORE `instance-apis` catch-all:

```yaml
# GAP-547 — Wave 78 Bucket F /api/v1/feedback owned by kitehub-subscription
# Must precede instance-apis catch-all. Rate-limit 10/min IP-keyed.
- id: kitehub-feedback-v1
  uri: http://kitehub-subscription:8080
  predicates:
    - Path=/api/v1/feedback
    - Method=POST
  filters:
    - name: RequestRateLimiter
      args:
        redis-rate-limiter.replenishRate: 10
        redis-rate-limiter.burstCapacity: 20
        redis-rate-limiter.requestedTokens: 1
        key-resolver: "#{@ipKeyResolver}"
    - name: CircuitBreaker
      args:
        name: subscriptionCircuitBreaker
        fallbackUri: forward:/fallback/subscription
```

Note: contract ghi "10 req/min" = 10/60 = ~0.167/sec replenish. Spring Cloud Gateway redis-rate-limiter dùng `replenishRate` = tokens/sec. Vậy thực tế cần tinh chỉnh:
- `replenishRate: 1` (1/sec = 60/min — gấp 6× contract)
- HOẶC accept "10/min" làm soft target và document spec mismatch.

Lựa chọn pragma: dùng `replenishRate: 1, burstCapacity: 10` (sau 10s liên tục burst, throttle xuống 1/sec) — phù hợp human-feedback frequency.

### Step 2: Wire tenantId từ X-Tenant-Id header (FeedbackController)

```java
private static String currentTenantId(HttpServletRequest request) {
    String header = request.getHeader("X-Tenant-Id");
    return (header != null && !header.isBlank()) ? header : null;
}
```

Inject `HttpServletRequest` vào `submit()` signature; pass tenantId to service. Idiomatic with existing `XUserRolesHeaderFilter` pattern.

### Step 3: Integration test

Verify:
1. Gateway-level: send 30 POSTs in 1s từ same IP → expect first ~20 succeed (burst), rest 429
2. Authenticated tenant submit → DB row `feedback_submissions.tenant_id` populated
3. Anonymous submit → DB row `tenant_id` = NULL (acceptable)

## Acceptance Criteria

- [ ] Gateway route `kitehub-feedback-v1` added BEFORE `instance-apis` in `application.yml`
- [ ] Rate-limit verified: 30 POST/s từ same IP → 429 sau burst threshold
- [ ] `FeedbackController.submit()` wire tenantId từ `X-Tenant-Id` header
- [ ] Integration test: authenticated tenant POST → DB row `tenant_id` matches header
- [ ] Anonymous POST → DB row `tenant_id` IS NULL (regression safe)
- [ ] Contract doc `documents/01-business/kitehub/feedback/api-contract.md` sync với actual rate-limit values

## Related

- Parent audit: `documents/04-quality/audits/security/2026-05-14-post-wave-78.md` §P0-1
- Closes part of: GAP-542 (Bucket F feedback widget) — security/routing follow-up
- OWASP A07 sister: `pre-launch-auth-hardening-checklist.md` §2.1 spirit (every public endpoint rate-limited)
- Gateway routing rule: `production-env-config-registry.md` §11 `audit-gateway-routes.sh` (would have caught this)

## Log

- **2026-05-14:** DONE — Wave 79 Bucket A closure. kitehub-feedback-v1 gateway route + tenant header propagation X-Tenant-Id × JWT claim cross-check shipped; OWASP A01+A04 closed (PR #1365).

- **2026-05-14**: Filed from Wave 78 post-wave Security audit /100 (89/100 B+ aggregate; P0-1 surface).
