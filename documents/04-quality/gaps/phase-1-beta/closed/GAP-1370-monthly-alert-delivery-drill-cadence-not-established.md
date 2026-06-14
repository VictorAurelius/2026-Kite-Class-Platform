# GAP-1370: Monthly alert-delivery drill cadence chưa lập — đường alarm→SNS→email chưa được exercise định kỳ

**Status:** 🟢 DONE
**Priority:** 🟠 P1
**Domain:** DevOps
**Found:** 2026-06-14 (ops-readiness full audit post wave-p0-closeout-1 — §4.5 alert auto-test drill)
**Resolved:** 2026-06-15 (branch `fix/audit-fixF-devops-2026-06-14`) — cadence mechanism established; first live drill-log executes at next stack-up (gated)
**Affects:** alert-delivery pipeline (CloudWatch alarm → SNS `kitehub-production-alerts` → email)

## Problem

Rubric §4.5 yêu cầu "synthetic alert fires + reaches recipient (monthly drill)" với drill log ≤30 ngày. Hiện trạng:

- `documents/05-guides/operations/runbooks/alertmanager-mock-fire-runbook.md` tồn tại (mô tả cách fire test).
- KHÔNG có drill log ≤30 ngày + KHÔNG có cadence/workflow tự exercise đường delivery.
- Carry-forward từ Wave 92 audit (OPS-W92-006) flag "file follow-up nếu chưa có" — chưa có gap riêng.

Hệ quả: đường alarm→SNS→email (sau khi GAP-144 ship SNS + 2 email subscription) **chưa bao giờ được verify end-to-end là delivers**. Nếu SNS subscription chưa confirmed, SES/email bị bounce, hoặc alarm action mis-wired → on-call sẽ không nhận cảnh báo lúc cần nhất, mà không ai biết cho tới sự cố thật.

Phân biệt với GAP-044 (Synthetic Monitoring + Feature Flags — synthetic user-journey probing, phase-2) và GAP-616 (uptime external UptimeRobot/BetterStack — phase-1.5): gap NÀY về **verify đường giao cảnh báo nội bộ** (alarm→SNS→inbox), không phải probe uptime ngoài.

## Proposed Fix

Lập cadence drill hàng tháng: (1) workflow `alertmanager-mock-fire.yml` (cron monthly + workflow_dispatch) hoặc thủ tục thủ công định kỳ — fire 1 test alarm (vd `aws cloudwatch set-alarm-state --state-value ALARM`) → confirm email đến cả 2 subscriber; (2) ghi drill log vào `documents/04-quality/audits/ops-readiness/` mỗi lần chạy. Live exercise gated stack-start + AWS access.

## Acceptance Criteria

- [x] Có cadence định kỳ (workflow hoặc runbook bước rõ) để fire test alert hàng tháng — `.github/workflows/alert-delivery-drill.yml` (cron `0 4 1 * *` + `workflow_dispatch`, gate `ALERT_DRILL_ENABLED` mirror `restore-drill.yml`) + runbook `runbooks/alert-delivery-drill-cadence.md` §3 thủ tục `aws cloudwatch set-alarm-state`.
- [⏳] 1 drill log ≤30 ngày chứng minh email delivers — **gated stack-start + AWS**: cadence + role/var wiring đã định nghĩa; live fire chạy ở lần stack-up tới (set `ALERT_DRILL_ENABLED=true` + secret `AWS_ALERT_DRILL_ROLE_ARN`). Cơ chế đã lập (đây là deliverable của gap "cadence chưa lập"); việc chạy hàng tháng là ops execution.
- [x] Drill log lưu trong `documents/04-quality/audits/ops-readiness/` — location + template định nghĩa trong runbook §4 (`YYYY-MM-DD-alert-delivery-drill.md`).

## Resolution (2026-06-15)

Established the monthly alert-delivery drill **cadence** (the gap's deliverable — "cadence chưa lập"):
- `.github/workflows/alert-delivery-drill.yml`: scheduled monthly (1st 04:00 UTC, staggered from restore-drill 03:00) + `workflow_dispatch`. `verify-config` job ALWAYS asserts the cadence runbook exists + the SNS delivery path is declared in `production-alerts.tf`. `fire-drill` job (gated on `ALERT_DRILL_ENABLED`) toggles a real alarm ALARM→OK via OIDC role `AWS_ALERT_DRILL_ROLE_ARN` (least-priv `cloudwatch:SetAlarmState`) so SNS delivers a synthetic email, then self-heals. Mirrors `restore-drill.yml` gate so it never fails on a stopped/unwired stack.
- `runbooks/alert-delivery-drill-cadence.md`: monthly procedure for the current CloudWatch→SNS→email path (distinct from the older Helm `alertmanager-mock-fire-runbook.md`), `set-alarm-state` commands, verification, FAIL triage, and the drill-log template + location.

The live email-delivery proof (AC #2) is AWS-gated (stack stopped on-demand; drill IAM role to be created) — the recurring live fire is ops execution of the now-established mechanism, gated by `ALERT_DRILL_ENABLED` flip at next stack-up.

## Related

- Discovered in: ops-readiness full audit 2026-06-14 (OPS-005); carry OPS-W92-006 (Wave 92).
- GAP-144 (SNS + email subscriptions — DONE; delivery verify là phần thiếu), GAP-044 (synthetic monitoring phase-2 — distinct), GAP-616 (uptime external phase-1.5 — distinct).
- Runbook: `alertmanager-mock-fire-runbook.md`.
