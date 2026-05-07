# ADR-025: AWS-only Deploy for Phase 1 BETA (Free Tier, Singapore region)

**Status:** ACCEPTED
**Date:** 2026-05-07
**Deciders:** @nguyenvankiet (solo-dev, acting CTO)
**Reviewers:** N/A (solo-dev mode per CLAUDE.md decision context locked 2026-05-06)
**Related Gap(s):** GAP-103 (deployment-strategy.md), GAP-369 (DNS), GAP-370 (email), GAP-379 (secrets), GAP-394 (account-prep)
**Supersedes:** Implicit "Oracle Cloud primary" architecture per `deployment-strategy.md` v1.0 (GAP-103 DONE 2026-04-18)

---

## Context

Phase 1 BETA launch (~10-20 invite-only tenants, persona P1+P2) yêu cầu cloud infrastructure cho 6 KiteHub backend services + frontend + Postgres + Redis + RabbitMQ + object storage + transactional email.

**Original architecture** (`deployment-strategy.md` GAP-103, ADR-015):
- **Primary:** Oracle Cloud Infrastructure (OCI) Always Free tier — 4 OCPU ARM Ampere + 24GB RAM + 200GB block storage; **region Hanoi (VN-HAN)** thỏa Luật An ninh mạng 2018 + Nghị định 53/2022/NĐ-CP data localization
- **Secondary:** AWS cho SES email + ECR images

**Pain point trigger 2026-05-07:** Solo-dev đăng ký Oracle Cloud Always Free fail với generic error "Oops, we're sorry, an error occurred while creating your account". Đây là pattern reject phổ biến với user Việt Nam 2024+ do Oracle siết anti-fraud cho ARM Always Free; reject rate ~50% và risk permanent ban sau 2-3 lần thử.

**Forces at play:**
- **Time pressure:** PDPL hard deadline 2026-07-01 (~7 tuần countdown), Phase 1 BETA window 9-12 tuần (`release-1-plan-2026.md`); không thể stuck ở account creation
- **Solo-dev budget:** "Free tier only" cho Release 1 (user-explicit constraint 2026-05-07); không có budget paid cloud >$0/tháng
- **Compliance posture:** Risk tolerance Moderate (CLAUDE.md), Phase 3 trigger gate = counsel review + 4 sub-conditions — compliance review formal sẽ diễn ra trước GA, không phải trước BETA invite-only
- **Phase 1 BETA scope:** invite-only ~10-20 tenants, persona P1 (Solo Teacher) + P2 (Center Owner) — không phải public general availability; user được invite + ký consent (per Bucket B GAP-385 PDPL flow)

## Decision

**Switch primary deploy platform từ Oracle Cloud (Hanoi VN-HAN) sang AWS (Singapore `ap-southeast-1`) thuần Free Tier cho Phase 1 BETA.** Migration sang VN-resident infrastructure (Viettel/VNG/FPT cloud HOẶC retry Oracle VN-HAN) defer Phase 3 GA, gated by counsel review.

**Concrete:**
- Khai tử `infrastructure/terraform-oracle/` (archive `documents/07-archived/oracle-deploy-2026/`)
- AWS-only deploy artifacts: `infrastructure/terraform-aws/` + `infrastructure/helm/` (EKS path) HOẶC ECS Fargate (recommended cho free tier — xem Implementation Notes §5)
- Free tier scope: 1× t4g.small EC2 (2 vCPU ARM, 2GB RAM) + 1× RDS db.t3.micro Postgres (1GB RAM, 20GB storage) + S3 + SES + CloudWatch + Cloudflare DNS/CDN (free)

## Consequences

### Positive

- **Unblock Phase 1 BETA timeline** — AWS account creation KYC ~1 ngày so với Oracle 50% reject rate + 24h cooldown
- **Lower account-prep complexity** — 1 cloud provider thay vì 2 (Oracle + AWS hybrid trước đây)
- **Mature ecosystem** — AWS có ECR + Secrets Manager + SES + ALB + CloudFront integration sẵn; Oracle Always Free missing managed Redis (phải self-host) + missing managed RabbitMQ (cần AmazonMQ paid hoặc replace bằng SQS+SNS)
- **Better tooling/docs** — Terraform AWS provider mature hơn OCI provider; community runbooks dồi dào; `huong-dan-deploy-oracle-cloud.md` (287 lines) replaced bằng AWS-equivalent runbook (sẽ ship trong follow-up gap)
- **Clear migration path Phase 3** — terraform AWS module pattern cho phép swap region khi switch sang VN cloud provider compliant; data layer (Postgres + S3) export-friendly

