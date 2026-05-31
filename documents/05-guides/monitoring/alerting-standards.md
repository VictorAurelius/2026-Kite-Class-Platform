# Alerting Standards

**Closes:** GAP-122 (review-standard for platform alerts)
**Created:** 2026-04-28
**Last updated:** 2026-04-28
**Owner:** SRE / Ops lead
**Rules referenced:** [`output-review-mandate.md`](../../.claude/rules/output-review-mandate.md) §3, [`audit-to-gap-pipeline.md`](../../.claude/rules/audit-to-gap-pipeline.md), [`docs-folder-structure.md`](../../.claude/rules/docs-folder-structure.md)

This is the project-wide standard for **how we author, ship, and maintain Prometheus alerts**. It exists because Wave 1-4 shipped seven generic infrastructure alerts copied from a default monitoring template — adequate for "the JVM heap is full" but blind to platform-critical SaaS conditions (multi-tenant data leak, cert expiry, backup failure, billing webhook breakage). GAP-122 added 12 platform-critical alerts; this document codifies the practice so the next 12 alerts ship correctly without ad-hoc decisions.

---

## 1. Severity classification

Two and only two severity values. No "info", no "page-maybe-soon".

- **`critical`** — pages on-call immediately (PagerDuty / Opsgenie). Reserved for:
  - P0 security events (multi-tenant data leak, secret exposure)
  - Data-loss risk (backup failure, replication broken, migration corrupting schema)
  - Platform outage candidates (service down, cert expiry imminent, gateway down)
  - Compliance-breaking conditions (audit log silenced, retention rule violated)
- **`warning`** — Slack channel `#ops-alerts` only, no page. Reserved for:
  - Degradation (high error rate, high response time, queue backlog)
  - Operational signals that need investigation in business hours
  - Trend-watching conditions (eviction rate, DLQ growth, rate-limit breach)

If you find yourself wanting a third severity, use a **better expression**: tighter threshold for `critical`, looser for `warning`.

---

## 2. Required annotations

Every alert MUST have these four annotations. CI enforces (see §6).

| Annotation | Required? | Example |
|------------|-----------|---------|
| `summary` | yes | `Multi-tenant data leak detected on {{ $labels.service }}` |
| `description` | yes | `Cross-tenant query observed at {{ $value }}/sec on {{ $labels.service }}. Customer data may be exposed to other tenants. P0 escalation per multi-tenant-data-leak runbook.` |
| `runbook_url` | yes | `/documents/05-guides/operations/runbooks/multi-tenant-data-leak.md` |
| `severity` (label, not annotation) | yes | `critical` or `warning` |

`runbook_url` MUST point to a file that exists in `documents/05-guides/operations/runbooks/` and follows the GAP-121 template (title block + What does this alert mean? + Immediate checks + Likely causes + Mitigation + When to escalate + Related). Empty `runbook_url`, missing file, or templated placeholder = CI fails.

---

## 3. File locations

Alert rules live in three places, intentionally:

| File | Scope | Notes |
|------|-------|-------|
| `kitehub/docker/prometheus/alert-rules.yml` | Local dev / docker-compose stack | Mirrors prod's KiteHub-side groups; subset OK |
| `infrastructure/helm/kitehub/templates/prometheusrule.yaml` | Production K8s | Source of truth for prod alerts |
| `kiteclass/docker/prometheus/alert-rules.yml` | Single-tenant local dev | KiteClass-only alerts; subset of helm |

Production deploys via Helm into `monitoring` namespace as `PrometheusRule` CRDs (Prometheus Operator). Docker stacks are dev/CI conveniences — keep them aligned to avoid drift, but the helm chart is the contract.

---

## 4. Kitehub vs KiteClass split

**Platform-level alerts** — observe SaaS-wide invariants — live on the **kitehub side only**:

- Multi-tenant data leak (cross-tenant query)
- Cert expiry (platform domains)
- Backup job failure (cluster-wide)
- Flyway migration failure (any service)
- AI provider failure (kitehub-branding)
- Email queue DLQ
- Subscription webhook failure
- Tenant provisioning failure
- Branding quality gate fail rate
- JWT auth failure spike (gateway)
- Redis eviction rate (shared `kite-redis`)
- Rate-limit breach spike (gateway)

**Per-tenant alerts** — observe single-instance health — live on the **kiteclass side**:

- kiteclass-core service down (per-tenant)
- High response time on per-tenant API path
- Per-tenant database health (when sharded)

Rationale: kitehub is the SaaS control plane; kiteclass is the rendered education instance. An alert that observes "tenant X's grade-import job" is per-tenant scope. An alert that observes "tenants in general are failing to provision" is platform scope.

---

## 5. Metric-pending pattern

Sometimes the right alert references a metric that doesn't exist yet — the counter, exporter, or instrumentation isn't in code. Don't wait. Land the alert rule with the **intended expression** and a clear marker. Steps:

1. Land the alert rule with the final intended expression (e.g. `kite_cross_tenant_query_total > 0`)
2. Add an inline YAML comment noting metric source + tracking gap, e.g. `# metric pending — needs BaseEntity tenant filter Micrometer counter (GAP-XXX)`
3. The runbook for this alert MUST include a `## Note` section explicitly stating "metric-pending" and what unblocks it
4. Alert won't fire until the metric ships, but intent + runbook + threshold are captured at design time

**Precedent:** `DocumentBrandingCacheMissStorm` in helm prometheusrule.yaml (cf. GAP-219) was filed as metric-pending while the cache-miss counter was being instrumented. It activated automatically once the metric landed — no follow-up alert PR needed.

