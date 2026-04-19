# GAP-121: Per-Alert Runbooks Library

**Status:** 🔵 OPEN
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

- [ ] 7 runbooks cho 7 existing alerts
- [ ] Runbook README.md với index
- [ ] Alert rules có `runbook_url` annotation
- [ ] Slack notification includes runbook link
- [ ] PR template: "New alert added? → Runbook linked?" checkbox
- [ ] Quarterly review: check runbook accuracy sau incidents

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

- 2026-04-19 — Discovered in ops-readiness baseline audit
