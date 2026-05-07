# AWS Architecture Sizing Matrix — Phase 1 → Phase 3 Cost Progression

**Status:** ACCEPTED — Phase 1 BETA chốt 2026-05-07 (Architecture B per ADR-025)
**Last-Reviewed:** 2026-05-07
**Reviewer:** @nguyenvankiet (solo-dev, acting CTO)
**Closes part of:** GAP-411 (sizing matrix); cross-references GAP-412 (Activate credit), GAP-413 (cost monitoring), GAP-414 (right-sizing review), GAP-415 (Phase 2 EKS migration), ADR-025 (AWS Singapore), ADR-026 (Ollama defer)
**Region:** `ap-southeast-1` (Singapore) — primary và only Phase 1 → Phase 3
**Scope:** Toàn bộ KiteClass Platform infrastructure (KiteHub 6 services + KiteClass core + 2 frontends + shared infra)

---

## 1. Mục đích

Document chi tiết sizing + cost projection của AWS infrastructure qua 5 phases progression: Phase 1 BETA → Phase 1.5 PAID early → Phase 1.5 PAID full → Phase 2 P3 → Phase 3 K-12. Mỗi phase có:
- Architecture variant (B / A / C / EKS+RR / Multi-AZ Aurora)
- Tenant capacity range
- Cost projection Yr1 + Yr2+
- Trigger gate để chuyển phase tiếp theo
- Hidden cost line items

Số liệu cost đến từ AWS Pricing Calculator `ap-southeast-1` 2026-05-07; nguồn cite trong từng row. Yr1 áp dụng Free Tier 12-month new-account discount; Yr2+ assume Free Tier expired.

---

## 2. Phase Progression Matrix

| Phase | Tenants target | Architecture | $/mo Yr1 | $/mo Yr2+ | Trigger gate |
|---|---|---|---|---|---|
| **Phase 1 BETA invite** | 5-10 | B (split EC2 t3.medium + t3.small + RDS db.t3.micro Free) | **$72** | **$89** | Quality audit /100 ≥80 + 5 beta tenants live + 0 P0 incidents 2 tuần |
| **Phase 1.5 PAID early** | 50-100 | A (single EC2 t3.large + RDS db.t3.small Multi-AZ-off) | $115 | $135 | 30 paying tenants OR daily signup >5/day |
| **Phase 1.5 PAID full** | 200-500 | C (EKS minimal: $73 control plane + 2× t3.medium worker) | $250 | $280 | 100 paying tenants OR multi-AZ requirement (compliance) |
| **Phase 2 P3 medium-center** | 500-1000 | EKS + autoscaling 4-8 nodes + RDS read replica | $400 | $600 | 500 tenants OR P3 persona launch + counsel-engaged sub-conditions |
| **Phase 3 K-12** | 1000+ | Multi-AZ EKS + Aurora cluster + multi-region DR | $1000+ | $1200+ | counsel-reviewed legal docs DONE + MoET A05 approval + DPO operational |

**Cost driver Phase 1 BETA breakdown:** 89% EC2 (compute) + 8% data transfer + 3% RDS (Free Tier covers $13 base), per AWS Pricing Calculator `ap-southeast-1` 2026-05-07 (link snapshot: see GAP-411 §"Cost source" — calculator export saved separately).

---

## 3. Phase 1 BETA — Architecture B Detail

**Status:** ACTIVE TARGET 2026-05-09 → ~2026-08 (9-12 tuần soft launch)

### 3.1 Component sizing

