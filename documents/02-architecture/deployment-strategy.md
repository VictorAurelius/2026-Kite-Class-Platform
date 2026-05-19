---
title: Deployment Strategy — Single Source
audience: dev
created: 2026-04-18
last-reviewed: 2026-05-19
status: living
---

# Deployment Strategy — Single Source

**Status:** 🟢 ACCEPTED (2026-04-18, GAP-103) — **REVISED 2026-05-07** per [ADR-025](adr/ADR-025-aws-only-deploy-phase-1-free-tier.md): Phase 1 BETA shifts to **AWS Singapore Free Tier thuần** (Oracle Cloud archived to `documents/07-archived/oracle-deploy-2026/`). VN-resident migration deferred Phase 3 GA pending counsel review.
**Audience:** Developers, Reviewers, DevOps, Thesis readers
**Consolidates:** 6 scattered deploy docs (list ở §6)

---

## 1. Philosophy — 5 Nguyên Tắc Cốt Lõi

### 1.1 Cloud-agnostic Helm-first
Mọi production artifact đóng gói thành Helm charts (`infrastructure/helm/`). Không lock vào AWS-specific managed services (DynamoDB, Lambda, RDS Proxy) nếu có alternative portable (K8s Job, StatefulSet, pgbouncer). Helm chart sẵn sàng deploy lên AWS EKS, ECS Fargate, hoặc VN cloud (Viettel/VNG/FPT) — quyết định Phase 3 GA per ADR-025.

**Why:** Options open — Phase 3 migration sang VN cloud (compliance) chỉ tốn ~1 tuần thay vì 3 tháng nếu lock-in AWS managed services.

### 1.2 Cost-conscious Single-cloud (Phase 1 BETA)
**Phase 1 BETA** chạy AWS Singapore (`ap-southeast-1`) **Free Tier thuần** — 1× t4g.small EC2 + RDS db.t3.micro Postgres + S3 + SES + CloudWatch + Cloudflare DNS/CDN. **Phase 3 GA target** TBD: counsel review quyết định AWS continued vs migrate sang VN cloud (compliance Decree 53/2022). Development local qua Docker Compose.

**Why:** Solo-dev "free tier only" budget Release 1 (chốt 2026-05-07 per ADR-025). Oracle Cloud Always Free đã try fail (reject rate ~50% VN); AWS KYC dễ hơn. Compliance debt acknowledged + risk-managed (Phase 1 invite-only ~10-20 tenants không trigger regulator).

### 1.3 Reproducible local = prod
Docker images giống hệt giữa local / staging / prod (same Dockerfile, same build). Config khác qua Helm values + env vars, không rebuild image cho env mới. `docker-compose.kitehub.yml` dùng cùng images như K8s deployment.

**Why:** Dev environments khác prod = hidden bugs lộ ra lúc deploy. Reproducibility is debugging.

### 1.4 GitOps Ready
Mọi change qua PR → merge to main → CI/CD deploy. Helm chart versioned trong git. Không ai SSH vào prod để hotfix (ngoại trừ SEV1 với post-mortem ticket). Secrets qua Sealed Secrets (GitOps-compatible).

**Why:** Audit trail + rollback = git revert, không phải "ai đó fix rồi nhưng quên update".

### 1.5 Thesis-Defensible
Mọi deployment decision phải justify được trong graduation thesis với context K-12 Vietnamese education market. Không chọn "vì cool", chọn "vì measurable benefit X trong Vietnamese SaaS context Y".

**Why:** Defense phải trả lời "tại sao anh dùng cái này mà không phải cái kia" — cần ADR + citation literature.

---

## 2. Environment Matrix

