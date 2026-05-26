# Runbook: Outbox DLQ Non-Empty

**Alert:** `OutboxDLQNonEmpty`
**Severity:** `critical` (paging — `critical-webhook` → PagerDuty per `alertmanager-config.yaml`)
**Last updated:** 2026-05-26
**Related gap:** GAP-742 (Wave beta-readiness-7 Bucket E)
**Related audit:** `documents/04-quality/audits/ops-readiness/2026-05-25-wave-br-4-ops-readiness-audit.md` §OPS-BR4-001

---

## 1. Cảnh báo này nghĩa là gì?

Một queue có suffix `.dlq` (Dead-Letter Queue) trong cluster RabbitMQ đang chứa **≥1 message** đã >5 phút. Source metric: `rabbitmq_queue_messages_ready{queue=~".*\\.dlq$"}` (Prometheus rabbitmq-exporter).

Trong KiteHub hiện tại có 2 DLQ chính:

| Queue DLQ | Producer | Mục đích message | Mỗi message dropped = tác động |
|---|---|---|---|
| `email.dlq` | `kitehub-subscription` `EmailQueueConfig` | Email transactional sau khi retry policy (3 attempts) cạn | 1 tenant không nhận được mail (welcome / hóa đơn / reset mật khẩu / xác nhận thanh toán) |
| `kitehub.migration.dlq` | `kitehub-subscription` outbox dispatcher (`MigrationEventType.TOPIC_MIGRATION_DLQ`) | Migration event (trial→paid lifecycle, branding refresh, payment.reversed) không dispatch được sau retry budget | 1 tenant lifecycle event mất → drift state KiteHub vs KiteClass |

Bất kỳ DLQ nào tăng dù chỉ 1 message = **tenant-facing message dispatch đã fail silently**. Trước Wave br-7 không có alert cho ngưỡng `>0` (chỉ có `EmailQueueDLQGrowing` `>10/10m` — quá lỏng cho outbox production scope per `pre-handoff-self-test-completeness.md` §2.9 C4-4).

---

## 2. Triage tức thì (0-5 phút)

### Bước 1 — Xác định DLQ nào đang non-empty

Mở RabbitMQ admin UI (`http://kite-rabbitmq:15672` — credentials trong K8s secret `rabbitmq-management` hoặc env `RABBITMQ_DEFAULT_USER` / `RABBITMQ_DEFAULT_PASS`).

```bash
# Hoặc query qua kubectl + curl (nếu admin UI port-forward chưa available)
kubectl exec -n kitehub deploy/kite-rabbitmq -- rabbitmqctl list_queues name messages_ready \
  | grep -E '\.dlq\s' \
  | awk '$2 > 0 {print}'
```

Output kỳ vọng:
```
email.dlq                 3
kitehub.migration.dlq     0
```

### Bước 2 — Inspect message gần nhất

Trong RabbitMQ admin UI:
1. Queues tab → click queue đang non-empty
2. "Get messages" panel → `count=5`, `Ackmode=Reject and requeue`
3. Đọc header `x-death` của mỗi message:
   - `count` = số lần đã retry (kỳ vọng = `max-attempts` configured)
   - `exchange` + `routing-key` = nguồn gốc
   - `reason` = `rejected` (consumer NACK) hoặc `expired` (TTL hết)
   - `time` = thời điểm DLX route — so với deploy gần nhất

### Bước 3 — Check log producer + consumer

| DLQ | Producer service | Consumer service | Log command |
|---|---|---|---|
| `email.dlq` | `kitehub-subscription` `EmailServiceClient` + outbox dispatcher | `kitehub-email` `EmailConsumer` | `kubectl logs -n kitehub deploy/kitehub-email --tail=200 \| grep -E 'ERROR\|Failed\|DLQ\|RetryExhausted'` |
| `kitehub.migration.dlq` | `kitehub-subscription` outbox dispatcher (`SubscriptionOutboxDispatcher`) | `kitehub-platform` migration consumer | `kubectl logs -n kitehub deploy/kitehub-platform --tail=200 \| grep -E 'migration\|MIGRATION_FAILED\|outbox'` |

