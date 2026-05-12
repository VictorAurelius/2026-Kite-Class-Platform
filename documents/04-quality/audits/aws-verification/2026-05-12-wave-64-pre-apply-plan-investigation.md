---
title: AWS Verification — Wave 64 pre-apply plan investigation (terraform-apply run 25712117342)
status: complete
created: 2026-05-12
phase: wave-64-cutover
wave: 64
gaps: [GAP-369, GAP-449, GAP-450, GAP-477]
---

# AWS Verification Report — Wave 64 Pre-Apply Plan Investigation

## Scope

Investigation drift trong terraform plan output trước khi apply HTTPS:443 listener (Wave 64 Step E). Workflow run [25712117342](https://github.com/VictorAurelius/2026-Kite-Class-Platform/actions/runs/25712117342) ran `terraform plan` với `TF_VAR_alb_acm_certificate_arn` injected từ GH Variable `ALB_ACM_CERTIFICATE_ARN`.

Plan summary: **11 to add, 14 to change, 4 to destroy** — đáng để investigate vì lớn hơn dự kiến (chỉ định làm HTTPS listener).

Per `terraform-apply-retry-reconfirm.md` + `audit-to-gap-pipeline.md` §2.8 — investigate root cause trước khi apply.

## Commands run (Tier 1 read-only per `agent-aws-access.md` §2.1)

```bash
gh run view 25712117342 --json status,conclusion
gh run download 25712117342 --name terraform-plan-25712117342
aws ec2 describe-instances --region ap-southeast-1 --query 'Reservations[].Instances[].{...}' --output table
grep -B2 -A12 "aws_instance.kc_app must be replaced" /tmp/tfplan-25712117342/plan-output.txt
grep -B2 -A12 "random_password.jwt will be updated" /tmp/tfplan-25712117342/plan-output.txt
```

## Findings

### Real changes (must verify intent)

| # | Resource | Action | Root cause | Risk |
|---|----------|--------|-----------|------|
| 1 | `aws_lb_listener.https[0]` | + create | Wave 64 Step E (intentional) — TF_VAR injection of ACM cert ARN | None — purpose of this wave |
| 2 | `aws_lb_listener.http[0]` | ~ update | Default action flips from forward→redirect-to-https | None — cascades from #1 |
| 3 | `aws_lb_listener_rule.kc_app_default[0]` | -/+ replace | listener_arn changes from http→https | None — cascades from #1 |
| 4 | `aws_instance.kc_app` | -/+ replace | **AMI bump:** `ami-0697819d9c6d3a227` → `ami-04a8a2b994a2a7176` (Amazon Linux security update) | OK — pre-launch, no data to preserve. New instance = security patches applied |
| 5 | `aws_instance.kh_backend` | -/+ replace | Same AMI bump | OK — same reasoning as #4 |
| 6 | `aws_lb_target_group_attachment.kc_app[0]` | + create | Was missing entirely (drift) | None — adds missing attachment |
| 7 | `aws_lb_target_group_attachment.kh_backend[0]` | -/+ replace | Cascades from EC2 replacement (#5) | None |
| 8 | `aws_iam_role.github_rollback` + `aws_iam_role_policy.github_rollback_inline` | + create | Wave 63 PR #1188 code, never applied | None — desired (unblocks GAP-477 user-action gate) |
| 9 | `aws_iam_role.github_tier_3_cutover` + `aws_iam_role_policy.github_tier_3_cutover_inline` | + create | Wave 44/Wave 61 code, never applied | None — needed for tier-3-cutover.yml workflow |
| 10 | `aws_cloudwatch_metric_alarm.kc_app_memory_high` | + create | Was missing — only kh_backend had memory alarm | None — drift fix |

### Phantom updates (no real change — terraform state metadata refresh)

These resources show "updated in-place" with `# (N unchanged attributes hidden)` — terraform refresh marks them changed but no actual diff:

| Resource | Why phantom |
|----------|-------------|
| `random_password.jwt`, `random_password.encryption_raw`, `random_password.rds` | GAP-450 known issue — state shows `id="none"`. `lifecycle.ignore_changes = [result, length, ...]` prevents real rotation. Apply will NOT generate new password values. |
| `aws_secretsmanager_secret_version.jwt`, `aws_secretsmanager_secret_version.encryption`, `aws_secretsmanager_secret_version.db_password` | Depends on `random_password.*.result` — values stable due to ignore_changes |
| `aws_db_instance.main` | 58 attributes hidden, no real change — metadata refresh |
| `aws_cloudwatch_dashboard.phase_1_overview` | Dashboard JSON re-marshaled (string format change), same content |
| `aws_cloudwatch_metric_alarm.kh_backend_memory_high` | Tags/description re-formatted |
| `aws_scheduler_schedule.*` × 4 (start/stop weekday/monday/friday) | Wave 43 drift refresh, no real change |

### Verdict

- **Real changes are all desired or acceptable:**
  - HTTPS:443 listener (intended)
  - 2 IAM roles from Wave 63 code (catching up)
  - EC2 AMI bump (pre-launch, security beneficial)
  - Missing memory alarm + target group attachment (drift fixes)
- **Phantom updates are non-functional** — won't rotate secrets or change DB
- **No production data at risk** — release prep, beta tenants not invited yet

## Prior actions verified (per `audit-to-gap-pipeline.md` §2.8 — avoid duplicate work)

### Already done (verified via audit history + git)

| Action | When | Where verified |
|--------|------|----------------|
| Initial terraform apply (chicken-and-egg bootstrap) | 2026-05-07 | `aws-verification/2026-05-08-wave-43-44-bootstrap-apply.md` |
| kite-readonly IAM key rotation | 2026-05-08 | `aws-verification/2026-05-08-key-rotation-readonly-wsl.md` + `2026-05-11-kite-readonly-key-rotation.md` |
| Cost-saving schedulers (Wave 43) | 2026-05-07 | `aws-verification/2026-05-08-wave-43-44-bootstrap-apply.md` |
| OIDC apply role + plan role | 2026-05-07/08 | GH Variables `AWS_TERRAFORM_APPLY_ROLE_ARN` + `AWS_TERRAFORM_PLAN_ROLE_ARN` set |
| Cloudflare zone activation (`kitehub.me`) | 2026-05-09 | `aws-verification/2026-05-11-wave-61-bucket-a-dns-state.md` + CF API query |
| Cloudflare Email Routing setup (`support@`, `admin@`) | 2026-05-09 | CF API `/email/routing/rules` — 2 active rules |
| Cloudflare Origin Cert via CF Origin CA | 2026-05-06 | CF API `/certificates` — cert exists nhưng private key không lưu |
| Wave 63 rollback IAM role (PR #1188) | 2026-05-11 | Code committed, **never applied to AWS** (catching up in this wave) |
| GAP-450 phantom plan acknowledged + lifecycle ignore_changes shipped | 2026-05-11 | `secrets.tf` has `lifecycle.ignore_changes` on random_password |

### Done THIS session (Wave 64 cutover)

| Action | When (UTC) | Result |
|--------|-----------|--------|
| `aws ses verify-domain-identity --domain kitehub.me` | 2026-05-12 02:50 | Pending → tokens generated |
| `aws ses verify-domain-dkim --domain kitehub.me` | 2026-05-12 02:50 | 3 DKIM tokens generated |
| Generated RSA 2048 private key (local `/tmp/kitehub-origin.key`) | 2026-05-12 03:30 | Private key, never sent to CF |
| Generated CSR with SAN `kitehub.me` + `*.kitehub.me` | 2026-05-12 03:30 | Submitted to CF Origin CA |
| `POST /certificates` to CF Origin CA | 2026-05-12 03:33 | Cert ID `4927960...605` (15yr validity) |
| `aws acm import-certificate` | 2026-05-12 03:38 | ARN `e0adcd76-9d72-4567-a32e-a62d7987ccb1` ISSUED |
| `shred -u /tmp/kitehub-origin.key` | 2026-05-12 03:38 | Private key destroyed locally (ACM now holds) |
| Added 4 DNS records (SES verification TXT + 3 DKIM CNAMEs + DMARC TXT) via CF API | 2026-05-12 03:48 | Verified via DoH query |
| Patched apex SPF (merged include:amazonses.com with existing include:_spf.mx.cloudflare.net) | 2026-05-12 03:48 | Verified via DoH |
| Set GH Variable `ALB_ACM_CERTIFICATE_ARN` | 2026-05-12 03:42 | Visible in `gh variable list` |
| PR #1197 — inject TF_VAR in 2 workflows | 2026-05-12 03:42 | MERGED |

### Pending (Wave 64 remaining)

| Action | Owner | Notes |
|--------|-------|-------|
| terraform apply (this plan) | User trigger workflow_dispatch confirm=APPLY dry_run=false | Plan reviewed in this audit |
| CF SSL mode `full` → `full strict` | Claude (CF API) | After ALB :443 listener live |
| CF Always Use HTTPS `off` → `on` | Claude (CF API) | After SSL mode strict |
| SES production access form | User (Console only) | Submit per `email-ses-setup-runbook.md` §4.1.1 |
| Wait SES production approval (24-48h) | AWS | External dependency |
| Resume stack (if stopped) + production seed + smoke | User scripts | `start-stack.sh` + `seed-production.sh` + `smoke-test.sh` |

## Recommendations

1. **Apply this plan with `dry_run=false`** — analysis confirms changes are safe in pre-launch state:
   - Real changes are all intentional or beneficial (security patches, missing drift fixes, Wave 63 catch-up)
   - Phantom changes are non-functional (verified via plan diff details)
   - No production data exposure (beta tenants not yet invited)

2. **Post-apply verification commands** (Tier 1 read-only):
   ```bash
   # Verify HTTPS listener live
   aws elbv2 describe-listeners --load-balancer-arn <ALB_ARN> --query 'Listeners[?Port==`443`]'
   # Verify cert binding
   aws elbv2 describe-listener-certificates --listener-arn <HTTPS_LISTENER_ARN>
   # Verify new EC2 IDs running
   aws ec2 describe-instances --filters Name=tag:Name,Values=kitehub-* --query 'Reservations[].Instances[].[InstanceId,State.Name,LaunchTime]'
   # Verify ALB target health (post EC2 boot + app startup ~5min)
   aws elbv2 describe-target-health --target-group-arn <KH_BACKEND_TG_ARN>
   aws elbv2 describe-target-health --target-group-arn <KC_APP_TG_ARN>
   ```

3. **Watch for**:
   - Targets may be `unhealthy` for ~5-10min after EC2 replacement (app boot time)
   - New EC2 instance IDs will replace `i-0b65c3947d36cae61` + `i-07f6de54544162124`
   - Old EC2 will be terminated — no data loss because deployed via container, not local state

## Next steps (this session)

Proceed: user triggers terraform-apply.yml with `confirm=APPLY` + `dry_run=false`. Then post-apply verify + flip CF SSL mode strict.

## References

- Workflow run: https://github.com/VictorAurelius/2026-Kite-Class-Platform/actions/runs/25712117342
- Plan artifact: `terraform-plan-25712117342/plan-output.txt`
- Related: GAP-369, GAP-449, GAP-450, GAP-477
- Rules applied: `agent-aws-access.md` §2.1, `terraform-apply-retry-reconfirm.md`, `audit-to-gap-pipeline.md` §2.8, `release-deploy-standard.md` §9
