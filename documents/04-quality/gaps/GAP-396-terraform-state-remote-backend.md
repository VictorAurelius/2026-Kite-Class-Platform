# GAP-396: Terraform State Remote Backend (S3 + DynamoDB lock)

**Status:** 🔵 OPEN
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
    bucket         = "kite-terraform-state-prod"
    key            = "phase-1-beta/terraform.tfstate"
    region         = "ap-southeast-1"
    encrypt        = true
    dynamodb_table = "kite-terraform-locks"
  }
}
```

Bootstrap script `infrastructure/terraform-aws/bootstrap/` to create S3 + DynamoDB BEFORE backend init (chicken-egg problem).

## Acceptance Criteria

- [ ] `bootstrap/main.tf` creates `kite-terraform-state-prod` bucket (versioning + encryption + lifecycle)
- [ ] `bootstrap/main.tf` creates `kite-terraform-locks` DynamoDB table
- [ ] Main `terraform-aws/backend.tf` configured S3 backend
- [ ] Migration runbook: `terraform init -migrate-state` from local to S3
- [ ] State file NOT in git (verify `.gitignore` covers `*.tfstate*`)

## Related

- GAP-395 (parent infra setup)
- HashiCorp recommended state pattern
- PDPL Art 11 audit trail compliance
