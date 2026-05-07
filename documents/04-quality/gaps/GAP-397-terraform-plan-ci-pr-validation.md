# GAP-397: Terraform Plan in CI (PR Validation)

**Status:** 🟢 DONE 2026-05-09 — Wave 37 Bucket A
**Priority:** 🟠 P1
**Domain:** CI/CD / Terraform
**Found:** 2026-05-07 (Wave 37 planning)
**Affects:** Infra change PR review — preview blast radius before merge

## Problem

PR đụng `infrastructure/terraform-aws/` không có automated `terraform plan` output trong PR comment. Reviewer phải clone branch + run plan locally.

## Proposed Fix

`.github/workflows/terraform-plan.yml`:
- Trigger: `pull_request` paths `infrastructure/terraform-aws/**`
- Steps: setup-terraform v3 → init (read-only state) → fmt check → validate → plan → comment plan output to PR
- Uses GitHub OIDC for AWS auth (no long-lived keys per security best practice)

KHÔNG chạy `terraform apply` trong CI — vẫn human-only Phase 2 per GAP-381.

## Acceptance Criteria

- [x] `.github/workflows/terraform-plan.yml` exists
- [x] OIDC role `arn:aws:iam::xxx:role/kitehub-github-terraform-plan` (read-only AWS perms — `ReadOnlyAccess` managed policy + state-access inline)
- [x] `actions/github-script` posts plan output as PR comment (with truncate-to-50KB guard)
- [x] Workflow YAML validated (`python3 -c "import yaml; yaml.safe_load(...)"` exit 0)
- [x] Self-test: workflow YAML loads cleanly + has correct `pull_request.paths` trigger; live plan execution gated by repo variable `AWS_TERRAFORM_PLAN_ROLE_ARN` (graceful skip when unset — surfaces "Plan skipped" comment until AWS account ready per GAP-394)

## Resolution

Wave 37 Bucket A shipped:

- `.github/workflows/terraform-plan.yml` — triggers on PR touching `infrastructure/terraform-aws/**` or workflow file itself.
- `permissions: id-token: write` — OIDC token claim.
- `aws-actions/configure-aws-credentials@v4` with `role-to-assume` from repo variable `AWS_TERRAFORM_PLAN_ROLE_ARN`.
- Graceful degradation: if AWS variable not set (initial bring-up before AWS account ready), workflow runs `fmt -check` + `init -backend=false` + `validate` only and posts a "Plan skipped" comment. Once AWS account ready, set the repo variable and full plan runs.
- `actions/github-script@v7` finds existing plan-comment by header pattern and updates in-place (avoids spam on re-runs).

OIDC role provisioned in `iam.tf`:
- `aws_iam_openid_connect_provider.github` with GitHub Actions thumbprints
- `aws_iam_role.github_terraform_plan` trusts `repo:VictorAurelius/2026-Kite-Class-Platform:*` (var `github_repo`)
- Permissions: `ReadOnlyAccess` managed + inline policy for S3 state read/write + DynamoDB lock CRUD

YAML self-test:
```
$ python3 -c "import yaml; yaml.safe_load(open('.github/workflows/terraform-plan.yml')); print('YAML OK')"
YAML OK
```

## Related

- GAP-395 (Terraform stack)
- GAP-396 (S3 backend)
- GAP-381 agent role: agent writes workflow file ✅, human runs apply ❌
- Wave plan: `documents/03-planning/waves/wave-2026-05-09-37-release-hardening-5-layer.md` Bucket A

## Log

- **2026-05-09:** Status flipped 🔵 OPEN → 🟢 DONE. Workflow + OIDC provider + role shipped same PR. Live plan execution gated by AWS account creation (tracked in GAP-394); workflow ships with graceful skip mode that runs fmt + validate alone until variable set.
