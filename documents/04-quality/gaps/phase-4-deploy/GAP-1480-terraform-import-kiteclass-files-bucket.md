# GAP-1480: terraform import kiteclass-files bucket + IAM (IaC drift reconcile)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** DevOps
**Found:** 2026-06-18 (kiteclass-core S3 storage wiring, internal-test deploy)
**Affects:** `infrastructure/terraform-aws/s3.tf`, `iam.tf` ↔ live AWS state

## Problem

Để wire production storage cho kiteclass-core ngay cho internal test, bucket
`kiteclass-files-production-906286017800` + 5 sub-resources (versioning / encryption /
public-access-block / CORS / lifecycle) được tạo **out-of-band qua AWS CLI**, và IAM
policy `kitehub-ec2-secrets-s3` được extend qua `put-role-policy`. Terraform source
(`s3.tf` + `iam.tf`) đã ship cùng PR khớp tên/ARN, NHƯNG live resources CHƯA nằm trong
terraform state → IaC drift: lần `terraform apply` tới sẽ cố **create** bucket đã tồn tại
→ `BucketAlreadyOwnedByYou` error, hoặc teardown/restore mất bucket.

Pattern giống GAP-717 (JWT secret manual-create không có terraform IaC). Per
`local-fix-production-parity-check.md` §2.1 — manual resource creation = same-PR IaC
ship (done) + follow-up import (this gap).

Phụ: live IAM policy `kitehub-ec2-secrets-s3` thiếu SES statement mà iam.tf §70-89 khai
báo (drift cũ, pre-existing — SES chưa bao giờ apply; email dùng Resend HTTP API nên
không ảnh hưởng). Apply tới sẽ ADD lại SES (superset, acceptable).

## Proposed Fix

`terraform import` 6 resources vào state, rồi `terraform plan` xác nhận zero-diff
cho bucket (SES statement sẽ show as add — acceptable):

```bash
cd infrastructure/terraform-aws
terraform import aws_s3_bucket.kiteclass_files kiteclass-files-production-906286017800
terraform import aws_s3_bucket_versioning.kiteclass_files kiteclass-files-production-906286017800
terraform import aws_s3_bucket_server_side_encryption_configuration.kiteclass_files kiteclass-files-production-906286017800
terraform import aws_s3_bucket_public_access_block.kiteclass_files kiteclass-files-production-906286017800
terraform import aws_s3_bucket_cors_configuration.kiteclass_files kiteclass-files-production-906286017800
terraform import aws_s3_bucket_lifecycle_configuration.kiteclass_files kiteclass-files-production-906286017800
terraform plan   # expect 0 change for bucket; ec2_secrets_s3 may show SES stmt add
```

## Acceptance Criteria

- [ ] 6 bucket resources imported vào terraform state
- [ ] `terraform plan` cho bucket = no-change (config khớp live)
- [ ] IAM `ec2_secrets_s3` S3 statement plan = no-change (2 new ARNs khớp)
- [ ] SES statement add (nếu apply) verified intentional

## Related

- Discovered in: branch `fix/kiteclass-core-s3-iam-storage` (S3 storage wiring)
- Audit: `documents/04-quality/audits/aws-verification/2026-06-18-kiteclass-core-s3-storage-wiring.md`
- Pattern sister: GAP-717 (JWT secret IaC drift, same class)
- Rule: `local-fix-production-parity-check.md` §2.1, `pre-mutation-state-check.md`
