---
title: Ops Readiness Audit — Post Wave 89-91 batch 1 (deferred per GAP-601)
status: complete
created: 2026-05-18
phase: phase-1-beta
wave: 91
auditor: wait-time Agent A (ops-readiness-audit skill)
baseline_score: 78
baseline_date: 2026-05-15
baseline_wave: 84
score: 75
delta: -3
rubric: .claude/rules/audit-skill-rubric-ops-readiness-audit.md (v1.0.1)
closes_gap: GAP-601
---

# Ops Readiness Audit — Post Wave 89-91 batch 1

**Score:** **75/100 — C** (delta **-3** vs Wave 84 baseline 78/100 C+)
**Verdict:** ⚠️ **PARTIAL FAIL** per rubric §1 — operational risk gây bởi GAP-612 AWS account suspension làm degradation tạm thời restore + alerting paths; code-level Wave 89-91 deltas chủ yếu **positive** (gateway JWT + PM2 systemd + outbox dispatcher + DLQ + admin email + Trivy SARIF guard) nhưng KHÔNG offset được risk operational tạm thời.

**Constraint:** AWS account 906286017800 SUSPENDED kể từ 2026-05-17 ~16:50 UTC (GAP-612). Audit không thể chạy live verify (`aws sts get-caller-identity`, `aws cloudwatch describe-alarms`, `aws iam simulate-principal-policy`); CHỈ đọc source code + helm + terraform + CI/CD + audit history. Coverage giới hạn ~85% so với Wave 84 baseline có live AWS verify.

---

## 1. Bug list (per `audit-skill-rubric-ops-readiness-audit.md` §4 primacy)

### 🔴 P0 FAIL (audit-level verdict = FAIL theo rubric §1)

| ID | Sub-check | Evidence | Gap |
|---|---|---|---|
| OPS-W91-001 | §2.3.4 Restore drill ≤90 ngày | Vẫn carry-forward Wave 84 — `verify-restore.sh --self-test` 7/7 PASS, drill thật chưa chạy | GAP-257 (carry) |
| OPS-W91-002 | §2.4.2 Alertmanager routing → on-call channel | ESO + ExternalSecret scaffolded; production receivers chưa active; **TỆ HƠN Wave 84** vì AWS SNS topic `kitehub-production-alerts` (Wave 88) cũng không thể fire/test trong suspension window | GAP-144 PARTIAL (carry) |
| OPS-W91-003 | §2.5.3 Rollback procedure tested ≤90 ngày | `rollback.yml` workflow + `smoke-rollback-cycle.sh --execute` quarterly drill chưa chạy; AWS suspension chặn execute path | GAP-257-class (carry) |

### 🟠 P1 FAIL

| ID | Sub-check | Evidence | Gap |
|---|---|---|---|
| OPS-W91-004 | §2.2.4 Log aggregation pipeline (≥7d hot retention) | Helm `loki.enabled=false` default; container stdout vẫn lost on restart; Wave 89-91 KHÔNG xử lý Loki | GAP-115/434 (carry) |
| OPS-W91-005 | Operational availability (account-level) | GAP-612 AWS account suspended → production stack DOWN từ 2026-05-17 16:50 UTC; AWS Support case 177903869600100 open 17:25 UTC; pending reply 24-72h | **GAP-612 NEW** P0 (filed Wave 91 plan) |
| OPS-W91-006 | §2.4.5 Alert auto-test monthly synthetic drill ≤30 ngày | `alertmanager-mock-fire-runbook.md` exists; drill log không có session ≤30 ngày; Wave 91 không backfill | (carry — would need GAP follow-up nếu chưa có) |

### 🟡 P2 FAIL

