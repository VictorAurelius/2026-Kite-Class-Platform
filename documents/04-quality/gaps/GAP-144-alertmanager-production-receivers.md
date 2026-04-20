# GAP-144: Alertmanager Production Receivers (Slack + PagerDuty + SMTP)

**Status:** 🔵 OPEN
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

## Proposed Fix

1. **Decide secret strategy** — recommended: `external-secrets-operator` with
   AWS Secrets Manager backend (matches existing Terraform pattern)
2. **Provision secrets** in AWS Secrets Manager:
   - `kitehub/alertmanager/slack-webhook`
   - `kitehub/alertmanager/pagerduty-key`
   - `kitehub/alertmanager/smtp-password`
3. **Add `ExternalSecret` template** under `infrastructure/helm/kitehub/templates/`
   that materializes a k8s `Secret` named `alertmanager-receivers`
4. **Update Alertmanager config in values.yaml**:
   - Switch `default-webhook` to real Slack webhook (`api_url_file`)
   - Switch `critical-webhook` to PagerDuty receiver type with `service_key_file`
   - Set `global.smtp_smarthost` to real SES SMTP endpoint
   - Mount secret keys as files into Alertmanager pod via `alertmanagerConfigMatcherStrategy`
5. **Test each receiver**:
   - Trigger mock alert → verify Slack message arrives within 2 min
   - Trigger critical alert → verify PagerDuty incident creates within 2 min
   - Trigger warning alert → verify email delivers within 5 min
6. **Add per-alert runbook annotations** (depends on GAP-121)

## Acceptance Criteria

- [ ] Secret strategy documented (ADR or inline in helm README)
- [ ] All 3 receiver placeholder URLs replaced with real-secret-backed values
- [ ] Mock ServiceDown alert delivers to Slack within 2 min
- [ ] Mock critical alert pages PagerDuty within 2 min
- [ ] Inhibition rules verified (ServiceDown suppresses HighErrorRate same job)
- [ ] No secret values committed to git (verified via `git diff` + `git-secrets` scan)

## Related

- Depends: GAP-111 (foundation — DONE), GAP-120 (foundation — DONE)
- Enables: GAP-121 (per-alert runbooks)
- Audit: `documents/04-quality/audits/ops/ops-readiness-audit-2026-04-19.md` §7

## Log

- 2026-04-20 — Split from GAP-120 foundation work; production-secret wiring deferred for org-level secret strategy alignment.
