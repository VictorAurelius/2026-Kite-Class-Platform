# GAP-1370: Monthly alert-delivery drill cadence chưa lập — đường alarm→SNS→email chưa được exercise định kỳ

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** DevOps
**Found:** 2026-06-14 (ops-readiness full audit post wave-p0-closeout-1 — §4.5 alert auto-test drill)
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

- [ ] Có cadence định kỳ (workflow hoặc runbook bước rõ) để fire test alert hàng tháng.
- [ ] 1 drill log ≤30 ngày chứng minh email delivers tới support@ + backup subscriber.
- [ ] Drill log lưu trong `documents/04-quality/audits/ops-readiness/`.

## Related

- Discovered in: ops-readiness full audit 2026-06-14 (OPS-005); carry OPS-W92-006 (Wave 92).
- GAP-144 (SNS + email subscriptions — DONE; delivery verify là phần thiếu), GAP-044 (synthetic monitoring phase-2 — distinct), GAP-616 (uptime external phase-1.5 — distinct).
- Runbook: `alertmanager-mock-fire-runbook.md`.
