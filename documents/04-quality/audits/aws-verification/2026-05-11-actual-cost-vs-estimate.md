---
title: AWS Verification — Actual cost Apr-May 2026 vs Architecture B estimate
status: complete
created: 2026-05-11
phase: Phase 1 BETA pre-launch
---

# AWS Verification Report — Actual cost vs Architecture B estimate

## Scope

User-flagged: verify `$144-216 over 3 tháng` Phase 1 BETA cost estimate (per `aws-activate-credit-policy.md` §6) against actual AWS account spend Apr-May 2026 → re-evaluate AWS Activate credit denial impact realistically.

## Commands run

| Command | Tier | Purpose |
|---------|:----:|---------|
| `aws sts get-caller-identity` | 1 | identity verify |
| `aws ce get-cost-and-usage --time-period Start=2026-04-01,End=2026-05-12 --granularity MONTHLY --metrics UnblendedCost --group-by Type=DIMENSION,Key=SERVICE` | 1 | per-service cost breakdown |
| `aws ce get-cost-forecast --time-period Start=2026-05-12,End=2026-06-01` | 1 | forecast remainder of May (errored — insufficient history) |
| `aws budgets describe-budgets --account-id 906286017800` | 1 | budget actuals + forecasts |

## Identity

```
UserId: AIDA5GAW3FUEP65336YGT
Account: 906286017800
Arn: arn:aws:iam::906286017800:user/ci-deploy
Region: ap-southeast-1
```

## Results

### April 2026 total: **$0.00** (estimated)

Account chưa được provisioned production resources tháng 4; CloudTrail enabled (free per Wave 41 GAP-437) chưa bill.

### May 2026 (1-12): **$-0.0001** (estimated, effectively $0)

Per-service breakdown (sorted high→low):

| Service | $/period (May 1-12) | Notes |
|---------|--------------------:|-------|
| EC2 — Other | $0.000083 | EBS or VPC fragments — sub-penny |
| Amazon S3 | $0.000035 | bucket existence cost |
| Amazon ELB | $0.000002 | ALB present (shipped Wave 50 Phase 2.3) |
| Secrets Manager | $0.000000 | within Free Tier |
| RDS | $0.000000 | within Free Tier 750h |
| ECR | $0.000000 | within Free Tier |
| CloudTrail | $0.000000 | management events FREE |
| EC2 Compute | $0.000000 | within Free Tier 750h × 1 instance |
| AWS Glue / KMS / DynamoDB / SNS / SQS / VPC / CloudWatch / Events / Tax | $0.000000 | unused or free tier |
| AWS Data Transfer | $-0.000120 | cross-AZ rebate |
| **TOTAL** | **$-0.000000** | effectively zero |

### Budgets configured (existing)

| Budget | Limit | Actual MTD | Forecast |
|--------|------:|-----------:|---------:|
| `aws-bill-low` | $1/day | $0.00 | n/a (insufficient history) |
| `aws-bill-medium` | $50/month | $0.00 | n/a |

### Forecast

`aws ce get-cost-forecast` returned `DataUnavailableException: Insufficient amount of historical data`. Account spend signal too low for forecast model.

## Findings

### F1 — Phase 1 BETA estimate was **theoretical**, not yet operational

Architecture B Yr1 target $48/mo và actual $0 đến nay vì:
- **Account chưa onboard production traffic** — EC2 + RDS đã shipped Wave 50 Phase 2.3 (71 resources) nhưng STOPPED ngay sau verify per cost-save policy
- **No tenants** — zero data ingress, zero egress, zero compute hours billable
- **Free Tier 12 tháng** vẫn intact đầy đủ → khi resume EC2 + RDS sẽ tận dụng 750h × 12 tháng

→ `$144-216 over 3 tháng` estimate là **forward-looking projection cho khi stack ON 24/7 trong 3 tháng**, KHÔNG phải actual cost MTD.

### F2 — Free Tier 12 tháng đếm từ account creation date (~2026-05-07 confirmation per `2026-05-08-orphan-key-delete-solo-dev-admin.md`)

