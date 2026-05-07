# GAP-397: Terraform Plan in CI (PR Validation)

**Status:** 🔵 OPEN
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

- [ ] `.github/workflows/terraform-plan.yml` exists
- [ ] OIDC role `arn:aws:iam::xxx:role/github-actions-terraform-plan` (read-only AWS perms)
- [ ] `actions/github-script` posts plan output as PR comment
- [ ] Workflow YAML validated (`python3 -c "import yaml; yaml.safe_load(...)"`)
- [ ] Self-test: PR touching `terraform-aws/main.tf` → plan output comment appears

## Related

- GAP-395 (Terraform stack)
- GAP-396 (S3 backend)
- GAP-381 agent role: agent writes workflow file ✅, human runs apply ❌
