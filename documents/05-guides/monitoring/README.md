# monitoring — Alerting, SLO, Performance, Resource Limits

**Rules:** [`.claude/rules/docs-folder-structure.md`](../../../.claude/rules/docs-folder-structure.md)

Standards cho observability + performance: alerting rules, SLO targets, bundle/resource budgets. Audience: SRE, on-call, performance engineers.

---

## Directory Map

| File | Purpose |
|------|---------|
| `alerting-standards.md` | Alert rule format, severity, runbook_url mandate |
| `api-performance-slo.md` | API latency p95 SLO tiers + tagging rubric (GAP-135) |
| `frontend-bundle-budget.md` | Bundle size budgets per route + monitoring |
| `docker-resource-limits.md` | Container CPU/memory limits per service |

---

## File Placement Rules

- ✅ **Belongs here:** observability standards, performance budgets, SLO/SLI targets
- ❌ **Does NOT belong here:** alerting implementation (xem `infrastructure/helm/prometheus/`), runbooks bị trigger bởi alert (xem [`../operations/runbooks/`](../operations/runbooks/))

---

## Related

- Runbooks paged bởi alerts: [`../operations/runbooks/`](../operations/runbooks/) (mỗi runbook có alert_id mapping)
- Alert config implementation: `infrastructure/helm/prometheus/alerts/`
- Performance test results: `documents/04-quality/audits/performance/`

---

## Archive Policy

Move sang `documents/07-archived/monitoring-YYYY/` khi metric stack thay đổi (vd switch Prometheus → Datadog).
