---
title: AWS Verification — Wave 82 Bucket B pre-apply state-check
status: complete
created: 2026-05-15
phase: phase-1-beta
wave: 82
gaps: [GAP-565, GAP-566, GAP-567]
---

# AWS Verification Report — Wave 82 Bucket B pre-apply

## Scope

Pre-mutation audit per `.claude/rules/pre-mutation-state-check.md` §3 trước khi `terraform apply dry_run=false` cho Wave 82 Bucket B (FE EC2 t3.small + 4 P0 mitigation infra).

**Mutation pending:** create 9 NEW resources cho `aws_instance.kc_app_fe` Wave 82 Bucket B.

**Initial plan run (run #25914266227, 2026-05-15 10:59 UTC) flagged DESTRUCTIVE cascade** — 3 existing EC2 instances would be replaced due to AMI drift. **Pivoted per `release-fix-retry-budget.md` §4** — pin AMI in `data.aws_ami.al2023` filter (PR #1400). Re-ran plan (run #25914464130, 2026-05-15 11:05 UTC) post-pin → CLEAN.

## Commands run (Tier 1 read-only per `agent-aws-access.md` §2.1)

```bash
# Active workflow check (concurrent mutation guard per concurrent-production-mutation-ops.md)
gh run list --status in_progress --limit 5
# Result: empty — no in-flight terraform/deploy ops

# Current EC2 state (live)
aws ec2 describe-instances --profile dev-admin --region ap-southeast-1 \
  --query 'Reservations[].Instances[].[Tags[?Key==`Name`].Value|[0],State.Name,InstanceId]'
# Result: kitehub-kh-backend running i-05d7af46d01436b96
#         kitehub-kc-app    running i-01ad56b0067d0213b

# Plan run #1 (pre-pin) — DESTRUCTIVE
gh workflow run terraform-apply.yml -f version=main -f confirm=APPLY -f dry_run=true
# Result: Plan: 12 to add, 3 to change, 3 to destroy
#   - aws_instance.kc_app MUST be replaced (AMI bump)
#   - aws_instance.kh_backend MUST be replaced (AMI bump)
#   - aws_lb_target_group_attachment.kh_backend[0] MUST be replaced (cascade)

# Plan run #2 (post-pin AMI to ami-04a8a2b994a2a7176)
gh workflow run terraform-apply.yml -f version=chore/wave-82-pin-ami-bucket-b-prep -f confirm=APPLY -f dry_run=true
# Result: Plan: 9 to add, 0 to change, 0 to destroy ✅
```

## Findings

### Real changes (must verify intent) — 9 NEW resources

| # | Resource | Action | Root cause | Risk |
|---|----------|--------|-----------|------|
| 1 | `aws_instance.kc_app_fe` | create | Wave 82 Bucket B FE EC2 t3.small ap-southeast-1 | NEW resource ~$15/mo recurring |
| 2 | `aws_security_group.kc_app_fe` | create | GAP-565 F6 — SG with ASCII descriptions per `aws-sg-description-ascii.md` | low — restricts ingress |
| 3 | `aws_iam_role.kc_app_fe` | create | IAM role least-priv (SSM Param read + CloudWatch put) | low |
| 4 | `aws_iam_instance_profile.kc_app_fe` | create | attach IAM role to EC2 | low |
| 5 | `aws_iam_role_policy.kc_app_fe_certbot_metrics` | create | inline policy: SSM cloudflare-api-token + CloudWatch FE metrics | low |
| 6 | `aws_iam_role_policy_attachment.kc_app_fe_cloudwatch` | create | attach CloudWatchAgentServerPolicy managed | low |
| 7 | `aws_iam_role_policy_attachment.kc_app_fe_ssm` | create | attach AmazonSSMManagedInstanceCore managed | low |
| 8 | `aws_cloudwatch_metric_alarm.kc_app_fe_memory_high` | create | GAP-566 F7 — memory >85% 5min alarm | low |
| 9 | `aws_cloudwatch_metric_alarm.kc_app_fe_cert_expiry` | create | GAP-567 F10 — cert <30d alarm | low |

### Phantom updates (no real change)

None — plan show `0 to change`. Earlier `cloudwatch_dashboard.phase_1_overview` in-place update + 2 existing memory alarm updates were artifacts of AMI replacement cascade; pin eliminates them.

### Verdict

✅ **SAFE to apply.** 9 NEW resources only, no replacement of existing infra. Wave 82 Bucket B prerequisites (GAP-565/566/567) embedded trong infra. GAP-568 BE CORS sweep audit (separate scope, gateway already allowlisted `kitehub.me`).

## Prior actions verified (per `audit-to-gap-pipeline.md` §2.8)

| Action | When | Where verified |
|--------|------|----------------|
| Backend deploy v0.9.0-beta-staging.16 | 2026-05-15 10:43 UTC | Run #25913661225 completed/success |
| Post-deploy smoke 12/14 PASS | 2026-05-15 10:50 UTC | F4 beta-status 200 + CORS 4/4 + routing fixes |
| Bucket B drafts PR #1398 merged | 2026-05-15 ~10:55 UTC | terraform-aws/ec2-kc-app.tf on main |
| AMI pin PR #1400 (THIS audit's predicate) | 2026-05-15 11:03 UTC | chore/wave-82-pin-ami-bucket-b-prep branch |

## Pending (this op)

| Action | Owner | Notes |
|--------|-------|-------|
| Merge PR #1400 (AMI pin) | user / auto | required pre-apply |
| **Concurrent op check** | Agent verification 2026-05-15 11:00 UTC | ZERO in-flight terraform/deploy. Safe. |
| `terraform apply dry_run=false` trigger | USER ACTION per `agent-aws-access.md` §4.3 + `terraform-apply-retry-reconfirm.md` §3 | $15-30/mo recurring cost commitment |

## Recommendations

1. **Merge PR #1400 first** (AMI pin lands on main; future plan runs stable).
2. **User explicit confirm** dry_run=false trigger — first user authorize cho Wave 82 Bucket B EC2 provisioning. Cost: ~$15/mo EC2 + $2/mo EBS + ~$0/mo IAM/CloudWatch = ~$17/mo recurring.
3. **Apply on `main`** branch (post-merge PR #1400) via `gh workflow run terraform-apply.yml -f version=main -f confirm=APPLY -f dry_run=false`.
4. **Post-apply verification** required:
   - `aws ec2 describe-instances` → kc_app_fe `running`
   - `aws ec2 describe-security-groups` → all rules có `Description` (per GAP-565 AC)
   - `aws cloudwatch describe-alarms` → 2 new alarms exist
   - Save post-apply audit artifact `2026-05-15-wave-82-bucket-b-post-apply.md`
5. **NEXT STEP** post-apply: SSM SendCommand bootstrap server (install nginx + Node 20 + PM2 + certbot DNS-01). Per `fe-self-host-runbook.md` §Bucket B server bootstrap.

## References

- Plan run #1: `gh run view 25914266227` (DESTRUCTIVE — rejected)
- Plan run #2: `gh run view 25914464130` (CLEAN — proceed)
- PR #1400: AMI pin chore branch
- PR #1398: Bucket B drafts (merged)
- Wave 82 plan: `documents/03-planning/waves/wave-2026-05-15-82-fe-self-host.md` §3 Bucket B
- ADR-031: `documents/02-architecture/adr/ADR-031-fe-self-host-aws-ec2.md`
- Runbook: `documents/05-guides/deploy/fe-self-host-runbook.md`
- Gaps: GAP-565 (F6) / GAP-566 (F7) / GAP-567 (F10) / GAP-568 (F11)
- Rules: `pre-mutation-state-check.md` §3 / `release-fix-retry-budget.md` §4 pivot / `terraform-apply-retry-reconfirm.md` §3 / `aws-observability-first.md` (CloudTrail verified `IsLogging=true`) / `concurrent-production-mutation-ops.md` §3.1 (no in-flight ops)
