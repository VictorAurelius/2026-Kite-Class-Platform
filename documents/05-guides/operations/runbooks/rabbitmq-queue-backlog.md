# Runbook: RabbitMQ Queue Backlog

**Alert:** `RabbitMQQueueBacklog`
**Severity:** warning
**Last updated:** 2026-04-28

## What does this alert mean?

A RabbitMQ queue has had **>1000 ready messages** for **10 minutes**. `{{ $labels.queue }}` is the queue name (e.g. `email.send`, `branding-jobs`, `ai.request.free`, `instance.purge.subscription`, `kitehub.admin.subscription-events`). Either consumers are down/slow, messages are poisoning + failing repeatedly, or producer rate spiked beyond consumer throughput. Asynchronous events stack up — users don't see immediate breakage, but downstream effects compound (delayed welcome emails, stalled branding jobs, lost cache invalidations).

## Immediate checks (0-5 min)

1. Identify queue + check consumer count + ready/unacked breakdown:
   ```bash
   # RabbitMQ Management UI: http://kite-rabbitmq:15672 (default kitehub:kitehub in dev)
   # Or CLI:
   kubectl exec -n kitehub kite-rabbitmq -- rabbitmqctl list_queues \
     name messages_ready messages_unacknowledged consumers
   ```
   - `consumers: 0` = no listener attached → service down (cross-ref [`service-down.md`](./service-down.md))
   - `messages_unacknowledged` high → consumer slow / stuck / hung
   - `messages_ready` climbing, `consumers > 0` → throughput mismatch
2. Dead-letter queue check — poison messages?
   ```bash
   kubectl exec -n kitehub kite-rabbitmq -- rabbitmqctl list_queues name messages_ready \
     | grep -E '\.dlq|-dlq'
   ```
3. Consumer health — if listener service is up:
   ```bash
   curl -s http://<consumer-pod>:<port>/actuator/health | jq '.components.rabbit'
   # Look for status: UP, version, virtualHost
   ```
4. Recent traffic spike vs steady state — Prometheus:
   ```
   rate(rabbitmq_queue_messages_published_total[5m])
   rate(rabbitmq_queue_messages_acknowledged_total[5m])
   ```
   If publish-rate >> ack-rate, gap is widening.

## Likely causes (per queue)

### `email.send` (kitehub-subscription → kitehub-email)
- **`kitehub-email` consumer down** — see `EmailQueueConfig.EMAIL_QUEUE` constant. Check `kubectl get pods -n kitehub | grep email`.
- **SMTP provider rate-limiting** — provider returning 429s causes consumer to slow / retry → backlog. Check email service logs for `MailSendException` rate.
- **Poison message in `email.dlq`** — bad template variable substitution, missing recipient. DLQ has its own queue (`EmailQueueConfig.EMAIL_DLQ`). Drain manually after fix lands.

### `branding-jobs` + `ai.request.{free,pro,enterprise}` (kitehub-branding)
- **Ollama slow / OOMing** — see `feedback_gap006_infra_blocker.md`. Ollama on WSL2 CPU-only is too slow for 9B models; in prod Ollama instance saturated by parallel requests. Check `http://ollama:11434/api/ps` for active models.
- **`AIQueueDispatcher` routing wrong queue** — see `design-patterns.md` §3.5.1 Exception D. Class is dedicated dispatcher infrastructure. Per-tier queues: `ai.request.free`, `ai.request.pro`, `ai.request.enterprise` (`AIQueueConfig`). FREE-tier backlog under load is expected — bound by `ai-branding-guidelines.md` §4.3 quotas.
- **Branding consumer crashed mid-message** — `messages_unacknowledged` will be high; broker will redeliver after consumer reconnects or visibility timeout expires.

### `instance.purge.subscription` (kitehub-subscription)
- **InstancePurgeService** path; backlog = tenants pending hard-delete after off-boarding. See `tenant-off-boarding-runbook.md`. Per `PurgeQueueConfig`. Backlog usually = scheduled job not yet run, NOT a failure.

