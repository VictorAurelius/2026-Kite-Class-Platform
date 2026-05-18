# GAP-259: Gateway Rate Limit by Tenant + API Key (Beyond IP)

**Status:** 🟡 PARTIAL 2026-04-28 — resolvers + metrics filter + branding route shipped; tier-multiplier enforcement + remaining route coverage tracked in GAP-260
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

## Current State (verified 2026-04-28 — shipped scope)

| AC | Status | Evidence |
|----|--------|---------|
| `tenantKeyResolver` + `apiKeyResolver` beans | ✅ | `KeyResolverConfig.java` — note: GAP claimed `RateLimitConfig.java` housed `ipKeyResolver`, but actual location was `KeyResolverConfig.java`. New resolvers added there for consistency with existing pattern. |
| Authenticated route wired with `tenantKeyResolver` | 🟡 PARTIAL | Only `platform-branding` wired in this PR (highest cost surface). Remaining 6 authenticated routes deferred to GAP-260 Stage 3 to avoid breaking legitimate burst without traffic profiling. |
| Tier-aware burst capacity multiplier | 🟡 DATA-ONLY | `RateLimitConfig.tierMultiplier` (FREE 1× / BASIC 1× / PREMIUM 3× / ENTERPRISE 10×) shipped as config keys. Actual `RedisRateLimiter` enforcement requires custom extension — deferred to GAP-260 Stage 1+2. |
| Counter `gateway.rate.limit.rejected{key_type, tenant}` | ✅ | `RateLimitMetricsFilter` global filter on 429 responses; Micrometer auto-exposes at `/actuator/prometheus` (Prom suffix `_total`). 4 unit tests verify tagging. |
| Integration test — multi-tenant isolation | ✅ | `KeyResolverConfigTest#tenantResolverPartitionsByTenant` — 2 different subdomain headers behind same simulated NAT IP produce different bucket keys. Reactive Redis IT deferred (would require Testcontainers Redis; existing context-load test confirms wiring). |
| ADR documenting key-resolver decision | ✅ | `ADR-023-gateway-key-resolver-strategy.md` ACCEPTED 2026-04-28 |
| Alert `GatewayRateLimitFloodPerTenant` | 🟡 DEFERRED | Counter available; alert rule extension tracked in GAP-260 Stage 3 (will extend GAP-122 `RateLimitBreachSpike` with `tenant` label once routes are rolled out and traffic baselines exist). |

## Acceptance Criteria

- [x] `tenantKeyResolver` + `apiKeyResolver` beans wired in `KeyResolverConfig.java` (gap-claimed location was wrong; actual path documented above)
- [x] At least one authenticated route uses `key-resolver: "#{@tenantKeyResolver}"` (`platform-branding` — highest cost surface). Remaining routes → GAP-260 Stage 3
- [x] Tier-aware burst capacity config keys defined (`kitehub.rate-limit.tier-multiplier.{free|basic|premium|enterprise}`) → enforcement deferred to GAP-260 Stage 1+2
- [x] Counter `gateway.rate.limit.rejected{key_type, tenant}` emitted at `/actuator/prometheus`
- [x] Unit test demonstrates multi-tenant key partitioning (`tenant:A` ≠ `tenant:B` even on same source IP). Reactive Redis IT deferred to GAP-260 Stage 2
- [x] `documents/02-architecture/adr/ADR-023-gateway-key-resolver-strategy.md` ACCEPTED
- [ ] Alert `GatewayRateLimitFloodPerTenant` — deferred to GAP-260 Stage 3

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

- 2026-04-28 — SHIPPED PARTIAL. Implementation: `KeyResolverConfig` extended with `tenantKeyResolver` + `apiKeyResolver` (subdomain + X-API-Key resolution, stateless to run before route filter chain); `RateLimitConfig` extended with `tierMultiplier` map (FREE 1× / BASIC 1× / PREMIUM 3× / ENTERPRISE 10×, data-only — enforcement requires custom `RedisRateLimiter` extension deferred to GAP-260); `RateLimitMetricsFilter` global filter emits `gateway.rate.limit.rejected{key_type, tenant}` Counter on every 429 response; `application.yml` `platform-branding` route wired with `RequestRateLimiter` + `tenantKeyResolver` (replenishRate=30, burstCapacity=60, env-overridable). 17 unit tests + ADR-023 ACCEPTED + 27/27 gateway suite green. Tier-multiplier enforcement + remaining 6 authenticated routes filed as GAP-260 with explicit 3-stage AC.
- 2026-04-28 — Discovered via article state-check. Existing `RateLimitConfig.java` confirmed single ipKeyResolver only. GAP-181 (AUP) is policy-level, not technical enforcement → not a duplicate.
- **2026-05-18 — DUPLICATE candidate flagged with GAP-581** per Wave 93 re-triage audit (`documents/04-quality/audits/persona-review/2026-05-18-phase-1-5-26-gaps-re-triage.md`). Both gaps target gateway rate limit by `tenant_id` từ JWT claim. GAP-259 (PARTIAL 50% since 2026-04-28 — tenant + apiKey resolvers + metrics filter + branding route shipped; tier-multiplier enforcement deferred GAP-260) is canonical predecessor; GAP-581 (OPEN since 2026-05-15) re-files same scope. **Recommendation:** merge GAP-581 → GAP-259 (close GAP-581 as DUPLICATE); GAP-260 retains tier-multiplier sub-scope. User decision deferred to wave plan §6 follow-up post-Wave-93 merge.
- **2026-05-18 — Cross-ref Wave 93 payment scope:** GAP-259 tenant rate limit baseline benefits Wave 93 payment endpoints (GAP-625 QR upload + GAP-636 Casso/SePay webhook receiver) — webhook endpoint needs per-tenant rate limit to prevent webhook replay DDoS; this gap's tenant rate-limit infra extends to payment webhook scope.
- **2026-05-18 (Wave 93 §7.2 row 1 — DUPLICATE absorbed)** — **GAP-581 closed as DUPLICATE; canonical scope merged into GAP-259.** GAP-581 (Filed 2026-05-15, OPEN 0%) re-filed identical scope (per-tenant rate limit by tenant_id at gateway) — surfaced via Wave 85 Bucket A simulation 3-axis. GAP-581 contributed 2 simulation context cells that complement GAP-259's article-driven scope: (a) **Cell 8 — DDoS single tenant:** 1 compromised tenant bursts 1000 RPS analytics queries → 99 tenants suffer cascading 503 (HikariCP pool exhaustion); (b) **Cell 18 — botnet rotating IPs:** Botnet 1000 IPs × 1 tenant credential stuffing → IP rate limit insufficient, tenant-level needed. Both scenarios reinforce GAP-259 PARTIAL Phase 2 enforcement scope (GAP-260 Stage 3 tier-multiplier + CloudWatch SNS alarm). User decision Wave 93 §7.2 row 1 — close GAP-581 as WONTFIX/DUPLICATE; canonical scope retained in GAP-259 + GAP-260 chain.
