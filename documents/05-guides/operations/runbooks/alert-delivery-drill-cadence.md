# Alert Delivery Drill — Monthly Cadence Runbook

**Status:** active
**Created:** 2026-06-15
**Owner:** SRE / solo dev (on-call)
**Closes:** GAP-1370 (ops-readiness audit 2026-06-14, OPS-005; carry OPS-W92-006)
**Scope:** Verifying the **production alert-delivery path** end-to-end on a monthly cadence:
`CloudWatch alarm → SNS topic kitehub-production-alerts → email`.

> Distinct from [`alertmanager-mock-fire-runbook.md`](alertmanager-mock-fire-runbook.md), which
> covers the older **Helm Alertmanager** path (Slack / PagerDuty / SES SMTP). The current Phase 1
> BETA architecture delivers alerts via **CloudWatch → SNS → email** (SNS-direct adaptation per
> GAP-144 closure). This runbook is the cadence for that path.

---

## 1. Why a monthly drill

GAP-144 shipped 8 P0 CloudWatch alarms + the SNS topic `kitehub-production-alerts` with two email
subscriptions (`support@kitehub.me` + `vannkite@outlook.com`, see `production-alerts.tf`). The
SNS→email leg, however, was **never exercised**. Failure modes that stay silent until a real
incident:

- SNS subscription left **unconfirmed** (no email ever arrives).
- SES / mailbox **bounces or spam-filters** the SNS notification.
- An alarm's `alarm_actions` mis-wired to the wrong/again-deleted topic ARN.

A monthly synthetic drill forces an alarm transition so a broken delivery path is discovered **before**
it matters. Cadence target per ops-readiness rubric §4.5: a drill log **≤ 30 days** old at all times.

---

## 2. Cadence

| Mechanism | Detail |
|---|---|
| **Automated** | `.github/workflows/alert-delivery-drill.yml` — cron `0 4 1 * *` (1st of month, 04:00 UTC) + `workflow_dispatch`. Gated by repo var `ALERT_DRILL_ENABLED` (mirrors `restore-drill.yml`). |
| **Enable live fire** | Settings → Variables → `ALERT_DRILL_ENABLED=true` once the AWS stack is restored and the drill IAM role exists. Secret `AWS_ALERT_DRILL_ROLE_ARN` (OIDC, least-priv `cloudwatch:SetAlarmState`). |
| **Manual fallback** | If the workflow is gated off (stack stopped), run §3 by hand whenever the stack is next started, at least once per 30-day window. |

The workflow's `verify-config` job runs **every** invocation (even gated) to assert this runbook exists
and the SNS delivery path is declared in terraform — keeping the cadence honest before the live body
is ever enabled.

---

## 3. Drill procedure (manual or what the workflow automates)

Prereq: AWS stack running (`bash scripts/aws/start-stack.sh`), AWS creds with `cloudwatch:SetAlarmState`.

```bash
ALARM=kitehub-rds-cpu-high   # any real alarm wired to the SNS topic; self-heals to OK

# 1. Force the alarm to ALARM → SNS publishes → both subscribers should get email
aws cloudwatch set-alarm-state --region ap-southeast-1 \
  --alarm-name "$ALARM" --state-value ALARM \
  --state-reason "GAP-1370 monthly alert-delivery drill $(date -u +%FT%TZ)"

# 2. Wait ~30s, then reset so the alarm self-heals (real metric re-evaluates next period)
sleep 30
aws cloudwatch set-alarm-state --region ap-southeast-1 \
  --alarm-name "$ALARM" --state-value OK \
  --state-reason "GAP-1370 drill reset $(date -u +%FT%TZ)"
```

Then **manually confirm** both `support@kitehub.me` and `vannkite@outlook.com` received the SNS
notification email (check inbox + spam). If either did not arrive → see §5.

---

## 4. Drill-log template + location

Record every drill (pass or fail) as a dated file in
`documents/04-quality/audits/ops-readiness/` named `YYYY-MM-DD-alert-delivery-drill.md`:

```markdown
# Alert Delivery Drill — YYYY-MM-DD

**Cadence:** monthly (GAP-1370)
**Operator:** <name>
**Trigger:** alert-delivery-drill.yml run <id> | manual

| Check | Result |
|---|---|
| Alarm toggled (`kitehub-rds-cpu-high` ALARM→OK) | ✅ / ❌ |
| Email reached support@kitehub.me | ✅ / ❌ (received HH:MM UTC) |
| Email reached vannkite@outlook.com | ✅ / ❌ (received HH:MM UTC) |
| Alarm self-healed to OK | ✅ / ❌ |

**Verdict:** PASS / FAIL — <notes>
**Next drill due:** YYYY-MM-DD (+30d)
```

---

## 5. If the drill FAILS (no email)

1. `aws sns list-subscriptions-by-topic --topic-arn <kitehub-production-alerts ARN>` — check each
   subscription `SubscriptionArn` is a real ARN, **not** `PendingConfirmation`.
2. If `PendingConfirmation`: the subscriber must click the confirmation link (re-trigger via
   `aws sns subscribe` if the original confirmation expired — 3-day window).
3. Check spam/junk; add `no-reply@sns.amazonaws.com` to allow-list.
4. Confirm the alarm's `alarm_actions` ARN matches the live topic ARN (`terraform state show
   aws_cloudwatch_metric_alarm.rds_cpu_high`).
5. File an incident gap if the path is broken — a silent alert path is a P1 ops risk.

---

## 6. Related

- `.github/workflows/alert-delivery-drill.yml` — the cadence workflow (gated, mirrors restore-drill.yml).
- `infrastructure/terraform-aws/production-alerts.tf` — SNS topic + email subscriptions.
- `infrastructure/terraform-aws/cloudwatch-p0-alarms.tf` — 8 P0 alarms wired to the topic.
- `alertmanager-mock-fire-runbook.md` — Helm Alertmanager path (older architecture).
- GAP-144 (SNS + email subscriptions — DONE), GAP-1370 (this cadence), GAP-257 (restore drill — sibling cadence).
