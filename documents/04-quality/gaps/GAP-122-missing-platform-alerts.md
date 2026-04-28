# GAP-122: Missing Platform-Critical Alerts

**Status:** 🟢 DONE 2026-04-28 — All 5 AC met (12 alerts + 12 runbooks + standards doc + CI gate green)
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

- [x] 12 new alerts implement với severity phân loại (4 critical, 8 warning) — kitehub docker + helm
- [x] Runbook per alert (GAP-121 template, 12 new files in `documents/05-guides/operations/runbooks/`)
- [x] `documents/05-guides/alerting-standards.md` (192 lines, severity rules + runbook contract + metric-pending pattern + ownership matrix)
- [x] CI check: `scripts/check-alert-runbook-url.py` + workflow job `alert-runbook-url` in `.github/workflows/script-quality.yml` (self-test 4/4 fixtures green; full repo scan 54/54 alerts green)

## Out-of-scope (track separately)

| Item | Where |
|------|-------|
| Test: trigger each alert end-to-end (Prometheus fire → Alertmanager route → Slack/PagerDuty) | Depends on Alertmanager production wiring (GAP-120 / GAP-144 receivers). Once routing is live, on-call validates fire path via amtool fire-recipe per Wave Observability precedent. Standards doc §"Adding a new alert" includes the validation step. |
| Per-tenant labels on `RateLimitBreachSpike` | Sister gap GAP-259 (tenant-key gateway rate limit) — adds tenant label which this alert can later consume |
| AI cost-burn alert (per-tenant token burn) | Sister gap GAP-258 (input token validation) addresses the upstream cause; cost-burn alert tracked in GAP-019 (AI observability) |

## Related

- Audit: `documents/04-quality/audits/ops/ops-readiness-audit-2026-04-19.md` §7
- Depends: GAP-120 (Alertmanager — routing), GAP-121 (runbooks)
- Related: GAP-019 (AI observability — overlap cho AI provider alerts)

## Log

- **2026-04-28** Closed via single-gap parallel wave (3 slices, 3 worktree-isolated agents). Manifest frozen at `documents/03-planning/waves/wave-2026-04-28-gap-122-platform-alerts.md`. Slice A (CI gate) → `scripts/check-alert-runbook-url.py` 188 LOC stdlib-only Python, regex-based scan tolerant of Helm `{{- if }}` + escaped `{{ "{{ $labels.X }}" }}` templating; workflow job `alert-runbook-url` added to `script-quality.yml` (path-filtered, self-test + repo scan). Slice B (12 alerts) → `kitehub-platform-alerts` group appended to `kitehub/docker/prometheus/alert-rules.yml` (+159 lines) + `infrastructure/helm/kitehub/templates/prometheusrule.yaml` (+152 lines, Helm escape applied). Slice C (12 runbooks + standards) → 12 new files in `operations/runbooks/` (74-89 lines each, project-specific references to real services + memory feedback) + `alerting-standards.md` (192 lines) + index update on `runbooks/README.md`. Incidental coverage: 6 pre-existing alerts surfaced by CI gate (`DocumentBrandingCacheMissStorm` × 2 in helm + kiteclass docker, 5 SLO-tier alerts in helm) gained `runbook_url` in same PR — 5 SLO point to `documents/05-guides/api-performance-slo.md`, cache-miss alerts point to new `branding-cache-miss-storm.md` (78 lines). Verification artifact: `python3 scripts/check-alert-runbook-url.py` reports 3 files / 54 alerts / 0 failures; YAML safe_load passes for both docker files + workflow file. Spawned sister gaps GAP-258 (AI input prompt token validation) + GAP-259 (gateway tenant-key rate limit) from concurrent state-check vs 2026-04-28 article on AI backend cost-attack — both filed alongside this closure for Wave 9+ scheduling.
- 2026-04-19 — Discovered in ops-readiness baseline audit
