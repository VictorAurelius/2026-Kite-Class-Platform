---
title: Ops-Readiness Full Audit — post wave-p0-closeout-1
status: complete
created: 2026-06-14
audit_type: ops-readiness
phase: phase-1-beta
wave: p0-closeout-1
auditor: ops-readiness full audit agent (Opus 4.8 1M)
baseline_score: 77
baseline_date: 2026-05-18
baseline_wave: 92 (Wave 94c GAP-619 suite)
score: 78
delta: +1
rubric: .claude/rules/audit-skill-rubric-ops-readiness-audit.md (v1.0.1)
audit_level_verdict: PARTIAL FAIL (2 P0 sub-checks FAIL — restore drill + rollback config-drift — chưa đạt gate 80)
constraint: STATIC review only — no AWS calls (agent-aws-access.md §2.1); stack STOPPED on-demand (cost-save)
---

# Ops-Readiness Full Audit — post wave-p0-closeout-1 (2026-06-14)

**Score: 78/100 — C+** (delta **+1** vs Wave 92 baseline 77/100).

**Verdict: ⚠️ PARTIAL FAIL** per rubric §4 bug-finding-primacy — 2 P0 sub-checks FAIL (§3.4 restore drill chưa chạy thật + §5.3 rollback workflow config-drift làm rollback path hỏng). Score < gate Phase 1 BETA 80.

## Ràng buộc môi trường (đọc kỹ — khác với mô tả task)

Task brief ghi "AWS account SUSPENDED (GAP-612)". **State thực tế 2026-06-14:**
- **GAP-612 (AWS suspension) đã DONE** — account restored Wave aws-restore-1 (2026-05-26), live smoke `api.kitehub.me/actuator/health` HTTP 200 verified tại thời điểm restore.
- **GAP-144 (Alertmanager production receivers) đã DONE** — Wave 86 Bucket H SNS-direct adaptation.
- Hiện stack **STOPPED on-demand** (CLAUDE.md AWS start/stop + `stack-on-demand-runbook.md`, ~$3-5/mo storage) để save Free Tier — KHÔNG phải suspended.

→ **2 trong 3 P0 carry của baseline (GAP-144 + GAP-612) thực ra đã đóng.** Baseline carry-forward list (output-review-mandate §3 row + audit refs gần đây) đang STALE — đây là 1 finding (GAP-1371).

→ Audit này chạy **STATIC** (code/IaC/config/workflow/runbook) per `agent-aws-access.md` §2.1 — KHÔNG gọi AWS. Mọi item cần live-fire (alarm actually fires, SNS email delivers, rotation Lambda đã rotate, RDS snapshot ≤24h, post-deploy smoke trên prod) → đánh dấu **❓ UNCHECKED** trung thực (stack stopped + no AWS call), KHÔNG mặc định PASS.

---

## 1. Bug list (per rubric §4 primacy — FAIL trước, score sau)

### 🔴 P0 FAIL (audit-level verdict = FAIL)

| ID | Sub-check | Evidence | Gap |
|---|---|---|---|
| OPS-001 | §3.4 Restore drill ≤90 ngày | `restore-drill.yml` workflow + `verify-restore.sh --self-test` PASS; **real drill body gated OFF** (`BACKUP_DRILL_ENABLED` unset → skip) + S3 backup chưa confirmed → drill thật CHƯA chạy bao giờ | GAP-257 / GAP-117 (carry) |
| OPS-002 | §5.3 Rollback procedure tested ≤90 ngày | **`rollback.yml` config-drift**: `ALB_DNS: kitehub-alb-224105328...` + smoke gate `curl http://${ALB_DNS}/actuator/health` — nhưng ALB đã **eliminated** Wave aws-restore-1; `deploy-production.yml` đã migrate sang Cloudflare Tunnel + dynamic EC2 lookup, `rollback.yml` thì KHÔNG → rollback smoke gate sẽ FAIL; thêm hardcoded `ROLLBACK_INSTANCE_ID_KH: i-0b65c3947d36cae61` (deploy đã bỏ hardcode per GAP-482) | **GAP-1368 NEW** |

### 🟠 P1 FAIL

