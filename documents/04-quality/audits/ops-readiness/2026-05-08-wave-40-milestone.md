# Ops Readiness Audit — Wave 40 Milestone (release-deploy-artifacts cluster)

**Date:** 2026-05-08
**Auditor:** Bucket E agent (ops-readiness-audit skill)
**Scope:** Domain-milestone audit cho cụm `release-deploy-artifacts` sau Wave 33+34+37+38+39 per `post-wave-audit-mandate.md` §2.4.2
**Baseline:** 2026-05-07 post-Wave-35 = **53/100 F**
**Skill:** `.claude/skills/quality/ops-readiness-audit/SKILL.md`

---

## Score: **60/100 — D** (delta +7 vs Wave 35 baseline 53/100)

| # | Category (20 pts) | Wave 35 | Wave 40 | Δ | Notes |
|---|-------------------|:-------:|:-------:|:--:|-------|
| 1 | Monitoring & Observability | 13 | 15 | +2 | 3 Grafana dashboards provisioned (`dashboard-http-traffic`, `dashboard-infra-pools`, `dashboard-jvm-heap-gc`); 29 alert rules; GAP-135 PARTIAL — `@Timed` on 4/5 services; distributed tracing still absent; Sentry not wired (stub only). |
| 2 | Logging Standards | 8 | 10 | +2 | Logstash JSON encoder active (GAP-114 PARTIAL — infrastructure shipped Wave 25); `PIIScrubber` bean shipped in `kitehub-platform`; logback-spring.xml per-service includes MDC keys (`tenantId`, `traceId`, `spanId`, `userId`, `requestId`). Still missing: live 3-service MDC tracing end-to-end verification (GAP-114 deferred); log aggregation (GAP-115 OPEN). |
| 3 | Backup & Recovery | 6 | 9 | +3 | `scripts/backup-production.sh` shipped (Wave 36); `scripts/verify-restore.sh` shipped (GAP-117 Phase 2); restore-procedure.md + DR plan + RTO/RPO matrix exist. GAP-117 PARTIAL (Phase 3 quarterly drill deferred GAP-257); `kite_backup_last_success_timestamp_seconds` metric producer NOT wired → `BackupJobFailure` alert is dead; MinIO backup (GAP-118) still OPEN. |
| 4 | Alerting | 12 | 13 | +1 | 29 alert rules (prometheusrule.yaml); honeypot wire-up fixed (BetaAccessController.java:208 calls `recordHoneypotRejection()`); 25 runbooks exist in `documents/05-guides/operations/runbooks/`; GAP-144 PARTIAL (AlertManager production receivers stub — no live Slack/PagerDuty receivers). |
| 5 | Deployment Pipeline | 14 | 13 | -1 | Smoke test gained `check_beta_signup_flow` (58 assertions total) ✅; Terraform S3 backend remote state ✅; staging.tf (Wave 38) ✅; rollback documented in 6 runbooks. GAP-425/426 DONE. Health probes (liveness+readiness) in Helm ✅. **Regression finding**: no `startupProbe` → liveness kill risk during Flyway migration startup. No blue/green in Phase 1 Architecture B. |

**Total: 60/100 (D)** — +7 vs Wave 35 baseline. Meaningful improvements in observability (Grafana dashboards + PIIScrubber + structured logging infra), backup (scripts shipped), and honeypot wire-up. Still below production-ready (80) due to: no log aggregation (GAP-115), AlertManager receivers not live (GAP-144), no distributed tracing, `kite_backup_last_success_timestamp_seconds` dead metric, Phase 1 single-EC2 architecture (no blue/green).

---

## Detailed Findings per Category

### Category 1: Monitoring & Observability (15/20)

**What improved since Wave 35:**
- 3 Grafana dashboards provisioned via Helm sidecar discovery (`templates/grafana-dashboards/dashboard-http-traffic.yaml`, `dashboard-infra-pools.yaml`, `dashboard-jvm-heap-gc.yaml`)
- Actuator endpoints exposed: `health,info,metrics,prometheus` in kitehub-admin and kitehub-gateway
- Docker Prometheus scrape config active (prometheus.yml scrapes `/actuator/prometheus` for all services)
- GAP-135 PARTIAL: `@Timed` instrumentation on 4/5 kitehub services; SLO Prometheus alert rules (5 alerts) in prometheusrule.yaml
- 4 custom beta-funnel counters wired with callsites (Wave 35 + GAP-387 honeypot wire-up fixed)

**Remaining gaps:**
- `monitoring.enabled: false` by default in Helm values (kube-prometheus-stack opt-in) — operators must explicitly set flag
- No distributed tracing (no Zipkin/Jaeger/OpenTelemetry)
- Frontend error tracking: Sentry stub ("Sentry-ready hook") in error.tsx/global-error.tsx but NOT wired to real Sentry endpoint
- `MultiTenantDataLeak` alert (`tenant_isolation_violations_total`) still METRIC PENDING — no producer