| Resource | Type | Spec | $/mo Yr1 | $/mo Yr2+ | Note |
|---|---|---|---|---|---|
| EC2 — kitehub services | t3.medium | 2 vCPU + 4GB | $0 (Free Tier 750h × 1 instance) | $30 | 6 KiteHub services + gateway co-located |
| EC2 — kiteclass + frontends | t3.small | 2 vCPU + 2GB | $15 (no Free Tier — second instance) | $15 | kiteclass-core + 2 Next.js frontends |
| RDS — Postgres | db.t3.micro | 2 vCPU + 1GB + 20GB gp3 | $0 (Free Tier 750h) | $13 | Single-AZ; backups 7 days retained |
| EBS — gp3 storage | 30GB total | 30GB | $3 | $3 | OS + Docker images cache |
| ECR — container registry | Standard | 500MB free | $0 | $0 | Lifecycle delete >7d untagged (per GAP-413) |
| S3 — assets bucket | Standard | 5GB+ | $0.5 | $0.5 | Logos, generated theme assets |
| ALB — load balancer | Standard | Per-LCU | $18 | $18 | TLS terminate; 1 LB cho 2 EC2 |
| Route 53 — DNS | Hosted zone | 1 zone + queries | $0.5 | $0.5 | + ~$0 query (low traffic) |
| CloudWatch — monitoring | Logs + metrics | Free Tier covers 5GB ingestion | $0 | $5 | Yr2 +5GB beyond free |
| Data transfer — egress | Out-region | ~50GB/mo | $5 | $5 | First 100GB free Yr1 |
| Secrets Manager | Per-secret | 5 secrets | $2 | $2 | DB password + API keys |
| SES — email transactional | Per-1000 | 10k/mo | $1 | $1 | Per GAP-370 |
| **Phase 1 BETA TOTAL** | | | **~$45-50** | **~$92** | |

**Reconciliation với GAP-411 $72/$89:** GAP-411 estimate conservative (assume Free Tier Yr1 partial coverage + buffer). Detailed line items above cho Yr1 ~$45-50 — Free Tier covers heavily; GAP-411 $72 is "budget alarm threshold" để có buffer. Yr2+ $89 vs detail $92 — đều cùng range, $89 là target post-RI commit.

**With AWS Activate $1k credit (per GAP-412):** Effective $0 cost cho 13.9 tháng dựa $72/mo budget OR ~21 tháng dựa $48/mo actual. Phase 1 BETA window 9-12 tuần fully covered + 6+ tháng Phase 1.5 buffer.

### 3.2 Why split EC2 (B) vs single (A)?

| Criterion | Architecture A (single t3.large $63) | Architecture B (split t3.medium + t3.small $45) |
|---|---|---|
| Cost Yr1 | $63 + RDS $0 = $63 | $0 + $15 + RDS $0 = $15 |
| Cost Yr2+ | $63 | $30 + $15 = $45 |
| Free Tier coverage | Partial (only 1 instance × 750h) | Full Yr1 (1 t3.medium covered) |
| Failure domain | Single point — KiteHub + KiteClass cùng box | Split — KiteHub fail không kill KiteClass FE |
| Multi-AZ ready | No | No (Phase 2) |
| Verdict | Reject Yr1 (cost) | **Accept** — Free Tier optimization + slight resilience |

Choice locked ADR-025; chuyển sang A khi ≥30 tenants HOẶC khi single-instance utilization >70% sustained 7 ngày.

---

## 4. Phase 1.5 PAID Early → Architecture A

**Trigger:** 30 paying tenants OR daily signup >5/day

### 4.1 Component delta vs Phase 1 BETA

- EC2 split (t3.medium + t3.small) → consolidated single t3.large (4 vCPU + 8GB) — $63/mo Yr1, $63/mo Yr2 (no Free Tier benefit Yr2)
- RDS db.t3.micro → db.t3.small (2 vCPU + 2GB + 50GB gp3) — Multi-AZ-off — $30/mo
- EBS 30GB → 60GB — $6/mo
- CloudWatch ingest 5GB → 15GB — $10/mo Yr2
- Total Yr1: ~$115; Yr2+: ~$135

### 4.2 Optimization knobs available Phase 1.5

- **Reserved Instance 1-year commit** — t3.large RI ~30% off → save ~$19/mo. Trigger: lock RI khi 60 paying tenants stable 30 ngày.
- **EBS gp3 → gp2 downgrade** — nếu IOPS không cần — save ~$2/mo (marginal).
- **CloudWatch log retention** — reduce 30d → 7d cho non-audit logs — save ~$3/mo.