### `kitehub.admin.subscription-events`, `kitehub.admin.instance-events` (kitehub-admin)
- **Admin service consumer issues** — consumer is fanout subscriber from subscription/instance domain events. `RabbitListenerConfig` in admin module. Backlog = admin dashboard going stale; non-critical but should drain.

### `email.branding.updated` (kitehub-email subscriber to branding)
- **Branding events backing up** — `BrandingEventsConfig`. Email service consumes branding-update events to invalidate template cache. Backlog = stale template cache, edge-case impact.

## Mitigation

```bash
# 1. Consumer down → restart
kubectl rollout restart deployment/<consumer-svc> -n kitehub
# Verify queue starts draining within 1-2 min

# 2. Throughput mismatch → scale consumer horizontally
kubectl scale deployment/<consumer-svc> --replicas=4 -n kitehub
# RabbitMQ load-balances across consumers automatically

# 3. Poison messages stuck in DLQ → drain after fix
# View DLQ first to know what's there
kubectl exec -n kitehub kite-rabbitmq -- rabbitmqadmin get \
  queue=email.dlq count=10 ackmode=ack_requeue_false
# After analyzing + shipping fix:
kubectl exec -n kitehub kite-rabbitmq -- rabbitmqctl purge_queue email.dlq
# Coordinate with on-call before purging — those are real user actions

# 4. Producer rate spike (legitimate) → throttle producer or accept delay
# kitehub-subscription has @RateLimiter on EmailServiceClient; ensure config hasn't drifted

# 5. Move messages back from DLQ to main queue (after fix)
kubectl exec -n kitehub kite-rabbitmq -- rabbitmqadmin \
  --username=kitehub --password=<pass> \
  shovel from-uri=amqp://kitehub:<pass>@localhost \
  to-uri=amqp://kitehub:<pass>@localhost \
  src-queue=email.dlq dest-queue=email.send
```

## When to escalate

- Backlog grows past 10,000 messages → P1, escalate per `incident-response-runbook.md` §1
- Multiple queues co-fire (broker-wide issue, e.g. disk pressure on RabbitMQ node) → cross-ref [`high-disk-usage.md`](./high-disk-usage.md). Broker enters memory-paging mode at high watermark.
- DLQ has poison-loop pattern (same message ID retrying forever) → file P1 gap; do NOT blindly purge DLQ before understanding the failure
- AI queue (`ai.request.free`) backlog due to Ollama outage → coordinate with AI Branding lead before scaling consumers (each consumer holds an Ollama session = bulkhead per `design-patterns.md` §3.6)

## Related

- Alert rule: `kitehub/docker/prometheus/alert-rules.yml` line 80, `infrastructure/helm/kitehub/templates/prometheusrule.yaml` line 79 (note: `kiteclass/docker/prometheus/alert-rules.yml` does not currently include this — KiteClass-side queues consumed by KiteHub side)
- Queue config sources:
  - `kitehub/kitehub-subscription/.../config/EmailQueueConfig.java` — `email.send`, `email.dlq`
  - `kitehub/kitehub-subscription/.../config/PurgeQueueConfig.java` — `instance.purge.subscription`
  - `kitehub/kitehub-branding/.../config/RabbitMQConfig.java` — `branding-jobs`, `branding-jobs-dlq`
  - `kitehub/kitehub-branding/.../config/AIQueueConfig.java` — `ai.request.{enterprise,pro,free}`
  - `kitehub/kitehub-admin/.../event/RabbitListenerConfig.java` — `kitehub.admin.*`
- Memory: `feedback_gap006_infra_blocker.md` (Ollama infra), `feedback_objectmapper_test_jsr310.md` (silent serialization failure can mask outbox writes)
- Rules: `design-patterns.md` §3.5 Outbox + §3.5.1 Exception D dispatcher pattern, `ai-branding-guidelines.md` §3.3 (heavy tasks async), `project_outbox_per_module_pattern.md`
- Related runbooks: [`service-down.md`](./service-down.md), [`high-disk-usage.md`](./high-disk-usage.md), [`high-memory-usage.md`](./high-memory-usage.md)
