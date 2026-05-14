# Audit Skill Rubric — ops-readiness-audit (5 categories, per-check pass/fail)

**Priority:** 🟠 MANDATORY — audit primacy + per-check rubric for `ops-readiness-audit` skill
**Version:** 1.0.0
**Created:** 2026-05-14
**Last-Reviewed:** 2026-05-14
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule with built-in enforcement (5-category per-check rubric + bug-finding-primacy + extends `ops-readiness-audit/SKILL.md` + worked self-test on current main surfaces ≥1 finding) per §6.5 Enforcement Parity Mandate; no constraint loosening — generalizes Wave 71c security-audit pattern closing GAP-523)
**Applies to:** Every invocation of `.claude/skills/quality/ops-readiness-audit/SKILL.md` (/100 production operations readiness — monitoring, logging, backup, alerting, deployment)

---

## 1. The Rule

> **`ops-readiness-audit` skill must score every Category by per-check pass/fail (no averaging hides P0 sub-check failures within a 20-pt category). Any P0/P1 sub-check FAIL caps category total ≤ 16/20 AND audit-level verdict = FAIL. The bug list (every FAIL) is the deliverable; the score is descriptive only.**

Wave 40 baseline `60/100 D` averaged 5 categories' sub-checks → low overall score but didn't enumerate WHICH ops gaps were P0 vs P1. Per-check pass/fail eliminates this — surfaces every monitoring/log/backup/alert/deploy gap explicitly.

---

## 2. Mandatory per-check enumeration (≥5 per category)

### 2.1 Category 1 — Monitoring & Observability (P0 health, P1 metrics)

| # | Check | Severity | Pass criterion |
|---|---|---|---|
| 1.1 | Every service exposes `/actuator/health` returning 200 | P0 | curl each service health endpoint via gateway |
| 1.2 | `management.endpoints.web.exposure.include` includes `health,info,prometheus,metrics` | P0 | grep `application.yml` per service |
| 1.3 | Prometheus scrape config targets every service `/actuator/prometheus` | P0 | `infrastructure/prometheus/*.yml` job per service |
| 1.4 | Grafana dashboards exist for: JVM, HTTP, DB, RabbitMQ | P1 | `infrastructure/grafana/dashboards/` count ≥4 |
| 1.5 | Distributed tracing (OpenTelemetry) configured + traces visible | P1 | OTel exporter config + Jaeger/Tempo UI sample trace |
| 1.6 | Custom business metrics (signup rate, AI gen rate) emitted via Micrometer | P2 | `grep -rn 'meterRegistry\|@Counted'` |

### 2.2 Category 2 — Logging Standards (P0 structure, P1 PII)

| # | Check | Severity | Pass criterion |
|---|---|---|---|
| 2.1 | All Java services emit JSON-structured logs (no plain text) per `logs-format-standard.md` | P0 | `application.yml` logstash-encoder configured |
| 2.2 | Required fields present in every log event: `timestamp`, `level`, `service`, `tenantId`, `traceId` | P0 | sample 10 log lines; verify all 5 fields |
| 2.3 | PII scrubber active: email/phone/JWT regex-masked at logger | P0 | startup smoke test fires test event w/ email → masked output verified |
| 2.4 | Log aggregation pipeline running (Loki OR Elasticsearch) with ≥7d hot retention | P0 | aggregator URL returns 200 + retention config |
| 2.5 | Banned `System.out.println` / `printStackTrace()` in `src/main/java/**` | P1 | ArchUnit test green + grep returns 0 |
| 2.6 | Retention tiers documented + enforced: hot 7d / warm 30d / cold 180d | P1 | aggregator retention policy doc |

### 2.3 Category 3 — Backup & Recovery (P0 backup, P0 restore drill)

