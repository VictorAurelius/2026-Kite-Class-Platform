# GAP-1369: CloudWatch P0 alarms với metric-source chưa wired (Nginx5xxCount + RabbitMQ QueueDepth) → INSUFFICIENT_DATA silent

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** DevOps
**Found:** 2026-06-14 (ops-readiness full audit post wave-p0-closeout-1 — §4 alerting metric-source)
**Affects:** `infrastructure/terraform-aws/cloudwatch-p0-alarms.tf` (Alarm 6 nginx-5xx, Alarm 7 outbox-dlq)

## Problem

8 P0 CloudWatch alarms (`cloudwatch-p0-alarms.tf`, GAP-144) được wire vào SNS topic `kitehub-production-alerts` + email subscriptions — IaC tốt. Nhưng 2 alarm dựa trên **custom-namespace metric chưa có nguồn emit**:

- **Alarm 6 `nginx-5xx-rate-high`** — metric `Nginx5xxCount` namespace `KiteHub/Nginx`. Cần nginx access-log → CloudWatch Logs metric filter (extract status≥500). Comment trong .tf tự flag: "If filter not yet wired post Wave aws-restore-1, alarm stays INSUFFICIENT_DATA". ALB đã bị eliminate → đây là **alarm thay thế chính cho edge 5xx**, nếu silent thì mất tín hiệu lỗi backend spike.
- **Alarm 7 `outbox-dlq-non-empty`** — metric `QueueDepth` namespace `KiteHub/RabbitMQ` dimension `Queue`. Cần rabbitmq-exporter / CWAgent scrape RabbitMQ. Comment tự flag INSUFFICIENT_DATA nếu exporter chưa scrape. DLQ non-empty = tenant-facing dispatch fail (welcome email / payment confirm) → silent alarm = miss P0 signal.

Alarm trong trạng thái `INSUFFICIENT_DATA` + `treat_missing_data="notBreaching"` → **không bao giờ fire** dù điều kiện thực tế xảy ra → false sense of coverage. (Alarm 8 root-login có nguồn: `cloudtrail-metric-filters.tf` RootUserLogin filter tồn tại ✅. Disk/CPU/status-check dùng AWS-native + CWAgent disk plugin — cần verify CWAgent disk plugin active live, ❓ UNCHECKED.)

## Proposed Fix

Wire 2 metric sources: (1) nginx access-log → CloudWatch Logs log group `/kite/nginx/access` + metric filter `Nginx5xxCount` (status≥500); (2) rabbitmq-exporter hoặc CWAgent custom metric script emit `QueueDepth` cho `kitehub.outbox.dlq` vào `KiteHub/RabbitMQ`. Sau wiring, verify alarm rời `INSUFFICIENT_DATA` (cần AWS live — gate sau khi stack start).

## Acceptance Criteria

- [ ] Nginx access-log metric filter `Nginx5xxCount` emit vào `KiteHub/Nginx` (verify alarm 6 có data point).
- [ ] RabbitMQ DLQ depth metric emit vào `KiteHub/RabbitMQ` (verify alarm 7 có data point).
- [ ] Cả 2 alarm rời trạng thái `INSUFFICIENT_DATA` khi stack live.

## Related

- Discovered in: ops-readiness full audit 2026-06-14 (OPS-004).
- GAP-144 (8 P0 alarms IaC — DONE; metric-source wiring là phần còn thiếu), GAP-437 (CloudTrail observability baseline — DONE, RootUserLogin filter ✅), GAP-742 (OutboxDLQ Prometheus alert — sister Helm path).
- Note: live verify gated stack-start (per `agent-aws-access.md` §2.1 audit không gọi AWS).