| ID | Sub-check | Evidence | Gap |
|---|---|---|---|
| OPS-003 | §2.4 Log aggregation pipeline (Loki ≥7d hot) | Helm `loki.enabled=false` default; container stdout → CloudWatch Logs (awslogs driver, partial mitigant) nhưng Loki/Promtail chưa deploy | GAP-115 / GAP-434 (carry, re-triaged phase-2) |
| OPS-004 | §4.x CloudWatch P0 alarm metric-source un-wired | 8 P0 alarms (`cloudwatch-p0-alarms.tf`) wired vào SNS, nhưng **Nginx5xxCount** (`KiteHub/Nginx`) + **RabbitMQ QueueDepth** (`KiteHub/RabbitMQ`) chưa có metric filter/exporter → alarm stay `INSUFFICIENT_DATA` = silent (chính .tf comment cũng flag) | **GAP-1369 NEW** |
| OPS-005 | §4.5 Alert-delivery drill ≤30 ngày | `alertmanager-mock-fire-runbook.md` tồn tại; KHÔNG có drill log ≤30 ngày + chưa có cadence/workflow exercise đường alarm→SNS→email | **GAP-1370 NEW** (carry OPS-W92-006) |
| OPS-006 | §2.3 PII scrubber active | `AuthService.java:48` log `email=` plaintext khi login fail (PII); GAP-116 scrubber platform-deferred Wave 7 → site mới chưa được mask | GAP-116 (carry) + **GAP-1372 NEW** (quick-mask) |

### 🟡 P2 FAIL / WARN

| ID | Sub-check | Evidence | Gap |
|---|---|---|---|
| OPS-007 | §3.5 MinIO/object-storage backup | Phase 1 chưa có cross-region replication / snapshot cron cho MinIO | GAP-118 (carry) |
| OPS-008 | §1.5/4.1 Prometheus rule METRIC PENDING | `prometheusrule.yaml` ~30 rules nhưng nhiều rule depend exporter chưa deploy (blackbox cert / redis / rabbitmq / custom counters) + `alertmanager-config` `production.enabled=false` default = **alerts SILENTLY DROP** nếu ai enable Helm monitoring mà không flip flag. Phase 1 live path = CloudWatch (không phải Prometheus) → EKS-future, nhưng latent risk | GAP-115 / GAP-044 (note) |
| OPS-009 | §5.1 Deploy strategy không blue-green/rolling | Phase 1 Architecture B = single EC2 docker-compose **full-restart** (stop-and-redeploy) — accepted Phase 1 per ADR-025 | N/A-accepted |
| OPS-010 | §1371 Audit baseline staleness | GAP-144 + GAP-612 đã DONE nhưng vẫn bị cite là "3 P0 carry" trong output-review-mandate §3 + audit refs → audit accuracy drift | **GAP-1371 NEW** (P3) |

### ❓ UNCHECKED (stack stopped + no AWS call this session — KHÔNG default PASS)

| ID | Sub-check | Lý do |
|---|---|---|
| U-01 | §1.1 health endpoints live 200 | Stack stopped; code-level config preserved → presumed-OK nhưng chưa verify live |
| U-02 | §3.1 RDS daily backup snapshot ≤24h | RDS automated backup `backup_retention_period=7` configured (rds.tf) nhưng live snapshot timestamp chưa query |
| U-03 | §3.6 Secrets Manager auto-versioning + rotation đã chạy | `secrets-rotation.tf` 90-day Lambda cho jwt/encryption/seed-admin + RDS-managed db-password (IaC ✅); VersionStages + rotation-executed chưa verify (GAP-869 rotation exec defer) |
| U-04 | §4.2 SNS → email thực sự delivers | `production-alerts.tf` 2 email subscriptions (support@ + backup) (IaC ✅); live email delivery chưa fire/confirm |
| U-05 | §5.5 post-deploy smoke trên prod | `deploy-production.yml` inline smoke `api.kitehub.me/actuator/health` + `smoke-tests.yml` 6 scripts (workflow ✅); live deploy chưa run session này |
| U-06 | §4.x CloudWatch alarm OK/ALARM state | 8 P0 + RDS-storage alarm IaC shipped; live state (vs INSUFFICIENT_DATA) chưa query |

---

## 2. Score table (5 categories × 20pts, per rubric §3)

