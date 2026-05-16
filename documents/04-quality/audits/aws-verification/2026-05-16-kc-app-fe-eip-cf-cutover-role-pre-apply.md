---
title: AWS Verification - kc-app-fe EIP + github_cloudflare_cutover IAM role pre-apply
status: draft
created: 2026-05-16
phase: phase-1-beta
wave: post-86
gaps: [GAP-573]
---

# AWS Verification Report - kc-app-fe EIP + CF cutover OIDC role pre-apply

## Scope

Terraform apply ship 2 new resources (+ 1 association + 1 inline policy + 2 outputs)
in `infrastructure/terraform-aws/`:

1. `aws_eip.kc_app_fe` - Elastic IP allocated for FE self-host EC2 (`kitehub-kc-app-fe`).
   Closes Wave 82 follow-up GAP-573 "Elastic IP not bound to EC2" (handoff
   `documents/03-planning/session-handoffs/2026-05-15-post-wave-82-handoff.md:135`).
2. `aws_eip_association.kc_app_fe` - binds EIP to existing `aws_instance.kc_app_fe`.
   AWS transparently swaps auto-assigned public IP for EIP without instance restart
   (instance keeps `associate_public_ip_address = true`).
3. `aws_iam_role.github_cloudflare_cutover` + inline policy + output - new OIDC role
   for the companion workflow `.github/workflows/cloudflare-apex-cutover.yml` to
   fetch the CF API token from Secrets Manager and read EIP allocation. Least-priv
   per `pre-launch-infra-hardening-checklist.md` §2.5.

Companion workflow + cloudflare-dns.sh `set-apex` extension ship in same PR; the
DNS cutover audit (`2026-05-16-apex-dns-flip-eip-cutover.md`) covers the
post-apply DNS mutation separately.

## Commands run (Tier 1 read-only per `agent-aws-access.md` §2.1)

To be executed by dev before `terraform-apply.yml dry_run=true`:

```bash
# Confirm no concurrent prod-mutation ops in flight
gh run list --status in_progress --json name,workflowName

# Confirm current kc-app-fe instance state + no existing EIP collision
aws ec2 describe-instances \
  --filters "Name=tag:Name,Values=kitehub-kc-app-fe" \
  --query 'Reservations[].Instances[].[InstanceId,State.Name,PublicIpAddress,PrivateIpAddress]' \
  --output table

aws ec2 describe-addresses \
  --filters "Name=tag:Name,Values=kitehub-kc-app-fe-eip" \
  --query 'Addresses[].[AllocationId,PublicIp,InstanceId]' --output table
# Expected: empty (EIP not yet allocated)

# Confirm existing OIDC roles (sanity-check no name collision)
aws iam list-roles --query 'Roles[?starts_with(RoleName, `kitehub-github-`)].RoleName' --output table
# Expected: no `kitehub-github-cloudflare-cutover` yet

# Confirm CF token secret exists + ARN matches policy resource pattern
aws secretsmanager describe-secret \
  --secret-id kitehub/production/cloudflare-api-token \
  --query '[ARN, Name, KmsKeyId, LastChangedDate]' --output table
```

## Reconciliation per `pre-mutation-state-check.md` §3.5

Predicted plan summary based on PR scope alone (this scope):
`5 to add, 0 to change, 0 to destroy` (the 2 outputs do NOT count as resources).

| Resource | Plan action | Wave-source | Intent | Decision |
|---|---|---|---|---|
| `aws_eip.kc_app_fe` | create | This PR | Real | Apply |
| `aws_eip_association.kc_app_fe` | create | This PR | Real | Apply |
| `aws_iam_role.github_cloudflare_cutover` | create | This PR | Real | Apply |
| `aws_iam_role_policy.github_cloudflare_cutover_inline` | create | This PR | Real | Apply |

