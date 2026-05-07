# GAP-412: AWS Activate Founders Pack Application ($1k credit)

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 v0.9.0-beta
**Domain:** Infrastructure / Cost / Business
**Found:** 2026-05-07 (Wave 37 — Layer 5)
**Affects:** Phase 1 BETA + Phase 1.5 PAID early effective $0 cost

## Problem

Architecture B = $72/mo Yr1, $89 Yr2+. Solo dev pre-revenue → cash burn ~$216 Phase 1 BETA (3mo). AWS Activate Founders Pack offers $1,000 credit cho startup self-applied → cover 13.9 tháng Phase 1.

## Proposed Fix

Apply AWS Activate Founders Pack:
1. Eligibility: pre-revenue startup, có pitch deck (ngay cả 1-page concept), không yêu cầu investor
2. URL: https://aws.amazon.com/activate/founders-pack
3. Submit: company name (KiteClass), description 100-200 từ, AWS account ID, contact email
4. Approval: typical 1-2 tuần
5. Credit applied: dashboard `aws.amazon.com/billing/home#/credits`

Optional enhancement: với investor verified → $5,000 credit (69 tháng).

Document credit usage policy:
- Reserve credit cho production EC2 (highest cost item)
- KHÔNG dùng cho experimental/learning
- Set up AWS Budgets alarm (GAP-413) khi credit <20% remaining

## Acceptance Criteria

- [ ] Application submitted (screenshot confirmation)
- [ ] Pitch deck draft `documents/00-brd/kite-pitch-deck.md` (1-page minimum)
- [ ] Credit approval received → balance reflected billing dashboard
- [ ] Document `documents/05-guides/deploy/aws-activate-credit-policy.md` (usage rules)
- [ ] Budget alarm wired (GAP-413 dependency)

## Related

- GAP-411 (sizing matrix — credit cover Phase 1+1.5 early)
- GAP-413 (AWS Budgets alarm for credit depletion)
- ADR-025 AWS Singapore decision
