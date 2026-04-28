# Runbook: High Disk Usage

**Alert:** `HighDiskUsage`
**Severity:** warning
**Last updated:** 2026-04-28

## What does this alert mean?

Filesystem usage on `{{ $labels.instance }}` (mountpoint `{{ $labels.mountpoint }}`) has exceeded **85%** for 10 minutes. Expression: `(node_filesystem_size_bytes - node_filesystem_avail_bytes) / node_filesystem_size_bytes > 0.85`. Once free space hits ~5%, Postgres refuses writes, MinIO bucket uploads fail with 500, and RabbitMQ pauses producers (memory_high_watermark_paging_ratio kicks in). Catch this at 85% — at 95% you're already losing user data.

## Immediate checks (0-5 min)

1. Identify the volume — node-level or pod PVC?
   ```bash
   kubectl get pvc --all-namespaces \
     | grep -E 'kite-postgres|kite-minio|kite-rabbitmq|loki|prometheus'

   # Node-level (host) disk
   kubectl get nodes -o wide
   kubectl describe node <node> | grep -A5 'Capacity\|Allocatable'
   ```
2. Top consumers on the affected mount:
   ```bash
   # If pod has shell access:
   kubectl exec -n <ns> <pod> -- sh -c 'du -sh /var/lib/* 2>/dev/null | sort -h | tail -10'
   # Postgres data dir specifically:
   kubectl exec -n kitehub kite-postgres -- du -sh /var/lib/postgresql/data/*
   ```
3. Postgres-side breakdown:
   ```bash
   kubectl exec -n kitehub kite-postgres -- psql -U kitehub -d kitehub -c \
     "SELECT
        schemaname || '.' || relname AS table,
        pg_size_pretty(pg_total_relation_size(c.oid)) AS total_size,
        pg_size_pretty(pg_relation_size(c.oid)) AS table_size,
        pg_size_pretty(pg_total_relation_size(c.oid) - pg_relation_size(c.oid)) AS index_size
      FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
      WHERE c.relkind = 'r' AND n.nspname NOT IN ('pg_catalog','information_schema')
      ORDER BY pg_total_relation_size(c.oid) DESC LIMIT 15;"
   ```
4. WAL backlog (replication lag or archive failure can stack WAL):
   ```bash
   kubectl exec -n kitehub kite-postgres -- du -sh /var/lib/postgresql/data/pg_wal
   # >10GB usually = replication target is down OR archive_command failing
   ```

## Likely causes

- **Audit log table unbounded growth** — `audit_log` and `outbox_events` tables grow forever if no retention job runs. `outbox_events` should be pruned post-publication; `audit_log` retains 7 years per `logs-format-standard.md` §4 for security events but day-to-day debug audit can rotate to cold tier. Check row count + size against expected baseline.
- **MinIO bucket unbounded — AI-generated assets accumulating** — `kitehub-branding` writes generated banners/logos/heroes to MinIO under `branding/<tenantId>/<resourceId>/...`. Without lifecycle policy, every regenerate keeps the old version. Per `ai-branding-guidelines.md` §4.3 the regenerate quota is enforced at app layer but old artifacts persist. **Fix:** MinIO lifecycle policy (delete after N days) or app-layer cleanup on `RegenerateBranding` event.
- **Postgres bloat — heavy UPDATE/DELETE without VACUUM** — `pg_stat_user_tables.n_dead_tup` shows dead-row count. `branding_versions`, `outbox_events`, `audit_log` are common bloat tables in this project. Manual VACUUM FULL reclaims but locks the table — use only off-hours.
- **Log files on host node** — if container logging driver writes to `/var/log/containers/` without rotation, busy services (gateway, branding) accumulate gigabytes. Check `/var/log/containers/<pod>_*.log` size.
- **Heap dumps / hprof files left behind** — `/tmp/heap.hprof` from previous incident response (see [`high-memory-usage.md`](./high-memory-usage.md) mitigation step 4) can be 1-4GB each. Forgotten hprof files compound across incidents.
- **WAL accumulation due to broken archive/replication** — if `archive_command` fails (e.g. S3 perms broken) Postgres keeps WAL forever waiting to archive. Catastrophic after a few days.

## Mitigation

```bash
# 1. Postgres bloat → manual VACUUM (off-hours preferred)
kubectl exec -n kitehub kite-postgres -- psql -U kitehub -d kitehub -c \
  "VACUUM (VERBOSE, ANALYZE) outbox_events;"
# Or VACUUM FULL for severe bloat — locks table, plan window

# 2. Outbox / audit log retention (manual prune — long-term: scheduled job)
kubectl exec -n kitehub kite-postgres -- psql -U kitehub -d kitehub -c \
  "DELETE FROM outbox_events
   WHERE published_at IS NOT NULL
     AND published_at < NOW() - INTERVAL '7 days';"
# Then VACUUM the table to actually reclaim space

# 3. MinIO orphan branding artifacts → mc rm with lifecycle filter
kubectl exec -n kitehub kite-minio -- mc rm --recursive --force \
  --older-than 90d kite-minio/branding-artifacts/

# 4. Heap dumps left over from prior incidents
kubectl exec -n kitehub <pod> -- find /tmp -name '*.hprof' -mtime +1 -delete

# 5. Broken WAL archive → check archive_command, fix S3 creds, then
kubectl exec -n kitehub kite-postgres -- psql -c \
  "SELECT pg_switch_wal();"
# Watch pg_wal/ shrink as backlog drains

# 6. Emergency expansion (Helm volume autosize)
# Edit infrastructure/helm/kitehub/values.yaml: storageSize: 50Gi → 100Gi
# helm upgrade kitehub ./infrastructure/helm/kitehub --namespace kitehub
```

## When to escalate

- Disk >95% AND mitigation hasn't reclaimed 5%+ in 15 min → P0 — Postgres about to refuse writes, MinIO uploads will fail
- WAL backlog >50GB and growing → archive subsystem broken; escalate to DBA + infra lead
- Multiple instances co-fire (cluster-wide) → likely shared storage class running out, may need volume class change
- Customer impact: failed uploads / 500 on POST endpoints → tenant communication per `incident-response-runbook.md` §5

## Related

- Alert rule: `kitehub/docker/prometheus/alert-rules.yml` line 69, `infrastructure/helm/kitehub/templates/prometheusrule.yaml` line 70 (note: `kiteclass/docker/prometheus/alert-rules.yml` does not include this alert — node_exporter is shared infra)
- Rules: `logs-format-standard.md` §4 (retention tiers — drives outbox/audit retention design)
- Memory: `feedback_dependabot_pin_violations.md` (large lockfile churn)
- Architecture: `documents/02-architecture/` MinIO + Postgres deployment topology
- Related runbooks: [`service-down.md`](./service-down.md) (often follows when DB refuses writes), [`rabbitmq-queue-backlog.md`](./rabbitmq-queue-backlog.md) (RabbitMQ paging triggered by disk pressure)