If `terraform plan` returns a different summary (especially any destroy / replace):
**STOP**, write reconciliation table covering each line, and use workflow `targets`
input to apply only the 4 resources scoped above (per Wave 86 PR #1437 precedent).

## Cross-reference matrix per `pre-mutation-state-check.md` §1.5

| IAM Action | Resource pattern in policy | Actual resource verified | Workflow caller | Verdict |
|---|---|---|---|---|
| `secretsmanager:GetSecretValue` | `aws_secretsmanager_secret.placeholders["cloudflare-api-token"].arn` | `secrets.tf:108` defines key `cloudflare-api-token` -> ARN pattern `kitehub/production/cloudflare-api-token-*` | `cloudflare-apex-cutover.yml` step "Resolve Cloudflare API token" via `aws secretsmanager get-secret-value` | Match |
| `ec2:DescribeAddresses` | `*` (read-only, no tag-condition support for this action) | Used to lookup EIP by `Name=kitehub-kc-app-fe-eip` tag | `cloudflare-apex-cutover.yml` step "Resolve EIP allocation" | Match |
| `ec2:DescribeInstances` | `*` (Tier 1 read-only) | Used to verify kc-app-fe instance state post-association | `cloudflare-apex-cutover.yml` step "Verify EC2 instance state" | Match |

OIDC trust scope: `repo:${var.github_repo}:environment:production` - mirrors
`github_tier_3_cutover` + `github_rollback` pattern. Reviewer-gate on GitHub
Environment `production` provides 2nd cognitive checkpoint.

## SG / ASCII check per `aws-sg-description-ascii.md`

`grep -nE "description.*[^[:ascii:]]" infrastructure/terraform-aws/*.tf` expected
zero matches (no new SG in this PR; the EIP / IAM additions are description-text-free).

## Concurrent-op check per `concurrent-production-mutation-ops.md` §6

No overlap with:

- `terraform-apply.yml` (this IS the trigger for that workflow)
- `cloudflare-apex-cutover.yml` (will be triggered AFTER this terraform apply lands;
  serial per `concurrent-production-mutation-ops.md` §3.5 row "IAM role-policy update
  + deploy using that role" - wait >=10s for IAM eventual consistency before
  trigger)
- `deploy-production.yml` / `rollback.yml` (unrelated scope)

## Prior actions verified

| Prior action | Date | Where verified |
|---|---|---|
| Wave 82 Bucket B EC2 + SG + IAM + Secrets apply | 2026-05-15 | `documents/04-quality/audits/aws-verification/2026-05-15-wave-82-bucket-b-post-apply.md` (existing) |
| CF token placeholder secret created | Wave 43-44 bootstrap | `infrastructure/terraform-aws/secrets.tf:108` |
| CF token populated in Secrets Manager (real value) | 2026-05-15 PR #1403 | Per Wave 82 handoff §"Lessons" #3 |
| Wave 86 Bucket H+E targeted CF apply (page rules) | 2026-05-16 | `documents/04-quality/audits/cloudflare-verification/2026-05-16-wave-86-magic-link-bypass-page-rule.md` |
| `kitehub-github-tier-3-cutover` OIDC role baseline | 2026-05-08 | `infrastructure/terraform-aws/iam.tf:547` (pattern reference) |

## Pending (this apply)

| Action | Owner | Notes |
|---|---|---|
| Run `gh run list --status in_progress` pre-trigger | dev | Concurrent-op guard |
| Trigger `terraform-apply.yml -f confirm=APPLY -f dry_run=true` | dev | Read plan output |
| Reconcile plan summary against predicted `5 to add, 0 to change, 0 to destroy` | dev | If mismatch, use `targets` input |
| Trigger `terraform-apply.yml -f confirm=APPLY -f dry_run=false` | dev | After plan match |
| Capture EIP public IP from `kc_app_fe_public_ip` output | dev | Needed for next-step DNS audit |
| Set GitHub repo variable `AWS_CLOUDFLARE_CUTOVER_ROLE_ARN` to new role ARN | dev | Workflow reads `vars.AWS_CLOUDFLARE_CUTOVER_ROLE_ARN` |
| Wait >=10s post-apply (IAM eventual consistency) | dev | Per `concurrent-production-mutation-ops.md` §3.5 |
| Proceed to DNS flip via `cloudflare-apex-cutover.yml` | dev | Separate audit covers that mutation |

## Recommendations

1. **Apply**: blast radius is small (1 EIP $3.60/mo when associated, $0 detached;
   1 IAM role no cost). No EC2 replacement (verified: `associate_public_ip_address`
   stays `true`, EIP association is additive, AWS handles IP swap transparently).
2. **Verify post-apply**: `kc_app_fe_public_ip` output should return the new EIP;
   `aws ec2 describe-addresses` should show `InstanceId` populated.
3. **Watch-for**: if `terraform plan` shows `aws_instance.kc_app_fe` change/replace,
   STOP - association should not modify the instance. Likely cause = stale state;
   investigate before applying.
4. **Follow-up audit**: file `2026-05-16-kc-app-fe-eip-cf-cutover-role-post-apply.md`
   after apply with actual EIP IP + role ARN.

## References

- Wave 82 handoff GAP-573: `documents/03-planning/session-handoffs/2026-05-15-post-wave-82-handoff.md`
- Companion workflow: `.github/workflows/cloudflare-apex-cutover.yml`
- Companion DNS audit: `documents/04-quality/audits/cloudflare-verification/2026-05-16-apex-dns-flip-eip-cutover.md`
- Rule cross-refs: `pre-mutation-state-check.md` §1.5 + §3 + §3.5,
  `pre-launch-infra-hardening-checklist.md` §2.5,
  `concurrent-production-mutation-ops.md` §6,
  `release-deploy-standard.md` §9.
