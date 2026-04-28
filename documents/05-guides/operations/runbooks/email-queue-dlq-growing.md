# Runbook: Email Queue DLQ Growing

**Alert:** `EmailQueueDLQGrowing`
**Severity:** warning
**Last updated:** 2026-04-28

## What does this alert mean?

The Dead-Letter Queue for `kitehub-email`'s outgoing email queue has accumulated **>10 messages over 10 minutes**. Source: `rabbitmq_queue_messages_ready{queue="emails.send.dlq"}`. Messages land in the DLQ after the primary `emails.send` queue's retry policy (typically 3 attempts with exponential backoff) is exhausted. Each DLQ message is a customer not receiving their email — invitation, password reset, invoice, subscription renewal notice. Sustained growth = trust hit + support volume spike.

## Immediate checks (0-5 min)

1. **Queue depth + DLQ contents** via RabbitMQ admin UI:
   - Open `http://kite-rabbitmq:15672` (creds in `kite-rabbitmq` env or K8s secret)
   - Queues tab → search `email` → check `emails.send`, `emails.send.retry`, `emails.send.dlq`
   - Click DLQ → "Get Messages" → preview latest 5 messages (set Ackmode = Reject and requeue)
2. **kitehub-email service health:**
   ```bash
   kubectl logs -n kitehub deploy/kitehub-email --tail=200 \
     | grep -E 'ERROR|Failed|DLQ|RetryExhausted|SendGrid|SMTP|smtp' -A 2
   curl -fsS http://kitehub-email:8084/actuator/health | jq .
   ```
3. **External provider health** — SendGrid / SES / SMTP relay:
   ```bash
   # SendGrid status
   curl -sS https://status.sendgrid.com/api/v2/status.json | jq '.status'
   # SES (region-specific)
   aws sesv2 get-account --region <region> | jq '.SendingEnabled, .EnforcementStatus'
   ```
4. **Recent deploy?** Was kitehub-email or its template module updated in the last hour?

## Likely causes

- **Provider credential rotated** → API key/SMTP password in `email-credentials` secret no longer valid; provider returns 401/403, retry exhausts. **Fix:** rotate credential, update K8s secret, restart kitehub-email pods.
- **Template variable missing** → a new email template references `{{ user.firstName }}` but consumer payload doesn't include it; render throws `MissingPropertyException` per message, all retries fail identically. **Fix:** roll back template, OR fix payload producer (typically kitehub-subscription or kitehub-admin).
- **Rate limit exceeded with provider** → bulk send (e.g. trial-renewal blast) tripped SendGrid hourly limit. **Fix:** drain DLQ via slow replay (see Mitigation step 4), tune outbound rate limit.
- **DNS/domain reputation issue** → emails accepted by SMTP but bounced; bounce webhook re-queues to DLQ. **Fix:** check sender reputation, SPF/DKIM/DMARC alignment via `dig TXT _dmarc.<domain>`.
- **Recipient address blacklist** — hard bounces flagged the senders' domain. **Fix:** drop those addresses from queue, file with provider.
- **Outbox event published but consumer down** — if kitehub-email was crashlooping, retry exhausted while service recovering. **Fix:** restart, then replay DLQ from message-time stamp forward (per `design-patterns.md` §3.5.1, outbox is the reliability net for events; the DLQ holds the final-failed delivery attempts).

## Mitigation

```bash
# 1. Inspect the most recent DLQ messages to identify failure pattern
# RabbitMQ Admin UI: emails.send.dlq → Get messages → Requeue=false, count=5
# Look at x-death header: count, exchange, routing-key, reason

# 2. If failures are template/payload bugs (uniform error), fix code FIRST then replay
# Roll back deployment if needed:
kubectl rollout undo deployment/kitehub-email -n kitehub

# 3. If provider issue resolved, replay DLQ → retry queue via shovel or admin UI
# CLI shovel (one-time):
docker exec kite-rabbitmq rabbitmqctl set_parameter shovel email-dlq-replay '{
  "src-uri":"amqp://","src-queue":"emails.send.dlq",
  "dest-uri":"amqp://","dest-exchange":"emails","dest-exchange-key":"emails.send"
}'
# After drain, remove the shovel:
docker exec kite-rabbitmq rabbitmqctl clear_parameter shovel email-dlq-replay

# 4. If individual messages are poison (cannot ever succeed), purge selectively
# Capture them first:
curl -u "$RABBIT_USER:$RABBIT_PASS" -X POST \
  http://kite-rabbitmq:15672/api/queues/%2F/emails.send.dlq/get \
  -d '{"count":50,"ackmode":"ack_requeue_false","encoding":"auto"}' > /tmp/dlq-snapshot-$(date +%s).json
```

After mitigation, the queue should drain within 15-30 min. Watch for re-accumulation within 2h — if it returns, root cause not addressed.

## When to escalate

- DLQ exceeds 100 messages OR 1h of growth without drainage → escalate to platform lead; consider incident-response notification to customer-success
- Critical-path emails (password reset, payment failure notice) in DLQ → P0-bump severity, may require manual dispatch via CSV+admin tool
- Provider relationship issue (sender reputation, account suspended) → escalate to product/business owner

## Related

- Alert rule: `kitehub/docker/prometheus/alert-rules.yml` (kitehub-platform-alerts group), `infrastructure/helm/kitehub/templates/prometheusrule.yaml`
- Architecture: kitehub-email service config (`EmailQueueConfig`); `.claude/rules/design-patterns.md` §3.5.1 (Outbox reliability net)
- Related runbooks: [`rabbitmq-queue-backlog.md`](./rabbitmq-queue-backlog.md), [`subscription-webhook-failure.md`](./subscription-webhook-failure.md), [`service-down.md`](./service-down.md)
