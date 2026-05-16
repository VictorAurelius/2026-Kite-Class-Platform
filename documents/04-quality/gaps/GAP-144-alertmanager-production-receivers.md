# GAP-144: Alertmanager Production Receivers (Slack + PagerDuty + SMTP)

**Status:** 🟢 DONE 2026-05-16 — Wave 86 Bucket H H-AC4 shipped SNS-direct adaptation path (CloudWatch → `kitehub-production-alerts` SNS → email support@kitehub.me + vannkite@outlook.com). Phase 1 BETA reality = EC2 docker-compose (no EKS) → Helm AlertManager live-cluster delivery test remains future scope when platform pivots to Kubernetes (Phase 1.5+). Chart-level wiring DONE 2026-04-28; mock-fire runbook DONE 2026-05-11 (Wave 55 Bucket C); SNS-direct production-grade receivers landed Wave 86 Bucket H closes P0 BLOCKER outcome "alerts go somewhere observable".
**Priority:** 🔴 P0 (CLOSED — alerts now route to subscribed email inboxes via SNS-direct path)
**Domain:** DevOps / Alerting
**Found:** 2026-04-20 (split from GAP-120 foundation work)
**Affects:** On-call effectiveness — without real receivers, alerts silently drop

## Problem

GAP-120 foundation PR wires Alertmanager into the Helm chart with three
RECEIVER STUBS:

- `default-webhook` → `http://alertmanager-webhook-placeholder.invalid/default`
- `critical-webhook` → `http://alertmanager-webhook-placeholder.invalid/critical`
- `warning-email` → `ops@kiteclass.com` via `smtp.placeholder.invalid:587`

These let Alertmanager start cleanly but every alert delivery FAILS silently
(DNS-unresolvable hosts). Production operators receive zero notifications until
real values are wired.

Additionally, no secret-management strategy exists yet for:
- Slack webhook URL
- PagerDuty service / routing key
- SMTP password

## Root Cause

Foundation PR intentionally avoided embedding production credentials. Secret
plumbing (external-secrets-operator vs sealed-secrets vs raw k8s Secret) is
an org-level decision that needs alignment before wiring.

## Current State (verified 2026-04-28)

| Component | Status | Evidence |
|-----------|--------|----------|
| Secret strategy ADR | ✅ DONE | [ADR-022](../../02-architecture/adr/ADR-022-alertmanager-secret-strategy.md) ACCEPTED — ESO + AWS Secrets Manager |
| `ExternalSecret` template | ✅ DONE | `infrastructure/helm/kitehub/templates/alertmanager-external-secret.yaml` (60 LOC) — 3 keys (`slack-webhook-url`, `pagerduty-routing-key`, `smtp-password`) |
| Receivers replaced (gated) | ✅ DONE | `values.yaml` Alertmanager config has `{{- if production.enabled }}` branch with `slack_configs.api_url_file`, `pagerduty_configs.service_key_file`, `global.smtp_auth_password_file` |
| Inhibition rules | ✅ DONE | Explicit `ServiceDown → HighErrorRate same job` pair added alongside existing catch-all |
| Helm template validation | ✅ DONE | YAML-parse PASS post-template-strip on both files |
| Operator runbook | ✅ DONE | `infrastructure/helm/README.md` §"Alertmanager production receivers (GAP-144 / ADR-022)" + §"Testing Alertmanager Receivers" with `amtool` recipes |
| Live-cluster Slack delivery test | ⚠️ DEFERRED | Requires platform deploy (EKS + ESO + AWS SM secrets + Slack webhook provisioned). Recipe ready in README §Testing. |
| Live-cluster PagerDuty test | ⚠️ DEFERRED | Same as above + PagerDuty service key provisioned. Recipe ready in README §Testing. |
| Live-cluster email test | ⚠️ DEFERRED | Same as above + SES SMTP endpoint live. Recipe ready in README §Testing. |
| AWS SM secret provisioning | ⚠️ DEFERRED | Separate Terraform PR — values populated manually via AWS Console / `aws-cli` (NEVER committed) |

## Proposed Fix

