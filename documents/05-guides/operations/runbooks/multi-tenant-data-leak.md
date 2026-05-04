# Runbook: Multi-Tenant Data Leak

**Alert:** `MultiTenantDataLeak`
**Severity:** critical
**Last updated:** 2026-04-28

## What does this alert mean?

A query has been observed crossing tenant boundaries — i.e. one tenant's request returned (or attempted to return) rows owned by a different tenant. The metric is a counter (`kite_cross_tenant_query_total`) emitted by the JPA `BaseEntity` tenant filter / Hibernate interceptor whenever it detects a SELECT/UPDATE/DELETE that would have touched rows whose `tenant_id` does not match the request's authenticated `tenantId`. **Any non-zero increment is a P0 security event** — even one row leaked is GDPR/PDPL exposure and contractually a SaaS-instance breach.

## Note

> This alert is **metric-pending**. The counter `kite_cross_tenant_query_total` requires the `BaseEntity` tenant filter to emit a Micrometer counter on detection (currently logs only). Track instrumentation under the relevant follow-up gap; the alert rule ships in `kitehub-platform-alerts` group with the intended expression so it activates automatically once the metric exists.

## Immediate checks (0-5 min)

1. **Stop the bleeding** — identify the offending request in flight:
   ```bash
   # Find current long-running queries on kite-postgres
   docker exec kite-postgres psql -U postgres -c \
     "SELECT pid, usename, application_name, state, query \
      FROM pg_stat_activity WHERE state = 'active' AND query NOT ILIKE '%pg_stat_activity%';"
   ```
2. **Locate the violating service** — the alert label `{{ $labels.service }}` tells you which service emitted the counter (likely `kitehub-subscription:8081`, `kitehub-admin:8085`, or `kiteclass-core`). Pull last 200 lines:
   ```bash
   kubectl logs -n kitehub deploy/<service> --tail=200 | grep -E 'tenant_id|TenantFilter|cross.tenant'
   ```
3. **Check audit log** — `audit_log` table records every tenant-context switch:
   ```bash
   docker exec kite-postgres psql -U postgres -d kitehub -c \
     "SELECT created_at, actor_id, action, target_tenant_id, details \
      FROM audit_log WHERE created_at > now() - interval '15 minutes' \
      AND action ILIKE '%TENANT%' ORDER BY created_at DESC LIMIT 50;"
   ```
4. **Recent deploys?** — `gh run list --workflow=docker-build-push.yml --limit 5`. A new entity class shipping without `BaseEntity` extension is the most common source.

## Likely causes

- **New entity class missing `extends BaseEntity`** → developer added a `@Entity` class that bypasses the global Hibernate `@Filter` because BaseEntity's `tenant_id` column + filter annotation were skipped. **Fix:** revert the entity, re-introduce extending `BaseEntity`, redeploy. Verify via grep: `grep -L "extends BaseEntity" $(grep -rl "@Entity" kitehub/*/src/main/java)`.
- **Native SQL query without `WHERE tenant_id = :tenantId`** → `@Query(nativeQuery = true)` calls bypass the JPA filter. **Fix:** rewrite as JPQL, or add explicit tenant predicate. Search: `grep -rn "nativeQuery = true" kitehub/*/src/main/java`.
- **Admin endpoint with stale tenant context** → an admin tool uses `RequestContextHolder` after the original request thread ended (e.g. async work queued without re-establishing tenant). **Fix:** ensure `@Async` / RabbitMQ consumers explicitly set `TenantContext.setTenantId(...)` from message headers.
- **JWT decoded but `tenantId` claim missing** → gateway forwarded request without injecting the tenant header; service falls back to last-cached tenant. **Fix:** verify `kite-gateway:9000` `JwtAuthenticationFilter` always sets `X-Tenant-Id` header from claim.
- **Cache cross-contamination** → Redis/Caffeine cache key missing tenant prefix returns prior tenant's data. **Fix:** audit cache `@Cacheable` keys for `#tenantId` participation.

## Mitigation

```bash
# 1. IMMEDIATE — quarantine the suspect service replicas (preserve pods for forensics, route traffic away)
kubectl scale deployment/<service> -n kitehub --replicas=0
# Capture pod state before deletion if needed
kubectl logs -n kitehub deploy/<service> --tail=2000 > /tmp/incident-$(date +%s).log

# 2. Roll back to last known good version
kubectl rollout undo deployment/<service> -n kitehub
# OR Docker dev: cd kitehub && ./scripts/rebuild.sh <service>  (after reverting commit)

# 3. Snapshot audit_log + pg_stat_activity for forensics
docker exec kite-postgres pg_dump -U postgres -t audit_log kitehub \
  | gzip > /tmp/audit-snapshot-$(date +%s).sql.gz

# 4. Notify security lead — DO NOT WAIT for full root-cause analysis
# Per incident-response-runbook.md §1, multi-tenant leak = mandatory all-hands page.
```

After mitigation, verify the counter stops incrementing and no further `cross.tenant` log entries appear for at least 30 min.

## When to escalate

- **Immediate (T+0):** any non-zero increment of this counter pages security lead AND backup on-call. No 5-min triage window — this is "page first, investigate while paging."
- **Customer notification (T+15 min):** if the leaked rows include another tenant's PII, legal + customer-success must be looped in for tenant breach notification per PDPL/GDPR (72h notification window starts from confirmation).
- **External regulator notification:** if confirmed material exposure, per `documents/05-guides/infrastructure/SECRET-MANAGEMENT.md` and PDPL governance, escalate to compliance owner within 24h.

## Related

- Alert rule: `kitehub/docker/prometheus/alert-rules.yml` (kitehub-platform-alerts group), `infrastructure/helm/kitehub/templates/prometheusrule.yaml`
- Architecture: `documents/02-architecture/` — tenant isolation model; `BaseEntity` tenant filter
- Memory: `feedback_jpa_jsonb_jdbctypecode.md` (latent JPA gotchas), `feedback_repo_status_security_coverage.md`
- Related runbooks: [`service-down.md`](./service-down.md), [`jwt-auth-failure-spike.md`](./jwt-auth-failure-spike.md), [`../../incident-response-runbook.md`](../../incident-response-runbook.md) §1 (P0 security)
