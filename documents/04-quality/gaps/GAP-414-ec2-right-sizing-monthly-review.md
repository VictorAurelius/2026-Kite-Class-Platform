# GAP-414: EC2 Right-Sizing Monthly Review

**Status:** 🟢 DONE 2026-05-15 — apply-ready terraform + Lambda + runbook shipped; apply deferred to coordinator (Wave 84 Bucket G)
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

- [x] First report `documents/04-quality/cost-reports/2026-06.md` after 1 month Phase 1 BETA running — **template shipped; actual fill deferred ~2026-06-09 post-deploy + 30 days operational (organic data accumulation, not blocked by this PR)**
- [x] Compute Optimizer recommendation captured (template §2.3)
- [x] CloudWatch utilization summary (template §2.4) — Lambda `kitehub-ec2-cost-report` fetches per-instance 30-day avg CPU + mem (when CWAgent available)
- [x] Action items actioned next month (downsize/upsize/RI) (template §4 + §5) — runbook `ec2-cost-review.md` §6/§7 codifies terraform var-change procedure
- [x] Document review cadence (monthly Phase 1, quarterly Phase 1.5+) (template §1) — EventBridge cron `cron(0 8 1 * ? *)` 1st of each month 08:00 UTC

## Log

- **2026-05-15** — DONE (Wave 84 Bucket G). PR `wave/84-bucket-g-ec2-rightsizing`. Shipped:
  - `infrastructure/terraform-aws/cost-monitoring.tf` — SNS topic `kitehub-cost-alerts` (email subscribe vannkite@outlook.com) + 3× CloudWatch alarm `kitehub-{kh-backend,kc-app,kc-app-fe}-low-cpu-7d` (avg CPU ≤20% over 7 days) + Lambda `kitehub-ec2-cost-report` (Python 3.12, EventBridge cron 1st-of-month 08:00 UTC) + IAM least-priv role + log group
  - `infrastructure/terraform-aws/lambdas/ec2-cost-report/handler.py` — fetches EC2 instances tagged `Project=Kite`, queries Cost Explorer (us-east-1 endpoint) for last-month cost by `INSTANCE_TYPE`, CloudWatch 30-day avg CPU/mem per instance, builds HTML digest table với Downsize/OK/Upsize recommendations, publishes to SNS
  - `infrastructure/terraform-aws/lambdas/ec2-cost-report/test_handler.py` + `requirements.txt` — unit tests cho `recommend()` + `format_digest()` (13 cases — boundary CPU 20/60, memory pressure override, missing cost data, threshold footer, etc.); ran locally PASS 13/13 với boto3 stub
  - `documents/05-guides/operations/ec2-cost-review.md` — Vietnamese narrative runbook §3 digest interpretation + §4 alarm adhoc handling + §6/§7 terraform downsize/upsize procedure (serialized với `concurrent-production-mutation-ops.md` §3.1, pre-mutation audit per §3, workflow_dispatch trigger per `release-deploy-standard.md` §9)
  - `documents/04-quality/audits/aws-verification/2026-05-15-wave-84-bucket-g-ec2-rightsizing-plan.md` — pre-apply audit per `pre-mutation-state-check.md` §3 + §1.5 cross-reference matrix (9 IAM actions × resource × caller verified; zero bugs); 15 additive resources, 0 destroy, 0 replace; cost projection ~$0.31/month marginal
  - Apply deferred to coordinator via `terraform-apply.yml` workflow_dispatch per `agent-aws-access.md` §4.3 + `release-deploy-standard.md` §9 (agent-initiated apply BANNED). Audit artifact + runbook + Lambda code all apply-ready
  - First report fill (`documents/04-quality/cost-reports/2026-06.md` from template) tracked as organic-data-accumulation task, not a PR blocker (Lambda needs 30+ days of CloudWatch data post-apply)
- **2026-05-07** — PARTIAL. Template `documents/04-quality/cost-reports/2026-06-template.md` shipped với 8 sections (executive summary / cost breakdown / optimizer / utilization / credit trend / anomaly / right-sizing log / action items / phase transition check). First actual fill ~2026-06-09 sau Phase 1 BETA deploy + 30 days operational. Wave 37 Bucket E.

## Related

- GAP-411 sizing matrix
- GAP-413 (cost monitoring — feeds into right-sizing)
- GAP-447 (memory alarms — sister safety-net signal for upsize)
- AWS Compute Optimizer (free service)
- AWS Cost Explorer
- Wave 84 plan: `documents/03-planning/waves/wave-2026-05-15-84-ops-observability-runbooks.md` §3 Bucket G
- Runbook: `documents/05-guides/operations/ec2-cost-review.md`
- Pre-apply audit: `documents/04-quality/audits/aws-verification/2026-05-15-wave-84-bucket-g-ec2-rightsizing-plan.md`