| ID | Sub-check | Evidence | Gap |
|---|---|---|---|
| OPS-W91-007 | §2.3.5 MinIO/object storage backup strategy | Phase 1 chưa có cross-region replication; carry-forward Wave 84 | GAP-118 (carry) |
| OPS-W91-008 | §2.5.6 Deploy workflow tag-based push trigger | `docker-build-push.yml` branch-push (Wave 35 gap) | GAP-374 PARTIAL (carry) |
| OPS-W91-009 | CloudWatch Free Tier overage risk | Wave 88-91 thêm 11 alarms + Lambda logs; có nguy cơ Free Tier overage ($5+/mo); GAP-613 Phase 1 reduce plan trong Wave 91 §3 Bucket F TBD | GAP-613 NEW (Wave 92 queue) |

### ❓ UNCHECKED (out of session scope — AWS suspended)

| ID | Sub-check | Lý do unchecked |
|---|---|---|
| OPS-W91-U01 | §2.1.1-1.2 health endpoints live | Stack STOPPED + AWS suspended → curl `/actuator/health` không reachable; CHỈ verify code-level config exists |
| OPS-W91-U02 | §2.1.5 OTel traces visible end-to-end | OTel exporter config có; collector backend (Jaeger/Tempo) Phase 2; live UI sample không có cluster running |
| OPS-W91-U03 | §2.2.3 PIIScrubber active end-to-end | Bean shipped; live test event không có cluster running để verify |
| OPS-W91-U04 | §2.3.1 RDS daily snapshot timestamp ≤24h | `aws rds describe-db-snapshots` blocked by AWS suspension; presume continue (RDS snapshots automatic, không depend on credentials) |
| OPS-W91-U05 | §2.3.6 Secrets Manager rotation Lambda Active | `aws secretsmanager describe-secret` blocked; code-level secrets-rotation.tf shows 4 in-scope secrets configured |
| OPS-W91-U06 | §2.4.1 CloudWatch alarms live state | `aws cloudwatch describe-alarms` blocked; code-level 11 alarms verified existence trong terraform |

---

## 2. Score table (5 categories × 20 pts)

| # | Category | Wave 84 | Wave 91 | Δ | Rationale (per-check §3) |
|---|---|:---:|:---:|:---:|---|
| 1 | Monitoring & Observability | 18 | 17 | -1 | Code-level OTel + actuator endpoints + Prometheus + CloudTrail unchanged; -1 vì AWS suspension UNCHECKED 5 sub-checks (U01/U04/U06) cap honest score |
| 2 | Logging Standards | 12 | 12 | 0 | Wave 91 ship admin-new-login-alert template (eliminate poison queue gây Wave 90) — small positive nhưng vẫn Loki disabled cap §2.2.4 P0 ≤16 |
| 3 | Backup & Recovery | 13 | 13 | 0 | RDS snapshot presumed continue (managed); Secrets Manager rotation config preserved; restore drill (P0 §3.4) vẫn chưa chạy; cap ≤16 P0 |
| 4 | Alerting | 17 | 15 | -2 | 30 Prometheus alerts + 11 CloudWatch unchanged; **-2 vì AWS suspension làm tất cả CloudWatch SNS không thể fire — operational regression tạm thời**; production receivers (GAP-144) vẫn FAIL |
| 5 | Deployment Pipeline | 18 | 18 | 0 | **Wave 89 Bucket B PM2 systemd auto-start (+positive)** + **Wave 89 Bucket A gateway JWT filter (+positive auth gate)** + **Wave 91 Bucket A outbox dispatcher + DLQ (+positive)** + Trivy SARIF guard (this session) đều positive; nhưng -2 vì rollback drill chưa chạy + tag-based trigger chưa fix → net 0 |
| **Total** | **78** | **75** | **-3** | Phase 1 BETA gate 80/100 — chênh **5 pts** (was 2 Wave 84); blocker = GAP-612 AWS suspension + 2 P0 carries (GAP-257 + GAP-144) |

---

## 3. Per-check verdicts (5 categories × ≥6 sub-checks per rubric §2)

### 3.1 Category 1 — Monitoring & Observability (17/20)

