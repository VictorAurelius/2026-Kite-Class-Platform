# GAP-413: AWS Budgets Cost Monitoring + Alerting

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Infrastructure / Cost / FinOps
**Found:** 2026-05-07 (Wave 37 — Layer 5)
**Affects:** Cost overrun protection + Activate credit depletion alarm

## Problem

Không có cost monitoring → có thể vượt budget Architecture B mà không biết. Risk: spike traffic → EC2 autoscale → cost surge; hoặc Activate credit cạn → bị charge full giá.

## Proposed Fix

3 AWS Budgets alarms (free service):

1. **Monthly cost alarm** — actual + forecast > $80/mo (Architecture B + buffer)
2. **Credit depletion alarm** — Activate credit balance < 20% remaining → email
3. **Per-service tag alarm** — `Service` tag: kitehub-branding, kitehub-subscription, etc. → identify cost outlier

Notification: email primary + Slack webhook (optional Phase 2).

Tag policy: Terraform `default_tags` apply `Service`, `Environment`, `Phase` tags to ALL AWS resources.

ECR cleanup policy: `lifecycle_policy.json` auto-delete untagged + old version images sau 7 ngày (free 500MB → tránh tràn).

## Acceptance Criteria

- [ ] 3 AWS Budgets alarms provisioned (Terraform via GAP-395)
- [ ] Email recipient: vannkite@outlook.com + on-call rotation
- [ ] Tag policy enforced via Terraform `default_tags`
- [ ] ECR lifecycle policy (delete untagged + >7d old)
- [ ] Document `documents/05-guides/deploy/aws-cost-monitoring.md` (runbook on alarm)

## Related

- GAP-411 (sizing matrix — $80 threshold matches B+buffer)
- GAP-412 (Activate credit — depletion alarm dependency)
- GAP-414 (right-sizing review monthly)