This pattern reduces the cycle time between "we know we need this alert" and "we have it" — instead of the alert getting forgotten while the metric is debated, the alert is the forcing function for the metric.

---

## 6. CI enforcement

`scripts/check-alert-runbook-url.py` runs in the `quality-infra.yml` workflow. It:

1. Parses every alert rule in the three locations from §3
2. Asserts each alert has non-empty `runbook_url` annotation
3. Asserts the path resolves to an existing file under `documents/05-guides/operations/runbooks/`
4. Asserts the file contains the required §sections (title block + the GAP-121 template headers)
5. Fails CI on regression

This is the §6.5 Enforcement Parity Mandate (`rule-change-process.md`) applied to alerts: the rule (this document) ships in the same wave as the detector script and the CI job. Solo-dev mode means we can't have N reviewers, but we can have a check that fails loud.

---

## 7. Adding a new alert — checklist

```
- [ ] Choose severity (critical vs warning per §1)
- [ ] Write the expression (PromQL) — use clamp_min on rate denominators to avoid division-by-zero NaN
- [ ] Add summary/description/runbook_url annotations + severity label (per §2)
- [ ] Create the runbook in documents/05-guides/operations/runbooks/<alertname-kebab>.md
      using the GAP-121 template; ≥40 lines, project-specific content
- [ ] Update documents/05-guides/operations/runbooks/README.md Directory Map
- [ ] Run python3 scripts/check-alert-runbook-url.py locally before commit
- [ ] Mirror the rule into infrastructure/helm/kitehub/templates/prometheusrule.yaml
      if production-bound (most platform alerts are)
- [ ] If metric is pending, follow §5 pattern + file tracking gap + add ## Note in runbook
- [ ] PR per CLAUDE.md Wave Branch Strategy — never push directly to main
```

---

## 8. PromQL conventions

- Use `rate(...[5m])` not `irate` for alert expressions; `irate` is too jittery for thresholds
- Always `clamp_min(denominator, 1)` when dividing — avoid NaN from zero-traffic windows
- `for: 5m` minimum on warnings, `for: 1m` on criticals (so transient blips don't page)
- Label cardinality budget: `tenant_id` is a high-cardinality label — only include when alert needs per-tenant routing; otherwise aggregate
- Recording rules go in a separate `*-recording.yml` group and are referenced by alert rules — keeps alert expressions readable
- Avoid `up{job=~".*"}` patterns — explicit job names so an exporter outage doesn't make every alert silently green

---

## 9. Ownership

| Role | Responsibility |
|------|---------------|
| **SRE / Ops lead** | Review alert thresholds quarterly; approve severity changes; own the runbook library |
| **Service owner** | Review metric availability when their service ships; respond to alerts on their service |
| **Author of new alert** | Write the runbook in the SAME PR; verify CI passes; update README index |
| **Reviewer of alert PR** | Verify §1-§7 followed; verify runbook has real project-specific content (not boilerplate) |

---

## 10. Alert review cadence

- **Per-PR:** new/changed alerts reviewed inline (this document is the criteria)
- **Per-wave:** wave-completion-check skill verifies new alerts from the wave have runbooks
- **Quarterly:** SRE samples 5 alerts and walks the runbook end-to-end (does it actually work?), files gaps for drift
- **On-incident:** every fired alert that wasn't actionable from the runbook → file gap to improve runbook (per `audit-to-gap-pipeline.md`)

---

## 11. Anti-patterns

| ❌ Don't | ✅ Do |
|---------|------|
| Ship an alert with `runbook_url: ""` "to fill in later" | Block CI; write the runbook in the same PR |
| Copy-paste a generic runbook ("check logs, restart service") | Real project paths, real service ports, real Hibernate/Outbox/JWT gotchas |
| Use `severity: info` or invent third severity | Two only — `critical` (pages) or `warning` (Slack) |
| Land an alert without testing the expression on real Prometheus | `promtool test rules <file>` or staging eval |
| Forget the `for:` clause — alert flaps on transient blips | Minimum 1m for critical, 5m for warning |
| Per-tenant alert rule emitted to platform side | Place per-tenant alerts on kiteclass side per §4 split |
| Land alert in helm but skip the docker mirror | Keep the three locations §3 in sync (or document the deliberate gap) |

---

## 12. Related

- `documents/04-quality/gaps/closed/GAP-121-per-alert-runbooks.md` — per-alert runbook library (closed)
- `documents/04-quality/gaps/closed/GAP-122-missing-platform-alerts.md` — this addition's source gap
- `.claude/rules/output-review-mandate.md` §3 — review-standards matrix (alerting-standards lands a row here)
- `.claude/rules/audit-to-gap-pipeline.md` — audit findings on alerts feed this pipeline
- `documents/05-guides/operations/runbooks/README.md` — runbook index
- `documents/05-guides/operations/incident-response-runbook.md` — severity matrix + escalation + comms templates

---

## 13. Log

- **2026-04-28** — Standards published as part of GAP-122 closure (12 new platform-critical alerts: multi-tenant-data-leak, cert-expiry-imminent, backup-job-failure, flyway-migration-failure, ai-provider-high-failure-rate, email-queue-dlq-growing, subscription-webhook-failure, tenant-provisioning-failure, branding-quality-gate-fail-rate, jwt-auth-failure-spike, redis-eviction-rate, rate-limit-breach-spike). Paired in same wave with `scripts/check-alert-runbook-url.py` CI enforcement and helm/docker alert-rule additions.