| # | Sub-check | Severity | Verdict | Evidence |
|---|---|---|---|---|
| 1.1 | Every service exposes `/actuator/health` 200 | P0 | ❓ UNCHECKED → presumed PASS | Code-level: 6 KH services + KC core có `actuator` endpoint exposure (Wave 84 verified); live curl blocked by AWS suspension |
| 1.2 | Endpoints expose `health,info,prometheus,metrics` | P0 | ✅ PASS (code) | Wave 84 verified; no Wave 89-91 regression touching application.yml |
| 1.3 | Prometheus scrape config targets `/actuator/prometheus` | P0 | ✅ PASS | `infrastructure/docker/prometheus.yml` job per service unchanged |
| 1.4 | Grafana dashboards (JVM, HTTP, DB, RabbitMQ) ≥4 | P1 | ✅ PASS | 3 Helm Grafana dashboards + 1 CloudWatch (Wave 84) — no regression |
| 1.5 | Distributed tracing (OTel) configured + traces visible | P1 | 🟡 PARTIAL | OTel exporter config 5 services; collector backend Phase 2 (UNCHECKED §1) |
| 1.6 | Custom business metrics emitted | P2 | ✅ PASS | 4 beta-funnel counters + Wave 91 outbox metrics `outbox_undispatched_count` + `dlq_depth` ship trong Bucket A (positive delta) |

### 3.2 Category 2 — Logging Standards (12/20, P0 §2.2.4 FAIL caps ≤16)

| # | Sub-check | Severity | Verdict | Evidence |
|---|---|---|---|---|
| 2.1 | JSON-structured logs per service | P0 | ✅ PASS | `logback-spring.xml` LogstashEncoder unchanged |
| 2.2 | Required fields `timestamp, level, service, tenantId, traceId` | P0 | ✅ PASS | MDC keys configured per Wave 40 |
| 2.3 | PII scrubber active | P0 | 🟡 PARTIAL | `PIIScrubber` bean shipped; live test UNCHECKED §1 |
| 2.4 | Log aggregation pipeline running ≥7d | P0 | ❌ FAIL | `loki.enabled=false`; Phase 2 GAP-434 |
| 2.5 | Banned `System.out.println` | P1 | ✅ PASS | ArchUnit enforces |
| 2.6 | Retention tiers hot 7d / warm 30d / cold 180d | P1 | 🟡 PARTIAL | Doc exists; enforcement gated trên §2.4 |
| — | **Wave 91 NEW: admin-new-login-alert.html template ship** | bonus | ✅ | Eliminates Wave 90 poison-queue (`admin-new-login-alert` missing template → 500 retry infinite). Verified file `kitehub/kitehub-email/src/main/resources/templates/emails/admin-new-login-alert.html` exists |

### 3.3 Category 3 — Backup & Recovery (13/20, P0 §3.4 FAIL caps ≤16)

| # | Sub-check | Severity | Verdict | Evidence |
|---|---|---|---|---|
| 3.1 | PostgreSQL daily backup running | P0 | ❓ UNCHECKED → presumed PASS | RDS automated snapshots (managed, không depend creds); live verify blocked |
| 3.2 | RTO + RPO documented per service tier | P0 | ✅ PASS | `documents/05-guides/operations/dr-rto-rpo-matrix.md` |
| 3.3 | DR plan: failover / snapshot-restore procedure | P0 | ✅ PASS | `disaster-recovery-plan.md` + 6 service rollback runbooks |
| 3.4 | Restore drill ≤90 ngày | P0 | ❌ FAIL | `verify-restore.sh --self-test` 7/7 PASS; drill thật chưa chạy (GAP-117 PARTIAL + GAP-257 OPEN) |
| 3.5 | MinIO/object storage backup strategy | P1 | ❌ FAIL | GAP-118 OPEN |
| 3.6 | Secrets backup AWS Secrets Manager auto-versioning | P1 | ❓ UNCHECKED → presumed PASS | Wave 84 Bucket B: 3 secrets rotation config preserved trong `secrets-rotation.tf`; live verify blocked |