### Category 2: Logging Standards (10/20)

**What improved since Wave 35:**
- `PIIScrubber` bean exists in `kitehub-platform` shared library (GAP-116 scope)
- Per-service `logback-spring.xml` with LogstashEncoder + MDC keys configured: kitehub-admin, kitehub-subscription, kitehub-email (confirmed)
- `Redact` utility class available for structured kv logging

**Remaining gaps:**
- GAP-114 PARTIAL: live 3-service traceId propagation verification not done (no running cluster)
- GAP-115 OPEN: no log aggregation (no Loki/ELK/CloudWatch). Container stdout logs lost on restart
- MDC injection filters not verified end-to-end in gateway → downstream service chain
- No DEBUG/TRACE level retention policy enforced (24h per `logs-format-standard.md`)

### Category 3: Backup & Recovery (9/20)

**What shipped since Wave 35:**
- `scripts/backup-production.sh` — full AWS RDS snapshot script with `--dry-run`, error handling, idempotency
- `scripts/verify-restore.sh` — 5-check restore verification (schema, row counts, FK integrity, sample read, Flyway history); `--self-test` mode 7/7 PASS
- `.github/workflows/restore-drill.yml` — monthly cron scheduled drill (gated by `vars.BACKUP_DRILL_ENABLED`)
- `documents/05-guides/operations/disaster-recovery-plan.md` — comprehensive DR plan with RTO/RPO matrix
- `documents/05-guides/operations/dr-rto-rpo-matrix.md` — RTO/RPO targets defined per component

**Remaining gaps:**
- GAP-117 PARTIAL (Phase 3): quarterly DR exercise with measured RTO baseline not done (tracked GAP-257)
- **Dead alert**: `BackupJobFailure` alert watches `kite_backup_last_success_timestamp_seconds`; backup script emits `kite_backup_snapshots_total` (different metric name and type) → alert NEVER fires
- GAP-118 OPEN: MinIO backup strategy not implemented
- No automated cron triggering `backup-production.sh` (script exists but no scheduler — manual or CI `workflow_dispatch` only)

### Category 4: Alerting (13/20)

**What improved since Wave 35:**
- Honeypot counter wire-up FIXED: `BetaAccessController.java:208` calls `service.recordHoneypotRejection(email, resolveClientIp(request))` — counter will increment on bot detection; `BetaHoneypotSpike` alert is now live
- 25 runbooks in `documents/05-guides/operations/runbooks/` covering all major alert scenarios
- `check-alert-runbook-url.py` script verifies alert↔runbook parity
- `documents/05-guides/account-prep/` — 4 new runbooks (Wave 39 GAP-394): AWS account creation, domain registrar, password manager, superadmin first login
- SES setup + DNS setup + secrets management + Cloudflare + incident comms runbooks in `05-guides/operations/`

**Remaining gaps:**
- GAP-144 PARTIAL: AlertManager production receivers not live (ESO + ExternalSecret chart scaffolded; `monitoring.alertmanager.receivers.production.enabled` not activated)
- 29 alert rules fire into /dev/null on production until GAP-144 closes
- `MultiTenantDataLeak` alert (`tenant_isolation_violations_total`) dead — no counter producer
- `BackupJobFailure` dead (see Category 3)

### Category 5: Deployment Pipeline (13/20)

**What improved since Wave 35:**
- GAP-426 DONE: `setup.sh` ENCRYPTION_MASTER_KEY base64 corruption fix
- GAP-425 DONE: cold rebuild includes BE images via `build-all.sh`
- Smoke test: `check_beta_signup_flow()` added (POST `/api/v1/auth/request-beta-access` E2E assertion); 58 total assertions
- `scripts/seed-staging-fixtures.sh` available for staging pre-seeding
- `staging.tf` (Wave 38) — Architecture B EC2+docker-compose staging environment defined
- Remote Terraform state: S3 backend with versioning + DynamoDB locking (`backend.tf`)
- Helm liveness + readiness probes in templates for all services
- Rollback documented in 6 runbooks + `deployment-procedures.md`

**Remaining gaps (including regression finding):**
- **No `startupProbe`** in Helm templates (regression finding this audit) — liveness probe kills Flyway migration before app is ready
- Phase 1 Architecture B = single EC2 docker-compose — no blue/green or canary; full container restart on deploy
- Smoke test does not validate honeypot counter increment post-deploy
- No feature flags system (full redeploy required for all toggles)

---

## Key Improvements vs Wave 35 Baseline