### Negative

- **🚨 Compliance debt** — AWS Singapore vi phạm Nghị định 53/2022/NĐ-CP §26 yêu cầu data localization VN. Risk-managed bằng:
  1. Phase 1 BETA invite-only ~10-20 tenants (không trigger regulator radar — Decree 53 enforcement focus là service ≥1M VN users)
  2. Mọi user ký explicit consent acknowledging "infrastructure provider AWS Singapore" (mở rộng PDPL consent flow Bucket B GAP-385)
  3. Phase 3 trigger gate = counsel review trước GA; nếu counsel flag, migrate data layer sang VN cloud (Viettel Cloud / VNG / FPT) trước public launch
  4. Document compliance debt explicitly trong `release-1-plan-2026.md` Phase 3 trigger condition mới: "✓ Counsel approved compliance scope OR data-layer migrated to VN-resident cloud"
- **AWS new account model 2024+ (CORRECTED 2026-05-07):** AWS đã thay model "Free Tier 12 months classic" bằng 2 options:
  - **Free 6 tháng:** $200 credits + free-tier services × 6 tháng → auto-close account sau đó (mất data + resources)
  - **Trả phí (Paid):** $200 credits + free-tier services + pay-per-use sau khi hết credit; không auto-close
  - **Decision: chọn Paid plan** vì auto-close 6 tháng = risk mất production data nếu Phase 1 BETA + Phase 2 chạy >6 tháng (timeline chốt ~5-6 tháng = borderline)
  - **Cost projection Paid plan:** $0/tháng trong vùng free tier (Phase 1 BETA scope đủ); ~$25-40/tháng nếu vượt free tier (t4g.small ~$13 + RDS db.t3.micro ~$13 + storage/transfer minimal). Billing alarm $5/$50/$150 set ngay sau signup
  - Phase 3 GA migrate VN cloud sẽ close AWS account chủ động (không bị force-close)
- **2GB RAM constraint** — KiteHub stack 6 backend services + frontend + Postgres + Redis + RabbitMQ + (S3 replaces MinIO) chạy trên 2GB là TIGHT. Solo-dev mitigation:
  - Docker Compose single-host mode (không Kubernetes — saves control plane RAM)
  - Postgres ngoài (RDS managed) — relieve EC2 RAM
  - Redis self-host on EC2 (no ElastiCache free tier) với memory cap 256MB
  - RabbitMQ self-host on EC2 với memory cap 256MB; HOẶC replace bằng SQS+SNS managed (trade infra simplicity for SDK rewrite cost)
  - JVM heap caps strict per service (vd: `-Xmx256m`)
  - Beta launch hard cap 20 tenants — nếu vượt, force upgrade trước Phase 1.5
- **Single-region SPOF** — Singapore `ap-southeast-1` không có cross-region failover Phase 1; latency VN→SG ~50-80ms (acceptable cho beta nhưng không lý tưởng GA)
- **Loss of Oracle ARM Always Free generosity** — Oracle's 4 OCPU/24GB ARM forever-free là deal tốt nhất market; AWS chỉ free 12 tháng, ARM 2 vCPU/2GB

### Neutral

- **Cloudflare DNS/CDN unchanged** — đã chọn Cloudflare cho DNS (ADR-018) + CDN (GAP-371 OPEN); không phụ thuộc cloud provider, tự do migrate
- **Terraform module pattern preserved** — `infrastructure/terraform-aws/` đã có nền móng từ Wave trước; Phase 3 migration sẽ tạo `infrastructure/terraform-vn-cloud/` paralleling current AWS structure
- **Existing AWS-centric runbooks already shipped** — `dns-setup-runbook.md` + `email-ses-setup-runbook.md` + `secrets-management-runbook.md` (Wave 33) — không cần viết lại

## Alternatives Considered

