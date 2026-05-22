---
id: GAP-717
title: JWT_CHALLENGE_SECRET production parity — terraform IaC drift (secret created manually Wave 81 but not declared in secrets.tf)
status: PARTIAL
priority: P1
phase: phase-1-beta
audience: dev
found: 2026-05-22
last_verified: 2026-05-22
completion_pct: 70
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

## Production parity required (revised post state-check 2026-05-22 per `audit-to-gap-pipeline.md` §2.8)

Original problem statement was over-broad. Actual state verified via `grep -n "jwt[-_]challenge"` across `secrets.tf` + `iam.tf` + `fetch-secrets.sh` + `docker-compose.production.yml` + `env-vars-registry.md`:

| Surface | State at fix-time | Required artifact | Wave 105 Bucket E0 outcome |
|---|---|---|---|
| Local Docker Compose | ✅ DONE | `kitehub/docker-compose.kitehub.yml` (Wave 104.5 commit `b45f9b28`) | unchanged |
| AWS Secrets Manager (live in AWS) | ✅ EXISTS | secret `kitehub/production/jwt-challenge-secret` created manually 2026-05-15 via Wave 81 jwt-secret-fix-runbook | unchanged |
| **Terraform IaC declaration** | ❌ **MISSING — IaC drift** | `infrastructure/terraform-aws/secrets.tf` `random_password.jwt_challenge` + `aws_secretsmanager_secret.jwt_challenge` + `aws_secretsmanager_secret_version.jwt_challenge` matching existing jwt/encryption pattern với `lifecycle ignore_changes` | ✅ DECLARED Wave 105 Bucket E0 |
| Terraform IAM grants | ✅ ALREADY COVERED | wildcard `${var.project_name}/${var.environment}/*` (= `kitehub/production/*`) in `iam.tf` line 50-54 (ec2_secrets_s3) + line 368-372 (deploy role) | no edit needed |
| EC2 systemd env injection | ✅ ALREADY WIRED | `scripts/fetch-secrets.sh` line 72 `JWT_CHALLENGE_SECRET=$(fetch_secret jwt-challenge-secret)` + line 157 writes to `/etc/kite/.env` (Wave 81 Bucket F PR #1388) | no edit needed |
| Production compose env passthrough | ✅ ALREADY WIRED | `docker-compose.production.yml` `x-kh-service-defaults` anchor `env_file: /etc/kite/.env` covers all KH services | no edit needed |
| Helm chart values (future K8s Phase 2) | ⏳ deferred Phase 2 K8s migration | `infrastructure/helm/values-production.yaml` (out-of-scope Phase 1 BETA single-EC2 deploy) | defer |
| env-vars-registry doc | ✅ ALREADY DOCUMENTED | `documents/02-architecture/env-vars-registry.md` line 43 (Wave 81 Bucket F) | enriched with Wave 105 IaC parity note |
| Terraform state binding | ⏳ deferred GAP-612 unblock | `terraform import aws_secretsmanager_secret.jwt_challenge kitehub/production/jwt-challenge-secret` post AWS account 906286017800 restore | runbook documented inline §Post-AWS-restore verify below |

**State-check meta-lesson:** original GAP-717 problem statement filed without `audit-to-gap-pipeline.md` §2.8 fix-time state-check. Actual scope ~85% narrower than claimed — 5 of 7 production surfaces already wired since Wave 81; only terraform IaC declaration genuinely missing. Reinforces value of GAP-718 META rule + cited in worked self-test §6 of `local-fix-production-parity-check.md` v1.0.0.

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

- [x] Terraform `random_password.jwt_challenge` + `aws_secretsmanager_secret.jwt_challenge` + `aws_secretsmanager_secret_version.jwt_challenge` declared in `infrastructure/terraform-aws/secrets.tf` (Wave 105 Bucket E0)
- [x] IAM grants verified — wildcard `${project}/${env}/*` covers (iam.tf line 50-54 + 368-372); no edit needed
- [x] `scripts/fetch-secrets.sh` already wires fetch + env injection (Wave 81 Bucket F PR #1388) — verified line 72 + 157
- [x] `documents/02-architecture/env-vars-registry.md` row enriched with Wave 105 IaC parity note
- [x] `production-env-config-registry.md` §11 cross-reference link added (Wave 105 Bucket E0)
- [x] `audit-to-gap-pipeline.md` §2.7 cross-reference link added (sister inverse direction)
- [ ] **Live verify deferred per GAP-612 unblock** — `terraform import` + smoke curl test documented inline §Post-AWS-restore live verify below
- [x] Wave 105 Bucket E0 commit lands các changes trên trong same PR/branch

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

## Post-AWS-restore live verify (deferred per GAP-612 unblock)

When AWS account 906286017800 restored (GAP-612 unblock), run the following sequence to bind terraform state + verify production parity:

### Step 1 — Verify secret exists in AWS

```bash
AWS_PROFILE=dev-admin AWS_DEFAULT_REGION=ap-southeast-1 \
  aws secretsmanager describe-secret \
  --secret-id kitehub/production/jwt-challenge-secret \
  --query '[Name,ARN,KmsKeyId]' --output table
```

**Expected:** Name `kitehub/production/jwt-challenge-secret` present, ARN populated, KmsKeyId default (`aws/secretsmanager`).

### Step 2 — Terraform import existing secret to state

```bash
cd infrastructure/terraform-aws
AWS_PROFILE=dev-admin AWS_DEFAULT_REGION=ap-southeast-1 \
  terraform import \
  aws_secretsmanager_secret.jwt_challenge \
  kitehub/production/jwt-challenge-secret
```

**Expected:** `Import successful! ... Resources Imported: 1`. Subsequent `terraform plan` should show NO new resource creation for `jwt_challenge` (lifecycle ignore_changes on random_password prevents drift on `result`).

### Step 3 — Smoke test 2FA challenge flow

```bash
# On kh-backend EC2 via SSM session
docker exec kitehub-gateway env | grep JWT_CHALLENGE_SECRET
# Expected: non-empty value (≥40 bytes)

docker exec kitehub-subscription env | grep JWT_CHALLENGE_SECRET
# Expected: non-empty value matching gateway

# Verify ChallengeTokenService startup health
docker logs kitehub-subscription 2>&1 | grep "ChallengeTokenService"
# Expected: no @PostConstruct fail-fast errors
```

### Step 4 — Live admin 2FA enroll smoke

Per `pre-handoff-self-test-completeness.md` §2.4 admin-flow checklist:

```bash
# (a) Credential retrieve from secrets
ADMIN_PASS=$(aws secretsmanager get-secret-value \
  --secret-id kitehub/production/seed-admin-password \
  --query SecretString --output text)

# (b) Login → expect 200 + JWT
curl -X POST https://api.kitehub.me/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"admin@kitehub.me\",\"password\":\"$ADMIN_PASS\"}"

# (c) 2FA enroll-init via gateway — expects 200 + qr_code (was 401 pre-fix)
curl -X POST https://api.kitehub.me/api/v1/auth/2fa/enroll-init \
  -H "Authorization: Bearer <JWT_from_step_b>"
```

**PASS criteria:** Step (c) returns HTTP 200 with `qr_code` + `challenge_token` in response body (Wave 79 GAP-509 enrollment flow). HTTP 401 = challengeSigningKey=null fail-fast guard triggered = production parity NOT achieved.

### Step 5 — Flip gap status

If Steps 1-4 PASS → flip GAP-717 `status: PARTIAL → DONE` + `completion_pct: 70 → 100` + Log entry citing PR ID for the live verify session.

## Related

- Triggered by: outside-in audit Wave 104.5 follow-up 2026-05-22 (meta gap surfaced by user inspection)
- Meta sister: **GAP-718** new rule `local-fix-production-parity-check.md` (force-multiplier prevention) — DONE Wave 105 Bucket E0
- Wave 104.5 origin: PR #1715 commit `b45f9b28` added JWT_CHALLENGE_SECRET to docker-compose only
- Wave 81 origin (deeper root cause): secret manually created 2026-05-15 via jwt-secret-fix-runbook without terraform IaC declaration → IaC drift latent
- Blocked verify by: GAP-612 AWS account 906286017800 suspended
- Production env source-of-truth: `production-env-config-registry.md` v1.1.1
- Wave 105 plan: `documents/03-planning/waves/wave-2026-05-22-105-persona-walk-beta-readiness.md` — Bucket E0 ship 2026-05-22

## Log

- **2026-05-22 (Wave 105 Bucket E0):** Status `OPEN` → `🟡 PARTIAL` (completion_pct 70). Terraform IaC declaration shipped per §Proposed Fix Step 1. State-check per `audit-to-gap-pipeline.md` §2.8 revealed original problem statement over-broad — 5 of 7 production surfaces already wired since Wave 81; only terraform IaC declaration + post-restore terraform import genuinely missing. Updated §Production parity required table with verified actual state. AC checked except live verify (deferred per GAP-612 unblock per `gap-done-discipline.md` §3 PARTIAL exit ramp). Sister GAP-718 META rule shipped same PR per `rule-change-process.md` §6.5 Enforcement Parity Mandate. Post-AWS-restore live verify procedure documented inline §Post-AWS-restore live verify.
