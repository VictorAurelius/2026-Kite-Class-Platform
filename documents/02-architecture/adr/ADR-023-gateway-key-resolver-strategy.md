# ADR-023: Gateway Rate-Limit Key Resolver Strategy

**Status:** ACCEPTED
**Date:** 2026-04-28
**Deciders:** @nguyenvankiet (solo-dev)
**Related Gap(s):** GAP-259 (this ADR), GAP-260 (follow-up tier-multiplier enforcement)

## Context

Spring Cloud Gateway's `RequestRateLimiter` filter uses a `KeyResolver` bean to derive the rate-limit bucket key per request. Before this ADR, `kite-gateway` shipped a single `ipKeyResolver` wired only on the `/api/auth/register` route. Two consequences:

1. **No tenant isolation under shared egress.** Any platform route reachable through the gateway (branding, instances, subscription, payments, etc.) had **no gateway-level rate limit at all**. The article state-check on 2026-04-28 (cited in GAP-259) flagged this as a multi-tenant fairness gap: a flooded tenant on a NAT/CGNAT egress would either (a) saturate the upstream service via no gateway throttle, or (b) if rate-limit was added IP-only, starve every co-tenant on the same NAT.
2. **Dead tier config.** `RateLimitConfig` loaded a `limits` map (FREE/BASIC/PREMIUM/ENTERPRISE → requests/min) but no rate limiter consumed it. Tier-aware behaviour was an unfulfilled aspiration.

We need a clear, future-proof key resolver strategy that:
- Partitions buckets per tenant for authenticated traffic (cost-attack defense + co-tenant fairness)
- Falls back to IP for anonymous routes
- Supports machine-to-machine integrations via API key
- Composes with — but does not depend on — the heavier `TenantResolverGatewayFilterFactory` (which performs DB lookups and is route-filter-time, after rate-limit filters run)

## Decision

We adopt **three composable `KeyResolver` beans** in `KeyResolverConfig.java`, picked per-route in `application.yml`:

| Resolver | Bean ID | Key shape | Use on |
|----------|---------|-----------|--------|
| IP-based | `ipKeyResolver` | `ip:<address>` | Anonymous routes (`/api/auth/*`, public config) |
| Tenant-based | `tenantKeyResolver` | `tenant:<subdomain>` (or `tenant:anon`) | Authenticated tenant traffic (platform/branding initially; rolled out incrementally) |
| API-key-based | `apiKeyResolver` | `apikey:<value>` (or `apikey:anon`) | Machine-to-machine integrations carrying `X-API-Key` |

**Tenant resolution is stateless** — the resolver extracts subdomain from the `X-Instance-Subdomain` header (dev) or the `Host` header's subdomain part (prod). It **does not** call `InstanceRepository`. Rationale: rate-limit filters run before route filters in Spring Cloud Gateway; the heavier `TenantResolverGatewayFilterFactory` (which validates instance status against the DB) executes later in the chain. Duplicating the cheap subdomain extraction keeps rate-limit decision local + fast.

**Rejection observability** ships as a `RateLimitMetricsFilter` global filter that emits Micrometer counter `gateway.rate.limit.rejected{key_type, tenant}` whenever the gateway response status is 429.

**Tier-aware burst capacity** (FREE 1× / BASIC 1× / PREMIUM 3× / ENTERPRISE 10×) is shipped as **data-only config** (`kitehub.rate-limit.tier-multiplier.*`) in this PR. Actual enforcement requires a custom `RedisRateLimiter` extension that reads tier from the resolved key and computes per-key replenish/burst — deferred to GAP-260 follow-up. The config keys land now so YAML and ops tooling can reference them without a code change later.

## Consequences

### Positive

- Multi-tenant traffic is isolated at the edge: tenant A's flood on a NAT shared with tenant B does not affect B's bucket
- Cost-attack surface (branding AI endpoint) gets a tenant-keyed RequestRateLimiter that complements service-level per-instance caps in `AIRateLimitService`
- New observability surface (`gateway.rate.limit.rejected{key_type, tenant}`) drives alerting per `GAP-122` pattern (`RateLimitBreachSpike`)
- Tier-multiplier config keys are visible to ops without a rebuild — partial fulfillment of GAP-259 AC, full enforcement deferred
- Stateless resolver doesn't introduce new DB pressure on the rate-limit hot path

### Negative