### Bước 4 — Deploy gần nhất?

```bash
gh run list --workflow=deploy-production.yml --limit=3 --json conclusion,createdAt,headSha
```

Nếu deploy <30 phút trước DLQ tăng → strong signal: regression từ code mới.

---

## 3. Nguyên nhân thường gặp

| Nhóm nguyên nhân | Triệu chứng | Hành động |
|---|---|---|
| **Consumer crash / crashloop** | DLQ tăng đột ngột; `kubectl get pods -n kitehub` cho thấy CrashLoopBackOff trên consumer pod | `kubectl rollout undo` consumer nếu deploy gần đây; investigate stack trace; restore service trước, drain DLQ sau |
| **Downstream service down** | Email: SendGrid/SES return 5xx hoặc credential rotated → 401/403; Migration: kitehub-platform health check fail | Check `kubectl logs <consumer>` for HTTP status; check provider status page; rotate credential trong `email-credentials` secret nếu cần |
| **Poison message** (payload không decode được, mất field bắt buộc) | DLQ chỉ 1-2 message, không tăng nữa; `x-death.reason=rejected` consistent | Inspect payload qua admin UI; identify producer commit gần nhất; rollback producer nếu vừa deploy; manually drop poison message sau khi capture snapshot |
| **Migration event schema mismatch** | `kitehub.migration.dlq` only; producer + consumer phiên bản lệch nhau sau deploy | Check `MigrationEventType` constants giữa producer (`kitehub-subscription`) và consumer (`kitehub-platform`) — phải align cùng version; redeploy lệch hướng |
| **Rate limit / quota provider** | `email.dlq` tăng sau bulk send (vd renewal blast); SendGrid hourly limit hit | Drain DLQ chậm qua shovel; tune outbound rate limit; xem `subscription-webhook-failure.md` cho billing impact |
| **Outbox dispatcher poll bị stuck** | `OUTBOX_DISPATCHER_ENABLED=true` nhưng dispatcher pod log không advance; queue message nằm trong DB outbox table; DLQ tăng do retry budget cạn | `kubectl rollout restart deploy/kitehub-subscription`; check `outbox.dispatcher.poll-interval-ms` config (default 10s) |

---

## 4. Mitigation

### 4.1 Drain DLQ sau khi root cause fixed

```bash
# Option A: Shovel DLQ → primary queue (RabbitMQ Shovel plugin)
kubectl exec -n kitehub deploy/kite-rabbitmq -- rabbitmqctl set_parameter shovel email-dlq-replay '{
  "src-uri":"amqp://",
  "src-queue":"email.dlq",
  "dest-uri":"amqp://",
  "dest-exchange":"emails",
  "dest-exchange-key":"emails.send"
}'

# Verify drain progress
watch -n 5 'kubectl exec -n kitehub deploy/kite-rabbitmq -- rabbitmqctl list_queues name messages_ready | grep email'

# Cleanup shovel sau khi drain xong
kubectl exec -n kitehub deploy/kite-rabbitmq -- rabbitmqctl clear_parameter shovel email-dlq-replay
```

### 4.2 Capture poison message snapshot trước khi drop

```bash
# Lưu snapshot DLQ tại thời điểm incident (forensic)
RABBIT_USER=$(kubectl get secret -n kitehub rabbitmq-management -o jsonpath='{.data.username}' | base64 -d)
RABBIT_PASS=$(kubectl get secret -n kitehub rabbitmq-management -o jsonpath='{.data.password}' | base64 -d)

curl -u "$RABBIT_USER:$RABBIT_PASS" -X POST \
  http://kite-rabbitmq:15672/api/queues/%2F/email.dlq/get \
  -d '{"count":100,"ackmode":"ack_requeue_true","encoding":"auto"}' \
  > /tmp/email-dlq-snapshot-$(date -u +%Y%m%dT%H%M%SZ).json
```