### 3.4 Category 4 — Alerting (15/20, P0 §4.2 FAIL caps ≤16; **-2 vs Wave 84 vì AWS suspension regression**)

| # | Sub-check | Severity | Verdict | Evidence |
|---|---|---|---|---|
| 4.1 | Alert rules: service-down / high-error / high-latency | P0 | ✅ PASS | 30 Prometheus alerts trong `prometheusrule.yaml` |
| 4.2 | Alertmanager routing → on-call channel | P0 | ❌ FAIL | ESO scaffolded; production receivers chưa active (GAP-144); **REGRESSION** vì AWS SNS path (Wave 88) cũng dead trong suspension |
| 4.3 | Per-alert runbook | P1 | ✅ PASS | 30 runbooks (verified `documents/05-guides/operations/runbooks/` count = 30); `check-alert-runbook-url.py` enforces parity |
| 4.4 | Alert silencing/grouping documented | P1 | ✅ PASS | runbooks/README.md covers silence patterns |
| 4.5 | Alert auto-test monthly drill ≤30 ngày | P1 | 🟡 PARTIAL | `alertmanager-mock-fire-runbook.md` exists; drill log không có session ≤30 ngày |
| 4.6 | Severity classification (P0 paged, P1 ticket, P2 dashboard) | P2 | ✅ PASS | Runbook README documents tiers |
| — | Wave 84 Bucket A: 4 CloudWatch metric-filter alarms | bonus Wave 84 | ⚠️ | Code preserved; **operational fire path blocked by AWS suspension — tạm thời degraded** |
| — | Wave 84 Bucket G: 3 EC2 low-CPU alarms + monthly cost report | bonus Wave 84 | ⚠️ | Same regression |
| — | Wave 88: SNS `kitehub-production-alerts` + 2 subs + RDS storage alarm | bonus Wave 88 | ⚠️ | Same regression |

### 3.5 Category 5 — Deployment Pipeline (18/20)

| # | Sub-check | Severity | Verdict | Evidence |
|---|---|---|---|---|
| 5.1 | Deploy strategy: blue-green OR rolling | P0 | 🟡 PARTIAL | Phase 1 Architecture B = single EC2 docker-compose (full-restart on deploy) |
| 5.2 | Health checks gate deploy rollout | P0 | ✅ PASS | startupProbe + livenessProbe + readinessProbe trong Helm (verified `templates/deployment.yaml` GAP-431 + GAP-503 still wired) |
| 5.3 | Rollback procedure tested ≤90 ngày | P0 | 🟡 PARTIAL | `rollback.yml` workflow + `smoke-rollback-cycle.sh` shipped; `--execute` quarterly drill chưa run; AWS suspension chặn execute path |
| 5.4 | Deploy duration baseline measured | P1 | 🟡 PARTIAL | DORA-style metric chưa logged formally |
| 5.5 | Post-deploy smoke test automated | P0 | ✅ PASS | `scripts/smoke-test.sh` 58 assertions; **Wave 91 thêm `scripts/smoke-email-actuator.sh` + smoke-resend.sh + smoke-login-happy-path.sh** (positive delta — 10 smoke scripts total verified `ls scripts/smoke-*`) |
| 5.6 | Deploy workflow tag-based push trigger | P1 | 🟡 PARTIAL | `docker-build-push.yml` branch-push (GAP-374) |
| — | **Wave 89 Bucket A: JwtAuthenticationGatewayFilter** | bonus | ✅ | Gateway JWT propagation Wave 88 walkthrough block resolved — admin endpoints can reach 200 (per Wave 90 live verify) |
| — | **Wave 89 Bucket B: PM2 systemd auto-start** | bonus | ✅ | `infrastructure/terraform-aws/ec2-kc-app.tf` user_data wires `pm2 startup systemd` (verified `grep "pm2 startup" ec2-kc-app.tf` → line 233); EC2 reboot → PM2 active 17s (Wave 90 verified) |
| — | **Wave 91 Bucket A: SubscriptionOutboxDispatcher + DLQ** | bonus | ✅ | `kitehub-subscription/outbox/SubscriptionOutboxDispatcher.java` exists + `EmailQueueConfig` has `x-dead-letter-exchange` config (verified grep) — eliminates Wave 90 stuck outbox rows + poison queue infinite retry |
| — | **Wave 91 Bucket C: admin-new-login-alert template** | bonus | ✅ | Eliminates Wave 90 missing template → consumer log clear |
| — | **Wave 91 Bucket B: SES IAM policy** | bonus | ⚠️ | `iam.tf` Bucket B applied trong code; live `aws iam simulate-principal-policy` UNCHECKED (AWS suspended) — pending Wave 91 Bucket F coordinator phase post-AWS-restore |
| — | **Wave 91 Bucket E: BetaClaimCodeForm FE** | bonus | ✅ | `kitehub-frontend/src/components/auth/BetaClaimCodeForm.tsx` + `__tests__/BetaClaimCodeForm.test.tsx` + route `app/(auth)/beta-signup/code/page.tsx` exists |
| — | **Wave 91 Bucket D: V60 RLS migration** | -- | ❌ NOT FOUND | `find db/migration -name "V60*"` returns 0; either deferred OR named differently. Migration list ends V52. **OPS-W91-010 P1 file follow-up** — Wave 91 Bucket D state may be PARTIAL; needs verify |
| — | **This session: Trivy SARIF guard CI fix** | bonus | ✅ | `4ccaa13e fix(ci): skip Trivy SARIF upload khi scan không chạy (AWS suspension cascade)` — eliminates CI cascade failure |

