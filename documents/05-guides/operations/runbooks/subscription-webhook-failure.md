# Runbook: Subscription Webhook Failure

**Alert:** `SubscriptionWebhookFailure`
**Severity:** warning
**Last updated:** 2026-04-28

## What does this alert mean?

Inbound webhooks from the payment provider (Stripe / VNPay / MoMo) into `kitehub-subscription:8081` are returning **5xx at >5% rate over 5 minutes**, OR the provider's webhook delivery dashboard shows `failed_delivery_count` rising. Each failed webhook means a subscription state event (`invoice.paid`, `customer.subscription.updated`, `payment_intent.failed`) wasn't processed by us — the provider will retry (Stripe: up to 3 days exponential), but during the window subscriptions may be in stale state, billing rows missing, or trial→paid transitions not happening. **Billing pipeline integrity is at risk.**

## Note

> Metric `kite_webhook_request_total{provider, status_class}` requires a Spring filter on `WebhookController` paths emitting Micrometer counters. Until instrumented, the alert may surface via correlated `HighErrorRate{service="kitehub-subscription", path=~"/api/v1/webhooks/.*"}`. Track instrumentation in follow-up gap.

## Immediate checks (0-5 min)

1. **Identify failing provider/endpoint:**
   ```bash
   kubectl logs -n kitehub deploy/kitehub-subscription --tail=300 \
     | grep -E '/webhooks/|WebhookController|Stripe|VNPay|signature|invalid' -A 3
   ```
2. **Check provider's webhook console:**
   - Stripe: Dashboard → Developers → Webhooks → endpoint → recent events
   - Look for `200`/`400`/`500` and the response body Stripe captured
3. **Verify endpoint health:**
   ```bash
   curl -fsS http://kitehub-subscription:8081/actuator/health | jq .
   curl -fsS -o /dev/null -w "%{http_code}\n" \
     http://kitehub-subscription:8081/api/v1/webhooks/stripe \
     -H "Stripe-Signature: noop"  # expect 400 (bad signature) not 5xx
   ```
4. **Outbox table** — webhook handler should write to outbox per `design-patterns.md` §3.5.1; verify no backlog stuck:
   ```bash
   docker exec kite-postgres psql -U postgres -d kitehub -c \
     "SELECT count(*) FROM outbox_event WHERE published_at IS NULL;"
   ```

## Likely causes

- **Webhook signing key rotated, not yet updated in app config** → controller rejects all incoming with `Invalid signature`, returns 400. Provider sees 400 storm. **Fix:** rotate `STRIPE_WEBHOOK_SECRET` in K8s secret, restart pods. Verify by replaying one event from Stripe console.
- **DB unavailable / outbox write failing** → @Transactional handler can't commit, returns 500. **Fix:** check `kite-postgres:5433` connectivity, see [`database-pool-exhausted.md`](./database-pool-exhausted.md).
- **Idempotency key collision** — a recent migration changed unique constraint on `webhook_event` table; duplicate events now throw constraint violation that gets re-thrown as 500 instead of returning 200 with no-op. **Fix:** wrap duplicate detection to return 200 OK (idempotent reply per Stripe contract).
- **Outbox bypass regression** — recent code change introduced direct `rabbitTemplate.convertAndSend(...)` from webhook handler without outbox row (per `design-patterns.md` §3.5.1 BANNED unless Exception A/B/C/D). On broker hiccup, event lost, downstream subscription state drifts. **Fix:** revert direct publish; route through `OutboxEventWriter` or `MigrationEventEmitter` (per ADR-021).
- **Service restart during high webhook volume** — pods cycled mid-batch; provider sees connection-reset → retries → 4-5 retries against unhealthy gateway. **Fix:** verify pod readiness gate; consider PodDisruptionBudget tightening.
- **JsonProcessingException without JSR-310** — see `feedback_objectmapper_test_jsr310.md`. ObjectMapper deserializing webhook payload with `Instant` timestamps fails silently if test infra used; same drift can hit prod if a `new ObjectMapper()` was introduced. **Fix:** verify all ObjectMappers use `findAndRegisterModules()` OR are `@Autowired` (Spring's bean is configured correctly).

## Mitigation

```bash
# 1. If credential issue, rotate webhook secret with provider + update K8s secret
kubectl create secret generic stripe-credentials \
  --from-literal=webhook_secret='whsec_NEW' \
  --namespace=kitehub --dry-run=client -o yaml | kubectl apply -f -
kubectl rollout restart deployment/kitehub-subscription -n kitehub

# 2. Replay missed events from provider dashboard (Stripe: events list → "Resend")
#    — replay rate-limited; do in batches of 20

# 3. Drain backlogged outbox events if outbox publisher itself was stuck
curl -X POST http://kitehub-subscription:8081/actuator/outbox/flush  # if endpoint exists; else restart

# 4. Manual reconciliation for a known event ID (if specific subscription drifted)
curl -X POST http://kitehub-subscription:8081/api/v1/webhooks/stripe/replay \
  -H "Authorization: Bearer $INTERNAL_API_SECRET" \
  -d '{"event_id":"evt_ABC123"}'
```

After mitigation, monitor failure rate for 30 min. Reconcile any subscription whose state may have drifted by reading recent provider events vs `subscription` table:

```bash
docker exec kite-postgres psql -U postgres -d kitehub -c \
  "SELECT id, status, current_period_end, updated_at \
   FROM subscription WHERE updated_at < now() - interval '30 minutes' \
   AND status IN ('past_due', 'unpaid');"
```

## When to escalate

- Failure persists >1h despite credential rotation → escalate to platform + finance lead; risk of revenue mis-recognition
- Outbox backlog growing while webhook 200s return — events accepted but not processed downstream → P0 (silent data drift, hardest to detect)
- Cross-provider failure (Stripe + VNPay simultaneously) → infra problem, not provider; check egress and DNS

## Related

- Alert rule: `kitehub/docker/prometheus/alert-rules.yml` (kitehub-platform-alerts group), `infrastructure/helm/kitehub/templates/prometheusrule.yaml`
- Architecture: `documents/02-architecture/` subscription billing flow, `.claude/rules/design-patterns.md` §3.5.1 (Outbox)
- ADR: `documents/02-architecture/adr/` ADR-021 (per-module outbox)
- Memory: `feedback_objectmapper_test_jsr310.md`, `feedback_post_merge_doc_sync.md`
- Related runbooks: [`email-queue-dlq-growing.md`](./email-queue-dlq-growing.md), [`database-pool-exhausted.md`](./database-pool-exhausted.md), [`high-error-rate.md`](./high-error-rate.md)