`ackmode=ack_requeue_true` đảm bảo message vẫn trong queue sau khi inspect — drop chỉ khi đã quyết định.

### 4.3 Drop poison message (chỉ khi consumer KHÔNG BAO GIỜ xử lý được)

```bash
# Drop bằng cách get với ackmode=ack_requeue_false
curl -u "$RABBIT_USER:$RABBIT_PASS" -X POST \
  http://kite-rabbitmq:15672/api/queues/%2F/email.dlq/get \
  -d '{"count":1,"ackmode":"ack_requeue_false","encoding":"auto"}'
```

⚠️ Tenant-impact: mỗi message drop = 1 tenant không nhận email/event. Cần log retroactively vào incident report + cân nhắc manual dispatch CSV nếu critical (password reset / billing notice).

---

## 5. Khi nào escalate

| Tình huống | Escalate đến | Lý do |
|---|---|---|
| DLQ >100 message HOẶC tăng liên tục >1h chưa drain | Platform lead + customer-success | Customer-facing trust hit; cần communications |
| Critical-path message trong DLQ (password reset / payment failure notice / billing) | P0 incident — page on-call + product owner | Tenant không nhận = user lockout / billing leak |
| Provider relationship issue (sender reputation, account suspended, SES production access revoked) | Product + business owner | Vendor-level escalation |
| Outbox dispatcher stuck >15 phút sau restart | SRE lead | Có thể là DB lock / connection pool exhaustion (xem `database-pool-exhausted.md`) |

---

## 6. Verification sau khi mitigation

```bash
# 1. DLQ về 0
kubectl exec -n kitehub deploy/kite-rabbitmq -- rabbitmqctl list_queues name messages_ready | grep -E '\.dlq\s'
# Kỳ vọng: tất cả .dlq queues = 0

# 2. Alert đã resolve trong Alertmanager
curl -s http://alertmanager:9093/api/v2/alerts | jq '.[] | select(.labels.alertname=="OutboxDLQNonEmpty")'
# Kỳ vọng: empty array hoặc state="resolved"

# 3. Watch 2h — không re-accumulate
# Set một calendar reminder; nếu DLQ tăng lại trong 2h → root cause chưa fixed thực sự
```

---

## 7. Liên quan

- **Alert rule:** `infrastructure/helm/kitehub/templates/prometheusrule.yaml` group `outbox-dlq-alerts`
- **Alertmanager routing:** `infrastructure/helm/kitehub/templates/alertmanager-config.yaml` (`severity=critical` → `critical-webhook` → PagerDuty)
- **Sister alert (warning, ngưỡng cao hơn):** `EmailQueueDLQGrowing` (>10 in 10m) trong group `kitehub-platform-alerts` — runbook `email-queue-dlq-growing.md`
- **Outbox config:** `kitehub/kitehub-subscription/src/main/resources/application.yml` (`outbox.dispatcher.*` Wave 91 Bucket A)
- **Outbox topic constants:** `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/outbox/MigrationEventType.java`
- **DLQ exchange wiring:** `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/config/EmailQueueConfig.java`
- **Design pattern:** `.claude/rules/design-patterns.md` §3.5.1 (Outbox reliability net)
- **Pre-handoff mandate:** `.claude/rules/pre-handoff-self-test-completeness.md` §2.9 (background job DLQ alert là C4-4 mandatory)
- **Related runbooks:** [`email-queue-dlq-growing.md`](./email-queue-dlq-growing.md), [`rabbitmq-queue-backlog.md`](./rabbitmq-queue-backlog.md), [`subscription-webhook-failure.md`](./subscription-webhook-failure.md), [`service-down.md`](./service-down.md)