### Alternative A: Retry Oracle Cloud (3 lần với checklist tightened)
**Pros:** ARM Always Free 24GB RAM + VN data residency compliant
**Cons:** Reject rate ~50% Vietnam 2024+; permanent ban risk sau 3 fails; account-prep blocking Phase 1 timeline
**Rejected because:** 1 fail rồi (2026-05-07); thử 2 lần nữa = 2-7 ngày stuck + ban risk; không phù hợp 7-tuần PDPL countdown

### Alternative B: Hybrid AWS (SES/ECR/Secrets) + VN Cloud (Viettel/VNG/FPT) cho data
**Pros:** Compliant ngay từ Phase 1 BETA; reduce Phase 3 migration cost
**Cons:** 2 cloud providers = 2 account prep + 2 billing + 2 IAM + 2 monitoring; Viettel/VNG/FPT free tier không tồn tại (paid từ ngày 1, ~$30-50/tháng minimum); learning curve VN cloud APIs khác AWS
**Rejected because:** "Free tier only" constraint user-explicit 2026-05-07 + solo-dev complexity budget

### Alternative C: AWS Singapore + Cloudflare WARP exit nodes ở VN
**Pros:** Latency tương đương VN-resident
**Cons:** Cloudflare WARP exit nodes không phải "data localization" theo Decree 53 (data lưu trữ vẫn ở SG); compliance debt giống Option A; thêm complexity
**Rejected because:** Compliance không khá hơn Option A nhưng chi phí cao hơn

### Alternative D: Wait Oracle 30 ngày + retry với phone/email khác
**Pros:** Có thể work; ARM Always Free
**Cons:** Block Phase 1 timeline 30 ngày; PDPL deadline crunches; user lose momentum
**Rejected because:** Time-sensitive Phase 1 BETA window không cho phép

### Alternative E: Self-hosted VPS (Vultr/DigitalOcean Singapore $6-12/tháng)
**Pros:** No KYC issues; full control; cheap
**Cons:** Vi phạm "Free tier only" constraint; manual backup/HA/security; không có managed Postgres/SES
**Rejected because:** Free tier constraint + ops overhead solo-dev

## Implementation Notes

### 1. Migration sequence

| Step | Owner | When |
|---|---|---|
| Archive Oracle artifacts | this PR | 2026-05-07 |
| Update `deployment-strategy.md` AWS-primary | this PR | 2026-05-07 |
| Update CLAUDE.md infra structure note | this PR | 2026-05-07 |
| GAP-394 drop Oracle account walkthrough | this PR | 2026-05-07 |
| AWS account creation (user) | user | T+1-3 ngày |
| AWS terraform completeness audit gap | follow-up gap | After Wave 35 |
| First production deploy AWS | release-1 plan | T+5-7 tuần |

### 2. Compliance acceptance trail

Document trong `release-1-plan-2026.md` Phase 3 trigger gate update:
> Compliance: counsel review with explicit scope mention "Phase 1 BETA deployed AWS Singapore (`ap-southeast-1`); data localization compliance debt acknowledged; Phase 3 GA trigger requires counsel approval OR data-layer migration to VN cloud."

PDPL consent form (Bucket B GAP-385) extend to mention "Hạ tầng cloud: AWS Singapore (`ap-southeast-1`)" trong terms.

### 3. Free Tier inventory + caps

| Service | Free Tier | KiteHub use | Cap |
|---|---|---|---|
| EC2 t4g.small | 750h/month × 12mo | 1× host all backend services + Redis + RabbitMQ | <80% RAM, <60% CPU sustained |
| RDS db.t3.micro | 750h/month × 12mo | Postgres primary | <15GB storage, <70% connections |
| S3 | 5GB storage, 20k GET, 2k PUT × 12mo | MinIO replacement (object storage) | <4GB total |
| SES | 62k outbound emails/month forever | Transactional email | <50k/tháng |
| CloudWatch logs | 5GB ingestion × 12mo | Service logs | Aggressive retention 7 days |
| Lambda | 1M requests + 400k GB-seconds forever | Future serverless | N/A Phase 1 |
| SNS/SQS | 1M publishes + 1M requests × 12mo | RabbitMQ replacement candidate | Optional Phase 1.5 |
| Cloudflare (free tier) | Unlimited DNS + CDN + DDoS | Edge + DNS | N/A |