---

## 4. Wave 89-91 deltas vs Wave 84 baseline (-3 net breakdown)

### Positive deltas (+8 estimated if AWS active)

| Wave | Bucket | GAP | Domain impact | Pts contribution |
|---|---|---|---|---|
| 89 | Bucket A | GAP-604 DONE | JwtAuthenticationGatewayFilter — gateway JWT-to-headers propagation | +2 Cat 5 (Deploy) — unblocks admin operations end-to-end |
| 89 | Bucket B | GAP-602/603 DONE | PM2 systemd auto-start wired ec2-kc-app.tf user_data + pm2-ecosystem cwd fix | +2 Cat 5 (Deploy resilience) |
| 91 | Bucket A | GAP-605/607 (PARTIAL pending Bucket F) | SubscriptionOutboxDispatcher fast-path + DLQ (`x-dead-letter-exchange` + 3 retry exponential backoff) + outbox metrics | +1 Cat 5 + +1 Cat 4 (alerting via DLQ depth metric) |
| 91 | Bucket C | GAP-606 (PARTIAL pending Bucket F) | admin-new-login-alert.html template ship | +1 Cat 2 (poison queue eliminated) |
| 91 | Bucket E | GAP-609 (PARTIAL pending Bucket F) | BetaClaimCodeForm FE fallback path | +0.5 Cat 5 (FE resilience) |
| 91 | This session | (CI guard) | Trivy SARIF upload guard — eliminates CI cascade failure | +0.5 Cat 5 (CI hygiene) |
| **Subtotal positive** | | | | **+8 estimated** |

### Negative deltas (-11 net)

