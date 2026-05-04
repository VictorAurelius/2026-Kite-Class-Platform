---
title: Ops Readiness Audit — Baseline 2026-04-19
audit_type: ops-readiness
score: 49/100
grade: F
previous_score: null
previous_date: null
status: complete
created: 2026-04-19
auditor: Claude (Audit 2 of governance catch-up plan)
---

# Ops Readiness Audit — Baseline 2026-04-19

> **Score: 49/100 (F — Not ready for production)**
> **First-ever baseline audit** cho category này (per `output-review-mandate.md` §4 VIOLATION list). Score dự kiến 40-60 range → rơi vào lower half do platform-wide observability + DR còn phôi thai.

Part A — Audit 2 of 5 theo `documents/03-planning/plans/plan-audit-catchup-2026-04-19.md` §3.2.

---

## 1. Executive Summary

Platform có infrastructure nền tảng tốt (Terraform AWS + Oracle, Helm charts, health probes K8s, Flyway migrations, deploy-go-nogo checklist, rollback procedure, incident response runbook) nhưng thiếu **nền tảng observability sản xuất** và **chiến lược backup/DR platform-wide**:

- **Monitoring & Grafana:** chỉ configured trong `docker-compose.kitehub.yml` với profile `monitoring` — KHÔNG triển khai vào Helm/k8s production (không có Prometheus/Grafana deployment manifests).
- **Logging:** mặc định Spring Boot text-based, không JSON structured, không có `traceId/tenantId/userId` fields cần thiết cho multi-tenant SaaS.
- **Distributed tracing:** ZERO (no Zipkin/Jaeger/OpenTelemetry SDK).
- **Alerting receivers:** rules có (7 alerts) nhưng không có Alertmanager config, không có Slack/PagerDuty/email routing.
- **Backup:** pg_dump + S3 upload đã implement (GAP-093 DONE) nhưng **không có restore test**, không có MinIO backup, không có platform-wide DR runbook.
- **Frontend error tracking:** không có Sentry/Rollbar/tương tự.

