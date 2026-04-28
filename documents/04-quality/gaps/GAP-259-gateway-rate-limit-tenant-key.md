# GAP-259: Gateway Rate Limit by Tenant + API Key (Beyond IP)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Security / Gateway / Multi-tenant fairness
**Found:** 2026-04-28 (article-driven check vs `kite-gateway` Spring Cloud Gateway config)
**Affects:** kite-gateway edge layer; multi-tenant fairness; abuse mitigation

## Problem

`kite-gateway/src/main/resources/application.yml` configures Spring Cloud Gateway `RequestRateLimiter` with **`#{@ipKeyResolver}`** as the only key resolver (replenishRate=3, burstCapacity=5). Behind any NAT or shared egress (corporate office, mobile carrier CGNAT, cloud egress IP), all tenants share a single rate-limit bucket. One abusive tenant on a shared IP can starve every other legitimate tenant on that IP.

This is the cost-attack scenario from the 2026-04-28 article: "rate limit theo IP" alone is insufficient for multi-tenant SaaS.

**Evidence:**
- `kite-gateway/src/main/resources/application.yml` line 32-37 — only `ipKeyResolver` wired
- `kite-gateway/src/main/java/com/kitehub/gateway/config/RateLimitConfig.java` — defines `ipKeyResolver` bean only; no `tenantKeyResolver`, no `apiKeyResolver`
- Multi-tenant SaaS: every request after auth has tenant claim in JWT — should be the primary partition

## Root Cause

Initial gateway scaffold was single-tenant style (IP rate limit was the default in Spring Cloud examples). Tenant context was added later in service layer (kitehub-branding `AIRateLimitService` per-instance) but the edge gateway never got the same partition treatment.

## Proposed Fix

1. Add `tenantKeyResolver` bean reading `tenant_id` claim from validated JWT (post `JwtAuthFilter`):
   ```java
   @Bean
   KeyResolver tenantKeyResolver() {
     return exchange -> {
       String tenant = exchange.getAttribute("authenticated.tenantId");
       return Mono.just(tenant != null ? "tenant:" + tenant : "anon");
     };
   }
   ```
2. Add `apiKeyResolver` for non-JWT machine-to-machine traffic (header `X-API-Key`):
   ```java
   @Bean
   KeyResolver apiKeyResolver() {
     return exchange -> Mono.just("apikey:" + exchange.getRequest().getHeaders().getFirst("X-API-Key"));
   }
   ```
3. Compose: keep `ipKeyResolver` as fallback for unauthenticated routes; switch authenticated routes to `tenantKeyResolver`. Per-route `key-resolver:` override in YAML.
4. Tier-aware capacity multiplier: FREE/BASIC use base rate; PREMIUM 3×; ENTERPRISE 10× (see `AIRateLimitConfig` for tier tier source-of-truth).
5. Emit Micrometer counter `gateway_rate_limit_rejected_total{key_type, tenant}` for observability.
6. Integration test: 2 tenants behind same IP → tenant A floods → tenant B unaffected.

## Acceptance Criteria

- [ ] `tenantKeyResolver` + `apiKeyResolver` beans wired in `RateLimitConfig.java`
- [ ] Authenticated routes in `application.yml` set `key-resolver: "#{@tenantKeyResolver}"` (with `ipKeyResolver` fallback for `/auth/*`)
- [ ] Tier-aware burst capacity (config key `gateway.rate-limit.tier-multiplier.{free|basic|premium|enterprise}`)
- [ ] Counter `gateway_rate_limit_rejected_total` emitted at gateway `/actuator/prometheus`
- [ ] Integration test `GatewayMultiTenantRateLimitIT` — 2 tenants same IP → independent buckets
- [ ] `documents/02-architecture/` updated with key-resolver decision
- [ ] Alert `GatewayRateLimitFloodPerTenant` candidate for next platform-alerts wave (or extend existing `RateLimitBreachSpike` GAP-122 alert with `tenant` label)

## Out-of-scope (track separately)

| Item | Where |
|------|-------|
| Replace Spring Cloud Gateway with Envoy/Kong (more advanced rate-limit) | Not on roadmap; SCG sufficient with proper key-resolver |
| Distributed rate-limit governance UI for ops | Wave 8+ admin console — file when needed |
| Per-endpoint cost-aware rate-limit (AI endpoint = expensive) | Belongs in branding service rate-limit (`AIRateLimitService` already does), not gateway |

## Related

- Source: 2026-04-28 article "Những lỗi 'chết người' khi build AI backend (Phần 2)" — §1 Multi-tier rate limit "Không chỉ theo IP. Phải kết hợp: user_id, API key, tenant"
- Code: `kite-gateway/src/main/java/com/kitehub/gateway/config/RateLimitConfig.java`, `application.yml` lines 32-37
- Existing: `AIRateLimitService` (per-tenant at branding service layer)
- Related gaps: GAP-181 (Acceptable Use Policy — policy doc), GAP-122 alert `RateLimitBreachSpike` (would benefit from tenant label after this fix)
- Rules: `.claude/rules/ai-branding-guidelines.md` §3 (queue tier routing precedent)

## Log

- 2026-04-28 — Discovered via article state-check. Existing `RateLimitConfig.java` confirmed single ipKeyResolver only. GAP-181 (AUP) is policy-level, not technical enforcement → not a duplicate.
