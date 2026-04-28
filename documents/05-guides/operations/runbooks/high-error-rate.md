# Runbook: High Error Rate

**Alert:** `HighErrorRate`
**Severity:** warning
**Last updated:** 2026-04-28

## What does this alert mean?

For a given service (`{{ $labels.job }}`), the ratio of HTTP 5xx responses to total responses, computed over a 5-minute rolling window, has exceeded **5%** for 5 minutes. Either the service is partially crashing on a class of requests, or a downstream dependency it calls is failing. This is a **degraded** state — not down, but users are seeing errors. If it crosses ~25% or persists past 15 min, treat as P1 (`incident-response-runbook.md` §1).

## Immediate checks (0-5 min)

1. Identify which endpoints are failing — group 5xx by URI:
   ```bash
   kubectl logs -n kitehub deploy/<svc> --tail=500 \
     | grep -E '"status":5\d\d' \
     | jq -r '"\(.status) \(.uri)"' | sort | uniq -c | sort -rn
   ```
2. Recent deploy correlated? — `gh run list --workflow=docker-build-push.yml --limit 5` then compare deploy time vs alert `for: 5m` start
3. Downstream health — check the likely culprits for each service:
   - `kitehub-subscription` → Stripe webhook responses, `kite-postgres`, `kitehub-email` queue
   - `kitehub-branding` → Ollama (`http://localhost:11434/api/tags`), `kite-minio` (`http://kite-minio:9000/minio/health/live`), `kite-postgres`
   - `kitehub-email` → SMTP provider, `kite-rabbitmq` `email.send` queue depth
   - `kiteclass-core` → `kite-postgres` HikariCP pool (see [`database-pool-exhausted.md`](./database-pool-exhausted.md))
   - `kite-gateway` → all downstream services + their service-discovery health
4. Circuit-breaker state — `curl http://<pod>:<port>/actuator/circuitbreakers | jq` — open breaker = upstream fault already isolated

## Likely causes

- **Hibernate jsonb VARCHAR-vs-jsonb mismatch on a write path** → see `feedback_jpa_jsonb_jdbctypecode.md`. If a recent migration added a `@Column(columnDefinition = "jsonb")` field without `@JdbcTypeCode(SqlTypes.JSON)`, every write to that column throws `column "X" is of type jsonb but expression is of type character varying` — surfaces as 500 on the controller. **Fix:** add the annotation, redeploy. Detection: run the grep in the feedback file against the affected service.
- **ObjectMapper missing JSR-310 module on a code path** → see `feedback_objectmapper_test_jsr310.md`. Production code uses Spring's autowired ObjectMapper (JavaTimeModule registered), but if any code constructed `new ObjectMapper()` directly (rare in main, common in adapter shims), `Instant`/`LocalDateTime` serialization throws. Outbox writes silently swallow the exception per Exception A in `design-patterns.md` §3.5.1, but downstream callers see the request fail. **Fix:** replace bare `new ObjectMapper()` with `findAndRegisterModules()` or inject the bean.
- **Downstream dependency degraded** — Stripe outage, Ollama OOM, MinIO unreachable. Circuit breaker should trip; if it doesn't, the resilience config is missing per `design-patterns.md` §3.6.
- **Validation regression after request-body schema change** — DTO field renamed/required, frontend still sending old shape. Returns 4xx (not 5xx) normally, but `@RequestBody` parse errors surface as 500 if no exception handler is wired.
- **DB connection pool exhausted** — under load, requests timeout waiting for a HikariCP connection (`SQLTransientConnectionException`). Cross-references [`database-pool-exhausted.md`](./database-pool-exhausted.md).

## Mitigation

```bash
# 1. Recent deploy is the smoking gun → rollback
kubectl rollout undo deployment/<svc> -n kitehub
# Verify error rate drops within 5-10 min on the next scrape window

# 2. Single endpoint failing → toggle feature flag if available
# (Most KiteHub services support config refresh via /actuator/refresh)

# 3. Downstream dep degraded but no circuit breaker → manually trip
curl -X POST http://<pod>:<port>/actuator/circuitbreakers/<name>/state \
  -d '{"state":"OPEN"}' \
  -H 'Content-Type: application/json'
# Buys time while you fix the upstream fault

# 4. Pool exhaustion → see database-pool-exhausted.md mitigation
```

After mitigation, watch error-rate ratio drop in Grafana (when GAP-143 lands) or directly via Prometheus query:
```
sum by (job) (rate(http_server_requests_seconds_count{status=~"5.."}[5m])) /
sum by (job) (rate(http_server_requests_seconds_count[5m]))
```

## When to escalate

- Error rate stays >5% for 15 min after mitigation attempt → P1, escalate per `incident-response-runbook.md` §1
- Error rate climbs above 25% → P0 (effectively service down for affected endpoints)
- Multiple services co-fire `HighErrorRate` → likely shared dep (DB, broker, MinIO) — pivot to infra-level investigation
- Customer escalation incoming → tenant communication per `incident-response-runbook.md` §5

## Related

- Alert rule: `kitehub/docker/prometheus/alert-rules.yml` line 17, `kiteclass/docker/prometheus/alert-rules.yml` line 13, `infrastructure/helm/kitehub/templates/prometheusrule.yaml` line 26
- Memory: `feedback_jpa_jsonb_jdbctypecode.md`, `feedback_objectmapper_test_jsr310.md`, `feedback_thymeleaf_ognl_pin.md`
- Related runbooks: [`service-down.md`](./service-down.md) (often follows), [`database-pool-exhausted.md`](./database-pool-exhausted.md), [`high-response-time.md`](./high-response-time.md) (often co-fires)
- Architecture: `documents/02-architecture/` + ADR-021 outbox pattern (`design-patterns.md` §3.5.1)
