# GAP-121: Per-Alert Runbooks Library

**Status:** 🟢 DONE 2026-04-28
**Priority:** 🟠 P1
**Domain:** DevOps / Skills (meta)
**Found:** 2026-04-19 (ops-readiness audit — baseline)
**Affects:** On-call response quality, MTTR (mean time to resolve)

## Problem

`incident-response-runbook.md` cung cấp overview (severity, escalation, common incidents per-service) nhưng **không có runbook riêng cho từng alert rule**. Khi alert fires, on-call engineer phải improvise.

Evidence:
- 7 alerts trong `alert-rules.yml` không có `runbook_url` annotation
- `documents/05-guides/operations/runbooks/` chỉ có 1 file (`deployment-procedures.md`)
- Không có `runbooks/high-error-rate.md`, `runbooks/db-pool-exhausted.md`, v.v.

**Đây là META-GAP** per `.claude/rules/meta-gap-priority.md` — runbooks = operational skill infrastructure. Fix 1 lần → tất cả future incident response cải thiện.

## Root Cause

Runbooks được ghi khi có incident (reactive), không được build proactively cho mỗi alert.

## Proposed Fix

1. Create `documents/05-guides/operations/runbooks/` folder standard structure:
   ```
   runbooks/
     README.md                        (index)
     service-down.md
     high-error-rate.md
     high-response-time.md
     high-memory-usage.md
     database-pool-exhausted.md
     high-disk-usage.md
     rabbitmq-queue-backlog.md
   ```
2. Template cho mỗi runbook:
   ```markdown
   # Runbook: [Alert Name]

   **Alert:** `<alertname>`
   **Severity:** critical | warning
   **Last updated:** YYYY-MM-DD

   ## What does this alert mean?
   [1 paragraph]

   ## Immediate checks (0-5 min)
   1. Check X
   2. Check Y

   ## Likely causes
   - Cause A → Fix A
   - Cause B → Fix B

   ## Mitigation
   [Steps]

   ## When to escalate
   [Criteria]

   ## Related
   - [Links]
   ```
3. Add `annotations.runbook_url` vào mỗi alert rule trong `alert-rules.yml`:
   ```yaml
   annotations:
     summary: "Service {{ $labels.job }} is down"
     runbook_url: "https://docs.kiteclass.com/runbooks/service-down"
   ```
4. Alertmanager (GAP-120) template render runbook URL trong Slack/PagerDuty payload
5. New alert (GAP-122) → runbook mandatory trước khi merge

## Acceptance Criteria

- [x] 7 runbooks cho 7 existing alerts (`service-down.md`, `high-error-rate.md`, `high-response-time.md`, `high-memory-usage.md`, `database-pool-exhausted.md`, `high-disk-usage.md`, `rabbitmq-queue-backlog.md` under `documents/05-guides/operations/runbooks/`)
- [x] Runbook README.md với index (`documents/05-guides/operations/runbooks/README.md` per `docs-folder-structure.md` §3)
- [x] Alert rules có `runbook_url` annotation (7/7 alerts in `kitehub/docker/prometheus/alert-rules.yml`; 5/5 applicable in `kiteclass/docker/prometheus/alert-rules.yml` — kiteclass-side only ships 5 of 7 alerts; 7/7 in `infrastructure/helm/kitehub/templates/prometheusrule.yaml`)

## Out-of-scope (track separately)

| Item | Where |
|------|-------|
| Slack notification template renders runbook link | Depends on GAP-120 (Alertmanager). Once Alertmanager ships with receivers (GAP-144 Wave Observability Agent C), the `runbook_url` annotation is automatically rendered into Slack/PagerDuty payloads — no additional work in this gap. |
| PR template: "New alert added? → Runbook linked?" checkbox | Out of scope per Wave Observability prompt ("DO NOT — Add PR template checkbox (out of this PR's scope)"). Filed separately when next platform-alerts wave (GAP-122) lands — at that point the rule "new alert requires runbook" naturally becomes a checkbox. |
| Quarterly review: check runbook accuracy sau incidents | Process item, not a build artifact. Tracked by `output-review-mandate.md` §3 (review standards matrix); SRE owns quarterly cadence per `docs-folder-structure.md` §5 ownership matrix. |

## Meta-Boost Justification

Per `meta-gap-priority.md` §3:
- **Blast radius:** mọi future incident response dùng runbooks
- **Regression severity:** silent (bad MTTR không obvious)
- **Unblocks:** GAP-122 (new alerts require runbooks)

## Related

- Audit: `documents/04-quality/audits/ops/ops-readiness-audit-2026-04-19.md` §7
- Depends: GAP-120 (Alertmanager — render runbook URL)
- Enables: GAP-122 (add new alerts with runbooks)
- Related: `incident-response-runbook.md` (overview) + `deploy-go-nogo-checklist.md`

## Log

- **2026-04-28** Closed via Wave Observability Agent A. Shipped `documents/05-guides/operations/runbooks/README.md` (template + index per `docs-folder-structure.md` §3) plus 7 per-alert runbooks (50-109 lines each, project-specific — references real Hibernate/jsonb/Thymeleaf gotchas from `feedback_jpa_jsonb_jdbctypecode.md`, `feedback_thymeleaf_ognl_pin.md`, `feedback_dev_profile_schema_workaround.md`, `feedback_objectmapper_test_jsr310.md`; real RabbitMQ queue names from `EmailQueueConfig`, `AIQueueConfig`, `BrandingEventsConfig`, `RabbitListenerConfig`). Added `annotations.runbook_url` to all 7 alerts in `kitehub/docker/prometheus/alert-rules.yml` (7), 5 applicable in `kiteclass/docker/prometheus/alert-rules.yml` (kiteclass doesn't ship `HighDiskUsage`/`RabbitMQQueueBacklog` — node_exporter + RabbitMQ are kitehub-side infra), and 7 in `infrastructure/helm/kitehub/templates/prometheusrule.yaml`. YAML validated via `python3 -c "import yaml; yaml.safe_load(...)"` per `feedback_yaml_validate_before_push.md`. Three AC items moved to §Out-of-scope per `gap-done-discipline.md` §3 Option B: Slack rendering depends on GAP-144 Alertmanager (downstream wave step), PR template checkbox explicitly out of scope per wave plan, quarterly review is process not artifact. Verification artifact pointer per `gap-done-discipline.md` §2.5: `grep -c "runbook_url"` → kitehub Docker 7, kiteclass Docker 7 (5 new + 2 pre-existing DocumentGen), Helm 9 (7 new + 2 pre-existing DocumentGen).
- 2026-04-19 — Discovered in ops-readiness baseline audit
