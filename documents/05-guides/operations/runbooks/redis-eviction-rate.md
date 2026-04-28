# Runbook: Redis Eviction Rate

**Alert:** `RedisEvictionRate`
**Severity:** warning
**Last updated:** 2026-04-28

## What does this alert mean?

`kite-redis` is evicting **>1000 keys/min** sustained. The metric `rate(redis_evicted_keys_total[5m]) * 60 > 1000` is built from the redis-exporter sidecar. Eviction means Redis is at `maxmemory` and is dropping keys per the configured policy (`allkeys-lru`, `allkeys-lfu`, etc.). Two consequences: (1) **cache miss rate spikes** — every evicted key that's re-requested causes a DB hit (latency + DB load); (2) **non-cache data loss** if any service stored ephemeral state in Redis without TTL discipline (sessions, in-flight job state). Sustained eviction is a sign cache sizing is wrong OR a cardinality leak just shipped.

## Immediate checks (0-5 min)

1. **Confirm pressure + current memory usage:**
   ```bash
   docker exec kite-redis redis-cli INFO memory | grep -E 'used_memory_human|maxmemory_human|maxmemory_policy|evicted_keys'
   docker exec kite-redis redis-cli INFO stats | grep -E 'evicted_keys|expired_keys|keyspace_hits|keyspace_misses'
   ```
2. **Top key prefixes by count** — find the cardinality offender:
   ```bash
   docker exec kite-redis redis-cli --scan --count 1000 | head -2000 \
     | awk -F: '{print $1":"$2}' | sort | uniq -c | sort -rn | head -10
   # Common prefixes: branding:, subscription:, jwt:blocklist:, session:, cache:tenant:
   ```
3. **Recent deploy?** Did a service ship a new cache key pattern?
   ```bash
   gh run list --workflow=docker-build-push.yml --limit 5
   git log --oneline --since='6 hours ago' -- '**/Cacheable*' '**/RedisConfig*'
   ```
4. **Hit rate trend** — drop in cache hit rate corroborates eviction pressure:
   ```
   keyspace_hits / (keyspace_hits + keyspace_misses)  # baseline ~0.85+, alarming if <0.5
   ```

## Likely causes

- **Cardinality explosion from new code** — recent change added a per-tenant + per-resource + per-locale key, multiplying namespace size by 10x. **Fix:** roll back the change, OR raise `maxmemory`, OR shorten TTL on the offending pattern.
- **Missing TTL on entries** — `@Cacheable` without `@CacheConfig(ttl=...)` (or equivalent) creates non-expiring keys; LRU has to evict them under pressure. **Fix:** audit `@Cacheable` annotations, set explicit TTLs (10min-1h typical for read-through caches).
- **Big-object cache** — someone cached a 5MB JSON blob per tenant (e.g. branding package); few keys, huge memory. **Fix:** redesign cache to store only an ETag + small metadata, fetch detail from object store.
- **`maxmemory-policy = noeviction`** — wrong policy means writes fail instead of evicting. Eviction wouldn't even register in this case; check separately. **Fix:** policy should be `allkeys-lru` for caches; `volatile-lru` if mixing cache + persistent data (NOT recommended; separate Redis logical DB or instance instead).
- **Wave 5 cache fix regression** — GAP-215 cache fix shipped Wave 5; if a follow-up reverted it, eviction pressure returns. **Fix:** verify GAP-215 cache changes still in place.
- **Redis instance under-provisioned for production traffic** — staging sized was ported to prod without review. **Fix:** scale `maxmemory` upwards (Helm values), restart Redis with persistence enabled to preserve data across restart.

## Mitigation

```bash
# 1. Immediate breathing room — raise maxmemory if instance has free OS memory
docker exec kite-redis redis-cli CONFIG SET maxmemory 4gb  # adjust per current sizing
# Persist the change:
docker exec kite-redis redis-cli CONFIG REWRITE

# 2. Identify and shorten TTL on the noisiest prefix to free memory immediately
# (example: shrink branding cache TTL from 1h to 10min during incident)
curl -X POST http://kitehub-branding:8083/actuator/env \
  -H 'Content-Type: application/json' \
  -d '{"name":"cache.branding.ttl_seconds","value":"600"}'
curl -X POST http://kitehub-branding:8083/actuator/refresh

# 3. Selective FLUSH of a low-priority prefix (rarely needed; loud last-resort)
# DO NOT FLUSHDB / FLUSHALL on shared Redis — it nukes other apps' data
# Targeted unlink (non-blocking delete):
docker exec kite-redis redis-cli --scan --pattern 'cache:debug:*' \
  | xargs -L 100 docker exec -i kite-redis redis-cli UNLINK

# 4. If TTLs missing in code, ship a hotfix that adds expiration:
# Spring example:
#   redisTemplate.opsForValue().set(key, value, Duration.ofMinutes(15));
```

After mitigation, eviction rate should drop below 200/min within 10 min. Hit rate should recover within an hour as caches warm again.

## When to escalate

- Eviction sustained >3000/min for 30 min after mitigation → P0; service latency starting to cascade. Consider [`high-response-time.md`](./high-response-time.md), [`database-pool-exhausted.md`](./database-pool-exhausted.md).
- Persistent data loss (sessions, job state) detected → escalate to platform lead; sessions in Redis without TTL = architecture issue (move to DB or set hard TTL with refresh)
- Multi-instance Redis cluster: if eviction concentrated on one shard, hot-keying issue → engage Redis specialist

## Related

- Alert rule: `kitehub/docker/prometheus/alert-rules.yml` (kitehub-platform-alerts group), `infrastructure/helm/kitehub/templates/prometheusrule.yaml`
- Audit reference: GAP-215 (Wave 5 cache fix, Sub-PR 5.6b)
- Related runbooks: [`high-response-time.md`](./high-response-time.md), [`database-pool-exhausted.md`](./database-pool-exhausted.md), [`high-memory-usage.md`](./high-memory-usage.md)