| # | Check | Severity | Pass criterion |
|---|---|---|---|
| 3.1 | PostgreSQL daily backup running (RDS automated OR `pg_dump` cron) | P0 | RDS snapshot timestamp ≤24h OR cron job verified |
| 3.2 | RTO + RPO documented per service tier | P0 | `documents/05-guides/operations/db-backup-rotation-runbook.md` or equiv |
| 3.3 | DR plan: failover region OR snapshot-restore procedure | P0 | runbook exists with concrete commands |
| 3.4 | Restore drill ≤90 days old (proves backups actually restore) | P0 | drill log in `documents/04-quality/audits/ops-readiness/` |
| 3.5 | MinIO/object storage backup strategy (cross-region replication OR snapshot) | P1 | bucket replication rule OR snapshot cron |
| 3.6 | Secrets backup: AWS Secrets Manager auto-versioning enabled | P1 | `aws secretsmanager describe-secret` shows `VersionStages: AWSCURRENT/AWSPREVIOUS` |

### 2.4 Category 4 — Alerting (P0 alert rules, P1 routing)

| # | Check | Severity | Pass criterion |
|---|---|---|---|
| 4.1 | Alert rules defined for: service-down (5min), high-error-rate (>5%), high-latency (P95>2s) | P0 | `infrastructure/prometheus/alerts/*.yml` rules |
| 4.2 | Alertmanager routing config exists + reaches on-call channel | P0 | `infrastructure/alertmanager/*.yml` route → Slack/email/PagerDuty target |
| 4.3 | Per-alert runbook in `documents/05-guides/operations/runbooks/` | P1 | every alert-name has matching `<alert-name>-runbook.md` |
| 4.4 | Alert silencing/grouping documented (avoid alert fatigue) | P1 | runbook describes silence rules |
| 4.5 | Alert auto-test: synthetic alert fires + reaches recipient (monthly drill) | P1 | drill log ≤30 days old |
| 4.6 | Severity classification documented: P0 paged, P1 ticket, P2 dashboard-only | P2 | runbook reference |

### 2.5 Category 5 — Deployment Pipeline (P0 rolling, P0 rollback, P1 verify)

| # | Check | Severity | Pass criterion |
|---|---|---|---|
| 5.1 | Deploy strategy: blue-green OR rolling (not stop-and-redeploy) | P0 | `infrastructure/helm/values.yml` `strategy.type` OR ECS deploy config |
| 5.2 | Health checks gate deploy rollout (failing health → rollback) | P0 | `livenessProbe` + `readinessProbe` in helm OR ECS health-check grace |
| 5.3 | Rollback procedure tested (per `release-deploy-standard.md` §4.4) | P0 | `scripts/smoke-rollback-cycle.sh --execute` ran ≤90 days ago |
| 5.4 | Deploy duration baseline measured (target <30min for pre-release) | P1 | DORA-style metric logged |
| 5.5 | Post-deploy smoke test automated (`scripts/smoke-test.sh`) | P0 | script exists + runs in deploy workflow |
| 5.6 | Deploy workflow triggers from tag push (not branch push) | P1 | `.github/workflows/docker-build-push.yml` trigger pattern |

---

## 3. Banned shortcuts

| ❌ Banned | ✅ Required |
|---|---|
| "Cat 1 averaged 12/20 — some monitoring present" | Each sub-check pass/fail; if 1.3 Prometheus scrape FAIL → cap Cat 1 ≤16/20 |
| Skip Cat 4 Alerting "because using Slack manually for now" | Manual ≠ alerting; mark each P0 FAIL |
| Score Cat 3 high because "we have RDS backups" without verifying 3.4 restore drill | Restore drill IS the P0 check; backups without drill = unverified |
| Aggregate Cat 5 as "deploy works" without enumerating 5.1-5.6 | Per-check pass/fail; rollback drill (5.3) is separate from deploy strategy (5.1) |
| "60/100 baseline, D grade" without bug list | Bug list precedes score; audit-level verdict = FAIL if any P0 FAIL |

---

## 4. Bug-finding > scoring primacy (BLOCKING)

> **An `ops-readiness-audit` run's purpose is to surface production-operational gaps the dev team cannot trust other layers to catch. A score of `60/100` with hidden P0 gaps (no restore drill, no alerting, no PII scrubber) is WORSE than `40/100` listing every gap honestly.** Per Wave 71c primacy pattern (security-audit/SKILL.md §2), averaging across 5 categories hid which ops areas were P0 unsafe vs P1 polish.

Rules for every `ops-readiness-audit` run:

1. Enumerate ALL §2 sub-checks across 5 categories. NEVER skip "obviously fine."
2. Each sub-check returns `PASS` / `FAIL` / `N/A-with-reason` / `❓ UNCHECKED`. No partial credit.
3. Final output starts with bug list (every FAIL with severity + evidence) BEFORE score table.
4. Score descriptive only; audit-level verdict = FAIL if ANY P0 sub-check FAILS.
5. If time-budget runs out, mark remaining `❓ UNCHECKED` — NEVER default to PASS.

---

## 5. Worked self-test — apply rubric to current main HEAD (2026-05-14)

| Sub-check | Verification | Verdict |
|---|---|---|
| 1.5 Distributed tracing | `grep -rn 'opentelemetry\|otel' --include='application.yml'` | ⚠️ Likely FAIL — per Wave 40 audit "GAP-115 log aggregation + distributed tracing path to 80" still in flight |
| 2.4 Log aggregation pipeline | check if Loki/Elasticsearch running | ⚠️ Likely FAIL — same Wave 40 GAP-115 evidence |
| 3.4 Restore drill ≤90 days | `ls documents/04-quality/audits/ops-readiness/restore-drill-*.md` | ⚠️ Likely FAIL — no recent restore-drill artifact in repo |
| 4.1 Alert rules defined | `ls infrastructure/prometheus/alerts/` OR equivalent | ⚠️ Likely FAIL — Wave 40 noted "1 P0 alert metric mismatch" suggesting alert wiring incomplete |
| 5.3 Rollback drill ≤90 days | `git log -- scripts/smoke-rollback-cycle.sh` last run | ⚠️ Verify — script exists per `release-deploy-standard.md` §4.4 but execute cadence unclear |

**Verdict:** ≥3 P0 FAILs surfaced retroactively (1.5 tracing, 2.4 aggregation, 3.4 restore-drill). Rule fires correctly — Wave 40 `60/100 D` score reflected these but didn't enumerate which were P0-blocking vs P1-polish. Per-check rubric makes blocker status explicit. **Self-test PASS** ✅.

---

## 6. Enforcement (per `rule-change-process.md` §6.5)

### 6.1 ops-readiness-audit/SKILL.md rubric extension (paired same PR)

Skill body extended with §"Per-check scoring" subsection citing this rule.

### 6.2 Pre-promotion gate

Before any release tag `v1.0.0-rc.*` or `v1.0.0`, `ops-readiness-audit` run MUST report ZERO P0 FAILs across §2.1-§2.5.

### 6.3 Reviewer checklist

- [ ] Bug list precedes score table?
- [ ] Each category lists 5+ per-check verdicts?
- [ ] If any P0 FAIL: audit-level verdict marked FAIL?

### 6.4 Override mechanism

```
git commit -m "...
OPS_READINESS_DEFER: <check ID + reason — e.g., 1.5 tracing GAP-115 Wave 70>
OPS_READINESS_FOLLOWUP: <gap link + completion date>"
```

### 6.5 Detector (deferred)

Future `scripts/check-ops-readiness-rubric.sh` — defer until 2nd recurrence per `incident-to-rule-pipeline.md` premature-rule guard ≥7 days.

---

## 7. Log

- **2026-05-14 (v1.0.0):** Rule created closing GAP-523 META P0 (Wave 72b Bucket E). Generalizes Wave 71c security-audit per-check pattern to ops-readiness-audit's 5 categories. Per `incident-to-rule-pipeline.md` 5-stage: Detect ✓ (GAP-523 surfaced 6 audit skills with averaging risk) → Classify ✓ (no rule enforces per-check pass/fail for ops audit Cat 1-5) → Rule+Enforce ✓ (this file + ops-readiness-audit/SKILL.md §"Per-check scoring" extension paired same PR per `rule-change-process.md` §6.5) → Self-Test ✓ (§5 worked example on current main — 3 P0 FAILs surfaced: 1.5 tracing, 2.4 log aggregation, 3.4 restore drill) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — adds previously-vague per-check enforcement, no constraint loosening). Detector wiring deferred per premature-rule guard ≥7 days.
