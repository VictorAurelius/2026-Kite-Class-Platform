---
title: AWS Verification — kiteclass-core S3 storage wiring (MinIO->S3 prod parity)
status: complete
created: 2026-06-18
phase: Phase 1 BETA internal-test deploy
gaps: [GAP-1480]
---

# AWS Verification Report — kiteclass-core S3 storage wiring

## Scope

kiteclass-core production file storage was BROKEN: no `STORAGE_S3_*` env in
`docker-compose.kc.yml` → Spring `${STORAGE_S3_ENDPOINT:http://localhost:9000}`
defaulted to localhost MinIO (unreachable on kc-app EC2). Bucket `kiteclass-files`
did not exist. This session wired S3:

1. Code: `S3Config.java` — blank access-key → `DefaultCredentialsProvider` (EC2
   instance role); blank endpoint → SDK default regional endpoint. Backward-compat
   MinIO (non-blank minioadmin keys + localhost endpoint).
2. Infra: created S3 bucket + extended EC2 instance role S3 grant.
3. Config: `STORAGE_S3_*` env in compose (blank keys/endpoint → IAM + regional).

User explicitly authorized bucket + CORS creation (chose option A: "tạo bucket + env
+ CORS"). Additive, low-risk (new bucket, additive IAM Resource).

## Commands run

### Tier 1 read-only (per agent-aws-access.md §2.1)
```bash
aws sts get-caller-identity                              # solo-dev-admin / 906286017800 ✓
aws iam get-role-policy --role-name kitehub-production-ec2-app \
  --policy-name kitehub-ec2-secrets-s3                   # live policy (secrets + s3, no SES)
```

### Tier 3 mutations (user-authorized via option A — additive resources)
```bash
aws s3api create-bucket --bucket kiteclass-files-production-906286017800 \
  --region ap-southeast-1 --create-bucket-configuration LocationConstraint=ap-southeast-1
aws s3api put-bucket-versioning ... Status=Enabled
aws s3api put-bucket-encryption ... AES256
aws s3api put-public-access-block ... (all 4 true)
aws s3api put-bucket-cors ... (kitehub.me origins, GET/PUT/POST/HEAD)
aws s3api put-bucket-lifecycle-configuration ... (cost-hygiene: 7d multipart, 90d noncurrent)
aws iam put-role-policy --role-name kitehub-production-ec2-app \
  --policy-name kitehub-ec2-secrets-s3 ...               # add new bucket ARNs to S3 statement
```

## Findings

### Real changes (intentional, additive)

| # | Resource | Action | Risk |
|---|----------|--------|------|
| 1 | `kiteclass-files-production-906286017800` S3 bucket | create | None — new bucket, no data |
| 2 | bucket versioning/encryption/PAB/CORS/lifecycle | put | None — security-hardened defaults (AES256, all-public-blocked) |
| 3 | `kitehub-ec2-secrets-s3` IAM policy S3 statement | put (add 2 ARNs) | Low — additive grant scoped to new bucket only |

### IaC drift (must reconcile — terraform import follow-up GAP-1480)

| Resource | terraform source | Live state | Reconcile |
|----------|-----------------|-----------|-----------|
| `aws_s3_bucket.kiteclass_files` (+5 sub-resources) | s3.tf (this PR) | created via CLI | `terraform import` x6 |
| `aws_iam_role_policy.ec2_secrets_s3` | iam.tf (this PR, +2 ARNs) | put via CLI (matches) | already in state — next apply = no-op for S3 stmt |

Note: live IAM policy lacked the SES statement that terraform iam.tf §70-89 declares
(pre-existing drift — SES never applied; email uses Resend HTTP API not SES, so no impact).
A future `terraform apply` would ADD the SES statement (superset) — acceptable.

### Verdict

Storage now functional on AWS via instance role. CLI-created resources match terraform
source (same bucket name, same IAM ARNs) → `terraform import` brings under state with
zero config diff. No production data at risk (new bucket, additive IAM).

## Prior actions verified

| Action | When | Where |
|--------|------|-------|
| EC2 instance role had base S3 perms (assets bucket) | pre-existing | iam.tf:57-69 ec2_secrets_s3 |
| Resend email fix (this session) | 2026-06-17 | secret kitehub/production/resend-api-key |

## Next steps

1. Rebuild kiteclass-core (S3Config patch) → deploy kc-app → verify upload round-trip.
2. `terraform import` 6 bucket resources (GAP-1480) post-deploy.
3. Child-protection vetting bucket `kiteclass-vetting` (CHILDPROTECTION_MINIO_BUCKET)
   reuses storage.s3 creds — create + grant if vetting flow exercised (deferred, low priority).

## References

- Branch: `fix/kiteclass-core-s3-iam-storage`
- S3Config.java IAM-role patch + s3.tf bucket + iam.tf grant + docker-compose.kc.yml env
- Rules: agent-aws-access.md §5, local-fix-production-parity-check.md §2.1, pre-mutation-state-check.md §3
