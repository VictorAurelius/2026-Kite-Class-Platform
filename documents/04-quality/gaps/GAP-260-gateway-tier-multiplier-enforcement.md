# GAP-260: Gateway Tier-Multiplier Enforcement + Remaining Route Coverage

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Security / Gateway / Multi-tenant fairness
**Found:** 2026-04-28 (filed as follow-up to GAP-259 PARTIAL ship)
**Affects:** kite-gateway tier-aware burst capacity; remaining unprotected authenticated routes
**Related:** GAP-259 (parent — shipped resolvers + metrics filter + branding route wiring), ADR-023 (key resolver strategy)

## Problem

GAP-259 shipped tenant + apiKey key resolvers, a `RateLimitMetricsFilter`, and the `platform-branding` route wired to `tenantKeyResolver`. Two parts of the original GAP-259 acceptance criteria were explicitly deferred to this gap:

1. **Tier-aware burst capacity enforcement** — `RateLimitConfig.tierMultiplier` (FREE 1× / BASIC 1× / PREMIUM 3× / ENTERPRISE 10×) is shipped as data-only config. The default `RedisRateLimiter` applies static per-route replenish/burst regardless of resolved key. To make `replenishRate` and `burstCapacity` scale with tier, we need a custom `RedisRateLimiter` extension that reads tier from the resolved key (e.g. `tenant:abc:tier:PREMIUM`) and computes per-key config dynamically.
2. **Remaining authenticated route coverage** — only `/api/platform/branding/**` got `RequestRateLimiter` in GAP-259 (highest cost surface, AI). All other authenticated routes still have **no gateway-level rate limit**:
   - `/api/v1/**` (instance APIs → kiteclass-core)
   - `/api/platform/instances/**`
   - `/api/platform/subscriptions/**`
   - `/api/platform/payments/**`
   - `/api/platform/admin/**`
   - `/api/platform/emails/**`

## Root Cause

Spring Cloud Gateway's `RedisRateLimiter` `Config` is per-route static. To get key-dependent capacity, we must subclass `RedisRateLimiter` and override `isAllowed(routeId, id)` to look up tier from the key suffix and produce per-call config. This is non-trivial — duplicating Spring's Lua-script-based atomic check is brittle, and Spring's API doesn't expose hooks for it cleanly.

Per-route YAML expansion to cover all authenticated routes was deferred to avoid breaking legitimate burst traffic without traffic profiling per route.

## Proposed Fix

### Stage 1 — Tier-aware key composition

Modify `tenantKeyResolver` to embed tier in the key when available:
```
"tenant:" + subdomain + ":tier:" + tier   // when tier resolvable from header/JWT
"tenant:" + subdomain                      // when tier not resolvable (fall back to base config)
```

Tier comes from `X-Subscription-Tier` header (already populated by upstream gateway filter / FE) — same source `kitehub-branding` reads.

### Stage 2 — Custom `TierAwareRedisRateLimiter`

New class extending `RedisRateLimiter`. Overrides config lookup to parse tier from `id`:

```java
@Override
public Mono<Response> isAllowed(String routeId, String id) {
    String tier = extractTier(id);  // returns null if id has no ":tier:" suffix
    Config base = super.getConfig().getOrDefault(routeId, defaultConfig);
    if (tier == null) {
        return super.isAllowed(routeId, id);   // unchanged
    }
    double multiplier = rateLimitConfig.getMultiplierForTier(tier);
    Config scaled = new Config()
            .setReplenishRate((int) Math.ceil(base.getReplenishRate() * multiplier))
            .setBurstCapacity((int) Math.ceil(base.getBurstCapacity() * multiplier))
            .setRequestedTokens(base.getRequestedTokens());
    // Replicate parent's Lua-call logic with `scaled` config
    // (or use reflection to set route-id-keyed override before super call)
}
```

Acceptable trade-off: replicate parent's atomic Lua call within the new method, with same Lua script + Redis key prefix. ~80 LOC.

### Stage 3 — Roll out per route incrementally

For each authenticated route, add `RequestRateLimiter` block with route-tuned `replenishRate`/`burstCapacity`. Rollout order (lowest to highest traffic):
1. `platform-payments` (low volume, tight tolerance)
2. `platform-subscriptions`
3. `platform-instances`
4. `platform-admin`
5. `platform-emails`
6. `instance-apis` (`/api/v1/**`) — highest volume, tune carefully

Each route ship is its own PR with traffic baseline + rate limit number justified in PR body.

## Acceptance Criteria

### Stage 1
- [ ] `tenantKeyResolver` embeds tier in key when `X-Subscription-Tier` header present
- [ ] Updated unit test asserts key shape `tenant:<subdomain>:tier:<TIER>` when header present, falls back to `tenant:<subdomain>` otherwise

### Stage 2
- [ ] `TierAwareRedisRateLimiter` class extending `RedisRateLimiter`
- [ ] Wired as primary `RateLimiter` bean (replaces default)
- [ ] Integration test: same tenant on FREE → 60 burst; on PREMIUM → 180 burst (3× multiplier confirmed at edge)
- [ ] No regression on existing `auth-register` route (still uses `ipKeyResolver` with static 5 burst)

### Stage 3
- [ ] All 6 listed authenticated routes wired with `RequestRateLimiter` + `tenantKeyResolver`
- [ ] Each route's `replenishRate`/`burstCapacity` tuned with traffic baseline cited in PR body
- [ ] Env-overridable per-route via `${ROUTE_RATE_REPLENISH:default}` / `${ROUTE_RATE_BURST:default}` pattern (consistent with `BRANDING_RATE_REPLENISH`)

## Out-of-scope (track separately)

| Item | Where |
|------|-------|
| Replace Spring Cloud Gateway with Envoy/Kong | Out of roadmap (per ADR-023 alternatives) |
| Distributed rate-limit governance UI for ops | Wave 8+ admin console — file when needed |
| Per-endpoint cost-aware rate-limit (AI endpoint = expensive) | Belongs in branding service rate-limit (`AIRateLimitService` already does), not gateway |
| Alert `GatewayRateLimitFloodPerTenant` | Stage 3 ship — extend GAP-122 `RateLimitBreachSpike` with `tenant` label after counter is in production |

## Related

- Parent: `documents/04-quality/gaps/GAP-259-gateway-rate-limit-tenant-key.md` (closes 🟡 PARTIAL via this gap)
- ADR: `documents/02-architecture/adr/ADR-023-gateway-key-resolver-strategy.md`
- Code: `kitehub/kitehub-gateway/src/main/java/com/kitehub/gateway/config/RateLimitConfig.java#tierMultiplier` (already shipped, awaiting consumer)
- Existing: `AIRateLimitService` (per-tenant branding service rate-limit), `RateLimitMetricsFilter` (counter)
- Rules: `.claude/rules/ai-branding-guidelines.md` §3 (queue tier routing precedent)

## Log

- 2026-04-28 — Filed as deferral of GAP-259 ACs (tier multiplier enforcement + remaining route coverage). GAP-259 ships PARTIAL with tenant + apiKey resolvers, branding route wiring, metrics filter, tier-multiplier config keys (data-only). This gap fully closes the `gap-done-discipline.md` §3 PARTIAL exit-ramp.
