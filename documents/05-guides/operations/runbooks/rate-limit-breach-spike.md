# Runbook: Rate Limit Breach Spike

**Alert:** `RateLimitBreachSpike`
**Severity:** warning
**Last updated:** 2026-04-28

## What does this alert mean?

Per-tenant rate limiter (Bucket4j or equivalent at `kite-gateway:9000` and downstream services) is rejecting requests with HTTP 429 at **>10x baseline over 5 minutes**, OR a single tenant's 429 share exceeds 50%. The metric `kite_rate_limit_rejected_total{tenant_id, route}` identifies the offender. Two profiles: **legitimate hot tenant** outgrowing their tier (need to raise limits or upsell), or **abuse** (compromised tenant API key, runaway client retry loop, scraper). Either way, it's a signal — don't auto-raise limits, investigate.

## Note

> Metric `kite_rate_limit_rejected_total` requires the rate-limit filter to emit Micrometer counters with `tenant_id` and `route` labels (cardinality-bounded — bucket tenant IDs by tier or hash if too high). Until wired, surface via correlated `HighErrorRate{path=..., status="429"}`.

## Immediate checks (0-5 min)

1. **Identify the noisiest tenant + route:**
   ```bash
   kubectl logs -n kitehub deploy/kite-gateway --tail=2000 \
     | grep ' 429 ' | awk '{print $<tenant-field>, $<route-field>}' \
     | sort | uniq -c | sort -rn | head -10
   ```
2. **Per-tenant subscription tier** — is the limit appropriate for their plan?
   ```bash
   docker exec kite-postgres psql -U postgres -d kitehub -c \
     "SELECT t.id, t.slug, s.tier, s.status \
      FROM tenant t JOIN subscription s ON s.tenant_id = t.id \
      WHERE t.id = '<offending_tenant_id>';"
   ```
3. **Pattern of rejected calls** — same endpoint repeatedly, or distributed?
   - Same endpoint + tight interval → likely runaway client retry loop
   - Distributed reads → legitimate growth or scraping
4. **Cross-correlate with auth signal** — is this paired with [`jwt-auth-failure-spike.md`](./jwt-auth-failure-spike.md)? Concurrent rate-limit + auth spike usually = abuse.

## Likely causes

- **Hot tenant, legitimate growth** → tenant adopted new feature, traffic genuinely 5x. **Fix:** reach out via customer-success, offer tier upgrade; raise limit only after commercial conversation.
- **Compromised tenant API key** → key leaked in their public repo, scraper using it. **Fix:** rotate the tenant's API key, notify their admin contact, document the incident in `documents/05-guides/infrastructure/SECRET-MANAGEMENT.md`.
- **Runaway retry loop in tenant's integration** → their custom integration hits a 5xx and infinitely retries without backoff. **Fix:** contact tenant; share retry-with-backoff guidance; consider temporarily lowering their limit further to force them to fix.
- **Recent feature ships with chatty client** — kiteclass-frontend pushed a polling loop that spams `/api/v1/branding/{id}/package` instead of using ETag conditional revalidate (per `ai-branding-guidelines.md` §7.1). **Fix:** verify FE caches with ETag + 304-honoring; backport fix; bump cache TTL.
- **Synthetic monitor / load test misconfigured** — internal load test fired against prod tenant inadvertently. **Fix:** identify the source (User-Agent, IP), shut it down.
- **Bucket4j config drift** → recent change tightened limits (e.g. accidentally set 100/hr instead of 100/min). **Fix:** revert config; per `output-review-mandate.md`, code config changes require review.

## Mitigation

```bash
# 1. Identify offending tenant from metrics, then check their request profile
docker exec kite-postgres psql -U postgres -d kitehub -c \
  "SELECT route, count(*) FROM api_request_log \
   WHERE tenant_id='<id>' AND ts > now() - interval '15 minutes' AND status=429 \
   GROUP BY route ORDER BY count(*) DESC LIMIT 10;"

# 2. If abuse: rotate tenant API key + force re-auth all sessions for that tenant
curl -X POST http://kitehub-admin:8085/api/v1/internal/tenants/<id>/api-keys/rotate \
  -H "Authorization: Bearer $INTERNAL_API_SECRET"
curl -X POST http://kite-gateway:9000/api/v1/internal/sessions/invalidate \
  -H "Authorization: Bearer $INTERNAL_API_SECRET" \
  -d '{"tenant_id":"<id>"}'

# 3. If legitimate growth: temporarily raise their limit while customer-success closes upsell
curl -X POST http://kite-gateway:9000/api/v1/internal/rate-limits/<id> \
  -H "Authorization: Bearer $INTERNAL_API_SECRET" \
  -d '{"requests_per_minute": 600, "expires_at":"2026-05-01T00:00:00Z"}'

# 4. If runaway client: contact tenant, then enforce stricter limit to force fix
curl -X POST http://kite-gateway:9000/api/v1/internal/rate-limits/<id> \
  -H "Authorization: Bearer $INTERNAL_API_SECRET" \
  -d '{"requests_per_minute": 30, "expires_at":"2026-04-29T00:00:00Z"}'
```

After mitigation, monitor 429 rate by tenant for 30 min. Document the incident with the tenant ID + decision in tenant audit log so customer-success has context.

## When to escalate

- Cluster-wide 429 rate >5% for >10 min (not single tenant) → infrastructure issue (limiter mis-configured or filter performance), engage platform lead
- Confirmed abuse with successful breaches OR data exfil signals → invoke [`multi-tenant-data-leak.md`](./multi-tenant-data-leak.md), security incident
- Repeat offender (same tenant 3rd incident in 7 days) → escalate to product lead for commercial action

## Related

- Alert rule: `kitehub/docker/prometheus/alert-rules.yml` (kitehub-platform-alerts group), `infrastructure/helm/kitehub/templates/prometheusrule.yaml`
- Memory: `feedback_repo_status_security_coverage.md`
- Doc: `documents/05-guides/infrastructure/SECRET-MANAGEMENT.md`
- Related runbooks: [`jwt-auth-failure-spike.md`](./jwt-auth-failure-spike.md), [`multi-tenant-data-leak.md`](./multi-tenant-data-leak.md), [`high-error-rate.md`](./high-error-rate.md)
