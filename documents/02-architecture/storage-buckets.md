---
title: Storage Buckets Inventory + Access Control Audit
status: complete
created: 2026-05-27
last_reviewed: 2026-05-27
audience: mixed
owner: backend + devops
---

# Storage Buckets — Inventory + Access Control Audit

Audit toàn bộ object-storage bucket (MinIO local + AWS S3 production); list + verify access control private vs public; cross-reference IaC declarations; flag bucket public ngoài kỳ vọng.

Rule reference: `.claude/rules/pre-launch-infra-hardening-checklist.md` §2.7. Applied Wave beta-prep-1 Bucket B item 4.

## 1. AWS S3 production buckets

| Bucket logical | Resource | Source | Access | Status |
|---|---|---|---|---|
| Assets | `aws_s3_bucket.assets` | `infrastructure/terraform-aws/s3.tf` | Private (block_public_acls + block_public_policy + ignore_public_acls + restrict_public_buckets all true) | COMPLIANT |
| CloudTrail audit logs | `aws_s3_bucket.cloudtrail_logs` | `infrastructure/terraform-aws/cloudtrail.tf` | Private (public_access_block all true; bucket policy CloudTrail service principal only) | COMPLIANT |
| Staging assets | `aws_s3_bucket.staging_assets` | `infrastructure/terraform-aws/staging.tf` | Private (public_access_block all true) | COMPLIANT |
| Terraform state backend | `kite-terraform-state-*` (partial backend) | `backend.config` gitignored | Private (account-bound 906286017800) | COMPLIANT |

### 1.1 Encryption posture

| Bucket | SSE | KMS | Versioning |
|---|---|---|---|
| assets | AES256 | AWS-managed `aws/s3` (CMK upgrade tracked per pre-launch-secrets-hardening §2.4) | Enabled |
| cloudtrail_logs | AES256 | AWS-managed | Enabled |
| staging_assets | AES256 | AWS-managed | Enabled |

### 1.2 Lifecycle policy

- assets: cost-hygiene rule active (abort incomplete multipart >7d; non-current version expire >90d)
- cloudtrail_logs: retention per CloudTrail config (90 days default; audit retention per logs-format-standard.md §4 = 7 years cho security logs — gap noted for extending lifecycle to 7-year tier)

## 2. MinIO local development buckets

| Bucket | Service | Purpose | Access |
|---|---|---|---|
| `kitehub-assets` | `kite-minio` | Dev branding asset storage | Private (dev-only) |
| `kitehub-branding` | same | AI Branding generated assets | Private |

Per ADR-025 Phase 1 BETA migration: MinIO buckets dev-only; production cutover to `aws_s3_bucket.assets`.

## 3. Public bucket scan

### 3.1 Terraform grep

```bash
grep -rn "block_public_policy\s*=\s*false\|block_public_acls\s*=\s*false" infrastructure/terraform-aws/*.tf
```

Result 2026-05-27: ZERO results — all 3 production S3 buckets enforce `block_public_*` = true.

### 3.2 AWS CLI verify (deferred)

Per `agent-aws-access.md` §2.1 Tier 1 read-only:

```bash
aws s3api list-buckets --query 'Buckets[].Name'
aws s3api get-public-access-block --bucket <name>
```

Status: Deferred — AWS account 906286017800 trạng thái live verify dependency GAP-612 unblock. IaC code-level audit per §1 + §3.1 sufficient cho Wave beta-prep-1 Bucket B scope.

## 4. Cross-reference với code consumers

Service consume S3 assets via `S3StorageService` (kitehub-branding):
- Upload path: server-side multipart upload via IAM role
- Read path: signed GET URL with TTL (no public direct-link)

Verified: no `aws s3 cp s3://...` or public URL pattern hardcoded trong source.

## 5. Verdict

PASS Phase 1 BETA scope (Bucket B item 4):
- 3 production S3 buckets ALL private + IaC enforced
- Encryption AES256 enabled
- Versioning enabled
- No bucket policy override / public ACL detected

## 6. Follow-ups (not Bucket B scope)

1. KMS CMK upgrade migrate from AWS-managed to customer-managed
2. CloudTrail bucket 7-year retention extension
3. AWS CLI live verify post GAP-612 unblock

## 7. References

- IaC source: `infrastructure/terraform-aws/{s3,cloudtrail,staging}.tf`
- Rule: `pre-launch-infra-hardening-checklist.md` §2.7
- Wave context: `documents/03-planning/waves/wave-2026-05-26-beta-prep-1-mega.md` Bucket B item 4
