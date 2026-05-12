# GAP-482: Deploy workflow blocked by IAM tag mismatch + hardcoded EC2 ID

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 BLOCKING (Wave 64 Step F deploy-production fails)
**Domain:** DevOps / Infrastructure
**Found:** 2026-05-12 (Wave 64 Step F deploy-production.yml run 25713149664 failed)
**Affects:** All deploys via `deploy-production.yml` workflow

## Problem

Wave 64 Step F (deploy-production workflow_dispatch for v0.9.0-beta-staging.9) failed with:

```
aws: [ERROR]: An error occurred (AccessDeniedException) when calling the SendCommand operation:
User: arn:aws:sts::906286017800:assumed-role/kitehub-github-deploy/GitHubActions is not authorized
to perform: ssm:SendCommand on resource: arn:aws:ec2:ap-southeast-1:906286017800:instance/i-0b65c3947d36cae61
```

State-check findings:

### Bug 1 — IAM tag value mismatch

`infrastructure/terraform-aws/iam.tf` line ~239 (kitehub-github-deploy-inline policy):
```hcl
Resource = "*"
Condition = {
  StringEquals = {
    "aws:ResourceTag/Project" = var.project_name   # → "kitehub" lowercase
  }
}
```

`infrastructure/terraform-aws/main.tf` provider default_tags:
```hcl
default_tags {
  tags = {
    Project = "Kite"   # capitalized — actual tag on all resources
  }
}
```

Actual EC2 tag (verified via describe-instances):
```json
{ "Key": "Project", "Value": "Kite" }
```

→ Condition `aws:ResourceTag/Project = "kitehub"` NEVER matches actual `"Kite"` → all SSM ops to EC2 instances denied.

`scheduler.tf` correctly hardcodes `"Kite"` in same pattern — so EventBridge schedulers work. Only IAM role conditions reference the wrong `var.project_name` value.

Affected roles (all with same bug):
- `kitehub-github-deploy` (this incident)
- `kitehub-github-rollback` (Wave 63 — would fail rollback exec)
- `kitehub-rollback-role` (also Wave 63 alternate name)

### Bug 2 — Hardcoded stale EC2 instance ID

`.github/workflows/deploy-production.yml`:
```yaml
env:
  DEPLOY_INSTANCE_ID_KH: i-0b65c3947d36cae61   # kh-backend EC2 (Phase 2.3 output)
```

This is the OLD instance, terminated 2026-05-12 04:11 when terraform apply replaced EC2 due to AMI bump. New ID: `i-00505094277deda29`.

Hardcoded values break on every EC2 replacement (AMI bump, instance class change, etc.). Should be dynamic lookup via tag.

## Proposed Fix

### Fix 1 — Correct IAM tag condition

Edit `infrastructure/terraform-aws/iam.tf`:
- Change `"aws:ResourceTag/Project" = var.project_name` → `"aws:ResourceTag/Project" = "Kite"` for all 3 SSM SendCommand conditions
- Alternative: change `default_tags.tags.Project` in main.tf from `"Kite"` to `var.project_name` (= "kitehub") — would change every existing resource tag, requires fleet-wide tag update

→ Pick: change iam.tf (3 lines edit, no resource tag churn)

### Fix 2 — Dynamic EC2 lookup in deploy workflow

Edit `.github/workflows/deploy-production.yml`:
- Replace hardcoded `DEPLOY_INSTANCE_ID_KH` env var
- Add step before "Send deploy command via SSM":
  ```yaml
  - name: Lookup current kh-backend instance ID
    id: ec2_lookup
    run: |
      INSTANCE_ID=$(aws ec2 describe-instances \
        --region "${AWS_REGION}" \
        --filters \
          "Name=tag:Name,Values=kitehub-kh-backend" \
          "Name=instance-state-name,Values=running" \
        --query "Reservations[0].Instances[0].InstanceId" \
        --output text)
      if [ -z "$INSTANCE_ID" ] || [ "$INSTANCE_ID" = "None" ]; then
        echo "::error ::No running kitehub-kh-backend instance found"; exit 1
      fi
      echo "instance_id=$INSTANCE_ID" >> "$GITHUB_OUTPUT"
  ```
- Reference: `${{ steps.ec2_lookup.outputs.instance_id }}` in subsequent SSM steps

### Apply order

1. Ship fixes in 1 PR
2. terraform apply (IAM-only this time, no EC2 replace) via workflow_dispatch
3. Re-trigger deploy-production.yml v0.9.0-beta-staging.9

## Acceptance Criteria

- [ ] `iam.tf` 3 SSM SendCommand conditions use `"Kite"` literal matching default_tags
- [ ] `deploy-production.yml` dynamically looks up instance ID by tag (no hardcoded ID)
- [ ] `rollback.yml` (Wave 63) — verify same fix applied if has hardcoded IDs
- [ ] terraform plan shows ONLY IAM policy update (no EC2 churn)
- [ ] terraform apply succeeds
- [ ] deploy-production.yml succeeds on v0.9.0-beta-staging.9
- [ ] kh-backend target group reports `healthy` after deploy

## Related

- **Surfaced by:** Wave 64 Step F deploy-production run 25713149664
- **Sibling:** GAP-481 (gateway path routing 404) — may be related once deploy works
- **Reference rules:**
  - `pre-mutation-state-check.md` v1.0.0 — applied this investigation
  - `release-fix-retry-budget.md` §3 — retry #1, root-cause fix not patch
  - `terraform-apply-retry-reconfirm.md` — apply after fix
- **Blocks:** Wave 64 closure + Release 1 invite

## Log

- **2026-05-12:** Filed Wave 64 Step F deploy fail investigation. 2 bugs in 1 gap (architectural fix needed in same PR for retry budget discipline).
