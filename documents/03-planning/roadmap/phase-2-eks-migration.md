---
title: Phase 2 EKS Migration Plan — EC2 Docker Compose → EKS
status: draft
created: 2026-05-07
updated: 2026-05-07
gaps: [GAP-415]
---

# Phase 2 EKS Migration Plan — EC2 Docker Compose → EKS Multi-AZ

**Status:** DRAFT (target execution Phase 1.5 PAID full → Phase 2 P3 trigger gate)
**Closes:** GAP-415 (Phase 2 EKS Migration Plan)
**Related:** GAP-411 (sizing matrix Architecture C), GAP-414 (right-sizing review feeds capacity decision), ADR-025 (Phase 1 AWS Singapore), ADR-026 (Ollama defer Phase 2)
**Owner:** @nguyenvankiet (solo-dev, acting CTO + acting SRE)

---

## 1. Trigger Gate

EKS migration triggers **WHEN ANY** điều kiện sau thỏa:

| Trigger | Threshold | Source |
|---|---|---|
| Paying tenants | ≥30 (early) HOẶC ≥100 (full migration mandate) | GAP-411 sizing matrix Phase 1.5 |
| Daily signup spike | >10 signup/day sustained 7 ngày | Operational observation |
| Multi-AZ requirement | Compliance audit demand HOẶC enterprise tenant SLA | Customer-driven |
| Ollama / FULL_AI re-enable | Per ADR-026 Phase 2 trigger gate (≥30 tenants + revenue ≥$400/mo) | ADR-026 §"Phase 2 trigger gate" |

**Pre-trigger Phase 1.5 PAID early (Architecture A single t3.large):** acceptable cho 30-100 tenants. Migration Phase 1.5 PAID full (Architecture C EKS minimal) = mandatory tại trigger.

---

## 2. Pre-Migration Checklist (T-30 days before cutover)

### 2.1 Helm charts production-ready

Hiện trạng (per state-check 2026-05-07):
- `infrastructure/helm/` directory exists (per CLAUDE.md project structure)
- Audit needed: charts cho 8 services + frontends. Filed gap nếu missing per `audit-to-gap-pipeline.md` §2.5 state-check.

Phase 2 migration prerequisite:
- [ ] Helm chart cho mỗi service (8 services + 2 frontends + ingress)
- [ ] Chart values chia per environment (dev/staging/production)
- [ ] HPA (Horizontal Pod Autoscaler) configs với CPU+Memory threshold
- [ ] PodDisruptionBudget cho rolling deploy resilience
- [ ] NetworkPolicy cho service mesh isolation (optional Phase 2)

### 2.2 EKS Terraform module

Separate from Phase 1 Terraform module (GAP-395 = EC2 stack). New module:
- `infrastructure/terraform-aws/eks/` (NEW post-trigger)
- VPC reuse từ Phase 1 (per GAP-395 VPC.tf)
- EKS control plane $73/mo
- Node group: 2× t3.medium initial; HPA scale 2-8
- IAM roles: cluster role, node group role, IRSA cho per-pod AWS access
- ALB Ingress Controller (replaces Phase 1 ALB → ingress)

### 2.3 Database connection migration

- RDS db.t3.medium Multi-AZ ON (per `sizing-matrix.md` §5.1)
- Dual-write pattern KHÔNG cần (single RDS shared) — chỉ cần EKS pods point tới same RDS endpoint
- Connection pooling: HikariCP per-pod (existing Phase 1 config OK)

### 2.4 Observability migration

- CloudWatch Container Insights enable (cost: +$5-10/mo)
- Logs: EKS pods → CloudWatch Logs (existing log driver OK; per `logs-format-standard.md`)
- Metrics: Prometheus operator on EKS (deploy via Helm) → CloudWatch metrics export (optional)
- Tracing: OpenTelemetry collector pod (defer Phase 2 mid-cycle)

### 2.5 CI/CD pipeline update

- `.github/workflows/docker-build-push.yml` (existing per Wave 37 Bucket B GAP-398) → push to ECR
- New workflow `eks-deploy.yml` — deploy Helm charts via `helm upgrade --install`
- Manual approval gate for production cluster (per `release-deploy-standard.md` §9 — human executes)

---

## 3. Cutover Strategy — Blue-Green

**Why blue-green vs rolling:** Architecture B (EC2 Docker Compose) khác paradigm hoàn toàn với EKS pods. Rolling impossible. Blue-green = parallel cluster + DNS swap.

### 3.1 Cutover sequence

**Day -7 (preparation):**
- Provision EKS cluster (`terraform apply infrastructure/terraform-aws/eks/`)
- Deploy Helm charts → verify pods healthy
- Run smoke test (per GAP-377)
- Run Playwright E2E (per Wave 37 Bucket C GAP-403)

**Day -3 (final dress-rehearsal):**
- DNS staging point tới EKS cluster
- Beta tenants subset (10%) sample test EKS path
- Verify metrics + logs flowing CloudWatch
- Re-run smoke + E2E

**Day 0 (cutover):**
- T-0:00 — Backup RDS snapshot (per `documents/05-guides/deploy/backup-runbook.md`)
- T-0:05 — Notification to beta tenants ("brief maintenance ~15 min")
- T-0:10 — DNS swap: `app.kitehub.me` → ALB Ingress Controller (EKS) endpoint
- T-0:10 — TTL waiting period 60s (Cloudflare proxy fast TTL)
- T-0:11 — Verify traffic flow: CloudWatch ALB metrics + EKS pod logs
- T-0:15 — Run smoke test against production endpoint
- T-0:20 — Public announcement "deploy complete" (status page per GAP-373)

