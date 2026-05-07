# Ops Readiness Audit — Wave 33 BETA Deploy Cluster

**Date:** 2026-05-07
**Auditor:** Background agent a0737b3f (Sonnet, Explore subagent)
**Scope:** Wave 33 (GAP-376/370/372/369/379) — infrastructure + helm + smoke/runbook artifacts

---

## Score: 50/100 — F (delta -2 vs 2026-04-25 baseline 52/100)

| Category | Score | Status |
|----------|:-----:|--------|
| Monitoring & Observability | 12/20 | Prometheus configured; **kube-prometheus-stack disabled by default** (production needs explicit opt-in); no Grafana dashboards deployed |
| Logging Standards | 8/20 | Logstash JSON encoder configured; no custom metrics for beta flow; no PII scrubber check |
| Backup & Recovery | 6/20 | Generic Helm rollback OK; **pre-deploy DB snapshot NOT automated**; RTO/RPO undefined |
| Alerting | 10/20 | 12 rules defined + 7 baseline; **AlertManager production receivers chưa wire** (GAP-144 PARTIAL — chart-level OK, live deploy pending) |
| Deployment Pipeline | 14/20 | Rolling updates + Helm rollback + liveness/readiness probes; smoke test exists but does NOT validate beta-signup flow |

---

## Twelve-Factor Compliance

| Factor | Status |
|--------|--------|
| III. Config in environment | 🟡 Helm values + K8s Secrets template ready; `.env.production.template` partial (GAP-379) |
| IV. Backing services | 🟢 RDS/Redis/SES via env var references |
| IX. Disposability | 🟡 Readiness probes 30s; no startup probe on stateless services |
| XI. Logs as event streams | 🟡 Logstash configured but no MDC for `tenantId`/`betaRequestId` |

---

## Top 5 Ops Gaps (Wave 33-specific)

| # | Sev | Gap | Recommendation |
|---|:---:|-----|----------------|
| 1 | 🔴 P0 NEW | Beta-invite metric holes — no `beta_signup_requests_total`, `beta_approvals_total`, `beta_rejections_total` counters | Add 3× Micrometer counters trong `BetaAccessService` + `/actuator/prometheus` |
| 2 | 🔴 P0 PARTIAL | GAP-369 DNS runbook contains `[USER_INPUT_REQUIRED]` placeholders (Oracle VM IP, Cloudflare proxy) | Already PARTIAL — user-executed steps per `gap-done-discipline.md` §3 |
| 3 | 🟠 P1 NEW | Pre-deploy backup not automated — checklist exists, no script to trigger RDS snapshot/pg_dump | Create `scripts/backup-production.sh` wrapping `aws rds create-db-snapshot`; pre-deploy CI gate |
| 4 | 🟠 P1 CARRY | AlertManager production receivers — GAP-144 still PARTIAL; alerts fire to /dev/null | Already tracked GAP-144; live-deploy mock-fire verification pending |
| 5 | 🟠 P1 NEW | No smoke test for beta-invite email delivery — post-deploy script checks `/actuator/health` only | Extend `scripts/smoke-test.sh`: POST beta request → retrieve token → verify email delivery (mock SES staging, live prod) |

---

## Delta vs 2026-04-25 Baseline (52/100 F)

| Category | Δ | Reason |
|----------|---|--------|
| Monitoring | -0 | Prometheus baseline untouched; no new instrumentation on beta endpoints |
| Logging | +1 | Beta-invite flows add custom debug logging |
| Backup | -1 | Pre-deploy automation NOT added (checklist exists, no script) |
| Alerting | -1 | 12 new rules defined but receivers config still missing |
| Deployment | -1 | Wave 33 runbooks reference manual user steps; no CI automation cho new beta-signup flow |
| **Subtotal** | **-2 → 50** | |

**Root cause:** Wave 33 ships **CODE-COMPLETE (5 GAPs PARTIAL/DONE)** nhưng **OPS-INCOMPLETE**. Beta invite mechanism + seed runner + email templates functional; observability + alerting stack chưa wire production.

---

## Gap Recommendations

- **NEW P0**: Beta metric counters
- **NEW P1**: Pre-deploy backup automation
- **NEW P1**: Beta-invite email delivery smoke test
- **EXISTING tracked**: GAP-369 DNS placeholders (PARTIAL intentional), GAP-144 AlertManager receivers (PARTIAL intentional)

---

## 1-line summary

Wave 33 BETA cluster ready to **deploy** (code + runbooks + seed complete) but **not ready to operate** (observability gaps: beta metrics, alerting routing live-deploy, DNS automation, backup scheduling, email smoke).