| # | Category | Wave 92 | 2026-06-14 | Δ | Rationale |
|---|---|:---:|:---:|:---:|---|
| 1 | Monitoring & Observability | 17 | 17 | 0 | 5 Helm Grafana dashboards + CloudWatch dashboard + servicemonitor; OTel collector Phase 2 (1.5 PARTIAL); custom metrics nhiều METRIC PENDING (1.6 PARTIAL) |
| 2 | Logging Standards | 13 | 13 | 0 | JSON LogstashEncoder + MDC fields ✅; 2.3 PII (GAP-116 + site mới AuthService:48) + 2.4 Loki (GAP-115/434) = 2 P0 cap ≤16; CloudWatch Logs partial mitigant cho 2.4 |
| 3 | Backup & Recovery | 14 | 15 | **+1** | `restore-drill.yml` monthly workflow + `verify-restore.sh` self-test CI gate + `restore-procedure.md`/`backup-runbook.md` matured; **3.4 real drill vẫn FAIL P0** cap ≤16 |
| 4 | Alerting | 15 | 17 | **+2** | **GAP-144 SNS-direct closed → 4.2 routing FAIL→PASS** (no P0 cap); 30+ Prometheus rules + 8 CloudWatch P0 alarms + 30 runbooks + severity tiers ✅; 4.5 drill cadence + metric-source wiring P1 |
| 5 | Deployment Pipeline | 18 | 16 | **−2** | **5.3 rollback.yml config-drift = NEW P0 FAIL** cap ≤16 (stale ALB DNS + hardcoded EC2 ID; deploy-production.yml đã migrate, rollback.yml chưa); 5.2 health-gate + 5.5 smoke + 5.6 tag-trigger ✅; 5.1 single-EC2 full-restart accepted |
| | **Total** | **77** | **78** | **+1** | GAP-144 + GAP-612 closure dominant positive; offset bởi rollback config-drift P0 mới |

**Net +1:** +2 (GAP-144 alerting closure) + ~+1 (GAP-612 operational availability restored — gỡ -2 carry depressor của Wave 92) + +1 (restore tooling matured) − 2 (rollback config-drift P0 mới) − ~1 (latent metric-source/PII gaps surfaced). Honest net +1.

---

## 3. Per-check verdicts (5 × 6 sub-checks)

### 3.1 Cat 1 — Monitoring & Observability (17/20)

| # | Sub-check | Sev | Verdict | Evidence |
|---|---|---|---|---|
| 1.1 | Service `/actuator/health` 200 | P0 | ❓ UNCHECKED→code PASS | Helm probes + actuator config preserved; stack stopped |
| 1.2 | Endpoints expose health/info/prometheus/metrics | P0 | ✅ PASS | application.yml exposure preserved (Wave 84/91 baseline) |
| 1.3 | Prometheus scrape `/actuator/prometheus` | P0 | ✅ PASS (code) | `servicemonitor.yaml` (EKS) + `docker/prometheus.yml` (Phase 1) |
| 1.4 | Grafana dashboards ≥4 (JVM/HTTP/DB/MQ) | P1 | ✅ PASS | 5 Helm dashboards (jvm-heap-gc, http-traffic, api-latency, infra-pools, logs-overview) + `cloudwatch-dashboard.tf` |
| 1.5 | Distributed tracing (OTel) | P1 | 🟡 PARTIAL | OTel exporter config preserved; collector backend Phase 2 |
| 1.6 | Custom business metrics | P2 | 🟡 PARTIAL | outbox metrics + beta-signup counters live; nhiều custom counter METRIC PENDING (tenant_isolation_violations, ai_provider_requests, branding_quality_gate) |

### 3.2 Cat 2 — Logging Standards (13/20, 2 P0 cap ≤16)

| # | Sub-check | Sev | Verdict | Evidence |
|---|---|---|---|---|
| 2.1 | JSON-structured logs | P0 | ✅ PASS | logback-spring.xml LogstashEncoder |
| 2.2 | Required fields (timestamp/level/service/tenantId/traceId) | P0 | ✅ PASS | MDC keys configured |
| 2.3 | PII scrubber active | P0 | ❌ FAIL | `AuthService.java:48` log email plaintext; GAP-116 scrubber Wave 7 deferred → site mới chưa mask (GAP-1372) |
| 2.4 | Log aggregation ≥7d hot | P0 | ❌ FAIL | `loki.enabled=false`; CloudWatch Logs (awslogs) partial mitigant; GAP-115/434 |
| 2.5 | Banned System.out.println | P1 | ✅ PASS | ArchUnit enforces |
| 2.6 | Retention tiers hot/warm/cold | P1 | 🟡 PARTIAL | `audit-log-retention-runbook.md` + `logging-standard.md` doc; enforcement gated trên 2.4 |

### 3.3 Cat 3 — Backup & Recovery (15/20, 3.4 P0 cap ≤16)