Các điểm mạnh: deploy-go-nogo-checklist.md, rollback-procedure.md, incident-response-runbook.md (PR #350, #352) đã exist và chi tiết; Terraform state remote encrypted; RDS backup_retention_period=7; Helm rolling update + auto-rollback trên CI failure.

---

## 2. Scope

| Artifact | Files inspected |
|---------|-----------------|
| Infrastructure IaC | `infrastructure/helm/**` (2 charts), `infrastructure/k8s/**` (2 templates), `infrastructure/terraform-aws/*.tf` (10 files), `infrastructure/terraform-oracle/*.tf` (5 files) |
| Docker Compose | `kitehub/docker-compose.kitehub.yml` (canonical), `kiteclass/docker-compose.dev.yml`, 4 variants khác |
| Monitoring | `kitehub/docker/prometheus/prometheus.yml`, `alert-rules.yml`, `kitehub/docker/grafana/dashboards/kitehub-overview.json` |
| Operations guides | `documents/05-guides/` (10 files) + `documents/05-guides/operations/runbooks/deployment-procedures.md` |
| CI/CD | `.github/workflows/deploy-staging.yml`, `deploy-production.yml`, 6 CI workflows khác |
| Application config | 7 `application.yml` files trong backend services |
| Scripts | `kitehub/scripts/*.sh` (18 files), `scripts/*.sh` (14 files) |

---

## 3. Category Scores

| # | Category | Score | /20 |
|---|----------|:-----:|:---:|
| 1 | Monitoring & Observability | **11** | 20 |
| 2 | Logging Standards | **4** | 20 |
| 3 | Backup & Recovery | **10** | 20 |
| 4 | Alerting | **10** | 20 |
| 5 | Deployment Pipeline | **14** | 20 |
| **Total** | | **49** | **100** |

---

## 4. Category 1 — Monitoring & Observability (11/20)

### Findings

✅ **Actuator enabled** trên tất cả 6 Spring Boot services (`health,info,metrics,prometheus` exposed).
✅ **Prometheus scrape config** tồn tại cho 6 jobs (subscription, branding, email, admin, gateway, kiteclass-core).
✅ **1 Grafana dashboard** provisioned (`kitehub-overview.json`).
✅ **Helm + K8s health probes** (livenessProbe + readinessProbe) configured cho tất cả deployments.
✅ **Circuit breaker resilience** (Resilience4j) configured cho 8 gateway routes.

❌ **Monitoring stack KHÔNG deploy production.** `docker-compose.kitehub.yml` gắn Prometheus + Grafana vào profile `monitoring` (chạy có chọn lọc). Không có Helm chart/k8s manifest cho Prometheus/Grafana. → Production sẽ không có monitoring khi deploy.
❌ **No distributed tracing** (Zipkin/Jaeger/OpenTelemetry). Không thể trace request xuyên qua 6 microservices.
❌ **Frontend error tracking ZERO** — không Sentry/Rollbar, lỗi FE chỉ stuck trong browser console.
❌ **Port mismatch** trong `prometheus.yml`: `kiteclass-core:8081` nhưng compose + helm expose port 8080.
❌ **No custom business metrics** (`@Timed` hoặc custom `MeterRegistry`). Chỉ có JVM + HTTP defaults.
❌ **No RED metrics per-tenant** — không thể monitor SLA breach per tenant.

### Gap seeds
- GAP-111 (monitoring stack không deploy prod)
- GAP-112 (distributed tracing missing)
- GAP-113 (FE error tracking missing)

---

## 5. Category 2 — Logging Standards (4/20)

### Findings

✅ Spring Boot default logging hoạt động (console output).
✅ Một vài service có `logging.level` config theo package.

❌ **No logback.xml / log4j2.xml** trong any backend service → toàn dùng Spring Boot default TEXT format.
❌ **No JSON structured logging** → logs không parsable programmatically.
❌ **No mandatory fields** cho multi-tenant SaaS: thiếu `tenantId`, `traceId`, `userId`, `correlationId` — không thể isolate tenant issues hoặc audit trail.
❌ **No MDC (Mapped Diagnostic Context) propagation** qua RabbitMQ hoặc HTTP boundaries.
❌ **No PII scrubbing** — emails, phones, student data có thể leak vào logs. FERPA/PDPA compliance risk.
❌ **No log aggregation** — không có Loki/ELK/CloudWatch config. Logs stuck trong container stdout.
❌ **No retention policy** documented.

**Đây là category yếu nhất. Logs là OUTPUT được `output-review-mandate.md` Section 4 flag là VIOLATION riêng.**

### Gap seeds
- GAP-114 (structured JSON logging standard)
- GAP-115 (log aggregation pipeline)
- GAP-116 (PII scrubbing trong logs)

---

## 6. Category 3 — Backup & Recovery (10/20)

### Findings

✅ **pg_dump + S3 upload implemented** (GAP-093 DONE — `DatabaseBackupScheduler` thực sự chạy pg_dump + upload qua `BackupStorageService`).
✅ **RDS automated backup** (`backup_retention_period = 7` trong `terraform-aws/rds.tf`).
✅ **Terraform state remote** (S3 bucket with encryption + DynamoDB lock cho AWS; OCI Object Storage cho Oracle).
✅ **Backup config** exists trong `application.yml` (`backup.retention-count: 7`, `pg-dump-path`).

⚠️ **GAP-030 exists** (P2 OPEN) cho DR nhưng **AI-branding scope only** — không cover platform-wide DR.
⚠️ RDS `multi_az` tunable via variable (default undetermined, phải check tfvars thực tế).

❌ **Restore NEVER TESTED** — không có restore verification script, không có documented restore drill.
❌ **MinIO backup strategy MISSING** — AI assets + template SVGs sẽ mất nếu MinIO volume corrupt. Không có S3 versioning / cross-region replication config.
❌ **RTO/RPO not defined platform-wide.** GAP-030 chỉ cover AI branding; các domain khác (core tenant data, subscription, billing) chưa có targets.
❌ **No DR runbook** trong `documents/05-guides/`. Không có file `*backup*` hoặc `*disaster-recovery*` (chỉ có `rollback-procedure.md` cover application rollback, không cover infrastructure DR).
❌ **Redis snapshots không configured** (session data có thể mất).
❌ **RabbitMQ durability** chưa verify — compose chạy `rabbitmq:3-management-alpine` default, không check queue durable/persistent mode.

### Gap seeds
- GAP-117 (restore drill test + runbook)
- GAP-118 (MinIO backup + replication strategy)
- GAP-119 (platform-wide RTO/RPO + DR runbook)

---

## 7. Category 4 — Alerting (10/20)

### Findings

✅ **7 alert rules** defined trong `alert-rules.yml`:
- ServiceDown, HighErrorRate (>5% 5xx), HighResponseTime (p99 > 2s), HighMemoryUsage (JVM heap >85%), DatabasePoolExhausted (HikariCP >80%), HighDiskUsage (>85%), RabbitMQQueueBacklog (>1000).

✅ **Incident response runbook** rất chi tiết (`documents/05-guides/operations/incident-response-runbook.md`) với severity levels, escalation path, per-service incidents table.

✅ **Deploy-go-nogo-checklist.md** mentions "Alerting rules configured for new features".

❌ **NO Alertmanager config** — alert rules dangling, không có receiver. Không Slack/PagerDuty/email webhook.
❌ **Alert rules dev-only** — chỉ load qua `docker-compose.kitehub.yml` profile `monitoring`; production không có Prometheus deploy.
❌ **No per-alert runbooks** — chỉ có incident-response-runbook overview, không có file riêng cho từng alert (vd. `runbooks/high-error-rate.md`, `runbooks/db-pool-exhausted.md`).
❌ **On-call rotation** mentioned trong runbook nhưng no PagerDuty/OpsGenie integration configured.
❌ **Missing critical platform alerts:**
- Certificate expiry (< 14 days)
- AI provider failure rate (Ollama/OpenAI)
- Multi-tenant data leak detection
- Subscription/billing webhook failures
- Email queue DLQ growing
- Tenant provisioning failure

❌ **No SLO/SLI definitions** — alerts dựa trên arbitrary thresholds, không có burn rate alerting.

### Gap seeds
- GAP-120 (Alertmanager + receiver setup)
- GAP-121 (per-alert runbooks library)
- GAP-122 (missing platform-critical alerts)

---

## 8. Category 5 — Deployment Pipeline (14/20)

### Findings

✅ **Helm charts** exist cho KiteHub + KiteClass instances với values.yaml.
✅ **Kubernetes probes** configured: livenessProbe + readinessProbe với reasonable delays (initialDelaySeconds 30-90s).
✅ **Actuator health groups** sử dụng: kiteclass-core split liveness (`/actuator/health/liveness`) + readiness (`/actuator/health/readiness`) — best practice.
✅ **Rollback automation**: `deploy-production.yml` có `helm rollback kitehub 0` on failure; `rollback-procedure.md` chi tiết per-service.
✅ **CI/CD workflows**: 8 workflows cover core-ci, frontend-ci, gateway-ci, deploy-staging, deploy-production.
✅ **HPA configured** cho kiteclass-core template (CPU 70%, memory 80%, tier-based max replicas).
✅ **Flyway on boot** cho database migration.
✅ **Deploy-go-nogo-checklist.md** rất chi tiết (pre-deploy, go/no-go matrix, rollback trigger conditions).
✅ **Terraform IaC** cả AWS (EKS + RDS + ElastiCache + S3 + ECR) và Oracle (VCN + ARM VMs + Object Storage).
✅ **Production deploy requires manual confirmation** (type `DEPLOY`).

❌ **No startupProbe** — services có init 60-90s delay trên liveness có thể bị restart loop nếu warm-up quá chậm.
❌ **No canary deployment configured** — `deploy-go-nogo-checklist.md` mentions canary nhưng Helm values không có `canary.enabled`.
❌ **No blue-green switch** configured.
❌ **HPA only cho kiteclass-core** — kitehub services (subscription, branding, admin, email, gateway) không có HPA → scaling phải manual.
❌ **No feature flags** system (LaunchDarkly/Unleash/tự build) → không thể tắt feature không redeploy.
❌ **No Pod Disruption Budget (PDB)** config → node drain có thể kill all replicas cùng lúc.
❌ **No NetworkPolicy** — pod-to-pod traffic unrestricted.
❌ **No automated canary analysis** (Argo Rollouts/Flagger).
❌ **helm rollback 0** chỉ rollback 1 revision — không clear nếu deploy này là bad nhưng previous cũng bad.

### Gap seeds
- GAP-123 (HPA cho kitehub services)
- GAP-124 (PDB + NetworkPolicy hardening)
- GAP-125 (canary deployment infra)

---

## 9. Top 10 Critical Findings (prioritized)

| # | Finding | Severity | Category | Gap |
|---|---------|:--------:|----------|-----|
| 1 | Monitoring stack (Prometheus/Grafana) không deploy production — chỉ dev docker-compose | 🔴 P0 | Monitoring | GAP-111 |
| 2 | Logs default text format, thiếu tenantId/traceId cho multi-tenant — compliance + debug risk | 🔴 P0 | Logging | GAP-114 |
| 3 | Alertmanager MISSING — 7 rules có nhưng không có receiver → alerts câm | 🔴 P0 | Alerting | GAP-120 |
| 4 | Restore không bao giờ tested — backup có nhưng không verify | 🔴 P0 | Backup | GAP-117 |
| 5 | Không có DR runbook platform-wide — chỉ có AI branding (GAP-030 P2) | 🟠 P1 | Backup | GAP-119 |
| 6 | MinIO backup strategy missing — AI assets có thể mất | 🟠 P1 | Backup | GAP-118 |
| 7 | Distributed tracing ZERO — không thể debug xuyên service | 🟠 P1 | Monitoring | GAP-112 |
| 8 | Log aggregation (ELK/Loki) không configured | 🟠 P1 | Logging | GAP-115 |
| 9 | Missing platform-critical alerts (cert expiry, AI provider, tenant leak) | 🟠 P1 | Alerting | GAP-122 |
| 10 | Per-alert runbooks không có — on-call phải improvise khi alert fires | 🟠 P1 | Alerting | GAP-121 |

Secondary (P2):
- FE error tracking (Sentry) missing — GAP-113
- PII scrubbing trong logs — GAP-116
- HPA cho kitehub services — GAP-123
- PDB + NetworkPolicy — GAP-124
- Canary deployment infra — GAP-125

---

## 10. Cross-References

- Related existing gap: **GAP-019** (AI observability & cost monitoring, P1 OPEN) — overlap với GAP-111 cho AI metrics, nhưng GAP-019 AI-scope, GAP-111 platform-scope.
- Related existing gap: **GAP-030** (DR for AI branding, P2 OPEN) — scope chỉ AI; GAP-119 platform-wide.
- Closed gap: **GAP-093** (backup implementation, DONE) — backup code chạy nhưng restore chưa tested (GAP-117 fills the gap).
- Existing guides: `deploy-go-nogo-checklist.md`, `rollback-procedure.md`, `incident-response-runbook.md` — đã có và chất lượng khá, chỉ thiếu per-alert runbooks (GAP-121) và DR runbook (GAP-119).

---

## 11. Meta-Gap Priority Note

Per `.claude/rules/meta-gap-priority.md`:
- **GAP-121** (per-alert runbooks library) = meta-gap vì runbooks = operational skill infrastructure → boost lên trước feature gaps cùng P1.
- GAP-117 (restore drill) = feature but blocks production readiness → P0 giữ nguyên.
- GAP-111, GAP-114, GAP-120 = infrastructure feature P0 (blast radius: toàn platform khi prod đi live).

---

## 12. Recommendation for Fix Order

Theo `.claude/rules/audit-to-gap-pipeline.md` §6:

### Sprint 1 (P0, blocks GA)
1. **GAP-111** — Deploy Prometheus + Grafana via Helm (unblocks GAP-120, GAP-122)
2. **GAP-120** — Alertmanager + Slack/PagerDuty receiver (depends on GAP-111)
3. **GAP-114** — Structured JSON logging + MDC propagation
4. **GAP-117** — Restore drill test automation

### Sprint 2 (P1)
5. **GAP-121** — Per-alert runbooks library (meta — boost)
6. **GAP-115** — Log aggregation pipeline (Loki or CloudWatch)
7. **GAP-119** — Platform-wide DR runbook + RTO/RPO
8. **GAP-118** — MinIO backup strategy
9. **GAP-122** — Missing platform alerts
10. **GAP-112** — Distributed tracing (OpenTelemetry)

### Sprint 3 (P2, batch)
11-15. **GAP-113, GAP-116, GAP-123, GAP-124, GAP-125** (FE tracking, PII scrubbing, HPA, PDB/NetworkPolicy, canary)

---

## 13. Out-of-Scope (per plan §3.2)

- Implement monitoring stack
- Setup Grafana dashboards
- Write runbooks
- Fix ops gaps
- Modify `.claude/rules/output-review-mandate.md` (parent sẽ update trong consolidation PR)
- Update `documents/04-quality/gaps/ROADMAP.md` (parent sẽ handle)

---

## 14. Links

- Plan: `documents/03-planning/plans/plan-audit-catchup-2026-04-19.md` §3.2
- Skill: `.claude/skills/quality/ops-readiness-audit/SKILL.md`
- Pipeline rule: `.claude/rules/audit-to-gap-pipeline.md`
- Mandate: `.claude/rules/post-wave-audit-mandate.md`
- Meta priority: `.claude/rules/meta-gap-priority.md`
- Previous audits: none (first-ever baseline)