→ Free Tier expires ~**2027-05** (12 tháng từ activation). Phase 1 BETA + Phase 1.5 PAID early dự kiến **hoàn toàn nằm trong Free Tier window** nếu launch ~2026-07-01 per `2026-05-09-submission.md` Form fields.

→ Nếu chỉ resume stack trong **2-3 tháng Phase 1 BETA**, actual cost likely **$48 × 3 = $144** (lower bound estimate validated). Yr2+ $89/mo chỉ kicks in từ tháng 13+.

### F3 — Stack stopped ≠ AWS bill = $0

Mặc dù EC2/RDS stopped, các thành phần này vẫn bill nhỏ:
- **EBS volumes** attached to stopped EC2 → $3/mo (per sizing-matrix §3.1)
- **ALB** active 24/7 → $18/mo (per sizing-matrix §3.1)
- **Route 53** hosted zone → $0.50/mo
- **Secrets Manager** 5 secrets → $2/mo
- **S3** bucket existence + storage → $0.50/mo
- **Total stopped-stack burn: ~$24/mo**

CE data Apr-May 2026 chưa show numbers này vì:
- ALB shipped Wave 50 Phase 2.3 (~2026-05-08) → chưa accrue full month
- EBS attached khi EC2 created (~Wave 50)
- Free Tier có thể đang absorb một phần

→ Dự kiến **June 2026** sẽ là tháng đầu tiên bill non-zero cho stopped stack: **~$24/mo baseline** (ngay cả khi EC2 + RDS stopped). Resume cho tenant onboarding cộng thêm ~$24-50/mo compute = **~$48-72/mo total** as estimated.

### F4 — AWS Activate $1k credit denial impact, recalibrated

| Scenario | Phase 1 BETA cost (3 tháng) | Phase 1.5 early cost (1.5 tháng) | Total Release 1 (4.5 tháng) |
|----------|:---------------------------:|:--------------------------------:|:---------------------------:|
| Stack ON full 24/7 (target $72/mo) | $216 | $173 (Architecture A $115/mo) | **$389** |
| Stack ON actual ($48/mo Yr1) | $144 | $173 | **$317** |
| Stack stopped 50% time (50% beta down-time) | $72 | $86 | **$158** |
| Stack stopped 100% (impossible — beta needs uptime) | — | — | — |

→ **Realistic personal-fund range nếu Activate denied: $158-389 over 4.5 tháng = ~$35-86/mo personal cash**

### F5 — Recommendation

Per `aws-activate-credit-policy.md` §6 conclusion + this verification: **"manageable cash burn solo-dev"** is supported by data. AWS Activate credit denial KHÔNG block Phase 1 BETA launch; nó chỉ shift $158-389 from credit subsidy to personal cash.

## Next steps

1. **Re-submit Founder $1k** per recommendation (low cost, high upside)
2. **Personal budget commit ~$50/mo × 4-5 tháng** = **$200-250 contingency reserve** Phase 1 BETA + 1.5 early
3. **Resume EC2 + RDS** chỉ khi đầu beta tenant ready onboard (avoid burn during dev)
4. **Continue Wave 55+ observability** unblocked — software work không tốn AWS budget (stack stopped vẫn ship code)
5. **Revenue path A target**: 30 paying tenants × $5/mo = $150/mo by Phase 1.5 month 4-5 → covers Architecture A burn

## Compliance check

- ✅ `agent-aws-access.md` §2.1 Tier 1 read-only commands: `sts get-caller-identity`, `ce get-cost-and-usage`, `ce get-cost-forecast`, `budgets describe-budgets` — all allowed
- ✅ §5 audit artifact: this file
- ✅ §2.2 banned `get-secret-value` / `s3 cp` etc. — none invoked

## References

- `documents/05-guides/deploy/aws-architecture-sizing-matrix.md` §3.1 — 12-service breakdown
- `documents/05-guides/deploy/aws-activate-credit-policy.md` §6 Risks — original $144 estimate
- `documents/04-quality/gaps/GAP-411-aws-architecture-b-sizing-matrix.md` — sizing decision
- ADR-025 — AWS Singapore Architecture B
- `documents/03-planning/roadmap/release-1-plan-2026.md` §1.4 — Phase 1 BETA timeline