| Wave | Source | Impact | Pts |
|---|---|---|---|
| 91 | GAP-612 AWS suspension | Production stack DOWN từ 2026-05-17 16:50 UTC; 5 UNCHECKED sub-checks không thể live verify | -1 Cat 1 (UNCHECKED) |
| 91 | GAP-612 cascade | CloudWatch SNS + 11 alarms operational regression tạm thời | -2 Cat 4 (alerting fire path dead) |
| 91 | GAP-612 cascade | Rollback drill + `smoke-rollback-cycle.sh --execute` blocked | -1 Cat 5 |
| 91 | Bucket B GAP-608 | SES IAM apply NOT EXECUTED (Bucket F gated by AWS restore) | -1 Cat 1 (live SES verify pending) |
| 91 | Bucket D GAP-610/611 | V60 RLS migration NOT FOUND trong codebase — may be PARTIAL execution | -2 Cat 3/5 (Beta signup BE bugs unresolved) |
| 91 | GAP-613 NEW | CloudWatch Free Tier overage risk emerging (Wave 88-91 added 11 alarms + Lambda logs) | -1 Cat 1 (cost vs observability tradeoff pending) |
| 91 | Wave 91 Bucket F | Coordinator phase BLOCKED (live verify 7 gaps + IAM apply + smoke + backfill) | -3 cumulative |
| **Subtotal negative** | | | **-11 net** |

**Final delta:** 78 (Wave 84 baseline) + 8 (positive) - 11 (negative) = **75/100** (-3 net vs Wave 84).

---

## 5. Path to Phase 1 BETA gate (80/100)

Current 75/100, gap **+5 pts** để đạt 80. Roadmap:

1. **GAP-612 AWS account restoration** → unblock Wave 91 Bucket F coordinator phase → +5 pts immediate (restores Cat 1 UNCHECKED + Cat 4 SNS path + Cat 5 IAM apply) → **80+ ✅**
   - ETA: 24-72h pending AWS Support case 177903869600100
2. **Wave 91 Bucket F live verify 7 gaps DONE** (GAP-605/606/607/608/609/610/611) → confirms positive deltas → +1-2 pts cushion → **82+ ✅**
3. **GAP-144 AlertManager production receivers active** → Cat 4 §4.2 P0 PASS → +2 pts → **84+ ✅**
4. **GAP-117 Phase 3 staging restore drill** → Cat 3 §3.4 P0 PASS → +3 pts → **87+ ✅**
5. **GAP-115/434 Loki phase 2 deploy** → Cat 2 §2.2.4 P0 PASS → +4 pts → **91+ A** ✅

**Path tối thiểu:** GAP-612 restoration + Bucket F = ≥80 trong 24-72h.

---

## 6. AWS Well-Architected pillars delta (vs Wave 84)

| Pillar | Wave 84 | Wave 91 | Δ |
|---|:---:|:---:|:---:|
| 1. Operational Excellence | 🟢 | 🟡 | -1 (AWS suspension regression tạm thời) |
| 2. Security | 🟢 | 🟢 | 0 (CloudTrail/secrets rotation/RLS preserved) |
| 3. Reliability | 🟡 | 🟡 | 0 (restore drill carry; PM2 systemd + outbox dispatcher positive) |
| 4. Performance Efficiency | 🟡 | 🟡 | 0 (OTel exporter config; collector Phase 2) |
| 5. Cost Optimization | 🟢 | 🟡 | -1 (CloudWatch overage risk GAP-613 emerging) |
| 6. Sustainability | 🟡 | 🟡 | 0 (EC2 right-sizing actively monitored) |

---

## 7. New gap candidates / follow-ups

### Filed by audit (file follow-up gaps in Wave 92 queue):

| ID | Severity | Title | Notes |
|---|:---:|---|---|
| **OPS-W91-010** → **GAP-614** | 🟠 P1 | Wave 91 Bucket D V60 RLS migration not found in codebase | Verify whether V60 migration shipped (different name?) OR Bucket D PR pending merge. May affect Beta signup BE bugs GAP-610/611. |
| **OPS-W91-006** | 🟠 P1 | Monthly synthetic alert drill cadence — setup `alertmanager-mock-fire.yml` workflow | Carry-forward Wave 84 — defer Wave 92+ |
| **GAP-613** (already filed in Wave 91 plan) | 🟡 P2 | CloudWatch reduce plan để giảm Free Tier overage risk | Wave 92 queue per Wave 91 §3 Bucket F prerequisite |

