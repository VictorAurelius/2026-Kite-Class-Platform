# GAP-607 — RabbitMQ Dead-Letter Queue (DLQ) chưa configured; poison messages retry vô hạn

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** DevOps + Backend
**Found:** 2026-05-17 (Wave 90 walkthrough — GAP-606 admin-new-login-alert poison message gây log spam)
**Affects:** Mọi RMQ consumer (kitehub-subscription `EmailConsumer`, branding consumers, audit consumers) — bất kỳ failure permanent (missing template, invalid payload, downstream 4xx) đều spam retry forever

## Problem

EmailConsumer listening RMQ queue. Khi message processing fails permanently (vd HTTP 500 do template missing — GAP-606), Spring AMQP default behavior = re-deliver indefinitely. Không có dead-letter exchange/queue configured.

Wave 90 evidence: `docker logs kitehub-subscription | grep ConditionalRejectingErrorHandler` shows same admin-new-login-alert message reprocessed ~10×/sec cho >24h kể từ Wave 88 cutover. RMQ container CPU + log volume waste; legitimate messages (như beta.invite nếu dispatcher chạy) phải chờ queue clear.

Config evidence (need verify):
- `EmailQueueConfig` likely declares queue WITHOUT `x-dead-letter-exchange` argument
- No DLX defined trong RMQ management
- Consumer container `concurrency` setting forces re-delivery on exception

## Root cause

Phase 1 BETA scaffolding shipped RMQ với happy-path config only. DLQ + backoff + max-redelivery is hardening that wasn't prioritized. Acceptable for dev but harmful for production with real poison messages.

## Production impact

🟠 (today) Log spam + RMQ disk pressure + CPU overhead. Not user-facing yet.
🔴 (escalates if scale) When beta cohort 10+ tenants với high event volume — DLQ becomes blocker; consumer thread stuck on poison message blocks all other tenants' events.

## Proposed Fix

### Phase 1 (config, ≤1h)
1. Declare DLX `email.dlx` + queue `email.dlq` trong `EmailQueueConfig` (similar `PurgeQueueConfig`)
2. Add queue arguments:
   ```yaml
   x-dead-letter-exchange: email.dlx
   x-dead-letter-routing-key: dlq
   ```
3. Set `SimpleRabbitListenerContainerFactory.setDefaultRequeueRejected(false)` + add `RetryInterceptor` với max 3 attempts + exponential backoff
4. Apply same pattern cho audit + purge consumers

### Phase 2 (observability)
1. Metric `rmq_dlq_depth{queue=email.dlq}` exposed
2. AlertManager rule (per GAP-144): DLQ depth > 0 cho >10 min → page on-call
3. Runbook `documents/05-guides/operations/rmq-dlq-triage.md` (4 sections per `docs-folder-structure.md` §3)

### Phase 3 (replay tooling)
1. Script `scripts/ops/rmq-dlq-replay.sh <queue> <action: list | replay | discard>` cho ops team
2. Web UI trong kitehub-admin /admin/ops/dlq (optional)

## Acceptance Criteria

- [ ] DLX + DLQ declared trong EmailQueueConfig + bound; verified via RMQ Management UI
- [ ] Test: send poison message (template name không tồn tại) → 3 retries → land in DLQ → consumer log "moved to DLQ" instead of spam
- [ ] Metric `rmq_dlq_depth` exposed; baseline 0
- [ ] AlertManager rule landed (paired GAP-144)
- [ ] Wave 90 stuck admin-new-login-alert poison messages drained: existing queue purged after Phase 1 deploy

## Related

- GAP-605 (sister — outbox dispatcher; both surfaced Wave 90)
- GAP-606 (sister — template missing triggered the poison message)
- GAP-144 P1 AlertManager receivers (Wave 84 carry-forward) — DLQ alert depends on it
- `design-patterns.md` §3.5 outbox pattern — DLQ is sister concern for consumer-side reliability
- Wave 67 RMQ initial setup (no DLQ shipped)

## Log

- **2026-05-17:** Gap filed during Wave 90 walkthrough. Found while investigating GAP-606 template-missing log spam. Recurrent pattern: any future producer bug (4xx from downstream, schema drift, vendor outage) will exhibit same infinite-retry symptom without DLQ. P1 because not user-facing TODAY but architecture-level risk for beta cohort scale.
