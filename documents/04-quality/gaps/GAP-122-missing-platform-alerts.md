# GAP-122: Missing Platform-Critical Alerts

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** DevOps / Alerting
**Found:** 2026-04-19 (ops-readiness audit — baseline)
**Affects:** Operational visibility cho critical business flows

## Problem

Chỉ có 7 generic alerts (infrastructure-level). Thiếu alerts cho các business-critical scenarios của platform multi-tenant SaaS.

Current alerts (`alert-rules.yml`):
- ServiceDown, HighErrorRate, HighResponseTime, HighMemoryUsage, DatabasePoolExhausted, HighDiskUsage, RabbitMQQueueBacklog

Missing alerts (evidence: grep alert-rules.yml):
1. **Certificate expiry** (SSL cert < 14 days) — website down nếu cert expire
2. **AI provider failure rate** (Ollama/OpenAI >10% failure in 5m) — branding generation broken
3. **Email queue DLQ growing** (emails.send.dlq > 10 messages) — customer không nhận email
4. **Subscription webhook failure** (Stripe/payment 5xx rate) — billing broken
5. **Multi-tenant data leak detection** (cross-tenant query counter > 0) — P0 security
6. **Tenant provisioning failure rate** (>5% fail) — trial sign-up broken
7. **Flyway migration failure** — deploy broken
8. **JWT signing error rate** (auth failures spike) — DDoS or config error
9. **Backup job failure** (pg_dump exit code != 0) — data loss risk
10. **Branding quality gate fail rate** (<70 score repeatedly) — AI drift
11. **Redis eviction rate** (eviction > 1000/min) — cache pressure
12. **Rate limit breaches** (429 rate spike per-tenant) — abuse or hot tenant

## Root Cause

Existing alerts copy từ generic monitoring template. Không có alert-review session khi add feature.

## Proposed Fix

1. Categorize missing alerts:
   - **Critical (page immediately):** multi-tenant data leak, cert expiry, backup failure, migration failure
   - **Warning (Slack):** AI provider failure, email DLQ, webhook failure, provisioning failure, quality gate fail, JWT errors, Redis eviction, rate limit breach
2. Implement Prometheus recording rules cho compound metrics (multi-tenant query counter, webhook failure rate)
3. Add expr + annotations per alert
4. Add runbook per alert (GAP-121 dependency)
5. PR template rule: "Feature add mới → alert rules added?" checkbox

## Acceptance Criteria

- [ ] 12 new alerts implement với severity phân loại
- [ ] Runbook per alert (GAP-121 template)
- [ ] Test: trigger từng alert → verify firing + routing (GAP-120)
- [ ] Document trong `alerting-standards.md` trong 05-guides
- [ ] CI check: new alert requires runbook_url annotation

## Related

- Audit: `documents/04-quality/audits/ops/ops-readiness-audit-2026-04-19.md` §7
- Depends: GAP-120 (Alertmanager — routing), GAP-121 (runbooks)
- Related: GAP-019 (AI observability — overlap cho AI provider alerts)

## Log

- 2026-04-19 — Discovered in ops-readiness baseline audit
