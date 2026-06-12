# Runbook: Tenant Provisioning Failure

**Alert:** `TenantProvisioningFailure`
**Severity:** warning
**Last updated:** 2026-04-28

## What does this alert mean?

The tenant provisioning Saga (driven from `kitehub-admin:8085`, orchestrating `kitehub-subscription`, `kitehub-branding`, `kiteclass-core`) is failing on **>5% of attempts over a 5-min window**. Each failed provisioning means a paying or trial customer cannot get their KiteClass instance — onboarding broken. Sources of failure span: subscription creation, schema/DB seed, branding initial generation, gateway routing setup, DNS record creation. Saga compensations should auto-roll-back, but failures during compensation leave half-provisioned tenants requiring manual cleanup.

## Note

> Metric `kite_tenant_provisioning_total{outcome}` requires the Saga orchestrator to emit Micrometer counters at each terminal state (success, failed_at_step_N, compensated). Until that is wired, alert is **metric-pending** in `kitehub-platform-alerts` group.

## Immediate checks (0-5 min)

1. **Scope** — single tenant or cluster of tenants?
   ```bash
   kubectl logs -n kitehub deploy/kitehub-admin --tail=300 \
     | grep -E 'Provisioning|Saga|TenantSetup|Compensation|Step' -A 3
   ```
2. **Saga state** — outbox + saga-state tables track which step failed:
   ```bash
   docker exec kite-postgres psql -U postgres -d kitehub -c \
     "SELECT tenant_id, current_step, status, error_message, updated_at \
      FROM provisioning_saga_state WHERE updated_at > now() - interval '15 minutes' \
      ORDER BY updated_at DESC LIMIT 20;"
   ```
3. **Per-step health check** — each downstream service the Saga calls:
   ```bash
   for svc in kitehub-subscription:8081 kitehub-branding:8083 kiteclass-core:8086 kite-gateway:9000; do
     echo "=== $svc ==="
     curl -fsS http://$svc/actuator/health | jq '.status, .components | keys'
   done
   ```
4. **Recent change?** — `gh run list --workflow=docker-build-push.yml --limit 5`. New Saga step deployed?

## Likely causes

- **Branding pipeline failing** → kitehub-branding's AI inference is degraded, initial branding step times out. **Fix:** see [`ai-provider-high-failure-rate.md`](./ai-provider-high-failure-rate.md). Saga should fall back to template-default and continue (per `ai-branding-guidelines.md` §3 — TEMPLATE-first).
- **Subscription create idempotency violation** → retry of a failed step hits unique-constraint on customer email. **Fix:** make Saga step idempotent — check existing subscription first, treat duplicate as success.
- **DNS/route registration timeout** → gateway needs to register `<slug>.kitehub.me`; DNS provider API slow or rate-limited. **Fix:** raise step timeout or batch DNS calls.
- **Database seed migration regression** → kiteclass-core's per-tenant schema bootstrap fails on a Flyway migration. See [`flyway-migration-failure.md`](./flyway-migration-failure.md) — same class of issue, scoped per-tenant.
- **Compensation step itself failing** → e.g. Saga rolled back, but DELETE from `subscription` table fails because of FK from `invoice` (created in a parallel branch). Tenant left half-provisioned. **Fix:** investigate FK constraint; re-run cleanup tool with admin override.
- **Internal API auth missing** — `INTERNAL_API_SECRET` rotated but not updated in kitehub-admin's outbound config; downstream services 401. **Fix:** sync secret across all services that call each other internally.

## Mitigation

```bash
# 1. Identify stuck/failed tenants
docker exec kite-postgres psql -U postgres -d kitehub -c \
  "SELECT tenant_id, current_step, error_message FROM provisioning_saga_state \
   WHERE status = 'FAILED' AND updated_at > now() - interval '1 hour';"

# 2. For each failed tenant, decide: retry from current step, OR full compensation rollback
# Retry (idempotent steps):
curl -X POST http://kitehub-admin:8085/api/v1/internal/provisioning/$TENANT_ID/retry \
  -H "Authorization: Bearer $INTERNAL_API_SECRET"

# Force compensation (rolls back all completed steps + cleans up):
curl -X POST http://kitehub-admin:8085/api/v1/internal/provisioning/$TENANT_ID/compensate \
  -H "Authorization: Bearer $INTERNAL_API_SECRET"

# 3. If issue is Branding step specifically, force template fallback for new provisionings
curl -X POST http://kitehub-admin:8085/actuator/env \
  -H 'Content-Type: application/json' \
  -d '{"name":"provisioning.branding.mode","value":"template_only"}'
curl -X POST http://kitehub-admin:8085/actuator/refresh

# 4. Notify customer-success of any tenant whose provisioning was retried/compensated
# (so they can reach out if customer experienced a UI error)
```

After mitigation, the failure rate should drop within 10 min. Monitor for new failures; if pattern repeats, file follow-up gap with the specific step + error.

## When to escalate

- Failure rate >20% sustained → P0; trial/paid sign-up broken at platform level, escalate to product + platform lead
- Saga compensation itself failing on multiple tenants → manual cleanup required; engage DBA
- Cross-tenant data leakage during half-provisioned cleanup (rare) → invoke [`multi-tenant-data-leak.md`](./multi-tenant-data-leak.md)

## Related

- Alert rule: `kitehub/docker/prometheus/alert-rules.yml` (kitehub-platform-alerts group), `infrastructure/helm/kitehub/templates/prometheusrule.yaml`
- Architecture: `documents/02-architecture/` (Saga pattern), `.claude/rules/design-patterns.md` §2 (Saga = distributed txn)
- Runbook (off-boarding inverse): `documents/05-guides/operations/tenant-offboarding-runbook.md` (GAP-201)
- Related runbooks: [`ai-provider-high-failure-rate.md`](./ai-provider-high-failure-rate.md), [`flyway-migration-failure.md`](./flyway-migration-failure.md), [`branding-quality-gate-fail-rate.md`](./branding-quality-gate-fail-rate.md)
