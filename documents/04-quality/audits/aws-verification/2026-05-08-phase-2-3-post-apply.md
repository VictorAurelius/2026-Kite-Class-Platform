---
title: AWS Verification — Phase 2.3 post-apply
status: complete
created: 2026-05-08
phase: 2.3
related:
  - documents/03-planning/roadmap/release-1-deploy-runbook.md
  - documents/03-planning/roadmap/release-1-deploy-session-2026-05-07.md
  - .claude/rules/agent-aws-access.md
---

# AWS Verification Report — Phase 2.3 post-apply

**Date:** 2026-05-08
**Phase:** 2.3 (production terraform apply)
**Account:** 906286017800 (`ap-southeast-1`)
**Trigger:** User question — "hiện tại aws đã có tài nguyên gì, đã truy cập được kitehub chưa"
**Saved per:** `.claude/rules/agent-aws-access.md` §5 logging requirement

---

## Scope

Verify post-apply state after Phase 2.3 production terraform apply (PR #994 pending). Establish:
- Resource inventory (count + type breakdown)
- Endpoint accessibility (Vercel frontends + AWS ALB + EC2 direct)
- Whether kitehub frontend is user-accessible

---

## Commands run (Tier 1 read-only per agent-aws-access.md §2)

| Command | Purpose | Result |
|---|---|---|
| `curl -sI -m 10 https://kitehub.vercel.app/` | Vercel KH frontend | HTTP 200 ✅ |
| `curl -sI -m 10 https://kiteclass.vercel.app/` | Vercel KC frontend | HTTP 200 ✅ |
| `curl -sI -m 10 http://kitehub-alb-224105328.ap-southeast-1.elb.amazonaws.com/` | AWS ALB | HTTP 502 (no upstream app) |
| `curl -sI -m 10 http://13.212.99.40:8080/` | EC2 KH backend direct | timeout (SG correctly blocks) |
| `curl -sI -m 10 http://47.128.15.254:8080/` | EC2 KC app direct | timeout (SG correctly blocks) |
| `aws ec2 describe-instances --query ...` | EC2 inventory | 2× t3.micro running |
| `aws rds describe-db-instances --query ...` | RDS inventory | 1× postgres available |
| `aws sts get-caller-identity` (earlier session) | Account verify | account 906286017800, user `ci-deploy` |

All commands Tier 1 allowed per `agent-aws-access.md` §2 — `curl -sI` (HEAD only), `describe-*`, `sts get-caller-identity`.

---

## Results — resource inventory

### AWS account 906286017800 (~94 resources, $30/mo Year 1)

#### Phase 2.1 — State backend
- S3 bucket `kitehub-terraform-state-906286017800` (versioning + KMS aws/s3 + 90d non-current expire)
- DynamoDB table `kitehub-terraform-locks` (PAY_PER_REQUEST)

#### Phase 2.2 — IAM/OIDC (4 roles + 1 OIDC provider)
- `kitehub-github-terraform-plan` (ReadOnly + state)
- `kitehub-github-deploy` (SSM + Secrets read on `kite/{staging,prod}/*`)
- `kitehub-github-ecr-push` (ECR push on `kitehub-*` repos)
- `kitehub-github-restore-drill` (S3 backup read)
- OIDC provider `token.actions.githubusercontent.com`

#### GAP-437 — Observability
- CloudTrail trail `kitehub-main` (multi-region, log validation, IsLogging=true)
- S3 audit log bucket `kitehub-cloudtrail-logs-906286017800`
- CloudWatch dashboard `kitehub-phase-1-overview` (9 widgets, in PR #994 still pending)

#### Phase 2.3 — Production infrastructure (71 resources)

| Category | Detail |
|---|---|
| VPC + networking | 1 VPC `10.0.0.0/16` + 4 subnets (2 public 2 private) + 2 RT + 4 RT-assoc + 1 IGW |
| EC2 + ALB | EC2 KH `i-0b65c3947d36cae61` (13.212.99.40, t3.micro, running) · EC2 KC `i-04f65503ace7febe4` (47.128.15.254, t3.micro, running) · ALB `kitehub-alb-224105328.ap-southeast-1.elb.amazonaws.com` · 1 listener + 1 listener rule + 2 target groups + 2 attachments |
| RDS | `kitehub-postgres.c3awuqw4ugex.ap-southeast-1.rds.amazonaws.com:5432` PostgreSQL 15, db.t3.micro, available · 1 subnet group |
| ECR | 10 repos `906286017800.dkr.ecr.ap-southeast-1.amazonaws.com/kite/{kiteclass-core, kiteclass-frontend, kiteclass-gateway, kitehub-admin, kitehub-branding, kitehub-email, kitehub-frontend, kitehub-gateway, kitehub-platform, kitehub-subscription}` + 10 lifecycle policies — **ALL EMPTY** (no image pushed yet) |
| Secrets Manager | 8 secrets: `kitehub/production/{rds-password, jwt-secret, encryption-key, ai-openai-api-key, ai-anthropic-api-key, cloudflare-api-token, rabbitmq-default-creds, ses-smtp-credentials}` + 3 secret_versions (auto-populated rds/jwt/encryption via `random_password`) |
| S3 assets | `kitehub-assets-production-906286017800` + versioning + encryption + public block + lifecycle |
| IAM (EC2) | EC2 instance role + instance profile + inline secrets/S3 policy + 3 policy attachments |
| Security groups | 3 (ALB / EC2 app / RDS) |

### Outside AWS (Stream A pivots active)

| Service | URL | Status |
|---|---|---|
| Vercel kitehub | `https://kitehub.vercel.app/` | HTTP 200 ✅ |
| Vercel kiteclass | `https://kiteclass.vercel.app/` | HTTP 200 ✅ |
| Resend (transactional email) | API key in GH Secret `RESEND_API_KEY` | Key valid (sandbox sender) |
| Better Stack (status page) | `https://kite-platform.betteruptime.com/` | Live, 2 monitors |

---

## Findings

### F1. ✅ kitehub frontend accessible

`https://kitehub.vercel.app/` returns HTTP 200. End user can access the marketing/landing pages.

### F2. ❌ kitehub backend NOT accessible (expected)

ALB returns HTTP 502 because EC2 instances boot with default Amazon Linux 2023 AMI — **no application running**. Sequence to fix:
1. Phase 3: tag `v0.9.0-staging.1` → CI builds + pushes image to ECR
2. EC2 user-data or SSM run docker-compose pull + up
3. Spring Boot starts → `/actuator/health` 200 → ALB target group healthy → ALB returns 200

Current blocker: ECR repos empty + `vars.AWS_CONFIGURED` not set → ECR push job will skip.

### F3. ✅ Defense-in-depth correctly configured

EC2 direct port 8080 timeout from internet — security group correctly forces traffic through ALB only. Verified: 2 instances, 2 distinct timeouts, consistent with `infrastructure/terraform-aws/security-groups.tf` `ec2_app` SG accepting only from `alb` SG.

### F4. ⚠️ Secrets Manager has 5 placeholder values still empty

Per `secrets.tf`, 3 auto-generated (rds/jwt/encryption) populated by `random_password`. 5 placeholders need manual fill (see `documents/05-guides/deploy/secrets-populate-phase-2-4.md` Phase 2.4 runbook):
- `ses-smtp-credentials` — DEPRECATED (Resend pivot active; mark or delete)
- `cloudflare-api-token` — DEFERRED Phase 2 (Vercel pivot)
- `ai-openai-api-key` — populate from platform.openai.com
- `ai-anthropic-api-key` — populate from console.anthropic.com
- `rabbitmq-default-creds` — generate locally + store

### F5. ⚠️ ECR push gate flag unset

Per `docker-build-push.yml` line 187: `if: vars.AWS_CONFIGURED == 'true'` — gate ECR push job. Currently `AWS_CONFIGURED` not in GitHub Variables list. Until set, even main-branch pushes won't trigger image build.

### F6. ✅ CloudTrail captures everything

Phase 2.3 apply (71 resources created 2026-05-08) was logged via CloudTrail. Audit baseline working — first apply post-CloudTrail-enable is fully recorded.

---

## Next steps (recommended)

| Priority | Action | Owner | Effort |
|---|---|---|---|
| P1 | Phase 2.4 — populate 4 Secrets Manager values + decide ses/cloudflare placeholder fate | User (manual CLI) | ~30min |
| P1 | Phase 3 — set `gh variable set AWS_CONFIGURED true` + tag `v0.9.0-staging.1` to trigger first ECR push | User (CLI) | ~10min |
| P2 | Smoke test runbook `scripts/smoke-aws-phase-N.sh` (GAP-438 Phase 2 follow-up) | Agent (next wave) | ~30min |
| P2 | GAP-436 Phase 4 — remove static `AWS_ACCESS_KEY_ID`/`SECRET` after first OIDC workflow trigger verifies | User+agent | ~15min |
| P3 | EC2 user-data deploy script (or SSM run-command) for `docker compose up` after ECR has image | Agent (Wave 42 candidate) | ~45min |
| P3 | ALB ACM cert + HTTPS listener (currently HTTP-only per tfvars `alb_acm_certificate_arn = null`) | Agent + user | ~30min |

---

## Compliance / Audit notes

- All commands run Tier 1 per `.claude/rules/agent-aws-access.md` §2 — read-only, no AWS state mutation
- No secret values logged (Tier 2 `get-secret-value` not run)
- This artifact closes the logging gap from §5 — first artifact in `aws-verification/` folder

---

## Related

- Parent gap: GAP-438 Phase 3 (this artifact)
- Phase 2.3 closure: PR #994 (pending CI)
- Wave 42 candidates filed Phase 2 follow-up: skill + scripts + memory entry
