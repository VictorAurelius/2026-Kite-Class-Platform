# GAP-144: Alertmanager Production Receivers (Slack + PagerDuty + SMTP)

**Status:** 🟡 PARTIAL — chart-level wiring DONE 2026-04-28; live-cluster mock-fire verification pending platform deploy (tracked separately, see Log + AC table)
**Priority:** 🔴 P0 (BLOCKS: alerts fire but go nowhere — same root concern as GAP-120 baseline)
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
- [ ] Mock ServiceDown alert delivers to Slack within 2 min → blocked by live-cluster prerequisite (recipe ready in README §Testing)
- [ ] Mock critical alert pages PagerDuty within 2 min → blocked by live-cluster prerequisite (recipe ready in README §Testing)
- [x] Inhibition rules verified (ServiceDown suppresses HighErrorRate same job) — explicit pair added in `inhibit_rules` block alongside existing catch-all
- [x] No secret values committed to git (verified via `git diff` + secret-scan) — `git diff main | grep -iE "(api_url|service_key|smtp.*password):" | grep -v "_file:"` returns empty

## Related

- Depends: GAP-111 (foundation — DONE), GAP-120 (foundation — DONE)
- Enables: GAP-121 (per-alert runbooks; parallel agent in same wave)
- Audit: `documents/04-quality/audits/ops/ops-readiness-audit-2026-04-19.md` §7
- ADR: [ADR-022](../../02-architecture/adr/ADR-022-alertmanager-secret-strategy.md) — secret strategy decision (ESO + AWS SM)
- Wave: `documents/03-planning/waves/wave-2026-04-29-observability.md`
- Follow-up gap (live-cluster mock-fire verification): TBD when platform deploy lands; expected from same operator that runs §Testing recipe in helm README. Recipe is the verification artifact pointer per `gap-done-discipline.md` §2.5.

## Log

- **2026-04-28** — Wave Observability Agent C shipped chart-level closure (PR #TBD): ADR-022 ACCEPTED (ESO + AWS SM); `ExternalSecret` template added; values.yaml Alertmanager config gated for production opt-in via `monitoring.alertmanager.receivers.production.enabled` flag; explicit inhibition pair `ServiceDown → HighErrorRate same job` added; helm README extended with operator runbook (provisioning + activation + amtool testing recipe). Status flipped 🔵 OPEN → 🟡 PARTIAL per `gap-done-discipline.md` §3 — live-cluster mock-fire ACs (#3, #4) pending platform deploy. Recipe in `infrastructure/helm/README.md` §"Testing Alertmanager Receivers" is the verification artifact pointer when those tests run. AC #1, #2, #5, #6 verified in this PR.
- 2026-04-20 — Split from GAP-120 foundation work; production-secret wiring deferred for org-level secret strategy alignment.