| # | Sub-check | Sev | Verdict | Evidence |
|---|---|---|---|---|
| 3.1 | PostgreSQL daily backup | P0 | ❓ UNCHECKED→code PASS | `rds.tf` `backup_retention_period=7` + backup_window 00:00 ICT; snapshot timestamp chưa query live |
| 3.2 | RTO/RPO documented | P0 | ✅ PASS | `dr-rto-rpo-matrix.md` |
| 3.3 | DR plan / restore procedure | P0 | ✅ PASS | `disaster-recovery-plan.md` + `restore-procedure.md` + `backup-runbook.md` + `rollback-procedure.md` + 6 service runbooks |
| 3.4 | Restore drill ≤90 ngày | P0 | ❌ FAIL | `restore-drill.yml` monthly + `verify-restore.sh --self-test` PASS; **real drill gated off** (`BACKUP_DRILL_ENABLED` unset); GAP-257/117 |
| 3.5 | MinIO/object-storage backup | P1 | ❌ FAIL | GAP-118 |
| 3.6 | Secrets auto-versioning | P1 | ❓ UNCHECKED→code PASS | `secrets-rotation.tf` 90-day rotation IaC; live VersionStages chưa query (GAP-869) |

### 3.4 Cat 4 — Alerting (17/20)

| # | Sub-check | Sev | Verdict | Evidence |
|---|---|---|---|---|
| 4.1 | Alert rules service-down/error/latency | P0 | ✅ PASS | `prometheusrule.yaml` ServiceDown/HighErrorRate/HighResponseTime + 8 CloudWatch P0 alarms |
| 4.2 | Routing → on-call channel | P0 | ✅ PASS (code) | **GAP-144 closed** — `production-alerts.tf` SNS topic + 2 email sub (support@ + backup); live delivery ❓ U-04 |
| 4.3 | Per-alert runbook | P1 | ✅ PASS | 30 runbooks; `check-alert-runbook-url.py` parity |
| 4.4 | Silencing/grouping documented | P1 | ✅ PASS | runbooks/README |
| 4.5 | Alert-delivery drill ≤30 ngày | P1 | ❌ FAIL | runbook tồn tại; không có drill log ≤30 ngày + chưa có cadence (GAP-1370) |
| 4.6 | Severity classification | P2 | ✅ PASS | runbook README P0-paged/P1-ticket/P2-dashboard |
| — | Metric-source wiring | P1 | ❌ FAIL | Nginx5xxCount + RabbitMQ QueueDepth chưa wired → INSUFFICIENT_DATA (GAP-1369) |

### 3.5 Cat 5 — Deployment Pipeline (16/20, 5.3 P0 cap ≤16)

| # | Sub-check | Sev | Verdict | Evidence |
|---|---|---|---|---|
| 5.1 | Blue-green / rolling | P0 | 🟡 N/A-accepted | Phase 1 Architecture B single-EC2 docker-compose full-restart (ADR-025) |
| 5.2 | Health checks gate rollout | P0 | ✅ PASS | `deploy-production.yml` smoke-200 gate (60s+ elapsed) + Helm startup/liveness/readiness probes (GAP-431) |
| 5.3 | Rollback tested ≤90 ngày | P0 | ❌ FAIL | `rollback.yml` config-drift: stale ALB DNS + hardcoded EC2 ID → smoke gate broken (GAP-1368); real drill chưa run (GAP-257) |
| 5.4 | Deploy duration baseline | P1 | 🟡 PARTIAL | rollback.yml emit TTR → CloudWatch `KiteHub/Rollback`; deploy duration chưa log formally |
| 5.5 | Post-deploy smoke automated | P0 | ✅ PASS | deploy-production inline smoke + `smoke-tests.yml` 6 scripts (login/email-verify/totp/k6/migration/rls) |
| 5.6 | Tag-based deploy trigger | P1 | ✅ PASS | `docker-build-push.yml` trigger `tags: v*.*.*` + push main + workflow_dispatch (GAP-374 80%) |

---

## 4. Điểm tích cực (verified static)

- **GAP-612 AWS restoration** (Wave aws-restore-1) — gỡ bỏ depressor lớn nhất của Wave 92 (production availability).
- **GAP-144 SNS-direct alerting** — 8 P0 CloudWatch alarms (`cloudwatch-p0-alarms.tf`) + RDS-storage alarm + SNS topic + 2 email sub.
- **deploy-production.yml** matured: dynamic EC2 lookup (GAP-482), RDS preflight (GAP-493), CloudWatch SSM log streaming (GAP-491), Cloudflare Tunnel smoke, confirm=DEPLOY gate.
- **Secrets rotation IaC** (`secrets-rotation.tf`): 90-day custom Lambda (jwt/encryption/seed-admin) + RDS-managed db-password + least-priv IAM.
- **restore-drill.yml** monthly + `verify-restore.sh` self-test CI gate; full DR doc set (5 deploy guides + 6 service runbooks).
- **30+ Prometheus rules + 30 runbooks** với `check-alert-runbook-url.py` parity; OutboxDLQNonEmpty critical alert (GAP-742).
- **docker-build-push** tag-based trigger + Trivy SARIF + Syft SBOM + Cosign signing.

