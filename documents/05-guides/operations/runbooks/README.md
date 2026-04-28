# Per-Alert Runbooks Library

**Rules:** [`.claude/rules/docs-folder-structure.md`](../../../../.claude/rules/docs-folder-structure.md)
**Closes:** GAP-121
**Created:** 2026-04-28

Operational playbooks for every Prometheus alert rule shipped in this project. When an alert fires, the alert payload includes `runbook_url` pointing to one of the files below — on-call engineer follows the runbook instead of improvising. Pairs with the higher-level [`incident-response-runbook.md`](../../incident-response-runbook.md) (severity matrix, escalation path, communication templates).

---

## Directory Map

| Path | Purpose | Source alert rule |
|------|---------|-------------------|
| `README.md` | This index + runbook template | — |
| `service-down.md` | Any monitored service unreachable ≥1 min | `ServiceDown` (critical) |
| `high-error-rate.md` | >5% 5xx responses over 5 min window | `HighErrorRate` (warning) |
| `high-response-time.md` | p99/p95 latency above 2s for 5 min | `HighResponseTime` (warning) |
| `high-memory-usage.md` | JVM heap above 85% for 5 min | `HighMemoryUsage` (warning) |
| `database-pool-exhausted.md` | HikariCP active >80% (warn) / >90% (crit) | `DatabasePoolExhausted` (warning/critical) |
| `high-disk-usage.md` | Filesystem usage above 85% for 10 min | `HighDiskUsage` (warning) |
| `rabbitmq-queue-backlog.md` | Queue ready messages >1000 for 10 min | `RabbitMQQueueBacklog` (warning) |
| `deployment-procedures.md` | Pre-existing — deploy + rollback procedures | — |

> Document-generation specific runbook (`pdf-generation-font-not-found.md`) lives in [`documents/05-guides/runbooks/`](../../runbooks/) (pre-existing, retained for path stability — `runbook_url` annotations on `DocumentGenerationHigh*` alerts already point there).

---

## File Placement Rules

- ✅ **Belongs here:** runbooks for Prometheus alert rules (`alert-rules.yml`, `prometheusrule.yaml`). Each runbook is named `<kebab-case-of-alert>.md` so the filename matches the alert's `runbook_url` annotation 1:1.
- ❌ **Does NOT belong here:** general SRE procedures (deploy, secret rotation) → `documents/05-guides/operations/`. Domain-specific incident playbooks tied to a single feature (e.g. PDF font missing) → `documents/05-guides/runbooks/`.
- Naming: `<alertname-lower-kebab>.md`. Match the Prometheus `alertname` field exactly so future readers can `grep` between rule and runbook without translation.

---

## Archive Policy

Move to `documents/07-archived/runbooks-YYYY/` khi:
- Alert rule retired (no longer in any Prometheus config) AND no recent reference in postmortems
- Service decommissioned (e.g. if `kitehub-branding` ever folds into `kiteclass-core`)
- Doc >180 days old AND no recent reference

Quarterly review: SRE confirms each runbook still matches current alert thresholds, queue names, and service ports. Drift between alert expression and runbook = file follow-up gap (per `audit-to-gap-pipeline.md`).

---

## Runbook Template

Use this template when adding a new runbook (e.g. when GAP-122 lands new platform alerts). Each runbook must be 50-100 lines, project-specific (real paths, real queue names, real Hibernate/JPA/Thymeleaf/jsonb gotchas), NOT generic copy-paste.

```markdown
# Runbook: [Alert Name Display]

**Alert:** `<alertname-from-rule>`
**Severity:** critical | warning
**Last updated:** YYYY-MM-DD

## What does this alert mean?

[1 paragraph — what condition triggered the alert in plain language. Include
threshold + duration. Avoid jargon; on-call may be woken at 03:00.]

## Immediate checks (0-5 min)

1. Check X — `kubectl logs ...` / `docker logs ...`
2. Check Y — Grafana panel link (when GAP-143 lands)
3. Check Z — recent deploys in last 1h via `gh run list --workflow=deploy.yml`

## Likely causes

- **Cause A** → Fix A (cite real project memory: feedback file, ADR, gap)
- **Cause B** → Fix B
- **Cause C** → Fix C

## Mitigation

[Concrete steps. Include exact commands where applicable. Reference scripts
under `kitehub/scripts/` and `kiteclass/scripts/` rather than raw docker commands.]

## When to escalate

- After [N min] without resolution
- If [secondary symptom — e.g. cascading service-down across stack]
- Severity-bump criteria (warning → critical)

## Related

- Alert rule: `<file>:<line>`
- Architecture doc: [link]
- Related runbooks: [links]
```

---

## Key Documents

- [`incident-response-runbook.md`](../../incident-response-runbook.md) — overview: severity, escalation, communication templates, postmortem
- [`deployment-procedures.md`](./deployment-procedures.md) — deploy + rollback procedures (Helm + Docker Compose)
- [`rollback-procedure.md`](../../rollback-procedure.md) — rollback playbook
- [`deploy-go-nogo-checklist.md`](../../deploy-go-nogo-checklist.md) — pre-deploy gating
- [`SECRET-MANAGEMENT.md`](../../SECRET-MANAGEMENT.md) — credential rotation

---

## Adding a New Runbook (when GAP-122 + future alerts land)

1. Branch off main
2. Copy template above into `<alertname>.md`
3. Add `annotations.runbook_url: "/documents/05-guides/operations/runbooks/<alertname>.md"` to the alert rule (both Docker Prometheus and Helm `prometheusrule.yaml`)
4. Update this README's Directory Map table
5. Reference `audit-to-gap-pipeline.md` if the runbook stems from an audit finding
6. PR + merge per `feedback_always_pr_even_docs.md` — never push directly to main
