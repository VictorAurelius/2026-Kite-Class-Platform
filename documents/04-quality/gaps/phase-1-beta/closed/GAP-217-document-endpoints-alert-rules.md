# GAP-217: Alert rules for `/api/v1/documents/*` endpoints

**Status:** 🟢 DONE 100%
**Priority:** 🔴 P0 — Wave 5 introduced new HTTP surface; alerts are required per `post-wave-audit-mandate.md` §2.1
**Domain:** Ops / Observability
**Found:** 2026-04-25 (Wave 5 audit suite — ops-readiness audit finding #1)
**Affects:** Sub-PR 5.5 endpoints + future document-generation expansion
**Blocked by:** GAP-120 (Alertmanager deployment) — alerts won't route until Alertmanager exists

## Problem

`POST /api/v1/documents/{format}/preview` and `POST /api/v1/documents/{format}/download` shipped in Sub-PR 5.5. Existing Prometheus alert rules in `infrastructure/.../prometheusrule.yaml` cover only generic categories (ServiceDown, HighErrorRate, HighResponseTime) — none specifically detect:

- Document p95 latency exceeding `BR-DOC-PDF-007` 2s budget
- Sustained 5xx rate > 1% on document endpoints
- Cold-cache latency spikes (when `branding-by-tenant` cache misses storm)
- Tomcat thread-pool saturation under bulk-export load (cross-ref GAP-220)

Generic alerts (e.g., `HighResponseTime`) WILL fire on document slowness but cannot distinguish doc-gen from other endpoints — on-call cannot act without per-endpoint context.

## Root Cause

Wave 5 scope (skills + generators + integration) did not include alerting. Per `post-wave-audit-mandate.md` §2.1 mapping, ops audit triggers when `infrastructure/` changes — Wave 5 didn't change infrastructure files, so the gap stayed below the radar until this refresh.

## Proposed Fix

Add to `infrastructure/.../prometheusrule.yaml`:

```yaml
groups:
- name: document-generation
  interval: 30s
  rules:
  - alert: DocumentGenerationHighP95
    expr: histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{uri=~"/api/v1/documents/.*"}[5m])) by (le)) > 2
    for: 5m
    labels:
      severity: warning
      service: kiteclass-core
    annotations:
      summary: "Document generation p95 > 2s (BR-DOC-PDF-007)"
      runbook: documents/05-guides/runbooks/document-generation-slow.md  # GAP-218
  - alert: DocumentGenerationHighErrorRate
    expr: sum(rate(http_server_requests_seconds_count{uri=~"/api/v1/documents/.*",status=~"5.."}[5m])) / sum(rate(http_server_requests_seconds_count{uri=~"/api/v1/documents/.*"}[5m])) > 0.01
    for: 5m
    labels:
      severity: critical
    annotations:
      summary: "5xx rate on /api/v1/documents/* exceeds 1%"
  - alert: DocumentBrandingCacheMissStorm
    expr: rate(spring_cache_misses_total{cache="branding-by-tenant"}[5m]) > 10
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "Branding cache miss rate elevated — possible eviction storm or cold start"
```

Predicate on `GAP-220` (Spring Cache Micrometer metrics enabled) for the third rule.

**Critical caveat:** alerts route through Alertmanager. Until `GAP-120` (Alertmanager deploy) closes, these rules will fire silently. **Filing this gap as P0 + blocked-by-GAP-120 documents the dependency**; actual alerting only becomes real after both close.

## Acceptance Criteria

- [ ] 3 rules added to `prometheusrule.yaml` (high p95, error rate, cache miss storm)
- [ ] Each rule references `runbook:` annotation pointing to a real `documents/05-guides/runbooks/*.md` (depends on GAP-218 for the font-missing runbook)
- [ ] Rules tested via `promtool check rules` in CI
- [ ] Helm/k8s deployment includes the updated rules
- [ ] Documented decision: rules are filed but route via Alertmanager (depends on GAP-120) — until that ships, on-call escalation path is "page via existing generic alerts + logs"

## Related

- Audit: `documents/04-quality/audits/ops/ops-readiness-audit-2026-04-25-wave5.md`
- GAP-120 (baseline): Alertmanager deployment — required for alert routing
- GAP-218 (parallel): font-missing runbook — referenced by `runbook:` annotations
- GAP-220 (P1, will be filed in catch-all GAP-219): Spring Cache Micrometer metrics — required for `DocumentBrandingCacheMissStorm` rule
- GAP-214: parent audit suite gap

## Log

- **2026-04-25:** Filed from Wave 5 audit suite (ops audit finding #1). P0 because Wave 5 introduced the endpoints; alert coverage is mandated for new HTTP surface. Blocked by GAP-120 for actual routing.

- **2026-05-26 (Wave br-7 Bucket C PR #1844 — 3 alert rules already exist (lines 100-144 prometheusrule.yaml from Sub-PR 5.6b era); normalized 2 runbook_url + new promtool CI job closure):** Flipped DONE 100% — . CSV row updated + file moved to phase-1-beta/closed/ per `gap-done-discipline.md` §2.
