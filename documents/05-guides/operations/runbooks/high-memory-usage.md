# Runbook: High JVM Heap Usage

**Alert:** `HighMemoryUsage`
**Severity:** warning
**Last updated:** 2026-04-28

## What does this alert mean?

JVM heap usage on `{{ $labels.job }}` (instance `{{ $labels.instance }}`) has been above **85%** of `jvm_memory_max_bytes{area="heap"}` for **5 minutes**. The expression is `jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.85`. At 85% the JVM is GC-thrashing — full-GC pauses block request threads, p99 latency spikes, eventually `OutOfMemoryError` and pod crashes (often visible as `ServiceDown` co-firing). Caught early, can usually be mitigated without rollback.

## Immediate checks (0-5 min)

1. Heap pressure trend — climbing or steady?
   ```
   sum by (instance) (jvm_memory_used_bytes{area="heap"}) / sum by (instance) (jvm_memory_max_bytes{area="heap"})
   ```
   Steep climb in last 30 min = leak; sawtooth around 85% = sized too small for steady-state.
2. GC pressure — pause time exploding?
   ```
   rate(jvm_gc_pause_seconds_sum[5m]) / rate(jvm_gc_pause_seconds_count[5m])
   ```
   >100ms average = GC thrashing.
3. Heap dump (if reproducible & safe — be careful, blocks the JVM briefly):
   ```bash
   kubectl exec -n kitehub <pod> -- jcmd 1 GC.heap_dump /tmp/heap.hprof
   kubectl cp <namespace>/<pod>:/tmp/heap.hprof ./heap.hprof
   # Open in VisualVM / Eclipse MAT for leak analysis
   ```
4. Top retained classes — quick look without dump:
   ```bash
   kubectl exec -n kitehub <pod> -- jcmd 1 GC.class_histogram | head -30
   ```

## Likely causes

- **Branding cache unbounded growth** — Spring Cache `branding-by-tenant` in `kiteclass-core` defaults to in-memory; at scale (1000+ tenants × Branding object graph) eats heap fast. **Fix:** GAP-215 cache eviction (LRU + size cap). Mitigation: temporarily scale up replicas + flag cache TTL via `/actuator/refresh` if config supports it.
- **MinIO upload buffering full asset in memory** — `kitehub-branding` `BrandingService` reads uploaded logo/banner via `MultipartFile.getBytes()` instead of streaming. At 5MB max file × 100 concurrent uploads = 500MB instant heap. **Fix:** stream via `getInputStream()` to MinIO; track gap if regression detected.
- **AI inference holding response payload** — Ollama responses for image generation can be 10MB+ JSON. If `kitehub-branding` `AIClient` doesn't bound the response, batched calls accumulate. Should be gated by `ai.timeout-seconds` config + circuit breaker per `design-patterns.md` §3.6.
- **JPA L1 cache + huge result set** — query like `findAll(Pageable.unpaged())` on `branding_resources` (10k+ rows) loads everything into Hibernate's session cache. Common in admin endpoints. Fix with `@QueryHints` + `org.hibernate.fetchSize` or replace with paginated query.
- **Heap sized too small for container** — `MaxRAMPercentage` default is 25% of container memory. If pod has 512Mi, JVM heap is 128Mi — easily exhausted under modest load. Check `kubectl describe pod` `Limits: memory:` vs `JVM_OPTS` / `JAVA_TOOL_OPTIONS` for `-XX:MaxRAMPercentage`. Realistic value: 70-75%.

## Mitigation

```bash
# 1. Imminent OOM → restart pod (rolling) to give breathing room
kubectl rollout restart deployment/<svc> -n kitehub
# Caveat: restart resets cache. Branding tenants will re-warm on next request.

# 2. Heap sized too small → bump JVM args temporarily
kubectl set env deployment/<svc> -n kitehub \
  JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"
# Pod restarts; verify heap headroom in 5 min

# 3. Cache leak suspected + can't ship fix immediately → scale horizontally
kubectl scale deployment/<svc> --replicas=4 -n kitehub
# Buys 4× heap; file gap for proper eviction

# 4. Heap dump for offline analysis (if pressure isn't critical)
kubectl exec -n kitehub <pod> -- jcmd 1 GC.heap_dump /tmp/heap.hprof
kubectl cp <namespace>/<pod>:/tmp/heap.hprof ./heap-$(date +%Y%m%d-%H%M).hprof
```

## When to escalate

- Heap usage stays >85% after restart + scale-out → genuine memory leak, file P1 gap with heap dump attached
- `OOMKilled` exit codes in `kubectl describe pod` → prod incident, P0 if user-facing
- Multiple services co-fire with `ServiceDown` → likely shared infra (Redis OOM, network buffer issues) — pivot to infra runbooks
- Branding tier upload pattern OOMing → coordinate with AI Branding lead, may need `ai.enterprise.advancedModeEnabled` flag flip for relief

## Related

- Alert rule: `kitehub/docker/prometheus/alert-rules.yml` line 47, `kiteclass/docker/prometheus/alert-rules.yml` line 31, `infrastructure/helm/kitehub/templates/prometheusrule.yaml` line 52
- Architecture: `documents/02-architecture/ai-branding-v2-redesign.md` (cache strategy)
- Memory: `feedback_jpa_jsonb_jdbctypecode.md` (related but different — VARCHAR-vs-jsonb is correctness, this is throughput)
- Rules: `ai-branding-guidelines.md` §3.3 (async heavy tasks reduces sync heap pressure), `design-patterns.md` §3.6 (resilience on external calls)
- Related runbooks: [`service-down.md`](./service-down.md) (often follows OOMKilled), [`high-response-time.md`](./high-response-time.md) (GC thrashing → latency)
