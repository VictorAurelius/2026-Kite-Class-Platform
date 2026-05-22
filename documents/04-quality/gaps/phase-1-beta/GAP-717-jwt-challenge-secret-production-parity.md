---
id: GAP-717
title: JWT_CHALLENGE_SECRET production parity — local fix Wave 104.5 added env var to docker-compose only, production deploy chain missing
status: OPEN
priority: P1
phase: phase-1-beta
audience: dev
found: 2026-05-22
related: [GAP-711, GAP-705, GAP-706, GAP-612, GAP-718]
---

# GAP-717 — JWT_CHALLENGE_SECRET production parity

## Problem

Wave 104.5 added `JWT_CHALLENGE_SECRET` env var to **`kitehub/docker-compose.kitehub.yml`** only (commit `b45f9b28` per PR #1715), enabling 2FA challenge token verification at gateway+subscription via HS256. **Production deploy chain KHÔNG có equivalent env injection** — when production deploy executes, gateway + subscription containers sẽ KHÔNG có `JWT_CHALLENGE_SECRET` env, fall back to `challengeSigningKey=null`, 2FA enroll-init via gateway sẽ fail HTTP 401 IDENTICAL với behavior local pre-fix Wave 104.5.

## Root cause

Per outside-in audit Wave 104.5 follow-up 2026-05-22, meta gap surfaced: **no rule mandates local code/config fix → check production-equivalent surface trong cùng PR**. Existing rules cover:
- `production-env-config-registry.md` §11 audit scripts run pre-release, NOT per-fix
- `audit-to-gap-pipeline.md` §2.7 Decision-Doc Code-Sync — covers decision-doc → code sweep, **NOT inverse direction** (code-fix → prod-env-sweep)
- `release-deploy-standard.md` §3.1 Secrets management — one-off pre-release checklist

→ Wave 104.5 fix slipped through without prod-parity check. Sister meta-rule GAP-718 paired with this fix.

## Production parity required

| Surface | Status | Required artifact |
|---|---|---|
| Local Docker Compose | ✅ DONE | `kitehub/docker-compose.kitehub.yml` (commit `b45f9b28`) |
| AWS Secrets Manager | ❌ missing | `kitehub/production/jwt-challenge-secret` (new secret resource) |
| Terraform IAM grants | ❌ missing | Grant gateway + subscription deploy roles `secretsmanager:GetSecretValue` on new secret ARN |
| EC2 systemd env injection (current Phase 1 deploy) | ❌ missing | Add `JWT_CHALLENGE_SECRET=$(aws secretsmanager get-secret-value ...)` to deploy script `scripts/deploy-prod.sh` |
| Helm chart values (future K8s deploy Phase 2) | ❌ missing | `infrastructure/helm/values-production.yaml` add `env: JWT_CHALLENGE_SECRET` from secret reference |
| Registry doc | ❌ missing | `documents/02-architecture/env-vars-registry.md` row for `JWT_CHALLENGE_SECRET` |

## Constraint

**Cannot verify production runtime** until AWS account 906286017800 restored (GAP-612 SUSPENDED). Code/terraform changes CAN ship now (not requiring production access); verification deferred to post-restore.

## Proposed Fix

**Wave 105 Bucket E0 (NEW — meta-priority before persona walks):**

1. Add secret to `infrastructure/terraform-aws/secrets.tf`:
   ```hcl
   resource "aws_secretsmanager_secret" "jwt_challenge_secret" {
     name = "kitehub/production/jwt-challenge-secret"
     description = "HS256 secret cho 2FA challenge token verify — Wave 104.5 GAP-705/706 enablement"
     kms_key_id = aws_kms_key.kitehub_main.arn
     tags = { Project = "Kite", Environment = "production" }
   }

   resource "aws_secretsmanager_secret_version" "jwt_challenge_secret" {
     secret_id = aws_secretsmanager_secret.jwt_challenge_secret.id
     secret_string = random_password.jwt_challenge_secret.result
   }

   resource "random_password" "jwt_challenge_secret" {
     length = 64
     special = false
   }
   ```

2. Update IAM in `infrastructure/terraform-aws/iam-deploy.tf` granting gateway + subscription roles read access.

3. Update `scripts/deploy-prod.sh` to fetch secret + inject env via SSM SendCommand (current deploy mechanism):
   ```bash
   export JWT_CHALLENGE_SECRET=$(aws secretsmanager get-secret-value \
     --secret-id kitehub/production/jwt-challenge-secret \
     --query SecretString --output text)
   ```

4. Add row to `documents/02-architecture/env-vars-registry.md`:
   ```
   | JWT_CHALLENGE_SECRET | gateway + subscription | AWS Secrets Manager | yes | Wave 104.5 GAP-711 |
   ```

5. Document live verify procedure để run sau AWS restore (GAP-612 unblock).

## Acceptance Criteria

- [ ] Terraform secret + version resources defined in `infrastructure/terraform-aws/secrets.tf`
- [ ] IAM grants gateway + subscription deploy roles `secretsmanager:GetSecretValue` on new secret ARN
- [ ] `scripts/deploy-prod.sh` fetches secret + injects env via SSM
- [ ] `documents/02-architecture/env-vars-registry.md` row added
- [ ] `production-env-config-registry.md` §11 cross-reference link added
- [ ] Live verify deferred per GAP-612 unblock dependency (document expected curl test trong runbook)
- [ ] Wave 105 Bucket E0 commit lands các changes trên trong same PR/branch

## Impact assessment

**Severity:** P1 (not P0) — production deploy currently blocked by GAP-612 AWS suspended; secret addition can ship code/IaC ngay, verification post-restore.

**Blast radius if not fixed before production cutover:**
- 2FA challenge token verify fails → admin login post-password but pre-2FA stuck at 401
- All Owner/Teacher 2FA paths broken
- Beta user enrolled trong 2FA → cannot complete enroll-init
- Workaround: disable 2FA per-user (table flip totp_required=false) — defeats security

**Symptom signature in prod logs:**
```
JwtAuthenticationGatewayFilter — challengeSigningKey=null; cannot verify HS256 challenge
HTTP 401 from /api/v1/auth/2fa/enroll-init via gateway
```

## Related

- Triggered by: outside-in audit Wave 104.5 follow-up 2026-05-22 (meta gap surfaced by user inspection)
- Meta sister: **GAP-718** new rule `local-fix-production-parity-check.md` (force-multiplier prevention)
- Wave 104.5 origin: PR #1715 commit `b45f9b28` added JWT_CHALLENGE_SECRET to docker-compose only
- Blocked verify by: GAP-612 AWS account 906286017800 suspended
- Production env source-of-truth: `production-env-config-registry.md` v1.1.1
- Wave 105 plan: `documents/03-planning/waves/wave-2026-05-22-105-persona-walk-beta-readiness.md` — amended to add Bucket E0
