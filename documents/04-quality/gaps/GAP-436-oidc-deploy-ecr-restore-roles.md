# GAP-436: OIDC roles for deploy + ECR push + restore drill workflows

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (BLOCKING Phase 2.3+ workflows that write to AWS)
**Domain:** DevOps / Security
**Found:** 2026-05-08 (Phase 2.2 OIDC scope split)
**Affects:** 4 GitHub workflows currently expecting role ARN secrets that don't exist yet.

## Problem

Phase 2.2 OIDC plan role (`kitehub-github-terraform-plan`, ReadOnly) đã apply 2026-05-08. Nhưng 4 workflows khác cần OIDC role với write/push perms — chưa được define trong `iam.tf` lẫn applied:

| Workflow | Expected secret/var | Permissions needed |
|---|---|---|
| `.github/workflows/deploy-staging.yml:55` | `secrets.AWS_DEPLOY_ROLE_ARN` | EC2 + ECS run-task + Secrets Manager read + S3 write (artifact bucket) |
| `.github/workflows/deploy-production.yml:47` | `secrets.AWS_ROLE_ARN` | Same as deploy-staging + production-tier write |
| `.github/workflows/docker-build-push.yml:256` | `secrets.AWS_ROLE_ARN` | ECR push + login |
| `.github/workflows/restore-drill.yml:119` | `secrets.AWS_RESTORE_DRILL_ROLE_ARN` | RDS describe + snapshot copy + restore + delete (sandbox) |

Nếu các workflows này chạy trước khi roles tồn tại → fail at `aws-actions/configure-aws-credentials` step.

## Root Cause

Phase 2.2 OIDC scope chỉ define `github_terraform_plan` role (read-only). Wave 37 PR #938 prep `iam.tf` không cover deploy/ECR/restore-drill roles vì:
- Phase 2.3 (production apply) chưa kích hoạt
- Phase 4 staging gate chưa active
- Phase 5+ (docker push) chưa cần

Giờ Phase 2.2 đã ship plan role, các role còn lại trở thành prereq cho Phase 2.3+.

## Proposed Fix

### Phase 1 — Add 3 OIDC roles to iam.tf

Mỗi role với scoped trust policy + least-privilege inline policy:

#### 1. `kitehub-github-deploy` (deploy-staging + deploy-production)
- Trust policy: `repo:VictorAurelius/2026-Kite-Class-Platform:ref:refs/heads/main` + `ref:refs/tags/v*`
- Permissions: SSM session start (EC2 access), ECS run-task, Secrets Manager get-value cho `kite/{staging,prod}/*`, S3 read deploy artifacts

#### 2. `kitehub-github-ecr-push` (docker-build-push.yml)
- Trust policy: `repo:VictorAurelius/...:ref:refs/heads/main` + `ref:refs/tags/v*`
- Permissions: ECR `BatchCheckLayerAvailability`, `BatchGetImage`, `CompleteLayerUpload`, `GetAuthorizationToken`, `InitiateLayerUpload`, `PutImage`, `UploadLayerPart` cho 7 ECR repos

#### 3. `kitehub-github-restore-drill` (restore-drill.yml)
- Trust policy: `repo:VictorAurelius/...:ref:refs/heads/main` (workflow_dispatch only — sub claim restrict)
- Permissions: RDS `DescribeDBSnapshots`, `CopyDBSnapshot`, `RestoreDBInstanceFromDBSnapshot`, `DeleteDBInstance` (with name-prefix condition)

### Phase 2 — Apply targeted

```bash
terraform apply -target=aws_iam_role.github_deploy \
  -target=aws_iam_role.github_ecr_push \
  -target=aws_iam_role.github_restore_drill \
  -target=aws_iam_role_policy.github_deploy_inline \
  -target=aws_iam_role_policy.github_ecr_push_inline \
  -target=aws_iam_role_policy.github_restore_drill_inline
```

Cost: $0 (IAM free).

### Phase 3 — Set GitHub Secrets/Variables

```bash
gh secret set AWS_DEPLOY_ROLE_ARN --body "arn:aws:iam::906286017800:role/kitehub-github-deploy"
gh secret set AWS_ROLE_ARN --body "arn:aws:iam::906286017800:role/kitehub-github-ecr-push"
gh secret set AWS_RESTORE_DRILL_ROLE_ARN --body "arn:aws:iam::906286017800:role/kitehub-github-restore-drill"
```

Note: `deploy-production.yml` + `docker-build-push.yml` both reference `AWS_ROLE_ARN` — disambiguate: production deploy needs broader perms than ECR push. Either:
(a) Single `AWS_ROLE_ARN` = composite role with both permission sets
(b) Rename references to `AWS_DEPLOY_ROLE_ARN` (existing) for prod, keep `AWS_ROLE_ARN` for ECR — requires workflow edit

Recommend (b) — least-privilege.

### Phase 4 — Remove static keys after OIDC verified

After all workflows green via OIDC, remove `AWS_ACCESS_KEY_ID` + `AWS_SECRET_ACCESS_KEY` GitHub Secrets (set 2026-05-07 for `ci-deploy` user). Track in Phase 4 of this gap.

## Acceptance Criteria

- [ ] 3 IAM roles defined in `iam.tf` with scoped trust + least-privilege policies
- [ ] Targeted `terraform apply` clean
- [ ] 3 role ARNs in GitHub Secrets
- [ ] Smoke: trigger `docker-build-push.yml` workflow_dispatch — green via OIDC (no static key)
- [ ] Smoke: trigger `restore-drill.yml` workflow_dispatch with `dry_run=true` — green via OIDC
- [ ] Static `AWS_ACCESS_KEY_ID` + `AWS_SECRET_ACCESS_KEY` GitHub Secrets removed
- [ ] `release-1-deploy-runbook.md` §2.2 updated reflecting full OIDC posture

## Related

- Phase 2.2 plan role: applied 2026-05-08, role ARN `arn:aws:iam::906286017800:role/kitehub-github-terraform-plan`, GitHub Variable `AWS_TERRAFORM_PLAN_ROLE_ARN` set
- GAP-396 (state backend) — closed by bootstrap apply 2026-05-08 + PR #990 partial-config
- GAP-397 (terraform-plan CI) — paired Wave 37; will activate fully when this gap closes
- BLOCKING Phase 2.3 production terraform apply (cần cleanup IAM model trước khi tạo EC2/RDS)

## Log

- **2026-05-08:** GAP filed during Phase 2.2 scope split. Plan role only shipped this session; deploy/ECR/restore-drill roles deferred to this gap.