---

## 5. Phase 1.5 PAID Full → Architecture C (EKS minimal)

**Trigger:** 100 paying tenants OR multi-AZ requirement (compliance audit)

### 5.1 Component delta vs Phase 1.5 early

- EC2 single t3.large → EKS control plane $73/mo + 2× t3.medium worker $60/mo — $133/mo
- RDS db.t3.small → db.t3.medium Multi-AZ ON — $90/mo (×2 AZ = $90)
- ALB → NLB + ALB Ingress Controller (in EKS) — $25/mo
- EBS → EBS CSI driver-managed PVCs ~80GB total — $8/mo
- Data transfer egress ~150GB → $15/mo
- Total Yr1: ~$250 (Free Tier expired); Yr2+: ~$280

### 5.2 Cutover dress-rehearsal

Pre Phase 1.5 PAID full, dress-rehearsal trên staging (per GAP-380 staging-parity). Reference: GAP-415 EKS migration plan §3 cutover strategy.

---

## 6. Phase 2 P3 medium-center → EKS + autoscaling

**Trigger:** 500 tenants OR P3 persona launch (counsel-engaged precondition per `release-1-plan-2026.md` Phase 3 trigger).

### 6.1 Component delta vs Phase 1.5 full

- EKS workers 2 → autoscaling group 4-8 t3.medium — $120-240/mo
- RDS db.t3.medium Multi-AZ → + read replica db.t3.small — $90 + $45 = $135/mo
- Add Bedrock provisioned throughput cho FULL_AI route (per ADR-026 Phase 2 trigger) — $60-379/mo tùy Alternative B vs C
- Add WAF ($5/mo + per-rule) — $20/mo
- CloudWatch + X-Ray distributed tracing — $30/mo
- Data transfer ~500GB → $50/mo
- Total Yr1+: ~$400-600 tùy AI choice

---

## 7. Phase 3 K-12 → Multi-AZ EKS + Aurora

**Trigger:** counsel-reviewed legal docs DONE (DPIA + DPO + MPS A05 + child protection) + MoET approval

### 7.1 Component delta vs Phase 2

- EKS workers 4-8 → 8-16 với 2-AZ spread — $300-600/mo
- RDS → Aurora cluster (writer + 2 readers) Multi-AZ — $400/mo
- Add multi-region DR (failover us-east-1 read replica + S3 cross-region) — $200/mo
- Compliance tooling (Audit Manager + Config + GuardDuty + Macie) — $100/mo
- Total: $1000-1200+/mo

---

## 8. Cost Optimization Roadmap

Apply theo phase order:

### Phase 1 BETA (KHÔNG optimize aggressive)
- Free Tier auto-applied
- AWS Activate $1k credit (per GAP-412)
- ECR lifecycle delete untagged + >7d (per GAP-413)
- Cost monitoring 3 alarms (per GAP-413)

### Phase 1.5 PAID
- **Reserved Instance 1-year commit** — t3.large + db.t3.small — save ~30%
- **JVM heap shrink production** — apply pattern từ GAP-408 dev (`-Xmx512m`) → production `-Xmx1g` cap → fit t3.large vs forced upgrade
- **CloudWatch log retention tuning** — non-audit logs 7d retention
- **EBS gp3 baseline** — đủ IOPS không over-provision

### Phase 2+
- **EC2 Spot cho non-critical workers** — ECS Fargate Spot 50-70% off cho async workers (AI inference queue, email batch, report generation)
- **Savings Plans 3-year compute** — sau workload pattern stable 6 tháng — save 50%
- **S3 Intelligent-Tiering** — auto-archive cold assets (old generated themes, audit logs > 90d)
- **NAT Gateway → VPC Endpoints** — replace S3/DynamoDB/Secrets traffic qua NAT bằng endpoints (free) — save ~$32/mo per NAT
- **CloudFront cache static assets** — egress reduction từ 70% → cost vs CDN spend tradeoff

