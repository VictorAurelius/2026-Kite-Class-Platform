# Runbook — RLS Policy Violation

**Alert:** `RLSPolicyViolation`
**Severity:** P0 (security incident)
**Owner:** SRE on-call + Security
**Related rule:** [`.claude/rules/business-logic-review.md`](../../../../.claude/rules/business-logic-review.md) §5 (PDPL 2023 Art 23 compliance)
**Related gap:** [GAP-466](../../../04-quality/gaps/GAP-466-multi-tenant-postgres-rls-defense-in-depth.md)
**Source:** Wave 56, Phase 3

---

## 1. Purpose

This runbook is fired whenever the Postgres logs surface an RLS policy violation on a
tenant-scoped table — either an `UPDATE`/`INSERT`/`DELETE` that fails the policy's
`WITH CHECK` clause, or repeated `permission denied` rows logged when a query touches a
row the current `app.current_tenant_id` does not own.

A single firing is treated as a P0 because the policy is the last line of defence
against a multi-tenant data leak; a single hit means either (a) a developer-introduced
regression in `TenantAwareDataSourceInterceptor` / `TenantContext` plumbing, (b) an
intentional break-glass operation that bypassed the audit trail, or (c) an active
attack probing tenant boundaries.

---

## 2. Detection

### 2.1 Prometheus alert

`infrastructure/helm/kitehub/templates/prometheusrule.yaml` defines the
`RLSPolicyViolation` alert. It fires when
`rate(postgres_logs_rls_violations_total[5m]) > 0` — any non-zero rate of RLS
denials over a 5-minute window.

### 2.2 Manual checks (when paged)

1. **Confirm the alert is real:**
   ```bash
   kubectl logs -n monitoring deploy/prometheus | grep RLSPolicyViolation
   ```
2. **Pull the offending log lines from Postgres** (CloudWatch / `kubectl logs deploy/postgres`):
   ```bash
   kubectl logs -n data deploy/kite-postgres --since=15m | grep -i "row violates row-level security"
   ```
3. **Identify the table + user** from the Postgres log line. The format is roughly:
   ```
   ERROR:  new row violates row-level security policy "tenant_isolation" for table "<table>"
   ```

---

## 3. Triage flowchart

```
┌─ Is the offending user a known admin/migration role? ──── YES ──► §4 Break-glass path
│
NO
│
├─ Is this a single endpoint / single tenant? ──── YES ──► §5 Application regression
│
NO
└─► §6 Suspected attack
```

---

## 4. Break-glass operations (legitimate admin / migration)

Only DB superusers (`postgres`, plus the Flyway runner if it owns the table) can bypass
RLS via `SET LOCAL row_security = off`. This is allowed for:

- One-off data migrations across all tenants
- Restoring from backup (the restore tool runs with `row_security = off`)
- Reconciliation jobs that need to scan every tenant

**Audit requirement:** every break-glass session MUST be recorded:

1. File a brief change ticket in `documents/04-quality/audits/aws-verification/` with the
   command sequence + business reason.
2. Annotate the Postgres session with the operator's name:
   ```sql
   SET application_name = 'breakglass-<user>-<purpose>';
   SET LOCAL row_security = off;
   ```
3. Close the loop in the on-call channel within 60 minutes.

---

## 5. Application regression (most common cause)

Symptoms: single endpoint produces the violation; reproducible from staging.

1. **Identify the request path** (Spring's `traceId` is in MDC logs per
   `.claude/rules/logs-format-standard.md`).
2. **Check `TenantContext` propagation** — does the path bypass `TenantFilterInterceptor`?
   - Async tasks: ensure `TenantContext.runAs(tenantId, lambda)` wraps the call.
   - Scheduler jobs: same.
3. **Check the SQL** — is the path issuing native SQL or projection DTOs that introduce
   `instance_id` not matching the GUC? If so, fix the query to scope to the active
   tenant explicitly.
4. **Hotfix:** roll back the offending deploy if recent; otherwise file a P0 incident
   and ship a fix-forward PR within 24 hours.

---

## 6. Suspected attack

Symptoms: distributed sources, repeated probes across many tenants, novel user agents.

1. **Page security on-call.**
2. **Block the offending IP / API key** at the gateway (`kite-gateway` rate-limit + IP deny).
3. **Snapshot** Postgres logs + gateway access logs for the affected 15-minute window
   into S3 audit bucket (`s3://kitehub-audit-logs/incidents/<date>/`).
4. **Notify** affected tenants per PDPL 2023 Art 23 if any row was actually returned
   (the RLS policy should have blocked, but verify with the Postgres logs).

---

## 7. Post-incident

- [ ] File or update [GAP-466](../../../04-quality/gaps/GAP-466-multi-tenant-postgres-rls-defense-in-depth.md) Log section with incident summary
- [ ] If a regression: file a new follow-up gap with regression test added to `RLSEnforcementIT`
- [ ] Update this runbook §3 flowchart if a novel root cause was discovered
- [ ] Quarterly retro reviews all firings per `.claude/rules/output-review-mandate.md` §6.4

---

## 8. Related artifacts

- **Migration:** `kiteclass/kiteclass-core/src/main/resources/db/migration/V58__enable_rls_tenant_scoped_tables.sql`
- **Interceptor:** `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/common/datasource/TenantAwareDataSourceInterceptor.java`
- **Test:** `kiteclass/kiteclass-core/src/test/java/com/kiteclass/core/common/datasource/RLSEnforcementIT.java`
- **Architecture:** `documents/02-architecture/kiteclass-architecture.md` §Multi-Tenant Isolation
- **Business rule:** `documents/01-business/kiteclass/multi-tenancy/rules.md` BR-MULTITENANT-001

---

## 9. Log

- **2026-05-11**: Runbook created as Phase 3 of GAP-466 / Wave 56 RLS hardening.
