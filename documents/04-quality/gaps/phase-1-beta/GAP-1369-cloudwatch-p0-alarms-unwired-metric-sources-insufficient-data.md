# GAP-1369: CloudWatch P0 alarms với metric-source chưa wired (Nginx5xxCount + RabbitMQ QueueDepth) → INSUFFICIENT_DATA silent

**Status:** 🟡 PARTIAL
**Priority:** 🟠 P1
**Domain:** DevOps
**Found:** 2026-06-14 (ops-readiness full audit post wave-p0-closeout-1 — §4 alerting metric-source)
**Updated:** 2026-06-15 (branch `fix/audit-fixF-devops-2026-06-14`) — static IaC half (nginx 5xx filter) shipped; live-apply + RabbitMQ exporter AWS-gated
**Affects:** `infrastructure/terraform-aws/cloudwatch-p0-alarms.tf` (Alarm 6 nginx-5xx, Alarm 7 outbox-dlq)

## Problem

8 P0 CloudWatch alarms (`cloudwatch-p0-alarms.tf`, GAP-144) được wire vào SNS topic `kitehub-production-alerts` + email subscriptions — IaC tốt. Nhưng 2 alarm dựa trên **custom-namespace metric chưa có nguồn emit**:

- **Alarm 6 `nginx-5xx-rate-high`** — metric `Nginx5xxCount` namespace `KiteHub/Nginx`. Cần nginx access-log → CloudWatch Logs metric filter (extract status≥500). Comment trong .tf tự flag: "If filter not yet wired post Wave aws-restore-1, alarm stays INSUFFICIENT_DATA". ALB đã bị eliminate → đây là **alarm thay thế chính cho edge 5xx**, nếu silent thì mất tín hiệu lỗi backend spike.
- **Alarm 7 `outbox-dlq-non-empty`** — metric `QueueDepth` namespace `KiteHub/RabbitMQ` dimension `Queue`. Cần rabbitmq-exporter / CWAgent scrape RabbitMQ. Comment tự flag INSUFFICIENT_DATA nếu exporter chưa scrape. DLQ non-empty = tenant-facing dispatch fail (welcome email / payment confirm) → silent alarm = miss P0 signal.

Alarm trong trạng thái `INSUFFICIENT_DATA` + `treat_missing_data="notBreaching"` → **không bao giờ fire** dù điều kiện thực tế xảy ra → false sense of coverage. (Alarm 8 root-login có nguồn: `cloudtrail-metric-filters.tf` RootUserLogin filter tồn tại ✅. Disk/CPU/status-check dùng AWS-native + CWAgent disk plugin — cần verify CWAgent disk plugin active live, ❓ UNCHECKED.)

## Proposed Fix

Wire 2 metric sources: (1) nginx access-log → CloudWatch Logs log group `/kite/nginx/access` + metric filter `Nginx5xxCount` (status≥500); (2) rabbitmq-exporter hoặc CWAgent custom metric script emit `QueueDepth` cho `kitehub.outbox.dlq` vào `KiteHub/RabbitMQ`. Sau wiring, verify alarm rời `INSUFFICIENT_DATA` (cần AWS live — gate sau khi stack start).

## Acceptance Criteria

- [~] Nginx access-log metric filter `Nginx5xxCount` emit vào `KiteHub/Nginx` — **IaC declared** (`cloudwatch-p0-alarms.tf`: `aws_cloudwatch_log_group.nginx_access` `/kite/nginx/access` + `aws_cloudwatch_log_metric_filter.nginx_5xx` status≥500, default_value=0). Live emit gated on CWAgent tailing nginx logs into the group + `terraform apply` (AWS-gated).
- [ ] RabbitMQ DLQ depth metric emit vào `KiteHub/RabbitMQ` — **NOT pure-terraform**: requires a rabbitmq-exporter / CWAgent custom-metric script on the kh-backend EC2 (deploy-side). Documented as the AWS-gated remainder in the alarm 7 comment. Deferred.
- [ ] Cả 2 alarm rời trạng thái `INSUFFICIENT_DATA` khi stack live — gated stack-start + `terraform apply` (cannot verify offline).

## Resolution (2026-06-15) — PARTIAL

**Static IaC half shipped** (the doable-now portion): added to `infrastructure/terraform-aws/cloudwatch-p0-alarms.tf` a CloudWatch Logs group `/kite/nginx/access` (30-day retention) + metric filter `Nginx5xxCount` (pattern matches the `fe_proxy` log_format token 6 `$status` >= 500; `default_value=0` keeps the metric out of INSUFFICIENT_DATA whenever access-log lines flow). This gives Alarm 6 a **defined source** instead of an un-declared filter. Mirrors the proven `cloudwatch-provisioning-alarms.tf` log-group+filter pattern; `terraform fmt` clean.

**AWS-gated remainder (PARTIAL):**
1. Alarm 6 live data needs the kh-backend CloudWatch agent to tail `/var/log/nginx/*access*.log` into the new log group + `terraform apply`.
2. Alarm 7 (`QueueDepth`) has NO pure-terraform source — it needs a rabbitmq-exporter / CWAgent custom-metric script on EC2 (deploy-side). Documented in the alarm 7 comment.
3. `terraform validate` not run offline (provider plugins not cached — AWS account in flux per GAP-612 history).

Both alarms leave `INSUFFICIENT_DATA` until the above land — itself a useful Phase 1 BETA observability gap signal. Follow-up = live wiring + apply at next stack-up.

## Related

- Discovered in: ops-readiness full audit 2026-06-14 (OPS-004).
- GAP-144 (8 P0 alarms IaC — DONE; metric-source wiring là phần còn thiếu), GAP-437 (CloudTrail observability baseline — DONE, RootUserLogin filter ✅), GAP-742 (OutboxDLQ Prometheus alert — sister Helm path).
- Note: live verify gated stack-start (per `agent-aws-access.md` §2.1 audit không gọi AWS).