- Subdomain extraction logic is duplicated between `KeyResolverConfig` and `TenantResolverGatewayFilterFactory`. Acceptable trade-off: the resolver MUST run before the filter chain in Spring Cloud Gateway; sharing logic via an instance-method dependency would create circular coupling
- Tier-aware burst is only partially shipped (config keys present, enforcement deferred). Visible tech debt is tracked as GAP-260 with explicit acceptance criteria
- One route (`platform-branding`) is wired in this PR; remaining authenticated routes (`/api/v1/**` instance APIs, `/api/platform/instances/**`, etc.) still lack gateway-level limits and will be added incrementally to avoid breaking legitimate burst traffic

### Neutral

- `X-Instance-Subdomain` header now has a second consumer (rate-limit), reinforcing its contract as the canonical dev-mode tenant identifier
- `RateLimitConfig#limits` map remains informational for now — rolling routes onto `RequestRateLimiter` is the path to making each tier limit "live"

## Alternatives Considered

### Alternative A: Resolve tenant via JWT claim only (no subdomain)

Pros: Single source of truth (JWT validated at gateway). No duplication with tenant filter.
Cons: Requires JWT verification at the gateway *before* rate limit, adding crypto cost on every request. Anonymous routes (auth/login) have no JWT and would still need a separate resolver. Public tenant pages (subdomain-only, no auth) — e.g. read-only schedules — would fall to IP and be vulnerable. Rejected.

### Alternative B: Defer all rate-limit until `TenantResolverGatewayFilterFactory` finishes

Pros: One canonical tenant resolution path. Consistent with downstream services that read `X-Tenant-Id`.
Cons: Rate-limit must run after that filter, which means re-architecting Spring Cloud Gateway's filter ordering. The standard `RequestRateLimiter` is a global filter wrapper; making it route-filter-time-dependent is non-idiomatic and brittle. Also defeats the purpose of cheap edge throttling — by the time `TenantResolver` runs (DB lookup, status check), we've already paid for an unauthenticated DB hit. Rejected.

### Alternative C: Use Envoy or Kong instead of Spring Cloud Gateway

Pros: Mature multi-tier rate-limiting, distributed governance UI, proven at scale.
Cons: Out of scope — adds a new infrastructure layer, requires rewriting all filters (TenantResolver, error fallback rendering) in Lua/Go/external service. Spring Cloud Gateway is sufficient with proper resolvers + a tier-aware extension. Rejected (also explicitly out-of-scope per GAP-259).

## Implementation Notes

- **Migration strategy:** add resolvers + filter + branding-route wiring in one PR (this PR). Roll remaining authenticated routes onto `tenantKeyResolver` in follow-up PRs as traffic patterns are profiled. Each rollout PR validates legitimate burst doesn't trip the new limit (per-route `replenishRate` / `burstCapacity` are env-configurable).
- **Rollback plan:** remove the `RequestRateLimiter` filter block from a problematic route's YAML — resolver beans + metrics filter can stay (no functional impact when not invoked).
- **Feature flags:** none. Per-route rate-limit args are env-overridable (`BRANDING_RATE_REPLENISH`, `BRANDING_RATE_BURST`); set high values to effectively disable.
- **Monitoring / success criteria:** `gateway.rate.limit.rejected_total` histogram should remain near zero under normal load; spikes attributable to a single tenant tag confirm isolation working. After GAP-260 lands, `replenishRate` per resolved key should reflect tier multiplier.

## References

- Design pattern used: `.claude/rules/design-patterns.md` §3.10 Leaky Abstraction (resolver returns domain string, never `ServerWebExchange` types)
- Related ADRs: ADR-016 (FE↔BE contract — header propagation), ADR-022 (Alertmanager — alerts for rate-limit spikes)
- Related rules: `.claude/rules/ai-branding-guidelines.md` §2.5 (input cap, sister cost-defense from same article)
- Related gaps: `documents/04-quality/gaps/GAP-259-gateway-rate-limit-tenant-key.md` (shipped PARTIAL via this ADR), GAP-260 (follow-up tier-multiplier enforcement), GAP-122 (`RateLimitBreachSpike` alert benefits from new tenant tag)
- External: Spring Cloud Gateway `KeyResolver` SPI — https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-mvc/filters/ratelimit.html

## Log

- 2026-04-28 — Initial proposal + ACCEPTED same-day (solo-dev). Closes GAP-259 PARTIAL (tenant + apiKey resolvers, branding route wiring, metrics filter, tier-multiplier config keys data-only). GAP-260 follow-up tracks tier-multiplier enforcement + remaining route coverage.
