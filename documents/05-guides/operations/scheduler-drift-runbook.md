# Scheduler Drift Runbook — BetaRequestAbortCleanupScheduler

**Last Updated:** 2026-05-18
**Applies to:** `BetaRequestAbortCleanupScheduler` (kitehub-subscription)
**Gap:** GAP-644 (Wave 97 Bucket D)
**Audience:** Platform ops, on-call engineer

---

## 1. Tổng quan

`BetaRequestAbortCleanupScheduler` sweep định kỳ (mỗi 6h) các `beta_access_request` row ở trạng thái `PENDING` quá `staleThresholdHours` (default 24h) và chuyển sang `ABORTED`.

Scheduler thực hiện 2 truy vấn DB liên tiếp trong 1 transaction:
1. `countStalePending(threshold)` → đếm số row sẽ bị abort
2. `markStaleAsAborted(threshold, now)` → bulk UPDATE sang ABORTED

**Drift** xảy ra khi `countStalePending` ≠ `markStaleAsAborted` — tức là giữa 2 truy vấn có admin approve/reject 1 số row (race condition lành tính). Khi drift được phát hiện, scheduler emit Micrometer counter:

```
kitehub.scheduler.beta_request.abort.drift_count
  tags: expected_count=<staleCount>, actual_count=<aborted>
```

---

## 2. CloudWatch Alarm configuration

| Field | Giá trị |
|---|---|
| Metric | `kitehub.scheduler.beta_request.abort.drift_count` |
| Namespace | `KiteHub/Schedulers` (Micrometer CloudWatch namespace) |
| Alarm condition | `drift_count > 0` trong 3 consecutive evaluation periods |
| Evaluation period | 6h (match cron interval) |
| SNS topic | `kitehub-ops-alerts` |
| Alarm name | `BetaRequestAbortScheduler-DriftDetected` |

**Lưu ý:** Counter accumulate — không reset về 0 giữa các runs. Alarm dùng `DIFF` metric (delta per period), không dùng raw total.

---

## 3. Drift severity matrix

| Delta (staleCount - aborted) | Ý nghĩa | Hành động |
|---|---|---|
| 1-2 rows | Bình thường — admin active trong 6h window | Monitor, không cần action |
| 3-10 rows | Cao hơn bình thường — batch admin operation? | Kiểm tra admin audit log |
| > 10 rows | Bất thường — cần investigate | Chạy diagnostic queries §4 |
| Liên tục nhiều runs | Có thể bug (index drift, schema issue) | Escalate §5 |

---

## 4. Diagnostic queries

Chạy trên kitehub production DB (readonly replica preferred):

```sql
-- 4.1: Kiểm tra số PENDING row hiện tại
SELECT COUNT(*)
FROM beta_access_request
WHERE status = 'PENDING'
  AND created_at < NOW() - INTERVAL '24 hours';

-- 4.2: Kiểm tra admin activity trong 6h gần nhất (approve/reject)
SELECT action, COUNT(*) as cnt, MAX(created_at) as last_action_time
FROM admin_audit_log
WHERE target_entity_type = 'beta_access_request'
  AND action IN ('BETA_REQUEST_APPROVE', 'BETA_REQUEST_REJECT')
  AND created_at > NOW() - INTERVAL '6 hours'
GROUP BY action;

-- 4.3: Kiểm tra rows đang trong trạng thái ABORTED gần đây
SELECT id, email, status, created_at, updated_at
FROM beta_access_request
WHERE status = 'ABORTED'
  AND updated_at > NOW() - INTERVAL '12 hours'
ORDER BY updated_at DESC
LIMIT 20;

-- 4.4: Verify scheduler threshold config (đọc từ application properties)
-- Threshold mặc định: kitehub.beta.cleanup.stale-threshold-hours = 24
```

---

## 5. Escalation path

1. **Drift < 5 rows, isolated run**: Log entry đã có trong CloudWatch — no action needed
2. **Drift ≥ 5 rows hoặc 3+ consecutive runs**: Notify platform team via `#kitehub-ops` Slack
3. **Drift ≥ 20 rows hoặc sustained**: Open incident, assign to backend lead
4. **Drift kết hợp với `markStaleAsAborted` FAIL (exception)**: P0 incident — scheduler không hoàn thành, stale rows tích lũy

---

## 6. Disable scheduler tạm thời (khi cần)

```yaml
# application.yml (hoặc override environment variable)
kitehub:
  beta:
    cleanup:
      enabled: false
```

Hoặc qua Spring Boot Actuator (nếu enabled):

```bash
# POST /actuator/scheduledtasks/<task-name>/stop (custom endpoint — check if wired)
```

**Lưu ý:** Disable scheduler KHÔNG xóa stale rows. Manual cleanup có thể trigger qua admin API (endpoint `/admin/v1/beta-requests/cleanup-stale` nếu wired — check ControllerAdvice).

---

## 7. Re-enable sau maintenance

```bash
# Verify scheduler active
grep "BetaRequestAbortCleanupScheduler" <log-stream> | tail -20

# Verify metric emitting (CloudWatch)
aws cloudwatch get-metric-statistics \
  --namespace "KiteHub/Schedulers" \
  --metric-name "kitehub.scheduler.beta_request.abort.drift_count" \
  --start-time "$(date -u -d '1 hour ago' +%FT%TZ)" \
  --end-time "$(date -u +%FT%TZ)" \
  --period 3600 \
  --statistics Sum \
  --profile dev-admin
```

---

## 8. Related

- **GAP-644**: `documents/04-quality/gaps/phase-1-beta/GAP-644-scheduler-cloudwatch-drift-metric.md`
- **GAP-600**: Scheduler implementation (Wave 92 Bucket C)
- **Scheduler source**: `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/scheduler/BetaRequestAbortCleanupScheduler.java`
- **Incident response**: `documents/05-guides/operations/incident-response-runbook.md`
- **Audit log context**: `documents/05-guides/operations/audit-log-retention-runbook.md`

---

## 9. Log

- **2026-05-18**: Runbook tạo lần đầu. GAP-644 Wave 97 Bucket D — scheduler emit Micrometer drift counter, runbook hướng dẫn CloudWatch alarm setup + diagnostic queries + escalation path. Per `professional-manual-content-standard.md` §2 audience: platform ops + on-call engineer.
