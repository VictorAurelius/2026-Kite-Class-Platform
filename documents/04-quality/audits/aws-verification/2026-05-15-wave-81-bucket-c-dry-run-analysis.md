---
title: AWS Verification — Wave 81 Bucket C dry-run plan analysis
status: complete
created: 2026-05-15
phase: phase-1-beta
wave: 81
gaps: [GAP-525]
---

# AWS Verification Report — Wave 81 Bucket C terraform-apply dry-run plan analysis

## Scope

Pre-mutation state-check per `pre-mutation-state-check.md` §3 cho Bucket C cred rotation (GAP-525). Triggered `terraform-apply.yml workflow_dispatch dry_run=true` (run 25903723024) để inspect plan scope before deciding apply path. **CRITICAL FINDING surfaced — plan scope FAR exceeds intended Bucket C work; apply BLOCKED pending re-scope.**

## Commands run (Tier 1 read-only)

```bash
gh workflow run terraform-apply.yml -f version=main -f confirm=APPLY -f dry_run=true
gh run view 25903723024 --json conclusion,status,jobs
gh run view 25903723024 --log | grep -E "Plan:|will be|forces replacement"

aws secretsmanager describe-secret --secret-id kitehub/production/seed-admin-password \
  --profile dev-admin --region ap-southeast-1

grep -A 10 'data "aws_ami"' infrastructure/terraform-aws/ec2.tf
grep -A 10 "seed_admin_password" infrastructure/terraform-aws/secrets.tf
```

## Findings

### Plan summary: 3 to add, 3 to change, 3 to destroy

| Resource | Action | Cascade source |
|---|---|---|
| `aws_instance.kc_app` | 🔴 **REPLACE** | AMI bump `ami-04a8a2b994a2a7176` → `ami-01f309fb59c80862f` (forces replacement) |
| `aws_instance.kh_backend` | 🔴 **REPLACE** | Same AMI bump |
| `aws_lb_target_group_attachment.kh_backend[0]` | 🟡 REPLACE | Cascades from `kh_backend` EC2 replace |
| `aws_cloudwatch_dashboard.phase_1_overview` | ⚪ update in-place | Metric ARN reference shift (post EC2 replace) |
| `aws_cloudwatch_metric_alarm.kc_app_memory_high` | ⚪ update in-place | Same |
| `aws_cloudwatch_metric_alarm.kh_backend_memory_high` | ⚪ update in-place | Same |

### Root cause: `data.aws_ami.al2023.most_recent = true` drift

`infrastructure/terraform-aws/ec2.tf` resolves latest Amazon Linux 2023 AMI dynamically via `most_recent = true`. AWS released newer AL2023 image since Wave 64 cutover (last apply ~2026-05-12). Terraform plan correctly detects diff → flags EC2 for replacement.

**This is the SAME phantom-drift pattern as Wave 64 Step E** (per `pre-mutation-state-check.md` §4 worked example) but with REAL consequence (forces_replacement is hard drift, not phantom).

### Bucket C scope drift verification

**Expected Bucket C scope:** Create `seed-admin-password` secret (rotation #1) + rotate `cloudflare-api-token` + rotate `resend-api-key` (rotations #2 + #3).

**Actual state verified:**

| Cred | Wrapper script reachability | Real state |
|---|---|---|
| `admin-seed-password` (wrong name) | ⚠️ WARN unreachable | n/a |
| `seed-admin-password` (correct TF name) | n/a (script bug) | ✅ EXISTS — `CreatedDate=2026-05-13T04:29:21Z` (TF-managed, in state) |
| `cloudflare-api-token` | ✅ reachable | EXISTS (rotation candidate) |
| `resend-api-key` | ✅ reachable | EXISTS (rotation candidate) |

**Plan does NOT include `seed_admin_password` resources** because TF state already has them from prior apply. Cred #1 is **NO-OP** — secret created Wave 72a/77 Bucket C drift fix.

### Verdict

🚫 **DO NOT APPLY** plan as-is. Reasons:

1. **Scope mismatch:** Plan affects 9 resources (2 EC2 replace + 7 cascades); Bucket C scope = 0 terraform resources (creds 2+3 = `put-secret-value` outside TF)
2. **Bucket C cred #1 already done** (TF-managed since 2026-05-13)
3. **AMI drift apply would force EC2 replacement** — unrelated to Wave 81 DEPLOY trigger; better deferred to Wave 81 Bucket D cutover or separate maintenance window
4. **Service deploy not done yet** — replacing EC2 now = re-bootstrap fresh state; downstream Bucket D deploy still works but wastes Bucket A start cycle (~5 min RDS cold + 2 EC2 fresh + ALB target re-attach)

## Prior actions verified

| Action | When | Where verified |
|---|---|---|
| `seed-admin-password` TF-managed creation | 2026-05-13 | `aws secretsmanager describe-secret` → CreatedDate match |
| Wave 64 Step E AMI drift pattern | 2026-05-12 | `documents/04-quality/audits/aws-verification/2026-05-12-wave-64-pre-apply-plan-investigation.md` |
| Bucket A AWS stack up | 2026-05-15 05:33-05:38 UTC | `documents/04-quality/audits/aws-verification/2026-05-15-wave-81-pre-deploy-state.md` |

## Pending (this op)

| Action | Owner | Notes |
|---|---|---|
| Bucket C cred #1 (seed-admin-password) | ✅ DONE pre-existing — no apply needed | Wrapper script naming bug (`admin-seed-password` vs `seed-admin-password`) caused false WARN |
| Bucket C cred #2 (cloudflare-api-token rotate) | USER (Cloudflare dashboard + `aws secretsmanager put-secret-value`) | Per Tier 3 ban — user executes |
| Bucket C cred #3 (resend-api-key rotate) | USER (Resend dashboard + `put-secret-value`) | Same |
| AMI drift handling | DEFER to Wave 81 Bucket D OR file separate gap | EC2 replace = service downtime; bundle with Bucket D deploy to share cycle |
| Wrapper script naming bug fix | Follow-up gap | `scripts/rotate-leaked-credentials.sh` checks `admin-seed-password` but TF uses `seed-admin-password` |

## Recommendations

1. **Skip terraform apply** for Bucket C — no TF change needed
2. **Proceed Bucket C creds #2 + #3** via user-action `put-secret-value` (Tier 3 banned for agent)
3. **File follow-up:** wrapper script naming bug → fix in same-PR with Bucket C closure
4. **Decide AMI drift:** options:
   - A. Bundle with Wave 81 Bucket D deploy (atomic: EC2 replace + service deploy + ALB attach)
   - B. Pin AMI version explicitly (remove `most_recent = true` drift), defer replace to maintenance
   - C. Apply now standalone (waste Bucket A start cycle but simpler scope)

## References

- Plan run: https://github.com/VictorAurelius/2026-Kite-Class-Platform/actions/runs/25903723024
- Wave 81 plan §3 Bucket C
- `pre-mutation-state-check.md` §3 mandatory artifact format
- `terraform-apply-retry-reconfirm.md` §3 required interaction (BLOCKED — no apply)
- `agent-aws-access.md` §4.3 Tier 3 banned (put-secret-value = user-action)
- Wave 64 Step E AMI drift precedent
- `scripts/rotate-leaked-credentials.sh` (naming bug)