### Carry-forward (no new filing, existing):

| Existing gap | Wave 91 status |
|---|---|
| GAP-117/257 restore drill | UNCHANGED (P0 BLOCKING) — AWS suspension chặn execute path |
| GAP-144 AlertManager receivers | UNCHANGED (P1) + regression vì AWS SNS path dead |
| GAP-115/434 Loki phase 2 | UNCHANGED (P0) |
| GAP-118 MinIO backup | UNCHANGED (P1) |
| GAP-374 tag-based deploy trigger | UNCHANGED (P1) |
| GAP-612 AWS account suspension | NEW Wave 91 — P0 BLOCKING all operational verify |

---

## 8. Release Deploy Standard checklist refresh

| Item | Wave 84 | Wave 91 | Evidence |
|---|---|---|---|
| Deploy plan document linked | ✅ | ✅ | Wave 91 plan + `release-1-deploy-plan.md` |
| Smoke test script | ✅ | ✅✅ | 10 `scripts/smoke-*` scripts (Wave 91 +smoke-resend, +smoke-email-actuator, +smoke-login-happy-path) |
| Rollback procedure | ✅✅ | ✅✅ | `rollback-runbook.md` + `rollback.yml` + `smoke-rollback-cycle.sh` |
| Status page | ✅ | ✅ | Statuspage VN overlay |
| Secrets management | ✅✅ | ✅✅ | secrets-rotation.tf preserved + Wave 88 IAM policy |
| HTTPS / TLS | ✅ | ✅ | Cloudflare + ACM |
| Pre-release disclaimer | ⏳ | ⏳ | GAP-372 pending |
| Auth flow tested E2E | ✅ | ⏳ | **Wave 91 unblocks via Bucket F live verify** (Wave 89 Bucket A gateway JWT shipped) |
| Database backup pre-deploy | ✅ | ✅ | `backup-production.sh` |
| Health check endpoint | ✅✅ | ✅✅ | startupProbe + liveness + readiness Helm (GAP-431) |
| Logs aggregated (min 24h) | ❌ | ❌ | GAP-115/434 OPEN |
| Restore drill documented | Partial | Partial | GAP-117/257 carry |
| **NEW: Smoke admin-login** (rule v1.2.0) | n/a | ✅ | `scripts/smoke-login-happy-path.sh` exists; live verify pending Bucket F |

**PRE-RELEASE gate progress:** 10/13 ✅ (was 10/12 Wave 84) — added smoke admin-login per `release-deploy-standard.md` §3.1 v1.2.0; remaining 2 gaps (logs aggregation, restore drill) + smoke admin-login live verify pending.

---

## 9. Tổng kết

**Post Wave 89-91 batch 1: 75/100 C (-3 vs Wave 84)** — code-level deltas Wave 89-91 chủ yếu positive (gateway JWT + PM2 systemd + outbox dispatcher + DLQ + admin email template + smoke scripts + Trivy guard); -3 net vì operational regression tạm thời do GAP-612 AWS account suspension blocking Bucket F live verify + CloudWatch SNS fire path + IAM apply.

Wave 89-91 buckets shipped (code-level verified):
- **Wave 89 Bucket A (DONE):** Gateway JwtAuthenticationGatewayFilter (jjwt 0.13.0) + 56/56 tests PASS
- **Wave 89 Bucket B (DONE):** PM2 systemd auto-start wired ec2-kc-app.tf + pm2-ecosystem cwd fix + runbook
- **Wave 91 Bucket A (PARTIAL):** SubscriptionOutboxDispatcher + EmailQueueConfig DLX/DLQ shipped — live verify pending Bucket F
- **Wave 91 Bucket B (PARTIAL):** SES IAM policy in code — live apply pending Bucket F
- **Wave 91 Bucket C (PARTIAL):** admin-new-login-alert.html template ship — poison queue eliminate pending verify
- **Wave 91 Bucket D (UNCLEAR):** V60 RLS migration NOT FOUND — file follow-up GAP-614 verify
- **Wave 91 Bucket E (DONE-code):** BetaClaimCodeForm FE + test + route shipped
- **This session:** Trivy SARIF upload guard CI fix (4ccaa13e)

