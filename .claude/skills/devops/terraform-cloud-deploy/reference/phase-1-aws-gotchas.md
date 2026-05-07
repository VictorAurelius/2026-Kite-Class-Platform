## Phase 1 BETA AWS Gotchas (2026-05-08, post Phase 2.3 apply)

Lessons học được khi apply 71 resources Architecture B trên AWS Singapore. Mỗi mục đã codified trong memory tương ứng — đọc memory chi tiết khi cần.

### G1. AWS Security Group `description` ASCII-only

`aws_security_group { description = "..." }` reject mọi non-ASCII char (em-dash `—`, en-dash `–`, smart quotes, Vietnamese diacritics). Apply fail với `InvalidParameterValue: Character sets beyond ASCII are not supported`.

**Pre-apply check:**
```bash
grep -nE "description.*[^[:ascii:]]" infrastructure/terraform-aws/*.tf
# Empty output = OK; any match = fix to ASCII before apply
```

Ref: `feedback_aws_sg_description_ascii_only.md`

### G2. Public-repo backend config — partial pattern

Repo PUBLIC + S3 backend bucket name hardcoded = AWS account ID leak. Use partial config:

```hcl
# backend.tf (committed)
terraform {
  backend "s3" {
    # bucket — supplied via -backend-config
    key = "..."; region = "..."; dynamodb_table = "..."; encrypt = true
  }
}
```

```bash
# Local
cp backend.config.example backend.config  # gitignored
# Edit bucket = "..."
terraform init -backend-config=backend.config

# CI
terraform init -backend-config="bucket=${{ vars.TERRAFORM_STATE_BUCKET }}"
```

Ref: `feedback_terraform_partial_backend_public_repo.md`

### G3. CloudTrail BEFORE Phase 2.3

New AWS accounts have CloudTrail OFF. Apply `aws_cloudtrail.main` (multi-region, log validation) BEFORE production infra apply — audit baseline ensures any incident post-apply has trail.

```bash
# Phase order (IAM/audit-only resources, NO cost)
terraform apply -target=aws_cloudtrail.main \
                -target=aws_s3_bucket.cloudtrail_logs \
                # ... related bucket + policy resources

# THEN Phase 2.3 (cost-incurring)
terraform apply
```

Ref: `feedback_aws_observability_first.md`

### G4. Phased apply via `-target=`

Big module (10+ resource categories) → split phases. Each phase = own PR + verification:
1. State backend (S3 + DynamoDB) — bootstrap module
2. IAM + OIDC roles — `-target=aws_iam_*`
3. CloudTrail + audit log — `-target=aws_cloudtrail.* -target=aws_s3_bucket.cloudtrail_logs`
4. Production infra (full apply, no -target)
5. Dashboard (CloudWatch) — `-target=aws_cloudwatch_dashboard.*`

Document `-target` use in PR body so reviewers don't mistake for ad-hoc fix. Final `terraform plan` (no target) should show no drift.

Ref: `feedback_terraform_targeted_apply_phases.md`

### G5. `count` attribute access requires `[0]` indexing

Resources với `count = var.flag ? 1 : 0` (e.g. `aws_lb.main`) cần `[0]` indexing trong references kể cả khi count = 1:

```hcl
# WRONG — terraform error: "Missing resource instance key"
metrics = [["AWS/ApplicationELB", "RequestCount", "LoadBalancer", aws_lb.main.arn_suffix]]

# RIGHT
metrics = [["AWS/ApplicationELB", "RequestCount", "LoadBalancer", aws_lb.main[0].arn_suffix]]
```

Caught 2026-05-08 trong `cloudwatch-dashboard.tf` after Phase 2.3 apply.

### G6. GitHub Variables vs Secrets — non-secret config

Bucket names, ARNs, role identifiers = NOT secrets per AWS official docs. Use GitHub **Variables** (`vars.X`) instead of Secrets:
- Variables visible to all workflow runs (PRs included) — OK for non-credential config
- Secrets masked in logs + restricted to non-PR runs by default

Examples this project:
- `vars.TERRAFORM_STATE_BUCKET` — bucket name (not secret)
- `vars.AWS_TERRAFORM_PLAN_ROLE_ARN` — read-only role (not secret)
- `secrets.AWS_DEPLOY_ROLE_ARN` — write-capable role (defense-in-depth as Secret)
- `secrets.RESEND_API_KEY` — credential (Secret)

### G7. Resume terraform apply after partial fail

Mid-apply failure preserves state. Fix offending file → re-run `terraform apply`. Terraform skips already-created resources + creates rest. No need to destroy + restart.

Pattern (this session):
1. Apply 71 resources → fail at SG description em-dash (resource #57 of 71)
2. Fix `security-groups.tf` ASCII-only
3. Re-run `terraform apply` → 14 remaining resources created cleanly

### G8. OIDC role secret name disambiguation

Don't share single `AWS_ROLE_ARN` secret across multiple workflows with different perm needs. Workflow disambiguation:
- `secrets.AWS_DEPLOY_ROLE_ARN` — deploy-staging + deploy-production (SSM + Secrets read)
- `secrets.AWS_ECR_PUSH_ROLE_ARN` — docker-build-push only (ECR push)
- `secrets.AWS_RESTORE_DRILL_ROLE_ARN` — restore-drill (S3 backup read)
- `vars.AWS_TERRAFORM_PLAN_ROLE_ARN` — terraform-plan (ReadOnly + state lock)

Each role least-privilege → blast radius isolated if any single role's credentials compromised.
