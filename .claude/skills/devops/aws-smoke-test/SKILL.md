---
name: aws-smoke-test
description: >
  Dùng khi user nói "smoke test AWS", "verify Phase 2.3", "kiểm tra AWS sau apply",
  "post-apply check", "verify AWS resources", "AWS health check", "kiểm tra hạ tầng AWS",
  "Phase 2.1 verify", "Phase 2.2 OIDC check", "Phase 4 staging verify",
  hoặc khi vừa hoàn tất `terraform apply` và cần verify state. Tier 1 read-only ONLY
  per `.claude/rules/agent-aws-access.md` §2 — không create/delete/modify resources,
  không đọc secret values. Output: pass/fail summary + audit artifact saved to
  `documents/04-quality/audits/aws-verification/YYYY-MM-DD-<topic>.md`.
---

# AWS Smoke Test Skill

Codifies the read-only verification pattern established by `.claude/rules/agent-aws-access.md`.
Use after every non-trivial `terraform apply` against the kitehub AWS account (906286017800, `ap-southeast-1`).

## When to invoke

| Phase | Script | Reference |
|-------|--------|-----------|
| 2.1 — State backend (S3 + DynamoDB) | manual checklist | `reference/phase-2-1-state-backend.md` |
| 2.2 — OIDC IAM roles | manual checklist | `reference/phase-2-2-oidc.md` |
| 2.3 — Production infra (EC2 + RDS + ALB + ECR + Secrets) | `scripts/smoke-aws-phase-2-3.sh` | `reference/phase-2-3-production.md` |
| 4 — Staging environment | `scripts/smoke-aws-phase-4.sh` (TBD) | `reference/phase-4-staging.md` |

## Process

1. **Confirm scope** — which phase? read the matching `reference/phase-*.md` file
2. **Verify Tier 1 only** — every command in checklist matches `agent-aws-access.md` §2.1 allowed verbs (`describe-*`, `list-*`, allowed `get-*`, `head-*`, `curl -sI`)
3. **Run script** if available (Phase 2.3), else execute checklist commands one-by-one and capture output
4. **Save artifact** — copy `reference/audit-artifact-template.md` to `documents/04-quality/audits/aws-verification/YYYY-MM-DD-<phase>-<topic>.md`, fill in
5. **Report back** — pass/fail summary + artifact path + any findings (anomalies → file follow-up gap per `audit-to-gap-pipeline.md`)

## Tier 1 ALLOWED commands (per agent-aws-access.md §2.1)

- `aws ec2 describe-*`, `aws rds describe-*`, `aws elbv2 describe-*`, `aws cloudtrail describe-*`
- `aws s3api list-buckets`, `aws s3api head-bucket`, `aws ecr describe-repositories`
- `aws secretsmanager describe-secret`, `aws secretsmanager list-secrets` (NOT `get-secret-value`)
- `aws sts get-caller-identity`, `aws cloudtrail get-trail-status`
- `aws dynamodb describe-table`, `aws iam list-roles`, `aws iam get-role` (metadata only)
- `curl -sI` / `curl -s -o /dev/null -w "%{http_code}"` against AWS-hosted endpoints

## Tier 2/3 BANNED in this skill

- ❌ `aws secretsmanager get-secret-value` (secret material — Tier 2 always-confirm)
- ❌ `aws ssm get-parameter --with-decryption` (Tier 2)
- ❌ `aws kms decrypt` (Tier 2)
- ❌ Any `create-*`/`delete-*`/`put-*`/`update-*`/`modify-*`/`terminate-*` (Tier 3 — banned for agent)
- ❌ `terraform apply`/`destroy`/`import`/`state rm/mv/push` (Tier 3)
- ❌ `aws s3 cp`/`sync` from unknown buckets (data exfil risk)

If verification needs Tier 2/3 → STOP and ask user via AskUserQuestion per `agent-aws-access.md` §3.

## Skill contents

- `reference/phase-2-3-production.md` — Phase 2.3 detailed checklist (most-used)
- `reference/phase-2-1-state-backend.md` — bootstrap S3 + DynamoDB
- `reference/phase-2-2-oidc.md` — IAM/OIDC roles + GitHub Actions
- `reference/phase-4-staging.md` — staging environment variant
- `reference/audit-artifact-template.md` — copy-to-fill template for `aws-verification/` folder
- `scripts/smoke-aws-phase-2-3.sh` (in repo `scripts/`) — automates Phase 2.3 checks

## Gotchas

- **Region pinning** — always `--region ap-southeast-1`; some commands inherit profile default and silently query wrong region
- **Account verify FIRST** — run `aws sts get-caller-identity` once at start; abort if account ≠ 906286017800 (prevent cross-account leakage)
- **`curl -sI` only for HEAD** — never use `curl -X POST/PUT/DELETE` even on AWS endpoints (Tier 3 banned)
- **Empty ECR repos are normal** until first `v0.9.0-staging.*` tag triggers `docker-build-push.yml`
- **ALB 502 is expected** if EC2 boots vanilla AMI — application not deployed yet (Phase 3 fixes)
- **Save artifact even if everything PASS** — §5 logging requirement is not just for failures; audit trail covers happy path too

## Related

- Rule: `.claude/rules/agent-aws-access.md` (Tier matrix + logging requirement)
- Rule: `.claude/rules/release-deploy-standard.md` §9 (Claude agent role in deploy)
- Rule: `.claude/rules/aws-observability-first.md` (CloudTrail before infra apply)
- First artifact: `documents/04-quality/audits/aws-verification/2026-05-08-phase-2-3-post-apply.md`
- Parent gap: GAP-438 (Phase 2 = this skill)