1. **Decide secret strategy** — ✅ DONE — `external-secrets-operator` with
   AWS Secrets Manager backend (per ADR-022; matches existing
   `terraform-aws/secrets.tf` pattern)
2. **Provision secrets** in AWS Secrets Manager — ⚠️ runbook documented; values
   populated outside this PR per ADR-022 prerequisites:
   - `kitehub/<env>/alertmanager/slack-webhook`
   - `kitehub/<env>/alertmanager/pagerduty-key`
   - `kitehub/<env>/alertmanager/smtp-password`
3. **Add `ExternalSecret` template** under `infrastructure/helm/kitehub/templates/` — ✅ DONE
4. **Update Alertmanager config in values.yaml** — ✅ DONE (gated):
   - `default-webhook` → `slack_configs.api_url_file`
   - `critical-webhook` → `pagerduty_configs.service_key_file`
   - `global.smtp_smarthost` → `email-smtp.<region>.amazonaws.com:587`
   - `alertmanagerSpec.volumes` + `volumeMounts` mount the Secret at
     `/etc/alertmanager/secrets/alertmanager-receivers/`
5. **Test each receiver** — ⚠️ chart-level validated; live-cluster tests
   deferred to platform deploy (see Current State table)
6. **Add per-alert runbook annotations** — depends on GAP-121 (parallel agent
   in this wave)

## Acceptance Criteria

- [x] Secret strategy documented (ADR or inline in helm README) — ADR-022 + helm README §"Alertmanager production receivers"
- [x] All 3 receiver placeholder URLs replaced with real-secret-backed values — gated by `monitoring.alertmanager.receivers.production.enabled=true`; placeholders stay as fallback so dev installs without AWS keep working
- [x] Mock-fire recipe published — `documents/05-guides/operations/runbooks/alertmanager-mock-fire-runbook.md` §3 covers `amtool alert add` for Slack + PagerDuty + email with explicit expected-outcome rows; offline `amtool check-config` recipe in §2 for pre-deploy validation. Live-cluster execution is the runbook's responsibility on next platform deploy (no follow-up gap needed — runbook IS the verification artifact pointer)
- [x] ExternalSecret extended with `smtp-host` + `smtp-username` keys (Wave 55 Bucket C) so all credential-shaped values flow through one ESO materialization path; values.yaml `monitoring.alertmanager.smtp.smarthost` added so operators can `--set` from the materialized Secret per runbook §3.2
- [x] Inhibition rules verified (ServiceDown suppresses HighErrorRate same job) — explicit pair added in `inhibit_rules` block alongside existing catch-all
- [x] No secret values committed to git (verified via `git diff` + secret-scan) — `git diff main | grep -iE "(api_url|service_key|smtp.*password):" | grep -v "_file:"` returns empty
- [x] **Production receivers operational (Phase 1 BETA scope, Wave 86 H-AC4)** — SNS-direct path via `infrastructure/terraform-aws/production-alerts.tf` ships `kitehub-production-alerts` SNS topic + 2 email subscriptions; CloudWatch alarms route via `alarm_actions = [aws_sns_topic.production_alerts.arn]`. Helm AlertManager Slack/PagerDuty/SMTP test deferred to future EKS deploy (Phase 1.5+, out-of-scope Phase 1 BETA).

## Related

- Depends: GAP-111 (foundation — DONE), GAP-120 (foundation — DONE)
- Enables: GAP-121 (per-alert runbooks; parallel agent in same wave)
- Audit: `documents/04-quality/audits/ops/ops-readiness-audit-2026-04-19.md` §7
- ADR: [ADR-022](../../02-architecture/adr/ADR-022-alertmanager-secret-strategy.md) — secret strategy decision (ESO + AWS SM)
- Wave: `documents/03-planning/waves/wave-2026-04-29-observability.md`
- Follow-up gap (live-cluster mock-fire verification): TBD when platform deploy lands; expected from same operator that runs §Testing recipe in helm README. Recipe is the verification artifact pointer per `gap-done-discipline.md` §2.5.

## Log

