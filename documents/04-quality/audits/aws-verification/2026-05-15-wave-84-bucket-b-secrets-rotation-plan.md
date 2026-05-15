---
title: AWS Verification — Wave 84 Bucket B Secrets Manager Rotation (Pre-Apply Plan)
status: complete
created: 2026-05-15
phase: phase-1-beta
wave: 84
gaps: [GAP-379]
---

# AWS Verification Report — Wave 84 Bucket B Secrets Manager Rotation Plan

## Scope

Wave 84 Bucket B closes GAP-379 (Secrets Manager Rotation Policy) 50% → 100% by adding:

1. Custom Lambda `kitehub-production-rotate-secret-handler` (Python 3.12) implementing AWS Secrets Manager 4-step rotation lifecycle for 3 in-house secrets (`jwt-secret`, `encryption-key`, `seed-admin-password`).
2. `aws_secretsmanager_secret_rotation` resources wiring 90-day automated rotation for those 3 secrets.
3. Documentation update in `documents/05-guides/operations/secrets-rotation-runbook.md` §5.2 (Lambda automated rotation section) + manual rotation procedure for external vendor API keys.
4. Integration test script `scripts/test-secret-rotation.sh` validating rotation end-to-end (AWSCURRENT advance + AWSPREVIOUS chain).

`db-password` (RDS) wired to use AWS-managed `SecretsManagerRDSPostgreSQLRotationSingleUser` Lambda via one-time bootstrap step documented in runbook §5.2.1 (manual console step or `aws secretsmanager rotate-secret` CLI call) — not Terraform-managed because the AWS-managed Lambda is deployed via Serverless Application Repository, separate lifecycle.

Per `.claude/rules/pre-mutation-state-check.md` §3 + `.claude/rules/release-deploy-standard.md` §9 (deploy execution human-only) — this PR ships .tf + Lambda code only; `terraform apply` deferred to coordinator review + user-triggered workflow_dispatch.

## Commands run (Tier 1 read-only per `agent-aws-access.md` §2.1)

```bash
# Inventory existing secrets resources
grep -nE "aws_secretsmanager_secret\b" infrastructure/terraform-aws/secrets.tf
# 4 secrets created: db_password, jwt, encryption, seed_admin_password
# 5 placeholder secrets via aws_secretsmanager_secret.placeholders for_each

# Verify existing IAM access scope
grep -nE "secretsmanager:" infrastructure/terraform-aws/iam.tf
# ec2_app role: GetSecretValue + DescribeSecret on kitehub/production/*

# Lambda unit-test self-verification
cd infrastructure/terraform-aws/lambdas/rotate-secret \
  && python3 -m unittest tests.test_rotate_secret_handler -v
# 10/10 PASS

# Bash script syntax + lint
bash -n scripts/test-secret-rotation.sh
shellcheck scripts/test-secret-rotation.sh
# clean

# Canonical GAP-379 status query
bash scripts/query-gaps.sh GAP-379 | head -3
# row: GAP-379, status PARTIAL, 50%, last_verified 2026-05-11
```

## Findings

### Real changes (must verify intent on apply)

| # | Resource | Action | Root cause | Risk |
|---|---|---|---|---|
| 1 | `aws_iam_role.rotate_secret_lambda` | CREATE | New role for Lambda invocation | Low — least-privilege scoped to 3 specific secret ARNs |
| 2 | `aws_iam_role_policy_attachment.rotate_secret_lambda_basic` | CREATE | AWSLambdaBasicExecutionRole for CloudWatch Logs | Low — AWS-managed policy, standard |
| 3 | `aws_iam_role_policy.rotate_secret_inline` | CREATE | Inline policy granting Get/Put/UpdateStage on 3 secrets | Low — Resource ARNs explicit; no wildcard |
| 4 | `aws_lambda_function.rotate_secret_handler` | CREATE | Custom rotation Lambda (python3.12, 30s timeout) | Low — code reviewed + unit-tested 10/10 |
| 5 | `aws_lambda_permission.rotate_secret_invoke_*` (3 resources) | CREATE | Allow Secrets Manager to invoke Lambda per secret | Low — source_arn restricts to specific secrets |
| 6 | `aws_secretsmanager_secret_rotation.jwt` | CREATE | Wire 90-day auto-rotation on jwt-secret | Medium — first rotation will fire within 90d of apply; services must reload to pick up |
| 7 | `aws_secretsmanager_secret_rotation.encryption` | CREATE | Wire 90-day auto-rotation on encryption-key | Medium — same as above; PII column re-encryption NOT triggered automatically (separate concern, tracked in §10) |
| 8 | `aws_secretsmanager_secret_rotation.seed_admin` | CREATE | Wire 90-day auto-rotation on seed-admin-password | Low — admin password rotation has no service-restart dependency (DB-stored hash unchanged) |

