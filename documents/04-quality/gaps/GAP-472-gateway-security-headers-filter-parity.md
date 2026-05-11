# GAP-472: Gateway SecurityHeadersFilter Parity (KiteHub missing entirely; KiteClass missing HSTS+CSP)

**Status:** 🟡 PARTIAL 2026-05-11 (Wave 61 Bucket E — code shipped + unit-tested; docs update + live post-cutover probe deferred to Wave 62 post-cutover audit)
**Priority:** 🟠 P1 → promoted 🔴 P0 (Wave 61 Bucket E pre-cutover guard)
**Domain:** Backend / Gateway / Security
**Found:** 2026-05-11 (Wave 60 Bucket A pen-test self-audit — source-level inspection)
**Affects:** Backend API responses sau cutover Phase 1.5+ (Phase 1 BETA chưa active vì FE đi qua Vercel; gateway sẽ active khi BE cutover)

## Problem

Audit source-level phát hiện gateway security headers asymmetry:

1. **kitehub-gateway** hoàn toàn KHÔNG có SecurityHeadersFilter — filter directory chỉ chứa `RateLimitMetricsFilter` + `TenantResolverGatewayFilterFactory`
2. **kiteclass-gateway** có `SecurityHeadersFilter.java` NHƯNG chỉ 4/6 headers (thiếu HSTS + CSP)

Khi BE cutover, API responses (JSON) sẽ thiếu defense-in-depth headers. Mặc dù JSON API ít vulnerable hơn HTML (no inline script execution), CSP `frame-ancestors` + X-Frame-Options vẫn cần để chặn API-trong-iframe attack vectors.

## Evidence

```bash
$ ls kitehub/kitehub-gateway/src/main/java/com/kitehub/gateway/filter/
RateLimitMetricsFilter.java
TenantResolverGatewayFilterFactory.java
# ❌ NO SecurityHeadersFilter

$ cat kiteclass/kiteclass-gateway/src/main/java/com/kiteclass/gateway/filter/SecurityHeadersFilter.java
# Sets X-Content-Type-Options, X-Frame-Options, X-XSS-Protection, Referrer-Policy
# ❌ Missing: Strict-Transport-Security
# ❌ Missing: Content-Security-Policy
```

## Root Cause

- kitehub-gateway scaffolded Wave 1-3, security filter chưa được port từ kiteclass-gateway pattern
- kiteclass-gateway filter shipped 2026-03-24 (Wave 2 era), HSTS+CSP chưa thêm vì lúc đó chưa có production deploy plan
- Phase 1 BETA cutover plan giả định Cloudflare edge inject HSTS → defense-in-depth không strictly required → P1, không P0

## Proposed Fix

1. **Port SecurityHeadersFilter sang kitehub-gateway**:
```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class SecurityHeadersFilter implements WebFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        var headers = exchange.getResponse().getHeaders();
        headers.add("Strict-Transport-Security", "max-age=63072000; includeSubDomains; preload");
        headers.add("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none';");  // JSON-API-strict
        headers.add("X-Content-Type-Options", "nosniff");
        headers.add("X-Frame-Options", "DENY");
        headers.add("Referrer-Policy", "strict-origin-when-cross-origin");
        return chain.filter(exchange);
    }
}
```

2. **Update kiteclass-gateway SecurityHeadersFilter** thêm HSTS + CSP cùng pattern

3. **Verify** với `curl -I` after deploy

## Acceptance Criteria

- [x] `kitehub/kitehub-gateway/src/main/java/com/kitehub/gateway/filter/SecurityHeadersFilter.java` shipped (Wave 61 Bucket E)
- [x] `kiteclass/kiteclass-gateway/src/main/java/com/kiteclass/gateway/filter/SecurityHeadersFilter.java` thêm HSTS + CSP + Permissions-Policy (Wave 61 Bucket E)
- [x] Unit test verify 6/6 headers trên kitehub-gateway (`SecurityHeadersFilterTest.allSixHeadersPresent`) + 7/7 headers trên kiteclass-gateway (`SecurityHeadersFilterTest.filter_addsSecurityHeaders`)
- [ ] Documentation update `documents/05-guides/security/owasp-top-10-baseline.md` §7 — gateway header coverage (deferred to post-cutover audit refresh — file does not yet exist; tracked via Wave 60 OWASP audit re-run)
- [ ] Post-cutover re-probe verify (deferred to post-Bucket A DNS cutover live probe — see closure PR for `curl -sI` once `api.kitehub.me` resolves)

## Related

- 2026-05-11 pen-test audit P1-C
- 2026-05-08 Wave 40 milestone P1-2 (carry-over + escalation)
- `kiteclass/kiteclass-gateway/src/main/java/com/kiteclass/gateway/filter/SecurityHeadersFilter.java` (existing pattern reference)
- GAP-471 (frontend headers — sister gap)
- OWASP Secure Headers Project · API security best practices

## Log

- **2026-05-11** Filed by Wave 60 Bucket A pen-test self-audit (GAP-406 follow-up). Source-level inspection xác nhận asymmetry. Phase 1 BETA mitigation: Cloudflare edge HSTS khi DNS qua CF. Promote P0 khi v1.0.0 PRODUCTION cutover gate fires.
- **2026-05-11** Wave 61 Bucket E — code shipped. `kitehub-gateway` mới có `SecurityHeadersFilter` (GlobalFilter @ `LOWEST_PRECEDENCE - 1`) injecting 6 headers (HSTS preload 1y / CSP `default-src 'none'` / X-Frame-Options DENY / X-Content-Type-Options nosniff / Referrer-Policy strict-origin-when-cross-origin / Permissions-Policy). `kiteclass-gateway` `SecurityHeadersFilter` mở rộng từ 4 → 7 headers (thêm HSTS + CSP + Permissions-Policy). Verification: `kitehub` `mvn -pl kitehub-gateway verify -P strict-warnings` 32 tests PASS; `kiteclass-gateway` `mvn verify -P strict-warnings` 168 tests PASS. Status PARTIAL vì 2 AC còn pending: (1) `documents/05-guides/security/owasp-top-10-baseline.md` §7 update — file chưa tồn tại, scope thuộc về Wave 60 OWASP audit doc refresh tách riêng; (2) post-cutover live `curl -sI` probe — phụ thuộc Bucket A DNS bind cho `api.kitehub.me`. Cả 2 sẽ flip DONE trong Wave 62 post-cutover audit refresh.