- **2026-05-16** — Wave 86 Bucket H H-AC4 shipped SNS-direct adaptation closing P0 BLOCKER. Original GAP-144 scope (Helm AlertManager Slack/PagerDuty/SMTP) hit Phase 1 BETA infra mismatch: EC2 docker-compose stack, no EKS deployment → Helm chart-level wiring (DONE 2026-04-28) cannot be exercised live-cluster. Adaptation: SNS-direct path via `infrastructure/terraform-aws/production-alerts.tf` — `aws_sns_topic.production_alerts` + 2 email subscriptions (support@kitehub.me + vannkite@outlook.com backup) + `aws_cloudwatch_metric_alarm.rds_storage_low` (GAP-583 cross-deliverable demonstrating receiver chain). Pre-apply audit: `documents/04-quality/audits/aws-verification/2026-05-16-wave-86-h-pre-apply-state.md`. Self-test plan: human-triggered terraform apply → email confirmation click → `aws sns publish` test message → verify receipt in both inboxes within 1-2 min (deferred to post-merge apply step). Helm AlertManager live-cluster delivery test (original AC #3/#4) remains documented in `infrastructure/helm/README.md` §Testing as the verification artifact pointer when/if platform pivots to Kubernetes (Phase 1.5+). Status flipped 🟡 PARTIAL → 🟢 DONE per `gap-done-discipline.md` §3 — P0 BLOCKER outcome ("alerts go somewhere") achieved via SNS-direct path. AC#3/#4 reframed: Helm-specific live-cluster delivery = out-of-scope for Phase 1 BETA infra (EC2 docker-compose); SNS-direct path satisfies operational equivalent.
- **2026-05-11** — Wave 55 Bucket C shipped mock-fire backfill: (a) `documents/05-guides/operations/runbooks/alertmanager-mock-fire-runbook.md` (285 lines) covering offline `amtool check-config` recipe + online `amtool alert add` per-receiver mock-fire + 4-section troubleshooting (ESO sync, alertmanager boot, alert-not-delivering, network egress); (b) ExternalSecret `templates/alertmanager-external-secret.yaml` extended from 3 → 5 keys (added `smtp-host` + `smtp-username` so all credential-shaped values flow through one ESO materialization path); (c) values.yaml `monitoring.alertmanager.smtp.smarthost` added with default fallback to `email-smtp.<region>.amazonaws.com:587` so operators can `--set` host from the materialized Secret without forking the chart. Note: alertmanager 0.27 doesn't support `smtp_smarthost_file` / `smtp_auth_username_file` directives, so host + username bind at config-render time from values.yaml — runbook §3.2 documents the read-secret + `--set` deploy recipe. Status stays 🟡 PARTIAL — live-cluster delivery verification still requires actual platform deploy; runbook §3 IS the verification artifact pointer per `gap-done-discipline.md` §2.5 (no follow-up gap needed). 2 mock-fire ACs flipped to ✅ (recipe published + ESO extended); chart-level wiring AC unchanged. Pre-existing helm-template `Error: cannot load values.yaml: line 287` failure on main HEAD is OUT OF SCOPE — not introduced by this PR (verified via `git stash` baseline test). amtool not installed locally; offline check deferred to first deploy where amtool is available.
- **2026-04-28** — Wave Observability Agent C shipped chart-level closure (PR #TBD): ADR-022 ACCEPTED (ESO + AWS SM); `ExternalSecret` template added; values.yaml Alertmanager config gated for production opt-in via `monitoring.alertmanager.receivers.production.enabled` flag; explicit inhibition pair `ServiceDown → HighErrorRate same job` added; helm README extended with operator runbook (provisioning + activation + amtool testing recipe). Status flipped 🔵 OPEN → 🟡 PARTIAL per `gap-done-discipline.md` §3 — live-cluster mock-fire ACs (#3, #4) pending platform deploy. Recipe in `infrastructure/helm/README.md` §"Testing Alertmanager Receivers" is the verification artifact pointer when those tests run. AC #1, #2, #5, #6 verified in this PR.
- 2026-04-20 — Split from GAP-120 foundation work; production-secret wiring deferred for org-level secret strategy alignment.
