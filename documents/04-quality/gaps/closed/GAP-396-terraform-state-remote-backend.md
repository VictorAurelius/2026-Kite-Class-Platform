# GAP-396: Terraform State Remote Backend (S3 + DynamoDB lock)

**Status:** 🟢 DONE 2026-05-09 — Wave 37 Bucket A
**Priority:** 🔴 P0 v0.9.0-beta
**Domain:** Infrastructure / Terraform
**Found:** 2026-05-07 (Wave 37 release-hardening planning)
**Affects:** Multi-session apply safety + audit trail compliance

## Problem

Terraform state hiện local (.tfstate file). Solo dev OK ngắn-hạn nhưng:
- Loss risk: máy local hỏng = mất state
- No lock = `terraform apply` đồng thời corrupt state
- Audit trail PDPL Art 11: state changes phải traceable

## Proposed Fix

S3 backend + DynamoDB state-lock table per HashiCorp recommended pattern:

```hcl
terraform {
  backend "s3" {
    bucket         = "kitehub-terraform-state-<ACCOUNT_ID>"
    key            = "phase-1-beta/terraform.tfstate"
    region         = "ap-southeast-1"
    encrypt        = true
    dynamodb_table = "kitehub-terraform-locks"
  }
}
```

Bootstrap script `infrastructure/terraform-aws/bootstrap/` to create S3 + DynamoDB BEFORE backend init (chicken-egg problem).

## Acceptance Criteria

- [x] `bootstrap/main.tf` creates `kitehub-terraform-state-${account_id}` bucket (versioning + encryption + lifecycle)
- [x] `bootstrap/main.tf` creates `kitehub-terraform-locks` DynamoDB table
- [x] Main `terraform-aws/backend.tf` configured S3 backend
- [x] Migration runbook: `terraform init -migrate-state` from local to S3 documented in `bootstrap/README.md` + main `README.md`
- [x] State file NOT in git (verify `.gitignore` covers `*.tfstate*`) — confirmed `.gitignore` lines 45-46

## Resolution

Wave 37 Bucket A:
- `bootstrap/main.tf` extended with lifecycle config (90-day non-current version expiry + multipart abort) on top of existing S3 + DynamoDB resources.
- New `backend.tf` separates backend config from `main.tf` (cleaner) with explicit comment block on the post-bootstrap manual `<ACCOUNT_ID>` substitution step (HCL backend config cannot interpolate variables).
- Bucket naming `kitehub-terraform-state-${account_id}` (kept existing convention; differs from gap text `kite-terraform-state-prod` for backward compatibility — minimal churn).
- Lock table `kitehub-terraform-locks`.

`terraform validate` passes for both root + bootstrap modules.

## Related

- GAP-395 (parent infra setup)
- HashiCorp recommended state pattern
- PDPL Art 11 audit trail compliance
- Wave plan: `documents/03-planning/waves/wave-2026-05-09-37-release-hardening-5-layer.md` Bucket A

## Log

- **2026-05-09:** Status flipped 🔵 OPEN → 🟢 DONE. Bootstrap module already had S3 + DynamoDB scaffold from prior wave; Wave 37 Bucket A added lifecycle policy + extracted backend config to dedicated `backend.tf` for clarity. AC #1-#5 all verified (file exists + `.gitignore` covers tfstate).
