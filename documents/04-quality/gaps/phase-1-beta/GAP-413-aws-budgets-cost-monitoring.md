# GAP-413: AWS Budgets Cost Monitoring + Alerting

**Status:** 🟡 PARTIAL 2026-05-07 (policy doc + runbook shipped; Terraform provisioning tracked GAP-395 Bucket A)
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

- [ ] 3 AWS Budgets alarms provisioned (Terraform via GAP-395) — **tracked GAP-395 Bucket A — Terraform implementation**
- [x] Email recipient: vannkite@outlook.com (on-call rotation N/A solo-dev)
- [ ] Tag policy enforced via Terraform `default_tags` — **spec ready §3, Terraform impl GAP-395**
- [ ] ECR lifecycle policy (delete untagged + >7d old) — **spec ready §4, Terraform impl GAP-395**
- [x] Document `documents/05-guides/deploy/aws-cost-monitoring.md` (runbook on alarm)

## Related

- GAP-411 (sizing matrix — $80 threshold matches B+buffer)
- GAP-412 (Activate credit — depletion alarm dependency)
- GAP-414 (right-sizing review monthly)
- GAP-395 (Terraform Bucket A — provisions alarms + Tag policy + ECR lifecycle)

## Log

- **2026-05-07** — PARTIAL. Cost monitoring policy doc + 3-alarm spec + runbook + Tag policy + ECR lifecycle policy shipped (Wave 37 Bucket E). Terraform provisioning deferred GAP-395 Bucket A — same wave parallel agent. Status flips DONE post-Bucket-A merge + alarm test trigger.
- **2026-05-08 (Wave 43 Bucket C — admin sweep)** — Note: GAP-446 (EventBridge stop/start scheduler) + GAP-447 (right-size compute) shipping concurrent in Wave 43 Buckets A/B reduce projected burn rate from $80/mo policy threshold to **$45-55/mo target** per `wave-2026-05-08-43-cost-discipline.md` §3. AWS Budgets alarm provisioning still tracked GAP-395 Bucket A (Terraform implementation). No scope change to this gap; status remains 🟡 PARTIAL pending Terraform apply + alarm test trigger. Combined Wave 43 cost-discipline outcome will be verified against §3 alarm 1 ($80 monthly) once GAP-395 lands.
