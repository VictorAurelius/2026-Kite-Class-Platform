# Resend Provisioning Runbook — Phase 1 BETA Email Delivery

**Status:** active
**Created:** 2026-05-13
**Related:** GAP-508 Phase 2, ADR-025 Stream A, [`scripts/fetch-secrets.sh`](../../../scripts/fetch-secrets.sh), [`secrets-seeding-runbook.md`](./secrets-seeding-runbook.md)
**Audience:** Solo dev / operator setting up production email delivery for Phase 1 BETA

---

## 1. Purpose + when to run

Phase 1 BETA uses **Resend HTTP API** (per ADR-025 Stream A pivot) for all transactional email delivery — beta invites, email verification links, password resets, system notices. The HTTP API path replaces the original SES SMTP design because (a) SES production-access approval is a multi-day vendor process and (b) Resend's free tier (100 emails/day, 3000/month) covers Phase 1 BETA capacity (≤5 tenants × ≤20 emails/day).

**When to run this runbook:** one-time per AWS account during initial Phase 1 BETA setup, OR when rotating the Resend API key (quarterly cadence per `secrets-rotation-runbook.md`). Audit 2026-05-13 found `RESEND_API_KEY` env var EMPTY in `kitehub-email` production container → emails never delivered → Plan 1 self-test Bước 3 (invite email) + Bước 5 (verification link) blocked.

---

## 2. Pre-flight: Resend account + domain verification

### 2.1 Create Resend account

1. Sign up at https://resend.com (free tier — 100/day, 3000/month)
2. Use a stable team-owned email (not personal Gmail)

### 2.2 Add `kitehub.me` domain in Resend dashboard

1. Resend dashboard → **Domains** → **Add Domain** → `kitehub.me`
2. Region: pick `Tokyo (ap-northeast-1)` (closest to `ap-southeast-1` workload — minimum latency)
3. Resend generates DNS records — keep this tab open

### 2.3 Configure DNS in Cloudflare

Resend will display the exact record values. Add them via Cloudflare dashboard (or `wrangler` CLI if preferred):

| Type | Name | Value (Resend will provide exact strings) |
|------|------|-------------------------------------------|
| TXT | `kitehub.me.` | `v=spf1 include:_spf.resend.com ~all` |
| CNAME | `resend._domainkey.kitehub.me.` | (Resend DKIM 1 — `resend._domainkey.<acct>.resend.com`) |
| CNAME | `resend2._domainkey.kitehub.me.` | (Resend DKIM 2) |
| CNAME | `resend3._domainkey.kitehub.me.` | (Resend DKIM 3) |
| TXT | `_dmarc.kitehub.me.` | `v=DMARC1; p=quarantine; rua=mailto:dpo@kitehub.me; ruf=mailto:dpo@kitehub.me; fo=1` |

**Note on Cloudflare proxy:** TXT + CNAME records used for email auth MUST be set to **"DNS only" (grey cloud)**, NOT proxied (orange cloud). Cloudflare proxy intercepts CNAME → breaks DKIM resolution.

### 2.4 Wait for verification

1. Resend dashboard refreshes status every 5-30 min
2. Look for green "Verified" badge on all 5 records
3. **Acceptance:** Resend dashboard domain `kitehub.me` status = **Verified** ✅

Troubleshooting:
- Cloudflare proxy enabled by mistake → toggle off → wait 5 min → re-check
- Typo in DNS value → fix in Cloudflare → wait 5 min
- DNS propagation delayed → wait up to 30 min before re-trying

---

## 3. Generate API key

1. Resend dashboard → **API Keys** → **Create API Key**
2. Name: `kitehub-production-phase-1-beta`
3. Permission: **Sending access** (full send permission scoped to `kitehub.me` domain)
4. Click **Create** — key is displayed ONCE (format: `re_<random_32_chars>`)
5. **Immediately copy key** — Resend will not show it again

---

## 4. Store in AWS Secrets Manager

Use AWS CLI with admin profile (one-time provisioning step — does NOT use the runtime OIDC role).

```bash
AWS_PROFILE=dev-admin AWS_DEFAULT_REGION=ap-southeast-1 aws secretsmanager create-secret \
  --name kitehub/production/resend-api-key \
  --description "Resend API key for kitehub-email service (Phase 1 BETA, Stream A per ADR-025)" \
  --secret-string '{"api_key":"<paste-key-here>","from_email":"noreply@kitehub.me","from_name":"KiteHub Beta"}'
```

**Schema note:** the secret payload is JSON with three fields — `api_key` (the `re_...` token), `from_email` (sender address — MUST match a verified domain, recommend `noreply@kitehub.me`), and `from_name` (display name). `fetch-secrets.sh` reads all three.