| Item | Wave 35 | Wave 40 | Evidence |
|------|---------|---------|---------|
| Grafana dashboards | 0 | 3 | `templates/grafana-dashboards/` 3 files |
| Honeypot counter wire-up | Dead (no callsite) | Fixed | `BetaAccessController.java:208` |
| Backup script | Missing | Shipped | `scripts/backup-production.sh` |
| Restore verification | Missing | Shipped | `scripts/verify-restore.sh` (7/7 self-test PASS) |
| DR plan + RTO/RPO | Missing | Shipped | `disaster-recovery-plan.md` + `dr-rto-rpo-matrix.md` |
| Smoke test beta flow | 0 coverage | `check_beta_signup_flow()` | `scripts/smoke-test.sh:270` |
| setup.sh ENCRYPTION_MASTER_KEY | Corrupted | Fixed | GAP-426 DONE |
| Account-prep runbooks | 0 | 4 | `documents/05-guides/account-prep/` |
| PIIScrubber | Planned | Shipped | `kitehub-platform/../logging/PIIScrubber.java` |
| Alert count | ~15 total | 29 total | `prometheusrule.yaml` grep: 29 `alert:` entries |

---

## Dead Alerts Inventory (Production Risk)

| Alert | Dead Since | Reason | Fix |
|-------|-----------|--------|-----|
| `BackupJobFailure` | Wave 33 | Watches `kite_backup_last_success_timestamp_seconds`; backup script emits `kite_backup_snapshots_total` (different name + type) | Add timestamp gauge push to `backup-production.sh` via Pushgateway OR rewrite alert |
| `MultiTenantDataLeak` | Wave 25 | Watches `tenant_isolation_violations_total`; no producer in BaseEntity/filter layer | Add counter in multi-tenant Hibernate filter |

---

## DR Drill Status

| Drill component | Status | Evidence |
|-----------------|--------|---------|
| Restore procedure documented | ✅ | `documents/05-guides/deploy/restore-procedure.md` |
| verify-restore.sh script | ✅ | `scripts/verify-restore.sh` + `--self-test` 7/7 PASS |
| Monthly CI drill workflow | ✅ | `.github/workflows/restore-drill.yml` (gated `vars.BACKUP_DRILL_ENABLED`) |
| Quarterly drill executed | ❌ | GAP-257 (deferred — needs S3 backups accumulated + staging env coordination) |
| Measured RTO baseline | ❌ | GAP-257 (same) |
| MinIO backup strategy | ❌ | GAP-118 OPEN |

---

## Monitoring Coverage

| Service | Actuator | Prometheus | Grafana | Alert rules |
|---------|:--------:|:----------:|:-------:|:-----------:|
| kitehub-gateway | ✅ | ✅ | ✅ (http-traffic) | ✅ |
| kitehub-admin | ✅ | ✅ | ✅ (infra-pools) | ✅ |
| kitehub-subscription | ✅ | ✅ | ✅ (jvm-heap-gc) | ✅ |
| kitehub-branding | ✅ | ✅ | Partial | ✅ |
| kitehub-email | ✅ | ✅ | Partial | ✅ |
| kitehub-platform | ✅ | ✅ | — | — |
| kiteclass-core | ✅ | ✅ | — | — |
| Frontend (KH+KC) | Sentry stub | — | — | — |

---

## Smoke Test Coverage

| Flow | Covered | Notes |
|------|:-------:|-------|
| KiteHub health check | ✅ | `/actuator/health` |
| KiteClass health check | ✅ | `/actuator/health` |
| Beta signup E2E | ✅ | POST `/api/v1/auth/request-beta-access` + row persistence assertion |
| Auth + login flow | ✅ | Multiple `check_page` assertions |
| Tenant provisioning | Partial | API-level only |
| AI Branding wizard | ❌ | GAP-272 cluster incomplete |
| Payment flows | ❌ | Not yet covered |
| **Total assertions** | **58** | `grep -c "check_\|assert" scripts/smoke-test.sh` |

---

## Release Deploy Standard Checklist (§3.1 PRE-RELEASE gate)

Per `release-deploy-standard.md` §3.1 pre-release checklist:

| Item | Status | Evidence |
|------|--------|---------|
| Deploy plan document linked | ✅ | `release-1-plan-2026.md` + `deployment-procedures.md` |
| Smoke test script | ✅ | `scripts/smoke-test.sh` (58 assertions, beta E2E flow) |
| Rollback procedure | ✅ | `rollback-runbook.md` + 6 per-service rollback steps |
| Status page | ⏳ | GAP-373 (P1) — Instatus setup walkthrough done (Wave 39); not configured in prod |
| Secrets management | ✅ | AWS Secrets Manager via Terraform + GAP-426 DONE (key corruption fixed) |
| HTTPS / TLS | ✅ | Cloudflare proxy + ACM cert via Terraform |
| Pre-release disclaimer | ⏳ | GAP-372 (beta invite mechanism) |
| Auth flow tested E2E | ✅ | Smoke test + Wave 35 admin auth |
| Database backup pre-deploy | ✅ | `scripts/backup-production.sh` |
| Health check endpoint | ✅ | `/actuator/health` all services |
| Logs aggregated (min 24h) | ❌ | GAP-115 OPEN — no aggregation pipeline |
| Restore drill documented | Partial | GAP-117 PARTIAL (Phase 3 quarterly drill deferred GAP-257) |