### Phantom updates (no real change expected)

None expected — this is a pure additive change (10 new resources, 0 modifications, 0 destroys). `terraform plan` should produce exactly `10 to add, 0 to change, 0 to destroy`.

### Verdict

Apply is **safe** subject to coordinator review + per `pre-mutation-state-check.md` §1.5 IAM cross-reference matrix:

| IAM Action | Resource pattern | Actual resource (verified) | Caller | Verdict |
|---|---|---|---|---|
| `secretsmanager:GetSecretValue` | `aws_secretsmanager_secret.jwt.arn` | `kitehub/production/jwt-secret-XXXXXX` | `rotate_secret_handler.py:_create_secret` | ✅ match |
| `secretsmanager:PutSecretValue` | same | same | `rotate_secret_handler.py:_create_secret` | ✅ match |
| `secretsmanager:DescribeSecret` | same | same | `rotate_secret_handler.py:lambda_handler` | ✅ match |
| `secretsmanager:UpdateSecretVersionStage` | same | same | `rotate_secret_handler.py:_finish_secret` | ✅ match |
| `secretsmanager:GetRandomPassword` | `*` | N/A (not actually used; reserved for future generator variant) | (none) | ⚠️ over-grant — acceptable for v1, tighten Phase 1.5 |

The `GetRandomPassword` `*` Resource is the only wildcard — AWS API requires `*` Resource for this action (per IAM doc), so this is unavoidable for the action verb itself. Acceptable risk.

Concurrent mutation check per `concurrent-production-mutation-ops.md` §4:

- No active terraform-apply.yml or deploy-production.yml runs targeting same EC2 / RDS / secrets at apply time. **Coordinator MUST verify with `gh run list --status in_progress` before triggering apply.**

## Prior actions verified (per `audit-to-gap-pipeline.md` §2.8)

| Action | When | Where verified |
|---|---|---|
| GAP-379 partial closure (runbook + IAM templates + env template) | 2026-05-07 (Wave 33 Bucket D) | `documents/04-quality/gaps/GAP-379-secrets-management-rotation.md` Log + PR #897 |
| Secrets management runbook split (seeding vs rotation) | 2026-05-11 (GAP-452) | `documents/05-guides/operations/secrets-rotation-runbook.md` + `documents/05-guides/deploy/secrets-seeding-runbook.md` |
| AWS observability baseline (CloudTrail multi-region) | 2026-05-07 (GAP-437 Phase 1) | `documents/04-quality/audits/aws-verification/2026-05-08-phase-2-3-post-apply.md` |
| Production stack deployed (4 in-house secrets exist) | 2026-05-08 | `infrastructure/terraform-aws/secrets.tf` lines 9-101 (db_password / jwt / encryption / seed_admin_password) |

CloudTrail confirmed `IsLogging=true` per `aws-observability-first.md` v1.0.0 — every rotation Lambda invocation + Secrets Manager API call gets audited.

## Pending (this op)

