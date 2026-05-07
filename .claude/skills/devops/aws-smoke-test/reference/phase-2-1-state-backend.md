# Phase 2.1 State Backend — Smoke Test Checklist

Verifies S3 state bucket + DynamoDB lock table created by Phase 2.1 bootstrap.

---

## Commands

```bash
# Account verify
aws sts get-caller-identity --query 'Account' --output text  # Expected: 906286017800

# State bucket
aws s3api head-bucket --bucket kitehub-terraform-state-906286017800
# Exit 0 = OK

aws s3api get-bucket-versioning \
  --bucket kitehub-terraform-state-906286017800 \
  --query 'Status' --output text
# Expected: Enabled

aws s3api get-bucket-encryption \
  --bucket kitehub-terraform-state-906286017800 \
  --query 'ServerSideEncryptionConfiguration.Rules[0].ApplyServerSideEncryptionByDefault.SSEAlgorithm' \
  --output text
# Expected: aws:kms or AES256

aws s3api get-public-access-block \
  --bucket kitehub-terraform-state-906286017800 \
  --query 'PublicAccessBlockConfiguration.[BlockPublicAcls,BlockPublicPolicy,IgnorePublicAcls,RestrictPublicBuckets]' \
  --output text
# Expected: True True True True

# Lock table
aws dynamodb describe-table \
  --table-name kitehub-terraform-locks \
  --query 'Table.[TableName,TableStatus,BillingModeSummary.BillingMode,KeySchema[0].AttributeName]' \
  --output table
# Expected: kitehub-terraform-locks ACTIVE PAY_PER_REQUEST LockID
```

---

## Pass criteria

- [ ] Account = 906286017800
- [ ] State bucket exists + accessible
- [ ] Versioning `Enabled`
- [ ] Server-side encryption configured
- [ ] All 4 public access blocks `True`
- [ ] Lock table `ACTIVE`, `PAY_PER_REQUEST`, partition key `LockID`

---

## Tier 1 only

All commands above are `describe-*` / `get-bucket-*` / `head-bucket` — read metadata only, never download object data.

BANNED in this phase: `s3 cp`, `s3 sync`, `s3api get-object`, `dynamodb scan` (could read state file containing IDs/secrets).

---

## When to run

- After initial `terraform apply` of bootstrap module
- After any change to `infrastructure/terraform-aws/backend.tf`
- Quarterly drift check
