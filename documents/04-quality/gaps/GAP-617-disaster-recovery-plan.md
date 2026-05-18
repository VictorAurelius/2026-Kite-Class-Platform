# GAP-617 — Disaster recovery plan (multi-region OR backup mechanism + RTO/RPO targets)

**Status:** 🔵 OPEN
**Priority:** 🟢 P3
**Domain:** DevOps
**Found:** 2026-05-18 (Wave 92 Bucket E meta backlog filing — carry-forward từ Wave 90 ROADMAP §🚀 long-term P2/P3 follow-ups line 158)
**Affects:** Long-term BCDR posture (Business Continuity + Disaster Recovery); không phải Phase 1 BETA blocker (chưa có tenant + production data); Phase 1.5+ candidate gated trên 5-10 beta tenant live + business value protected

## Problem

Hiện tại không có DR plan formal cho production stack. Sự cố GAP-612 (AWS account suspension 2026-05-17) đã expose blind-spot lớn:
- Toàn bộ infra single-region `ap-southeast-1` (AWS Singapore)
- Khi account suspended → toàn bộ EC2 + RDS + S3 + ECR đi cùng
- Không có cross-region replica
- Không có cross-account backup (S3 backup vẫn nằm trong cùng account 906286017800)
- RTO (Recovery Time Objective) không document
- RPO (Recovery Point Objective) không document
- Restore drill last successful = N/A (GAP-257 P0 carry-forward)

Outside-in audit (từ góc nhìn tenant onboarded): nếu KiteHub mất data hoặc downtime >24h, tenant edu (trung tâm gia sư / trường K-12) có legal risk (PDPL Art 11 data integrity + Luật An ninh mạng 2018) + business risk (mất class schedule + grade record + attendance).

Cụ thể context Wave 90 ROADMAP entry "Long-term follow-ups: P2 uptime monitoring + P2 DR plan" — gap này codify P3 DR portion (deprioritize từ Wave 90 P2 thành P3 vì chưa có tenant onboarded; nâng lên P2/P1 khi 5+ tenant live).

## Root Cause

Solo-dev mode + Phase 1 BETA scope ưu tiên launch ready (chưa có production data đáng protect). DR formal plan defer vì:
- AWS Free Tier không cover multi-region (cost burst $50+/tháng nếu enable cross-region replica)
- Chưa có tenant data → restore drill mock data only (GAP-257 đã file P0 carry-forward)
- Cognitive load cao — DR plan requires RTO/RPO business sign-off + tabletop exercise
- Trigger event GAP-612 chỉ vừa surface 2026-05-17 → lesson-learned đang được rút

Phase 1 BETA-acceptable strategy: rely on AWS RDS automated snapshot (7-day retention default) + EC2 AMI weekly snapshot + S3 versioning. Acceptable rủi ro cho 0-5 tenant scale.

## Proposed Fix

### Phase 1 — Document current state baseline (~2h)

Create `documents/05-guides/operations/disaster-recovery-plan.md`:

- **Asset inventory** — table list mọi production resource (EC2, RDS, S3 bucket, ECR repo, Secrets, ALB, CloudFront/CF, DNS records)
- **Current backup mechanism per asset:**
  | Asset | Current backup | Retention | Recovery procedure |
  |---|---|---|---|
  | RDS `kitehub-postgres` | Automated snapshot daily | 7 days | RDS console restore-to-point-in-time |
  | EC2 `kh-backend` / `kc-app` | AMI weekly snapshot | 30 days | Launch new EC2 from AMI |
  | S3 buckets | Versioning enabled | Indefinite | S3 console version restore |
  | ECR images | Image tag retention | 100 images | ECR push từ git tag rebuild |
  | Secrets Manager | Version history | Indefinite | Rotate API console |
  | Terraform state | S3 backend + DynamoDB lock | Versioning | `terraform refresh` |
- **Current RTO/RPO baseline:**
  - RTO (Recovery Time): unknown — measure via Phase 2 tabletop drill
  - RPO (Recovery Point): ~24h (RDS snapshot daily); ~7 days (EC2 AMI weekly)
- **Gap analysis vs target:** target RTO <4h, RPO <1h for Phase 1.5+ (5+ tenant); current baseline does NOT meet target

### Phase 2 — Tabletop exercise (~4h, defer Phase 1.5+)

