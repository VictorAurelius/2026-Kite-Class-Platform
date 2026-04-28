# Runbook: Database Connection Pool Exhausted

**Alert:** `DatabasePoolExhausted`
**Severity:** warning (kitehub: >80% for 5m) / critical (kiteclass: >90% for 2m)
**Last updated:** 2026-04-28

## What does this alert mean?

HikariCP's active connections on `{{ $labels.job }}` have exceeded the threshold (80% kitehub / 90% kiteclass) of the configured `maximum-pool-size`. New requests block waiting for a free connection (default `connectionTimeout: 30s`); under sustained load, requests time out with `SQLTransientConnectionException: Connection is not available, request timed out`. This alert often co-fires with [`high-response-time.md`](./high-response-time.md) and [`high-error-rate.md`](./high-error-rate.md). At >90% it's a P1.

## Immediate checks (0-5 min)

1. Confirm pool state — query Spring actuator metrics:
   ```bash
   curl -s http://<pod>:<port>/actuator/metrics/hikaricp.connections.active | jq .
   curl -s http://<pod>:<port>/actuator/metrics/hikaricp.connections.pending | jq .
   curl -s http://<pod>:<port>/actuator/metrics/hikaricp.connections.usage | jq .
   ```
   Or Prometheus:
   ```
   hikaricp_connections_active / hikaricp_connections_max
   hikaricp_connections_pending  # threads waiting for a connection
   ```
2. Long-running queries on Postgres side:
   ```bash
   kubectl exec -n kitehub kite-postgres -- psql -U kitehub -d kitehub -c \
     "SELECT pid, now() - query_start AS duration, state, query
      FROM pg_stat_activity
      WHERE state != 'idle' AND now() - query_start > interval '5 seconds'
      ORDER BY duration DESC LIMIT 20;"
   ```
3. Connection leaks (claimed but never returned):
   ```
   hikaricp_connections_acquire_seconds_max  # if this is climbing while pool full → leak likely
   ```
4. Recent migrations? — long-running `ALTER TABLE ... ADD COLUMN ... DEFAULT ...` rewrites whole table and holds locks. `kubectl exec kite-postgres -- psql -c "SELECT version FROM flyway_schema_history ORDER BY installed_on DESC LIMIT 5"`

## Likely causes

- **N+1 query × concurrent traffic** — see [`high-response-time.md`](./high-response-time.md) Likely causes. Each request opens 1+ connection × duration of N queries; under load the pool is held open for long enough that Hikari can't recycle. **Fix:** add `JOIN FETCH` or `@EntityGraph`; until fix lands, scale up pool size + replicas.
- **Long-held transactions in Outbox/PubSub path** — `OutboxEventWriter` writes within the same `@Transactional` as domain change (per `design-patterns.md` §3.5.1). If the txn fans out to multiple aggregates or holds locks awaiting a downstream RPC, connection is pinned for entire txn. Verify per-module outbox per `project_outbox_per_module_pattern.md`.
- **Connection leak — txn not committed/rolled back** — common in custom `JdbcTemplate` usage outside Spring's `@Transactional` boundary. Symptom: `hikaricp_connections_active` climbs but `hikaricp_connections_acquire_seconds` stays low (connections aren't being requested fresh; they're held). **Fix:** wrap in `try-with-resources` or use Spring's transaction template.
- **Postgres slow due to vacuum / bloat** — background VACUUM on a hot table holds a lock that blocks SELECTs/UPDATEs. Check `pg_stat_user_tables` for `n_dead_tup` ratio; if >20%, manual VACUUM ANALYZE or scheduled autovacuum tuning.
- **Pool sized too small for traffic** — default `maximum-pool-size: 10` is not enough for any production workload. Realistic: 20-50 per replica × N replicas, but watch Postgres `max_connections` (default 100; may need bump on the server side).
- **Schema-validation drift causing implicit retry storms** — see `feedback_dev_profile_schema_workaround.md`. If a column type mismatch (e.g. `created_by` VARCHAR vs Long) triggers Hibernate validation failures on first DB hit, requests retry repeatedly, exhausting pool. **Fix:** GAP-244, rollback to last-good schema.

## Mitigation

```bash
# 1. Find and kill the long-running query(ies)
kubectl exec -n kitehub kite-postgres -- psql -U kitehub -d kitehub -c \
  "SELECT pg_cancel_backend(pid)
   FROM pg_stat_activity
   WHERE state != 'idle' AND now() - query_start > interval '60 seconds';"

# 2. Pool too small → bump (requires restart or actuator-refresh-capable config)
# In application.yml: spring.datasource.hikari.maximum-pool-size: 30
# Then: kubectl rollout restart deployment/<svc>

# 3. Scale replicas to spread load (each replica gets full pool)
kubectl scale deployment/<svc> --replicas=4 -n kitehub
# Watch Postgres max_connections — too many replicas × pool size will saturate Postgres itself

# 4. Cancel the offending request(s) at gateway with rate limit
# (ad-hoc; coordinate with on-call)
```

## When to escalate

- Pool stays >90% after pool resize + replica scale → DB-side bottleneck, escalate to DBA
- `pending_connections` >0 sustained → users actively timing out, P1 (or P0 for kiteclass critical threshold)
- Leak suspected (active climbs while throughput flat) → file P1 gap with thread dump + active SQL snapshot
- Postgres `max_connections` saturated (`FATAL: too many clients already`) → infra emergency, may need PgBouncer

## Related

- Alert rule: `kitehub/docker/prometheus/alert-rules.yml` line 58 (warning, 80%, 5m), `kiteclass/docker/prometheus/alert-rules.yml` line 40 (critical, 90%, 2m), `infrastructure/helm/kitehub/templates/prometheusrule.yaml` line 61
- Memory: `feedback_dev_profile_schema_workaround.md` (schema drift causes retry storms), `feedback_jpa_jsonb_jdbctypecode.md` (write-path failures stretch txn lifetime)
- Rules: `design-patterns.md` §3.5 (Outbox), `project_outbox_per_module_pattern.md`
- Related runbooks: [`high-response-time.md`](./high-response-time.md), [`high-error-rate.md`](./high-error-rate.md), [`service-down.md`](./service-down.md)
- Architecture: `documents/02-architecture/adr/` for connection pool sizing decisions