**RAM partitioning t4g.small 2GB:**
- KiteHub Gateway: 256MB heap
- KiteHub Subscription: 256MB heap
- KiteHub Branding: 384MB heap (AI client memory)
- KiteHub Email: 192MB heap
- KiteHub Admin: 256MB heap
- KiteHub Platform: 192MB heap
- Frontend (Next.js production): 256MB
- Redis: 256MB cap
- RabbitMQ: 256MB cap
- OS + Docker overhead: 256MB
- **Total: ~2.5GB → OVER budget**

→ Mitigation: SOME services consolidate trong 1 JVM (Spring Boot multi-context) HOẶC drop RabbitMQ → SQS+SNS HOẶC merge non-critical services. **Decided trong follow-up gap (AWS terraform completeness audit).**

### 4. Rollback plan

Nếu AWS Singapore deploy fail Phase 1 BETA hoặc compliance complaint trước Phase 3:
- Restore Oracle archived artifacts từ `documents/07-archived/oracle-deploy-2026/` (sẵn để revive)
- Try Oracle Cloud signup lần 2 với 24h cooldown + checklist tightened (mobile data + new email + physical Visa card từ major bank + Singapore region SG-SIN)
- Migration AWS → Oracle: `pg_dump` Postgres, `aws s3 sync` → Oracle Object Storage, terraform apply Oracle module

### 5. EKS vs ECS Fargate decision (deferred)

Free tier favor **ECS Fargate** vì EKS control plane charge $73/tháng KHÔNG có free tier. Decision tracked trong follow-up gap; this ADR scope = Oracle→AWS switch.

### 6. Monitoring / success criteria

- Phase 1 BETA launch trên AWS Singapore: 0 P0 incidents 2 tuần đầu
- t4g.small CPU sustain <60% với 20 tenants
- RDS connection pool <70% utilization
- SES bounce rate <2% (quota maintenance)
- Phase 3 trigger gate: counsel sign-off compliance OR data-layer migrated to VN cloud — track trong `release-1-plan-2026.md`

## References

- Pattern used: ADR-015 (AWS Agent Plugins evaluation — defer Q3 2026)
- Related ADRs: ADR-018 (DNS/registrar — Cloudflare); ADR-022 (Alertmanager strategy)
- Related rules: `.claude/rules/release-deploy-standard.md` v1.0.0 §3 PRE-RELEASE/PATCH/MINOR/MAJOR checklist
- Related gaps: GAP-103 (deployment-strategy DONE 2026-04-18 — supersedes primary platform); GAP-369/370/379 (Wave 33 already AWS-centric runbooks); GAP-394 (account-prep checklist — drop Oracle walkthrough)
- Compliance: Luật An ninh mạng 2018 Điều 26; Nghị định 53/2022/NĐ-CP §26; PDPL 2023 Điều 11 (consent — Bucket B GAP-385 pipeline)
- Cost reference: AWS Free Tier https://aws.amazon.com/free/ (12-month + always-free tiers)
- VN cloud alternatives Phase 3: Viettel Cloud (https://viettelidc.com.vn/), VNG Cloud (https://vngcloud.vn/), FPT Smart Cloud

## Log

- **2026-05-07** — ACCEPTED. Trigger: Oracle Cloud Always Free signup fail (generic error common cho VN users 2024+). User chose Option A (AWS Singapore thuần) + Free Tier only sau khi flag compliance debt accepted as Phase 1 invite-only scope. Solo-dev acting CTO sign-off; counsel review queued Phase 3 GA trigger gate per `release-1-plan-2026.md`. Same-PR landing: archive `infrastructure/terraform-oracle/` + 2 docker-compose-oracle + Vietnamese Oracle guide; rewrite `deployment-strategy.md` (Phase 1 = AWS-primary); update CLAUDE.md folder structure note; update GAP-394 (drop Oracle prep). Follow-up gaps deferred: AWS terraform completeness audit (EKS vs ECS Fargate decision); RAM partitioning on 2GB t4g.small (services consolidation OR SQS replace RabbitMQ).