Simulate 4 disaster scenarios trong session:
1. **AWS account suspension** (replay GAP-612) — RTO measure restore-via-new-account path
2. **RDS data corruption** — restore từ snapshot 24h trước
3. **EC2 instance terminate** — relaunch từ AMI
4. **Region outage** (ap-southeast-1 down) — không có cross-region failover; documented as gap

Output: tabletop report + measured RTO/RPO + gap-to-target table.

### Phase 3 — Cross-account backup (~1 ngày)

Setup cross-account S3 backup:
- Create second AWS account "kitehub-dr-backup" (separate billing)
- Daily lifecycle rule: copy RDS snapshot + EC2 AMI + S3 critical buckets → DR account S3
- Cost estimate: ~$5-15/tháng S3 storage (Phase 1.5+ scale)
- Documented restoration procedure cho mỗi asset class

### Phase 4 — Cross-region read replica (deferred Phase 2+ scale)

RDS multi-AZ read replica trong `ap-southeast-2` (Sydney) — only khi 20+ tenant active justify cost. Cost: ~$30-50/tháng additional RDS instance.

### Phase 5 — DR runbook + quarterly drill (~ongoing)

Create `documents/05-guides/operations/runbooks/dr-restore-procedure.md`:
- Step-by-step restore mỗi asset class
- Communication template tới beta tenant ("đang khôi phục, ETA 4h")
- Quarterly drill: 1 random scenario from Phase 2 tabletop, measure actual RTO vs target

## Acceptance Criteria

- [ ] `disaster-recovery-plan.md` Phase 1 baseline document shipped (asset inventory + backup mechanism + RTO/RPO baseline)
- [ ] Tabletop exercise 4 scenarios documented (Phase 2 — defer Phase 1.5+ khi có tenant)
- [ ] Cross-account backup setup (Phase 3 — defer Phase 1.5+ gated on cost-benefit ROI ≥ 5 tenant)
- [ ] DR restore runbook shipped với step-by-step mỗi asset class
- [ ] Quarterly drill cadence established (4 drill/năm) — log result trong `documents/04-quality/audits/dr-drills/`
- [ ] RTO measured ≤ 4h actual baseline
- [ ] RPO measured ≤ 1h actual baseline (post-Phase-3 cross-account backup)
- [ ] ROADMAP entry "Long-term follow-ups: P2 DR plan" flipped DONE (sau Phase 1+2 ship)
- [ ] Cross-link với `incident-response-runbook.md` §X DR procedure section

## Related

- **Wave 92 plan:** [`documents/03-planning/waves/wave-2026-05-18-92-pre-tenant-cluster.md`](../../03-planning/waves/wave-2026-05-18-92-pre-tenant-cluster.md) §3 Bucket E
- **Sister gaps:** GAP-616 (uptime monitoring external) + GAP-618 (AWS Health daily check) — long-term observability cluster
- **Trigger event:** GAP-612 AWS account suspension 2026-05-17 — expose single-account-single-region blast radius
- **Carry-forward gap:** GAP-257 (P0 restore drill — Wave 84+ carry-forward, blocks Phase 1 BETA gate 80) — Phase 2 tabletop sẽ help close GAP-257
- **Wave 90 ROADMAP §🚀:** line 158 "Long-term follow-ups: P2 uptime monitoring + P2 DR plan" — gap formalize P3 DR portion (deprioritize từ P2 vì chưa tenant)
- **Rule:** `audit-to-gap-pipeline.md` §3 (gap template); `output-review-mandate.md` §3 (ops-readiness audit standard); `release-deploy-standard.md` §4.4 (rollback execution — sister mechanism)
- **Compliance:** PDPL Art 11 data integrity + Luật An ninh mạng 2018 + Decree 53/2022/NĐ-CP data localization — DR plan formal cần thiết khi có tenant production data

## Log

- **2026-05-18:** Gap filed by Wave 92 Bucket E meta backlog filing per inside-out audit ROADMAP §"Long-term P2/P3 follow-ups" carry-forward từ Wave 90 line 158. Trigger event = GAP-612 AWS suspension expose single-account single-region blast radius. Deprioritize từ P2 (Wave 90) thành P3 (Wave 92) vì chưa có tenant onboarded → business value protected = 0 hiện tại. Nâng priority lên P2/P1 khi 5+ tenant live. Defer implementation Wave 93+ hoặc Phase 1.5 gated trên tenant onboarding milestone.
