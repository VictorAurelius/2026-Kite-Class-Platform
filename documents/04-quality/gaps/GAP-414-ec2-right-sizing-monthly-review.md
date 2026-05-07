# GAP-414: EC2 Right-Sizing Monthly Review

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Infrastructure / Cost / FinOps
**Found:** 2026-05-07 (Wave 37 — Layer 5)
**Affects:** Continuous cost optimization sau Phase 1 BETA stable

## Problem

EC2 sizing chốt Architecture B based on ESTIMATE. Sau 1 tháng vận hành, actual CloudWatch utilization có thể cho thấy:
- Over-provisioned: t3.medium average 30% CPU → downsize t3.small (-50% cost)
- Under-provisioned: t3.small CPU steal >5% → upsize t3.medium

## Proposed Fix

Monthly cron task (1st of month) chạy AWS Compute Optimizer + Cost Explorer report:
1. AWS Compute Optimizer recommendation cho mỗi EC2 instance
2. CloudWatch CPU + Memory + Network utilization 30-day summary
3. Action items: downsize / upsize / RI commit / Spot eligible
4. Document monthly review report `documents/04-quality/cost-reports/YYYY-MM.md`

Skill: `quality/cost-review/SKILL.md` (NEW — Wave 38+ candidate, defer).

Phase 1 BETA: manual review acceptable. Phase 1.5+: automated cron.

## Acceptance Criteria

- [ ] First report `documents/04-quality/cost-reports/2026-06.md` after 1 month Phase 1 BETA running
- [ ] Compute Optimizer recommendation captured
- [ ] CloudWatch utilization summary
- [ ] Action items actioned next month (downsize/upsize/RI)
- [ ] Document review cadence (monthly Phase 1, quarterly Phase 1.5+)

## Related

- GAP-411 sizing matrix
- GAP-413 (cost monitoring — feeds into right-sizing)
- AWS Compute Optimizer (free service)
- AWS Cost Explorer
