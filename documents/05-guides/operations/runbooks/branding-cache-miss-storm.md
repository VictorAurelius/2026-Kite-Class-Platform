# Runbook: Branding Cache Miss Storm

**Alert:** `DocumentBrandingCacheMissStorm`
**Severity:** warning
**Last updated:** 2026-04-28

## What does this alert mean?

The Caffeine `branding-by-tenant` cache in `kiteclass-core` is missing more than **10 lookups per second over 5 minutes**. Every miss falls through to a PostgreSQL `SELECT` via `BrandingService.getBranding(tenantId)`. Steady-state miss rate should be near zero — the cache fronts every per-tenant theme + asset URL lookup that runs on the hot path of HTML/PDF rendering. A miss storm means either an eviction cascade, a cold start, or someone bypassing the cache.

> **Note:** This alert depends on Spring Cache Micrometer metrics that are not yet wired in production (tracked in GAP-219 P1 sub-bullet 4). Until those metrics ship the rule has no data and will not fire — see `documents/04-quality/gaps/GAP-219-*.md`.

## Immediate checks (0-5 min)

1. Confirm the storm is real (not a metric-source flap):
   ```bash
   kubectl logs -n kiteclass deploy/kiteclass-core --tail=200 \
     | grep -E "BrandingService.getBranding|cache=branding-by-tenant"
   ```
2. Was there a recent deploy of `kiteclass-core` (last 30 min)? Cold start expected after every rollout.
   ```bash
   gh run list --workflow=docker-build-push.yml --limit 5
   ```
3. Hit a sample tenant endpoint twice — second call must be served from cache:
   ```bash
   curl -fsS http://kiteclass-core:8081/api/v1/branding/<tenant>/package -H "X-Trace: warmup1"
   curl -fsS http://kiteclass-core:8081/api/v1/branding/<tenant>/package -H "X-Trace: warmup2"
   # Compare durationMs in structured logs
   ```
4. Caffeine cache stats — `kiteclass-core` exposes them via Actuator if `management.metrics.cache=true`:
   ```bash
   curl -s http://kiteclass-core:8081/actuator/metrics/cache.gets?tag=cache:branding-by-tenant
   ```

## Likely causes

- **Post-deploy cold start** → expected, self-heals within 1-2 min as warm-up traffic fills the cache. If alert clears within 5 min after deploy, no action.
- **Cache eviction storm** → JVM heap pressure forcing Caffeine to evict aggressively. Cross-check `HighMemoryUsage` alert; if both fire together, investigate heap leak.
- **Cache size mis-tuned** → `branding-by-tenant` `maximumSize` lower than active tenant count. Inspect `CacheConfig` in `kiteclass-core`. If active tenants > maximumSize, raise the cap.
- **Cache disabled / config drift** → `@EnableCaching` regression (see `feedback_repo_status_security_coverage.md`-class memory; precedent: GAP-132 missing `@EnableCaching` in kitehub services).
- **Bypass path** → caller invoking the underlying `BrandingRepository` directly (skips cache). Check git blame of recent `BrandingService` callers.
- **Bulk operation** → import / sync job iterating every tenant, blowing past cache size. Coordinate with operator before scaling cache up.

## Mitigation

```bash
# 1. Recent deploy → wait 5 min for warm-up; alert auto-clears.

# 2. Heap-pressure-driven eviction → scale up replica memory request OR add replicas
kubectl scale deployment/kiteclass-core --replicas=3 -n kiteclass-instances
# Bump JVM heap via -Xmx in image OR Helm values jvmOpts

# 3. Configuration check — verify CacheConfig is honored (not silently dropped on Wave 9.5-B regression).
kubectl exec -it deploy/kiteclass-core -- curl -s localhost:8081/actuator/caches | jq '.cacheManagers'

# 4. Bulk-op-driven storm → throttle bulk job OR raise cache size temporarily
#    Edit branding.cache.maximum-size in application.yml, redeploy.
```

After mitigation, verify miss rate falls below 1/s within 10 min. The accompanying `HighResponseTime` alert on tier-a endpoints often clears within 1 min of cache warm-up.

## When to escalate

- Storm persists >30 min AND no recent deploy → escalate to backend lead (likely a leak or bypass).
- Co-fires with `DatabasePoolExhausted` → P1, every miss is opening a DB connection; cap PG load before user-visible 5xx spike.
- Co-fires with `ApiLatencyP95HighTierA` → user-facing impact, document-rendering p95 budget breached. Treat as P1 until resolved.

## Related

- Alert rule: `kiteclass/docker/prometheus/alert-rules.yml` (kiteclass-document-generation-alerts group), `infrastructure/helm/kitehub/templates/prometheusrule.yaml`
- Architecture: `documents/02-architecture/` (BrandingService cache layer)
- Gaps: `documents/04-quality/gaps/GAP-219-*.md` (Spring Cache Micrometer metrics — wires this alert), `GAP-132` (precedent: missing `@EnableCaching` regression)
- Related runbooks: [`high-memory-usage.md`](./high-memory-usage.md), [`high-response-time.md`](./high-response-time.md), [`database-pool-exhausted.md`](./database-pool-exhausted.md)
