# GAP-414: EC2 Right-Sizing Monthly Review

**Status:** 🟡 PARTIAL 2026-05-07 (template + cadence doc shipped; first actual report deferred ~2026-06-09 post-deploy)
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

- [ ] First report `documents/04-quality/cost-reports/2026-06.md` after 1 month Phase 1 BETA running — **deferred ~2026-06-09 post-deploy + 30 days operational**
- [x] Compute Optimizer recommendation captured (template §2.3)
- [x] CloudWatch utilization summary (template §2.4)
- [x] Action items actioned next month (downsize/upsize/RI) (template §4 + §5)
- [x] Document review cadence (monthly Phase 1, quarterly Phase 1.5+) (template §1)

## Log

- **2026-05-07** — PARTIAL. Template `documents/04-quality/cost-reports/2026-06-template.md` shipped với 8 sections (executive summary / cost breakdown / optimizer / utilization / credit trend / anomaly / right-sizing log / action items / phase transition check). First actual fill ~2026-06-09 sau Phase 1 BETA deploy + 30 days operational. Wave 37 Bucket E.

## Related

- GAP-411 sizing matrix
- GAP-413 (cost monitoring — feeds into right-sizing)
- AWS Compute Optimizer (free service)
- AWS Cost Explorer