| Layer | Tech | Location | Rationale |
|-------|------|----------|-----------|
| **Local dev** | Docker Compose via `./scripts/up.sh` | Developer machine (WSL/Mac/Linux) | Reproducibility, WSL2 friendly, fast startup (<2 min) |
| **Phase 1 BETA staging** | AWS Free Tier (t4g.small ARM) | AWS `ap-southeast-1` (Singapore) | KYC-friendly, 12-month free; staging = same instance class as prod for parity |
| **Phase 1 BETA production** | AWS Free Tier (1× t4g.small + RDS db.t3.micro + S3 + SES) | AWS `ap-southeast-1` (Singapore) | Free Tier 12-month, ECS Fargate vs single-EC2-host TBD per follow-up gap |
| **Phase 3 GA target** | TBD per counsel review (AWS continued OR VN cloud Viettel/VNG/FPT) | TBD | Compliance debt resolution Phase 3 trigger gate per ADR-025 |
| **IaC** | Terraform | `infrastructure/terraform-aws/` (active); Oracle archived `documents/07-archived/oracle-deploy-2026/terraform-oracle/` | Single-cloud Phase 1; multi-cloud Phase 3 if VN cloud added |
| **K8s packaging** | Helm 3 charts | `infrastructure/helm/` | Version, rollback, value templating |
| **Secrets** | K8s Sealed Secrets (dev/staging) + Vault (prod future) | `infrastructure/k8s/sealed-secrets/` | GitOps-compatible encrypted-at-rest |
| **CI/CD** | GitHub Actions | `.github/workflows/` | Free tier, matrix builds, mature marketplace |
| **Container registry** | GitHub Container Registry (ghcr.io) | Organization scope | Free cho public, integrated với GHA |
| **Observability** | Prometheus + Grafana self-hosted | Cluster `observability/` namespace (Wave 6) | No vendor lock, cost control |
| **DNS + SSL** | Cloudflare + Let's Encrypt cert-manager | Automated wildcard `*.kiteclass.com` | Zero cost, automated renewal |

---

## 3. Deployment Workflow

### 3.1 Feature → Staging
```
PR merged to main
 ↓
GitHub Actions build images (per-service)
 ↓
Push to ghcr.io với tag :main-{sha7}
 ↓
Trigger deploy-staging.yml
 ↓
helm upgrade --install -n kite-staging --values staging.yaml
 ↓
Smoke tests (GAP-089 — future)
 ↓
Ready for manual promote to prod
```

### 3.2 Staging → Production (planned)
```
Manual approval on GitHub environment "production"
 ↓
Retag ghcr.io image :stable-{date}
 ↓
Trigger deploy-production.yml
 ↓
helm upgrade với --atomic (rollback nếu fail)
 ↓
Smoke tests + synthetic monitoring
 ↓
Announce in #releases Slack channel (future)
```

### 3.3 Emergency rollback
```
helm rollback <release> <revision>
```
Per-service rollback documented trong [`05-guides/deploy/rollback-procedure.md`](../05-guides/deploy/rollback-procedure.md).

---

## 4. Service Topology

```
┌────────────────────────────────────────────────────┐
│  Public Internet                                   │
└───────────────────┬────────────────────────────────┘
                    │ HTTPS (cert-manager)
                    ▼
          ┌────────────────────┐
          │  kite-gateway      │ ← Spring Cloud Gateway (port 8080)
          │  (routing + CORS)  │
          └─────┬──────────────┘
                │
     ┌──────────┼───────────┬──────────────┐
     ▼          ▼           ▼              ▼
 kitehub-*  kiteclass-core  frontends  kite-admin
 (6 svc)    (core module)   (FE apps)  (support)
     │          │
     └──────┬───┘
            ▼
    ┌──────────────────────────────────────┐
    │ Shared Infra (kite-* prefix)         │
    │  • kite-postgres   (5433)            │
    │  • kite-redis      (6379)            │
    │  • kite-rabbitmq   (5672, 15672)     │
    │  • kite-minio      (9000, 9001)      │
    │  • kite-ollama     (11434, local)    │
    └──────────────────────────────────────┘
```

Details trong [`docker-platform-architecture.md`](docker-platform-architecture.md).

---

## 5. Security Posture

- **Network:** K8s NetworkPolicy per-namespace. Gateway = only inbound. Services mesh via Istio (future).
- **Secrets:** Sealed Secrets committed to git (encrypted). Never plain .env in git.
- **Images:** Scan via Trivy trong CI. Vulnerability gate fails build on HIGH+ CVEs.
- **Runtime:** PodSecurityPolicy (deprecated) → Pod Security Standards "restricted" profile.
- **Compliance (REVISED 2026-05-07 per ADR-025):** Phase 1 BETA = AWS Singapore `ap-southeast-1`. Vi phạm Nghị định 53/2022/NĐ-CP §26 data localization VN — risk-managed bằng (1) invite-only ~10-20 tenants không trigger regulator radar, (2) explicit consent khi signup mention infrastructure provider AWS Singapore (Bucket B GAP-385 PDPL flow), (3) Phase 3 trigger gate counsel review compliance scope HOẶC migrate data layer sang VN cloud trước GA. PDPL 2023 Art 11 (consent collection) compliant Phase 1 BETA.

Operational procedures trong [`05-guides/infrastructure/SECRET-MANAGEMENT.md`](../05-guides/infrastructure/SECRET-MANAGEMENT.md) và [`05-guides/operations/incident-response-runbook.md`](../05-guides/operations/incident-response-runbook.md).

---

## 6. Cross-references — Operator Docs

Docs cụ thể theo task:

| Task | Doc | Type |
|------|-----|:----:|
| **Deploy AWS Singapore (Architecture B per ADR-025)** | [`03-planning/roadmap/release-1-deploy-runbook.md`](../03-planning/roadmap/release-1-deploy-runbook.md) | Runbook (Phase 0-9) |
| ~~Deploy to Oracle Cloud~~ (archived 2026-05-07 per ADR-025) | [`07-archived/oracle-deploy-2026/`](../07-archived/oracle-deploy-2026/) | Archived |
| **Deploy KiteClass Docker** | [`03-planning/implementation/kiteclass-docker-deployment.md`](../03-planning/implementation/kiteclass-docker-deployment.md) | Plan |
| **Standard deploy procedure** | [`05-guides/operations/runbooks/deployment-procedures.md`](../05-guides/operations/runbooks/deployment-procedures.md) | Runbook |
| **Pre-deploy gate** | [`05-guides/deploy/deploy-go-nogo-checklist.md`](../05-guides/deploy/deploy-go-nogo-checklist.md) | Checklist |
| **Oracle Cloud (VN)** | [`05-guides/vietnamese/huong-dan-deploy-oracle-cloud.md`](../05-guides/vietnamese/huong-dan-deploy-oracle-cloud.md) | Guide VN |
| **Thesis deployment chapter** | [`08-thesis/references/deployment-guide.md`](../08-thesis/references/deployment-guide.md) | Academic |
| **Rollback steps** | [`05-guides/deploy/rollback-procedure.md`](../05-guides/deploy/rollback-procedure.md) | Runbook |
| **Incident response** | [`05-guides/operations/incident-response-runbook.md`](../05-guides/operations/incident-response-runbook.md) | Runbook |
| **Secrets setup** | [`05-guides/infrastructure/SECRET-MANAGEMENT.md`](../05-guides/infrastructure/SECRET-MANAGEMENT.md) | Guide |
| **WSL dev setup** | [`05-guides/local-dev/wsl-migration-playbook.md`](../05-guides/local-dev/wsl-migration-playbook.md) | Playbook |

---

## 7. Future: Migration Paths

### 7.1 Oracle → AWS (khi commit production)
- **Trigger:** paying customer #1 yêu cầu SLA cao hơn Oracle Free Tier cho phép
- **Effort:** ~2 tuần (Terraform aws modules đã tồn tại, chỉ cần provision + helm upgrade)
- **Risk:** cost jump từ $0 → ~$200/tháng cho EKS + RDS + ALB

### 7.2 AWS → Oracle (contingency)
- **Trigger:** AWS bill spike, hoặc Oracle sponsor VN education
- **Effort:** ~1 tuần (Terraform oracle modules đã tồn tại)
- **Risk:** learning curve OCI-specific concepts (compartments, VNIC, block volumes)

### 7.3 Hybrid (không recommend)
Multi-cluster federation operationally complex, chưa có use-case justify.

---

## 8. Key Decisions (ADRs)

Deployment-related ADRs:

- [ADR-004](adr/ADR-004-instance-lifecycle.md) — Frontend Instance Provisioning Lifecycle (provisioning state machine)
- [ADR-007](adr/ADR-007-outbox-pattern-for-events.md) — Outbox Pattern for Reliable Event Publishing (cross-region messaging)
- [ADR-014](adr/ADR-014-async-jobs-queue-over-batch.md) — Async Jobs Queue (RabbitMQ) over Batch Framework (transport choice)
- [ADR-015](adr/ADR-015-aws-agent-plugins-evaluation.md) — AWS Agent Plugins Evaluation (tooling decision)

---

## 9. Planned Work

Tracked trong gaps:

- [GAP-086](../04-quality/gaps/GAP-086-incident-response-runbook.md) — Incident response runbook (P0)
- [GAP-087](../04-quality/gaps/GAP-087-deploy-go-no-go.md) — Deploy go/no-go checklist (P0)
- [GAP-088](../04-quality/gaps/GAP-088-rollback-procedure.md) — Rollback procedure per service (P0)
- [GAP-089](../04-quality/gaps/GAP-089-post-deploy-smoke-test.md) — Post-deploy smoke test (P1)
- [GAP-093](../04-quality/gaps/GAP-093-backup-not-functional.md) — Backup not functional (P0)
- [GAP-102](../04-quality/gaps/GAP-102-guides-completion-adr-kickoff.md) — 05-guides completion (monitoring, backup SOP, etc.)

---

## 10. Log

- **2026-04-18:** Created (GAP-103). Consolidates 6 scattered deploy docs. ADR-015 (AWS Agent Plugins) accepted trong cùng PR.