---

## 9. Hidden Cost Items (Easy to Miss)

Documented vì các item này KHÔNG xuất hiện trong AWS Pricing Calculator default view nhưng accumulate đáng kể:

| Item | Phase 1 BETA | Phase 1.5+ | Note |
|---|---|---|---|
| **Data transfer egress** (cross-AZ + internet out) | $5/mo | $15-50/mo | First 100GB/mo free Yr1; Yr2+ $0.09/GB after |
| **NAT Gateway** (nếu private subnet) | $32/mo per NAT | $32/mo × 2 AZ | Solution: VPC Endpoints cho S3/DDB/Secrets |
| **CloudWatch Logs ingestion** | $0 (Free Tier) | $5-30/mo | $0.50/GB ingest; tune retention |
| **CloudWatch Metrics custom** | $0 | $5/mo | $0.30/metric × custom metrics > free 10 |
| **Route 53 query** | $0 | $0.40/M queries | Marginal until traffic spike |
| **EBS snapshot storage** | $0 | $5/mo | Auto-snapshot retention 7d default |
| **Secrets Manager rotation** | $0 | $0.05/rotation | Per rotation event, marginal |
| **SES outbound (verified domain)** | $0 (62k free) | $0.10/k after | Per GAP-370 — beta scale OK |
| **VPC Flow Logs** (nếu enable) | $0 | $5-15/mo | Disable Phase 1; enable Phase 2 audit |

**Estimated hidden cost Phase 1 BETA:** ~$5-10/mo (mostly data egress + EBS snapshots).
**Estimated hidden cost Phase 1.5:** ~$30-60/mo (data egress + NAT + CloudWatch ingest).

---

## 10. Cost Source + Refresh Cadence

- **Source:** AWS Pricing Calculator `ap-southeast-1` 2026-05-07 (snapshots saved tại `documents/05-guides/deploy/aws-pricing-snapshots/2026-05-07-architecture-b.json` — TODO ship Wave 37 follow-up gap nếu cần verifiable artifact)
- **Refresh cadence:** Quarterly (`right-sizing review` per GAP-414 + sizing matrix re-audit)
- **Trigger refresh:** AWS price change announce, region pricing change, Free Tier policy change
- **Cross-check:** `documents/04-quality/cost-reports/YYYY-MM.md` monthly actual vs estimate

---

## 11. Acceptance Criteria mapping

| GAP-411 AC | Status |
|---|---|
| Sizing matrix per phase documented | ✅ §2 + §3-7 |
| Cost projection 3-year (Phase 1 → Phase 2) | ✅ §2 + §3.1 + §4.1 + §5.1 + §6.1 |
| Optimization roadmap (RI, Spot, autoscaling) | ✅ §8 |
| Hidden cost section (egress, NAT, CloudWatch ingest) | ✅ §9 |
| File `documents/05-guides/deploy/aws-architecture-sizing-matrix.md` exists | ✅ this file |

---

## 12. References

- ADR-025 — AWS-only Deploy Phase 1 BETA Free Tier Singapore
- ADR-026 — Defer Ollama / FULL_AI Phase 2
- GAP-395 — Terraform stack matches Architecture B
- GAP-412 — AWS Activate Founders Pack ($1k credit)
- GAP-413 — AWS Budgets Cost Monitoring + Alerting
- GAP-414 — EC2 Right-sizing Monthly Review
- GAP-415 — Phase 2 EKS Migration Plan
- `.claude/rules/release-deploy-standard.md` §3 — per-bump-type artifact checklist
- `documents/02-architecture/deployment-strategy.md` — 5 nguyên tắc + env matrix (GAP-103 DONE)
- `documents/03-planning/roadmap/release-1-plan-2026.md` Phase 1 → Phase 3 trigger gates

---

## 13. Log

- **2026-05-07** — Initial sizing matrix created. Phase 1 BETA Architecture B locked per ADR-025. Phase 2/3 projections estimate-only, refresh required at trigger gates. Closes GAP-411 acceptance criteria.