**PRE-RELEASE gate: 9/12 ✅, 3 gaps remain (status page, logs aggregation, beta invite)**

---

## New Gaps Filed by This Audit

### 🔴 P0 — BackupJobFailure alert metric name mismatch (NEW)

`prometheusrule.yaml:261` alert watches `kite_backup_last_success_timestamp_seconds` but `scripts/backup-production.sh:143` emits `kite_backup_snapshots_total` — different metric name AND type (gauge vs counter). Alert will NEVER fire regardless of backup failures.

**Fix:** Add `kite_backup_last_success_timestamp_seconds` gauge push at end of successful `backup-production.sh` run (via Pushgateway `PUSHGATEWAY_URL`). OR rewrite alert to use `increase(kite_backup_snapshots_total[25h]) == 0`.

### 🟠 P1 — No startupProbe in Helm deployment templates (NEW)

Liveness + readiness probes configured in `infrastructure/helm/kitehub/templates/deployment.yaml` but no `startupProbe`. Services with Flyway migration startup delays (~15-45s depending on migration count) risk false liveness kills on initial pod start, causing restart loops on first cold deploy.

**Fix:** Add `startupProbe` with `httpGet: /actuator/health`, `initialDelaySeconds: 30, failureThreshold: 30, periodSeconds: 5` to all Helm service deployment templates.

### 🟠 P1 CARRY — Log aggregation absent (GAP-115)

No Loki/ELK/CloudWatch. Container stdout lost on restart. Present since Wave 25.

### 🟠 P1 CARRY — AlertManager production receivers not activated (GAP-144)

ESO + ExternalSecret scaffolded; live receivers (Slack/PagerDuty/email) not wired. 29 alert rules fire into /dev/null.

### 🟡 P2 — Sentry not wired in frontend (NEW)

`error.tsx:32` and `global-error.tsx:31` have "Sentry-ready" stubs (`console.error` only). Frontend errors invisible in production APM.

**Fix:** Install `@sentry/nextjs`; replace stub with `Sentry.captureException(error)`.

### 🟡 P2 CARRY — MultiTenantDataLeak alert dead (METRIC PENDING)

`tenant_isolation_violations_total` counter has no producer. Present since Wave 25.

---

## Twelve-Factor Compliance Delta

| Factor | Wave 35 | Wave 40 | Note |
|--------|:-------:|:-------:|------|
| III. Config in env | 🟡 | 🟢 | GAP-426 fix — ENCRYPTION_MASTER_KEY now clean base64 |
| IV. Backing services | 🟢 | 🟢 | RDS/Redis/SES via env |
| IX. Disposability | 🟡 | 🟡 | Readiness probe OK; no startupProbe; Architecture B single-EC2 ~30s restart |
| XI. Logs as event streams | 🟡 | 🟡 | Logstash encoder configured; aggregation pipeline not deployed |
| XII. Admin processes | 🔴 | 🟢 | `setup.sh` now boots clean (GAP-426 fix) |

---

## Tổng kết

Wave 40 milestone audit (Bucket E): **60/100 D (+7 vs Wave 35 baseline 53/100)**

Cải thiện chính từ Wave 36-39:
1. **+3 Backup** — backup-production.sh + verify-restore.sh + DR plan + RTO/RPO matrix shipped
2. **+2 Monitoring** — 3 Grafana dashboards provisioned; PIIScrubber shipped
3. **+2 Logging** — structured logging infra (logback-spring.xml per-service) + MDC keys configured
4. **+1 Alerting** — honeypot counter wire-up fixed; 25 runbooks; 4 account-prep docs
5. **-1 Deploy** — `startupProbe` missing discovered as regression finding

**Phase 1 BETA gate status:** Ops Readiness 60/100 — BELOW 80-point threshold. Not a hard blocker for Phase 1 BETA launch (quality-audit /100 ≥80 and 5 beta tenants live are the primary trigger gates per `release-1-plan-2026.md`), but 3 high-priority gaps should close before first tenant onboards:
- P0: BackupJobFailure dead alert (metric name mismatch) — file as GAP-428
- P1: startupProbe missing — file as GAP-429
- P1: AlertManager receivers not activated (GAP-144 carry)

Path to 70/100: wire AlertManager receivers + fix BackupJobFailure metric + add startupProbe ≈ +8 pts.
Path to 80/100: also ship GAP-115 log aggregation ≈ +6 pts + distributed tracing ≈ +4 pts.