**Day +1 to +7 (soak):**
- Monitor: error rate, P95 latency, signup conversion
- EC2 still hot 24-48h cho instant rollback
- Daily metrics review per `release-deploy-standard.md` §4.3

**Day +7 to +14 (decommission EC2):**
- Final smoke test EKS path
- Decommission EC2 instances (terraform destroy Phase 1 module subset)
- Update Terraform state — remove EC2 resources
- Save final cost snapshot pre/post migration

### 3.2 Rollback criteria

**Auto-rollback triggers (within 24h soak):**
- P95 latency > 2× pre-migration baseline sustained 30 min
- Error rate > 1% sustained 15 min
- Database connection pool exhaustion
- CloudWatch alarm cascade (> 3 alarms simultaneously)

**Rollback execution:**
1. DNS swap back: `app.kitehub.me` → Phase 1 EC2 ALB endpoint
2. TTL 60s wait
3. Verify Phase 1 traffic resumed
4. EKS cluster idle (don't destroy — preserve for diagnosis)
5. Post-mortem within 48h
6. New ADR superseding migration decision (if architectural)

---

## 4. Cost Projection vs Architecture B

| Item | Architecture B Phase 1 | Architecture C Phase 1.5 full (EKS) |
|---|---|---|
| Compute | $15-45 (split EC2) | $133 (EKS control + 2× t3.medium) |
| Database | $0-13 (RDS db.t3.micro) | $90 (RDS db.t3.medium Multi-AZ) |
| Load balancer | $18 (ALB) | $25 (NLB + ALB Ingress) |
| Storage | $9 (EBS + S3 + ECR) | $20 (EBS CSI + S3) |
| Network | $5 (egress) | $15 (cross-AZ + egress) |
| Observability | $0-5 (CloudWatch) | $30 (Container Insights + tracing) |
| **Total** | **$48-92** | **$313-330** |

Step-up ~3.4× per `sizing-matrix.md` §5.1 ($72 → $250). Verify revenue before trigger:
- 100 paying tenants @ $5/mo = $500/mo → covers Architecture C $250 + buffer

---

## 5. Rollback Runbook (DNS Revert)

**Pre-condition:** Phase 1 EC2 still running (24-48h hot post-migration).

**Steps:**
1. Identify Phase 1 ALB DNS name từ Terraform state: `terraform output -raw phase1_alb_dns`
2. Open Cloudflare dashboard → Zone `kitehub.me` → DNS records
3. Update CNAME `app` → Phase 1 ALB DNS (TTL 60s)
4. Wait 90s for propagation
5. Verify: `curl -I https://app.kitehub.me` → response từ Phase 1 services (check `Server` header OR custom `X-Backend-Version` header)
6. Notify beta tenants via status page (per GAP-373) "rolled back; investigating"
7. Post-mortem schedule within 48h

**Why Cloudflare TTL 60s:** existing proxy config supports fast DNS swap. AWS Route 53 default TTL 300s acceptable but Cloudflare frontline gives 60s TTL for emergency.

**Backup rollback path (Phase 1 already destroyed):**
- Restore RDS from pre-cutover snapshot (per `restore-procedure.md`)
- Re-provision EC2 stack từ Terraform git history (`git checkout pre-eks-migration`)
- ETA: 60-90 phút from decision

---

## 6. Decommission Phase 1 — Terraform State Surgery

**Why surgical:** can't `terraform destroy` whole Phase 1 module — VPC + RDS + Route53 zone reused by EKS module.

**Steps:**
1. `terraform state list | grep -E "ec2|alb_phase1"` — identify Phase 1-specific resources
2. `terraform state rm <resource>` — remove from state (tracked separately)
3. AWS console: terminate EC2 instances + delete Phase 1 ALB
4. Update Terraform module references (remove Phase 1 module from `main.tf`)
5. `terraform apply` — verify no drift
6. Cost report next month verify EC2 charge dropped

---

## 7. Acceptance Criteria mapping

| GAP-415 AC | Status |
|---|---|
| `phase-2-eks-migration.md` plan exists với 5 sections | ✅ this file (§1 Trigger / §2 Pre / §3 Cutover / §4 Cost / §5 Rollback) + bonus §6 Decommission |
| EKS Terraform module reviewed (separate from Phase 1 module) | 🟡 PARTIAL — design spec §2.2; Terraform module creation = post-trigger |
| Helm charts cho 8 services + frontends provisioned | 🟡 PARTIAL — audit gap §2.1 (existing Helm dir state-check needed) |
| Cutover dress-rehearsal staging | ⏳ Phase 1.5 mid-cycle activity |
| Rollback runbook (DNS revert) | ✅ §5 |

**Status flip:** GAP-415 → 🟡 PARTIAL (plan ship; execution + Helm audit deferred Phase 1.5 mid-cycle).

---

## 8. References

- GAP-411 — sizing matrix Architecture C
- GAP-414 — right-sizing monthly review feeds capacity decision
- GAP-377 — smoke test script
- GAP-378 — rollback procedure
- GAP-373 — status page
- ADR-025 — AWS Singapore
- ADR-026 — Ollama defer Phase 2
- `release-deploy-standard.md` §4 process + §9 agent role
- `documents/05-guides/deploy/aws-architecture-sizing-matrix.md`
- `documents/05-guides/deploy/restore-procedure.md`
- `documents/05-guides/deploy/rollback-procedure.md`
- `documents/03-planning/roadmap/release-1-plan-2026.md` Phase progression
- AWS EKS docs: https://docs.aws.amazon.com/eks/

---

## 9. Log

- **2026-05-07** — Initial migration plan. Trigger gate + 7-section plan (pre-migration / cutover / cost / rollback / decommission). EKS Terraform module + Helm chart audit deferred Phase 1.5 mid-cycle. Closes GAP-415 acceptance criterion partial.
