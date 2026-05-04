# Runbook: High Response Time

**Alert:** `HighResponseTime`
**Severity:** warning
**Last updated:** 2026-04-28

## What does this alert mean?

p99 latency (kitehub) or p95 latency (kiteclass) on `{{ $labels.job }}` has been above **2 seconds** for 5 minutes. Under normal load Tier-A endpoints should respond in <200 ms (p95) per `documents/05-guides/monitoring/api-performance-slo.md`. A 10× spike suggests blocked threads, slow downstream calls, or a missing index on a hot query path. Different from the SLO-tier alerts (`ApiLatencyP95HighTierA-D`) which fire on labelled endpoints; this generic alert catches everything else.

## Immediate checks (0-5 min)

1. Identify slow endpoints — last 100 slowest scrape entries:
   ```bash
   kubectl logs -n kitehub deploy/<svc> --tail=2000 \
     | jq -r 'select(.durationMs > 1000) | "\(.durationMs)ms \(.uri)"' \
     | sort -rn | head -30
   ```
   (Logs format per `.claude/rules/logs-format-standard.md` §2.3 — `durationMs` field present once GAP-114 ships)
2. JVM thread dump — look for blocked threads:
   ```bash
   curl -fsS http://<pod>:<port>/actuator/threaddump | jq '.threads[] | select(.threadState=="BLOCKED" or .threadState=="WAITING") | .threadName' | sort | uniq -c | sort -rn
   ```
3. DB pool wait time — Prometheus query:
   ```
   rate(hikaricp_connections_acquire_seconds_sum[5m]) /
   rate(hikaricp_connections_acquire_seconds_count[5m])
   ```
   >50ms average = pool contention; see [`database-pool-exhausted.md`](./database-pool-exhausted.md)
4. Recent deploy bumping query plans — `gh run list --workflow=docker-build-push.yml --limit 5`. New migrations applied? `kubectl exec kite-postgres -- psql -c "SELECT version FROM flyway_schema_history ORDER BY installed_on DESC LIMIT 5"`

## Likely causes

- **N+1 query regression** — common in `kiteclass-core` controllers using JPA `findAll()` without `@EntityGraph` / `JOIN FETCH`. Hibernate emits one SELECT per parent + N for children. Verify with Hibernate filter logs (set `org.hibernate.SQL=DEBUG` temporarily on the affected pod via `/actuator/loggers` POST). Track GAP-126 family of N+1 gaps.
- **Missing index after migration** — V## migration created column but index lands in V##+1 not yet deployed. Check `pg_stat_user_indexes` for sequential scans on the table:
  ```sql
  SELECT relname, seq_scan, idx_scan
  FROM pg_stat_user_tables
  WHERE seq_scan > 1000 AND seq_scan > idx_scan * 10
  ORDER BY seq_scan DESC LIMIT 10;
  ```
- **Branding cache miss storm** — see `DocumentBrandingCacheMissStorm` alert (it fires alongside this one for `kiteclass-core`). Spring Cache `branding-by-tenant` evicted post-deploy; every request hits Postgres via `BrandingService.getBranding()`. **Fix:** GAP-215 cache warming, or wait for cache to refill (~5 min under steady traffic).
- **Ollama AI generation called synchronously** — violates `ai-branding-guidelines.md` §3.3 ("Heavy tasks async"). If a new endpoint accidentally calls `aiClient.generate()` in the request path instead of enqueuing on `ai.request.{tier}` queue, p95 will spike to multi-minute. **Fix:** rollback + refactor through `AIQueueDispatcher` per `design-patterns.md` §3.5.1 Exception D.
- **Thymeleaf template rendering blocking** — PDF generation path (`InvoiceRenderer`, `PdfGenerator`) in `kiteclass-core` is single-threaded. If a high-traffic endpoint accidentally renders inline (instead of async via `ai.generate.*`), the worker pool blocks. Cross-ref `feedback_thymeleaf_ognl_pin.md` for the parallel issue (OGNL bump breaks it entirely).
- **GC pressure** — heap above 70% with frequent full-GCs blocks request threads. Look at `jvm_gc_pause_seconds_max` and `jvm_memory_used_bytes{area="heap"}`. Cross-ref [`high-memory-usage.md`](./high-memory-usage.md).

## Mitigation

```bash
# 1. Recent deploy correlated → rollback (see rollback-procedure.md)
kubectl rollout undo deployment/<svc> -n kitehub

# 2. Branding cache empty → trigger warmup (when GAP-215 lands; until then, just wait)
# Cache fills naturally in ~5 min under traffic

# 3. N+1 verified → temporary mitigation: scale up to absorb load
kubectl scale deployment/<svc> --replicas=5 -n kitehub
# Then file gap for the missing JOIN FETCH / index

# 4. Sync AI call detected → revert immediately (NEVER ship sync AI in prod)
kubectl rollout undo deployment/kitehub-branding -n kitehub
```

After mitigation, watch p99/p95 in Prometheus:
```
histogram_quantile(0.99, sum by (job, le) (rate(http_server_requests_seconds_bucket[5m])))
```

## When to escalate

- p99 stays >2s after rollback → DB-level issue (vacuum needed, replication lag, hardware) — escalate to DBA / infra
- Multiple services co-fire HighResponseTime → shared dep (Postgres, RabbitMQ slow, Redis OOM) — pivot to infra runbooks
- Customer-facing dashboards show timeouts → P1 escalation per `incident-response-runbook.md` §1
- AI Branding sync-call regression → file P0 gap immediately + tenant communication if Enterprise tier affected (per `ai-branding-guidelines.md`)

## Related

- Alert rule: `kitehub/docker/prometheus/alert-rules.yml` line 33, `kiteclass/docker/prometheus/alert-rules.yml` line 22, `infrastructure/helm/kitehub/templates/prometheusrule.yaml` line 41
- SLO budgets: `documents/05-guides/monitoring/api-performance-slo.md`
- Memory: `feedback_thymeleaf_ognl_pin.md`, `feedback_objectmapper_test_jsr310.md`
- Rules: `ai-branding-guidelines.md` §3.3 (async heavy tasks), `design-patterns.md` §3.5.1 + §3.6
- Related runbooks: [`database-pool-exhausted.md`](./database-pool-exhausted.md), [`high-memory-usage.md`](./high-memory-usage.md), [`high-error-rate.md`](./high-error-rate.md) (often co-fires after timeouts)