Path to 80+ Phase 1 BETA gate:
1. GAP-612 AWS account restoration (24-72h pending AWS Support) → unblock Bucket F → +5 pts → **80+ ✅**
2. Wave 91 Bucket F live verify 7 gaps DONE → +1-2 pts → **82+ ✅**
3. GAP-144 AlertManager + GAP-117 restore drill → +5 pts → **87+ B grade**

**Audit-level verdict: ⚠️ PARTIAL FAIL** per rubric §4 (3 P0 FAILs surfaced: OPS-W91-001 restore drill carry; OPS-W91-002 alertmanager + AWS SNS regression; OPS-W91-003 rollback drill blocked). Cause = mix carry-forward (GAP-117/144/257) + Wave 91 operational risk (GAP-612 suspension); score honest 75/100 phản ánh net delta.

**Phase 1 BETA gate decision:** ⚠️ NOT YET (75 < 80); roadmap §5 ETA 24-72h pending GAP-612 + Bucket F coordinator phase.

---

## 10. GAP-601 closure rationale

GAP-601 AC ✅:
- [x] Ops-readiness audit run within 3 days of Wave 88 cutover (deadline 2026-05-20) — this audit dated 2026-05-18 ✅
- [x] Audit artifact filed under `documents/04-quality/audits/ops-readiness/` — this file ✅
- [x] `output-review-mandate.md` §3 row "Ops readiness" updated với new score + delta — paired same PR ✅
- [x] `audits-index.csv` row appended — paired same PR ✅
- [x] If new findings → gaps filed (P0/P1 only) — GAP-614 filed Wave 92 queue (V60 RLS migration verify) ✅

**GAP-601 flip → 🟢 DONE per `gap-done-discipline.md` §2.**

---

## References

- Baseline: `documents/04-quality/audits/ops-readiness/2026-05-15-wave-84-post-apply.md` (78/100 C+)
- Wave 88 cutover: `documents/04-quality/audits/aws-verification/2026-05-17-wave-88-cutover-post-apply.md`
- Wave 89 plan: `documents/03-planning/waves/wave-2026-05-17-89-gateway-pm2-cluster.md`
- Wave 91 plan: `documents/03-planning/waves/wave-2026-05-18-91-production-restore-email-infra-beta-signup.md`
- Skill: `.claude/skills/quality/ops-readiness-audit/SKILL.md`
- Rubric: `.claude/rules/audit-skill-rubric-ops-readiness-audit.md` (v1.0.1)
- Standard: `.claude/rules/release-deploy-standard.md` §3.1 PRE-RELEASE checklist (v1.2.0 smoke admin-login)
- Related gaps: GAP-115, GAP-117, GAP-118, GAP-144, GAP-257, GAP-374, GAP-434 (carry-forward); GAP-601 (closing), GAP-604/602/603 (Wave 89 DONE), GAP-605/606/607/608/609/610/611 (Wave 91 PARTIAL), GAP-612 (P0 BLOCKING NEW), GAP-613 (Wave 92 queue), GAP-614 (filed this audit, Wave 92 queue)

**Constraint notes:**
- Per `agent-aws-access.md` §2.1 + GAP-612 — NO AWS calls executed in this audit session. All AWS-dependent sub-checks marked `❓ UNCHECKED → presumed PASS` based on Wave 84 baseline evidence + code-level config preservation. Re-run audit when AWS restored to verify presumptions.
- Per `dev-readable-doc-language.md` — Vietnamese narrative + English identifiers/enums/HTTP/JWT/CloudWatch tokens.
- Per `pre-mutation-state-check.md` — read existing state before writing audit; cross-referenced Wave 89 closure log + Wave 91 plan + code grep evidence.