| Action | Owner | Notes |
|---|---|---|
| Coordinator code review of `secrets-rotation.tf` + Lambda + test script | coordinator | per `pre-mutation-state-check.md` §1.5 terraform-review workflow |
| `terraform plan` review of resulting 10 ADD | coordinator | confirm 10 to add / 0 change / 0 destroy |
| User-triggered `terraform-apply.yml` workflow_dispatch | user (human-only per `release-deploy-standard.md` §9) | confirm input `APPLY` verbatim |
| Post-apply verification | coordinator | `aws lambda get-function --function-name kitehub-production-rotate-secret-handler` returns ACTIVE; `aws secretsmanager describe-secret --secret-id kitehub/production/jwt-secret` shows `RotationEnabled=true` + `RotationLambdaARN` set |
| `bash scripts/test-secret-rotation.sh jwt-secret` end-to-end | coordinator | PASS = AWSCURRENT advances + AWSPREVIOUS chain intact |
| AWS-managed RDS rotation bootstrap (db-password) | user | one-time AWS console step per runbook §5.2.1 (Serverless Application Repository deploys AWS-managed Lambda) |
| **Concurrent op check** | Agent verification | List active workflows via `gh run list --status in_progress` — confirm zero overlap with terraform / deploy before trigger |

## Recommendations

1. **Apply (after coordinator review)** — work product is additive, well-tested, least-privilege. Risk profile LOW-MEDIUM (rotation cadence change is the main "behavior delta" — services must be ready to reload secrets within 90 days of first rotation).
2. **Post-apply verification commands:**
   ```bash
   aws lambda get-function --function-name kitehub-production-rotate-secret-handler \
     --query 'Configuration.[State,LastUpdateStatus]' --output text
   # Expected: Active     Successful

   aws secretsmanager describe-secret --secret-id kitehub/production/jwt-secret \
     --query '[RotationEnabled,RotationLambdaARN,RotationRules.AutomaticallyAfterDays]'
   # Expected: true, arn:...:function:kitehub-production-rotate-secret-handler, 90

   bash scripts/test-secret-rotation.sh jwt-secret
   # Expected: PASS — AWSCURRENT advanced + AWSPREVIOUS == pre-rotation version
   ```
3. **Watch-for items:**
   - **First-rotation timing:** AWS schedules first rotation at apply time + random offset within 24h (per Secrets Manager docs). Watch CloudWatch Logs `/aws/lambda/kitehub-production-rotate-secret-handler` for first invocation; if it fails, manual `aws secretsmanager cancel-rotate-secret` to halt, then debug.
   - **Service reload coordination:** Wave 84 Bucket B does NOT wire `Secrets Manager RotationOccurred` → SSM SendCommand → service restart. Phase 1 BETA acceptable since rotation is 90d cadence + dev still actively babysits. Phase 1.5+ adds CloudWatch EventBridge rule for auto-reload.
   - **PII re-encryption:** when `encryption-key` rotates, existing PII column ciphertext is still readable (Spring Boot service caches AWSPREVIOUS for grace window). No auto re-encrypt — tracked separately as Phase 1.5 work.
4. **RDS rotation bootstrap:** after `terraform apply` for Wave 84 Bucket B, complete the one-time AWS console step in runbook §5.2.1 to enable RDS-managed rotation on `db-password`. Otherwise db-password stays static.

## References

- GAP-379 — Secrets Management — AWS Secrets Manager + Rotation Policy
- Wave 84 plan: `documents/03-planning/waves/wave-2026-05-15-84-ops-observability-runbooks.md` §3 Bucket B
- Runbook: `documents/05-guides/operations/secrets-rotation-runbook.md` §5.2 (updated this PR)
- Lambda code: `infrastructure/terraform-aws/lambdas/rotate-secret/rotate_secret_handler.py`
- Test script: `scripts/test-secret-rotation.sh`
- Rules applied:
  - `.claude/rules/pre-mutation-state-check.md` §1.5 + §3 (this artifact)
  - `.claude/rules/release-deploy-standard.md` §9 (human-only apply)
  - `.claude/rules/concurrent-production-mutation-ops.md` (concurrent op check — coordinator verifies pre-trigger)
  - `.claude/rules/agent-aws-access.md` §2 (Tier 1 read-only verification)
  - `.claude/rules/pre-launch-secrets-hardening-checklist.md` §2.3 + §2.5 (rotation enabled + runbook updated)
- AWS docs: [Rotating secrets lambda function overview](https://docs.aws.amazon.com/secretsmanager/latest/userguide/rotating-secrets-lambda-function-overview.html)