### Verify the secret exists

```bash
AWS_PROFILE=dev-admin AWS_DEFAULT_REGION=ap-southeast-1 aws secretsmanager describe-secret \
  --secret-id kitehub/production/resend-api-key
```

Expected: returns metadata (ARN, CreatedDate, Description). Per `agent-aws-access.md` §2.2 — do **NOT** run `get-secret-value` to verify content (that's Tier 2 secret-revealing read; describe-secret is Tier 1 metadata-only).

### IAM permission scope

The EC2 instance profile (`kitehub-kh-backend-runtime-role`) already has `secretsmanager:GetSecretValue` scoped to `arn:aws:secretsmanager:ap-southeast-1:*:secret:kitehub/production/*` via the production IAM policy. New secret automatically inherits permission — no IAM change needed.

---

## 5. Trigger deploy + verify

### 5.1 Trigger production deploy

After `fetch-secrets.sh` extension lands on `main` (Wave 71 Bucket D PR), tag the next staging build and trigger deploy:

```bash
gh workflow run deploy-production.yml -f version=v0.9.0-beta-staging.12 -f confirm=DEPLOY
```

Per `release-deploy-standard.md` §9 and `concurrent-production-mutation-ops.md` — single mutation op, no concurrent terraform apply.

### 5.2 Post-deploy verification (Tier 1 read-only — see `agent-aws-access.md` §2)

After deploy completes (~3-5 min via SSM SendCommand):

```bash
# 1. Confirm env var present + non-empty in container (length check, NOT echo)
ssh ec2-user@<kh-backend-ip> "docker exec kitehub-email sh -c 'echo \"RESEND_API_KEY length: \${#RESEND_API_KEY}\"'"
# Expected: "RESEND_API_KEY length: 36" (or similar non-zero)
# If 0 → fetch-secrets.sh did not populate; check /var/log/kite-bootstrap.log

# 2. Confirm sender + name resolved
ssh ec2-user@<kh-backend-ip> "docker exec kitehub-email env | grep AWS_SES_FROM"
# Expected:
#   AWS_SES_FROM_EMAIL=noreply@kitehub.me
#   AWS_SES_FROM_NAME=KiteHub Beta
```

### 5.3 End-to-end test send

```bash
# Trigger a real send via beta-access endpoint (use a real inbox you control)
curl -X POST https://api.kitehub.me/api/v1/auth/request-beta-access \
  -H 'Content-Type: application/json' \
  -d '{"email":"<your-test-inbox@example.com>","fullName":"Test User","org":"Test Org"}'
# Expected: HTTP 202 Accepted
```

Verify in Resend dashboard → **Logs** tab:
- Status: `Delivered` (or `Bounced` if test inbox rejected)
- From: `noreply@kitehub.me`
- Subject + body rendered correctly

Verify in test inbox: invite email arrives within 30 seconds with working verification link.

**Acceptance:** test email visible in Resend Logs `Delivered` status + arrives in real inbox ✅

---

## 6. Rollback

If a leaked key is suspected OR rotation is scheduled:

### 6.1 Rotate via Resend dashboard

1. Resend dashboard → **API Keys** → find current key → **Revoke**
2. **Create API Key** with same naming pattern (or add `-rotated-YYYY-MM-DD` suffix for audit trail)
3. Copy new key

### 6.2 Update AWS Secrets Manager

```bash
AWS_PROFILE=dev-admin AWS_DEFAULT_REGION=ap-southeast-1 aws secretsmanager put-secret-value \
  --secret-id kitehub/production/resend-api-key \
  --secret-string '{"api_key":"<new-re_...>","from_email":"noreply@kitehub.me","from_name":"KiteHub Beta"}'
```

This creates a new secret version. Previous version is retained for 30 days by default — can be restored if rotation broke something.

### 6.3 Re-deploy to pick up new key

`fetch-secrets.sh` reads the current secret version on each EC2 bootstrap. Trigger a re-deploy:

```bash
gh workflow run deploy-production.yml -f version=<current-version-tag> -f confirm=DEPLOY
```

### 6.4 Verify new key

Repeat §5.2 + §5.3 with new key. Check Resend Logs for fresh delivery using the new API key.

**Rollback to previous key (if rotation broke something):**

```bash
# Restore previous secret version
AWS_PROFILE=dev-admin AWS_DEFAULT_REGION=ap-southeast-1 aws secretsmanager restore-secret \
  --secret-id kitehub/production/resend-api-key
# Re-deploy
gh workflow run deploy-production.yml -f version=<current-version-tag> -f confirm=DEPLOY
```

Cross-reference: `secrets-rotation-runbook.md` for the quarterly cadence + cross-secret coordination (DB password, JWT secret, encryption key, etc.).