---

## 5. Carry-forward & path to gate 80

| Gap | Status | Sub-check |
|---|---|---|
| GAP-257 / GAP-117 | OPEN/PARTIAL — restore drill chưa chạy thật | §3.4 P0 |
| GAP-115 / GAP-434 | PARTIAL phase-2 — Loki/Promtail | §2.4 P0 |
| GAP-116 | deferred Wave 7 — PII scrubber | §2.3 P0 |
| GAP-118 | OPEN — MinIO backup | §3.5 P1 |
| GAP-379 | PARTIAL 98% — db-password rotation one-time SAR bind (manual) | §3.6 |
| GAP-869 | OPEN — rotation execution (Resend/secret) | §3.6 |
| GAP-374 | PARTIAL 80% — tag-based release notification E2E | §5.6 |
| GAP-613 | OPEN — CloudWatch Free Tier reduce | cost |
| GAP-044 / GAP-616 | phase-2/1.5 — synthetic + uptime external | §4.5-adjacent |
| ~~GAP-144~~ / ~~GAP-612~~ | **DONE** — baseline carry list stale (GAP-1371) | — |

**Path to 80 (static-closable trước):** GAP-1368 rollback config-drift fix (+2, 5.3 P0→PASS) → **80 ✅**. Then GAP-1369 metric wiring + GAP-1370 drill cadence + GAP-1372 PII mask = polish. Live-gated (need AWS): GAP-257 restore drill thật + GAP-115 Loki.

---

## 6. New gaps filed (per audit-to-gap-pipeline §3 — 1 gap = 1 finding)

| Gap | Sev | Finding |
|---|---|---|
| GAP-1368 | P1 | rollback.yml stale ALB DNS + hardcoded EC2 ID config-drift (rollback path broken post-ALB-elimination) |
| GAP-1369 | P1 | CloudWatch P0 alarms Nginx5xxCount + RabbitMQ QueueDepth metric-source un-wired → INSUFFICIENT_DATA silent |
| GAP-1370 | P1 | Monthly alert-delivery drill cadence chưa lập (alarm→SNS→email path never exercised) |
| GAP-1371 | P3 | Ops-readiness baseline carry-forward stale (GAP-144 + GAP-612 closed nhưng vẫn cite P0 carry) |
| GAP-1372 | P2 | AuthService login-fail log plaintext email PII — quick-mask trước GAP-116 scrubber |

**Dup-avoided (reference, không re-file):** GAP-257/117 (restore drill), GAP-144 (alerting — DONE), GAP-612 (AWS — DONE), GAP-115/434 (Loki), GAP-116 (PII scrubber umbrella), GAP-118 (MinIO), GAP-379 (rotation 98%), GAP-869 (rotation exec), GAP-374 (tag release), GAP-613 (Free Tier), GAP-044/616 (synthetic/uptime), GAP-477 (rollback workflow existence).

---

## 7. References

- Baseline: `documents/04-quality/audits/ops-readiness/2026-05-18-wave-92-ops-readiness-audit.md` (77/100 C+)
- Rubric: `.claude/rules/audit-skill-rubric-ops-readiness-audit.md` v1.0.1
- Skill: `.claude/skills/quality/ops-readiness-audit/SKILL.md`
- IaC: `infrastructure/terraform-aws/{cloudwatch-p0-alarms,production-alerts,secrets-rotation,rds}.tf`
- Workflows: `.github/workflows/{deploy-production,rollback,restore-drill,smoke-tests,docker-build-push}.yml`
- Helm: `infrastructure/helm/kitehub/templates/{prometheusrule,alertmanager-config,deployment}.yaml`
- Runbooks: `documents/05-guides/deploy/{restore-procedure,backup-runbook,rollback-procedure}.md` + `documents/05-guides/operations/{disaster-recovery-plan,dr-rto-rpo-matrix}.md`
- Constraint: `agent-aws-access.md` §2.1 (no AWS calls); stack STOPPED on-demand (`stack-on-demand-runbook.md`)
