# Runbook: Service Down

**Alert:** `ServiceDown`
**Severity:** critical
**Last updated:** 2026-04-28

## What does this alert mean?

Prometheus' `up` metric for one of the monitored Spring Boot services has been `0` for at least **1 minute** — meaning the scrape against `/actuator/prometheus` has failed (connection refused, timeout, 5xx, or pod not Ready). The `{{ $labels.job }}` label tells you which service: `kitehub-subscription` (8081), `kitehub-branding` (8083), `kitehub-email` (8084), `kitehub-admin` (8085), `kite-gateway` (9000), `kiteclass-core`, or one of the platform deps. This is a **P0 candidate** — every minute of downtime is user-facing.

## Immediate checks (0-5 min)

1. Confirm scope — single instance vs all replicas:
   ```bash
   kubectl get pods -n kitehub --field-selector=status.phase!=Running
   kubectl get pods -n kiteclass-instances --field-selector=status.phase!=Running
   # Or dev: cd kitehub && ./scripts/status.sh --simple
   ```
2. Hit `/actuator/health` directly (bypass Prometheus scrape):
   ```bash
   curl -fsS http://<pod-ip>:<port>/actuator/health | jq .
   # Gateway 9000, subscription 8081, branding 8083, email 8084, admin 8085
   ```
3. Recent deploy in last 1h? — `gh run list --workflow=docker-build-push.yml --limit 5`
4. Pod logs — last 100 lines, look for boot-time stack traces:
   ```bash
   kubectl logs -n kitehub deploy/<svc> --tail=100 | grep -E 'ERROR|Exception|Caused by' | head -20
   ```

## Likely causes

- **Boot crash from Hibernate schema-validation drift** → see `feedback_dev_profile_schema_workaround.md`. V29+ migrations declare `created_by VARCHAR(100)` while `BaseEntity.createdBy` is `Long`. If a deploy slipped a Wave 4 schema bug into prod, Hibernate will fail validation on first DB hit. **Fix:** rollback (see `rollback-procedure.md`) — do NOT bypass with `ddl-auto: create-drop` in prod. Track GAP-244 for canonical fix.
- **OGNL/Thymeleaf NoSuchMethodError** → see `feedback_thymeleaf_ognl_pin.md`. If Dependabot bumped `ognl` past 3.3.x in `kiteclass-core/pom.xml`, every Thymeleaf evaluation (e.g. `PdfGeneratorTest`, invoice render path) throws `NoSuchMethodError: ognl.OgnlContext.<init>`. **Fix:** revert pom bump, repin to `3.3.4`, redeploy.
- **OOMKilled / liveness probe timeout** → resource limits exceeded. Check `kubectl describe pod` for `OOMKilled` or `Liveness probe failed`. Often follows a regression that holds memory (cache leak, JVM heap mis-sized vs container limit).
- **Database / RabbitMQ / Redis dep down** — service can't satisfy `/actuator/health` if any required component is down. Check `kite-postgres`, `kite-rabbitmq`, `kite-redis` first if multiple services flap together.
- **Missing required env var** — e.g. gateway needs `INTERNAL_API_SECRET` (`feedback_dev_profile_schema_workaround.md` notes the dev default). Prod missing this = `IllegalStateException` at boot.

## Mitigation

```bash
# 1. Single-pod hang/crash → restart
kubectl rollout restart deployment/<svc> -n kitehub

# 2. All replicas down + recent deploy → rollback
kubectl rollout undo deployment/<svc> -n kitehub
# OR Docker Compose dev: cd kitehub && ./scripts/rebuild.sh <svc>

# 3. Resource exhaustion → scale up + increase requests
kubectl scale deployment/<svc> --replicas=3 -n kitehub

# 4. Circuit-breaker stuck open after upstream recovered → flip via actuator
curl -X POST http://<pod>:8081/actuator/circuitbreakers/<name>/state -d '{"state":"CLOSED"}'
```

After mitigation, verify `up == 1` returns in Prometheus within 30s and `/actuator/health` reports `UP` for all reported components.

## When to escalate

- All replicas down AND rollback fails → escalate to backup on-call within **5 min** (P0 → all-hands per `incident-response-runbook.md` §1)
- Multiple services down simultaneously → infra-level (check `kite-postgres`, `kite-rabbitmq`, `kite-redis`, network)
- Customer impact >15 min → tenant-facing communication per `incident-response-runbook.md` §5
- If error is `Schema-validation: wrong column type` → escalate to GAP-244 owner; do NOT attempt schema rewrite under pressure

## Related

- Alert rule: `kitehub/docker/prometheus/alert-rules.yml` line 6, `kiteclass/docker/prometheus/alert-rules.yml` line 4, `infrastructure/helm/kitehub/templates/prometheusrule.yaml` line 17
- Architecture: `documents/02-architecture/` (service map)
- Memory: `feedback_dev_profile_schema_workaround.md`, `feedback_thymeleaf_ognl_pin.md`, `feedback_jpa_jsonb_jdbctypecode.md`
- Related runbooks: [`high-error-rate.md`](./high-error-rate.md) (often co-fires), [`high-memory-usage.md`](./high-memory-usage.md), [`deployment-procedures.md`](./deployment-procedures.md), [`../../rollback-procedure.md`](../../rollback-procedure.md)
