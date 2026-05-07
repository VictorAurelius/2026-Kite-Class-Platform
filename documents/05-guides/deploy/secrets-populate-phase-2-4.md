---
title: Phase 2.4 — Secrets Manager populate runbook
status: active
created: 2026-05-08
updated: 2026-05-08
related:
  - release-1-deploy-runbook.md
  - release-1-deploy-session-2026-05-07.md
phase: 2.4
---

# Phase 2.4 — Secrets Manager populate runbook

**Reference runbook:** [`release-1-deploy-runbook.md`](../../03-planning/roadmap/release-1-deploy-runbook.md) §2.4

**Pre-condition:** Phase 2.3 production apply ✅ DONE (creates 8 secret placeholders + 3 auto-generated secret_versions).

---

## Helper script (recommended fast path)

`scripts/populate-secrets.sh` automates the 7-secret populate flow below: 4 random-generated, 2 user-prompted (OpenAI + Anthropic, hidden via `read -r -s`), 1 fixed (`kitehub-prod`). Idempotent — safe to re-run. Never echoes secret values.

```bash
# 1. Preview without touching AWS
bash scripts/populate-secrets.sh --dry-run

# 2. Populate non-interactively (random + fixed secrets only)
bash scripts/populate-secrets.sh --yes

# 3. Full interactive run (per-secret confirm + prompts for OpenAI/Anthropic keys)
bash scripts/populate-secrets.sh
```

**Important — Tier 3 mutation, human-only:** the script issues `aws secretsmanager create-secret` + `put-secret-value` (Tier 3 per [`agent-aws-access.md`](../../../.claude/rules/agent-aws-access.md) §4.1). **Agents must NOT invoke this script** — execute as a human operator. Pre-flight verifies AWS account `906286017800` and region `ap-southeast-1` before any mutation.

For the 5 non-overlapping secrets the runbook below also covers (Resend / RabbitMQ / SES SMTP / Cloudflare / `kite/prod/ses-smtp-credentials` deprecation), follow the manual steps below — those values come from external dashboards or have pivoted scope per Stream A.

---

## What Phase 2.3 created

Per `infrastructure/terraform-aws/secrets.tf`, Phase 2.3 apply tạo:

### Auto-generated (no user action needed)

| Secret name | Source | Status |
|---|---|---|
| `kite/prod/rds-password` | `random_password.rds` (32 chars) | ✅ value populated |
| `kite/prod/jwt-secret` | `random_password.jwt` (64 chars) | ✅ value populated |
| `kite/prod/encryption-key` | `random_password.encryption_raw` (32 chars) | ✅ value populated |

### Placeholders (user MUST populate Phase 2.4)

| Secret name | Description | Source |
|---|---|---|
| `kite/prod/ses-smtp-credentials` | SES SMTP user/pass — **N/A** if using Resend pivot | Resend API key (Stream A pivot) |
| `kite/prod/ai-openai-api-key` | OpenAI API key | **DEFERRED Phase 2** (per ADR-026 Ollama defer) — leave placeholder |
| `kite/prod/ai-anthropic-api-key` | Anthropic API key | **DEFERRED Phase 2** (per ADR-026 Ollama defer) — leave placeholder |
| `kite/prod/cloudflare-api-token` | Cloudflare API token (deferred Phase 2 — pivoted to Vercel) | dash.cloudflare.com (defer) |
| `kite/prod/rabbitmq-default-creds` | RabbitMQ user/password | `random_password` or manual |

---

## Stream A pivot impact (per `release-1-deploy-session-2026-05-07.md`)

| Original | Pivoted | Affects placeholder |
|---|---|---|
| AWS SES production access | Resend free tier | `ses-smtp-credentials` → unused; populate `resend-api-key` instead |
| Cloudflare DNS proxy | Vercel native | `cloudflare-api-token` → defer Phase 2 |

→ **Action:** add 1 NEW secret `kite/prod/resend-api-key` (manual or terraform) + populate from existing GitHub Secret `RESEND_API_KEY`.

---

## Procedure

### Pre-flight

```bash
export AWS_PROFILE=default
export AWS_REGION=ap-southeast-1
export ACCOUNT_ID=906286017800

# Verify Phase 2.3 complete + secrets exist
aws secretsmanager list-secrets \
  --filters Key=name,Values=kite/prod/ \
  --query 'SecretList[].Name' \
  --output table
```

Expected: 8 secrets listed.

### 1. Populate Resend API key (NEW — pivot)

Create new secret + populate from GitHub Secret value:

```bash
# Get value from GitHub Secret (already set 2026-05-07)
RESEND_KEY=$(gh secret list --json name | jq -r '.[].name' | grep RESEND_API_KEY)
# (gh secret get NOT supported — must read from local notes or re-issue from Resend dashboard)

# Manual: from Resend dashboard https://resend.com/api-keys → copy `kite-platform-dev` key
# OR re-issue if not stored locally:
#   - resend.com → API Keys → kite-platform-dev → copy → save in password manager
read -s -p "Enter Resend API key: " RESEND_KEY; echo

# Create secret in AWS Secrets Manager
aws secretsmanager create-secret \
  --name kite/prod/resend-api-key \
  --description "Resend API key for transactional email (Stream A pivot from SES)" \
  --secret-string "${RESEND_KEY}" \
  --region "${AWS_REGION}"
```

Note: this should be added to `secrets.tf` for IaC parity (follow-up gap).

### 2. Populate AI provider keys — **SKIPPED Phase 1 BETA**

Per ADR-026 (Ollama defer Phase 2) + user decision 2026-05-07: AI provider integration deferred entirely. Placeholders left in Secrets Manager; populate when AI feature reactivated Phase 2:

```bash
# Phase 2+ only (when AI reactivated):
# read -s -p "Enter OpenAI API key (sk-...): " OPENAI_KEY; echo
# aws secretsmanager put-secret-value \
#   --secret-id kite/prod/ai-openai-api-key \
#   --secret-string "${OPENAI_KEY}"
# (Anthropic similar)
```

### 3. Populate RabbitMQ credentials

```bash
# Generate random password locally (or use existing if RabbitMQ already running)
RABBIT_USER="kite-rabbitmq"
RABBIT_PASS=$(openssl rand -base64 32 | tr -d '=+/' | cut -c1-24)

aws secretsmanager put-secret-value \
  --secret-id kite/prod/rabbitmq-default-creds \
  --secret-string "{\"username\":\"${RABBIT_USER}\",\"password\":\"${RABBIT_PASS}\"}"

# Save copy in password manager
echo "RabbitMQ creds: ${RABBIT_USER} / ${RABBIT_PASS}"
```

### 4. Skip SES SMTP credentials (pivoted)

```bash
# Per Stream A pivot, SES not used. Mark secret as deprecated:
aws secretsmanager update-secret \
  --secret-id kite/prod/ses-smtp-credentials \
  --description "DEPRECATED — Stream A pivoted to Resend (see kite/prod/resend-api-key)"
```

Or delete entirely if confident not coming back:
```bash
aws secretsmanager delete-secret \
  --secret-id kite/prod/ses-smtp-credentials \
  --recovery-window-in-days 30
```

### 5. Skip Cloudflare API token (deferred)

```bash
# Per Stream A pivot, Cloudflare deferred to Phase 2:
aws secretsmanager update-secret \
  --secret-id kite/prod/cloudflare-api-token \
  --description "DEFERRED Phase 2 — Vercel pivot active 2026-05-08; populate when promoting to custom domain"
```

---

## Verification

```bash
# All 8-10 secrets should have values
for secret in $(aws secretsmanager list-secrets \
  --filters Key=name,Values=kite/prod/ \
  --query 'SecretList[].Name' --output text); do
  echo -n "${secret}: "
  aws secretsmanager describe-secret --secret-id "${secret}" \
    --query 'LastChangedDate' --output text
done

# Auto-generated values check (should NOT be placeholder text)
aws secretsmanager get-secret-value --secret-id kite/prod/jwt-secret \
  --query SecretString --output text | wc -c
# Expected: 64+1 (newline)
```

---

## EC2 → Secrets Manager IAM access (already wired)

`infrastructure/terraform-aws/iam.tf` line 40 `aws_iam_role_policy.ec2_secrets_s3` granted EC2 instance role read access to `kite/prod/*` secrets.

EC2 user-data scripts (Phase 3 image deploy) sẽ:
1. `aws secretsmanager get-secret-value --secret-id kite/prod/<name>` từ EC2 (no static creds — uses IAM instance profile)
2. Pipe vào application config (Spring Boot `@Value` env vars OR Docker secrets mount)

---

## Out of scope

- Secret rotation policy (manual rotation OK Phase 1 BETA; auto-rotate via Lambda → Phase 2 follow-up)
- Cross-region secret replication (single-region ap-southeast-1 OK Phase 1)
- Audit log analysis (CloudTrail captures `GetSecretValue` calls — review weekly Phase 1)

---

## Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Hardcode API keys in `terraform.tfvars` | Use `aws_secretsmanager_secret_version` with `lifecycle { ignore_changes = [secret_string] }` + populate via CLI |
| Print secret values to terminal/logs | Always use `read -s` for input; never `echo $KEY` |
| Reuse same key across staging/prod | Different secrets per environment (`kite/staging/*` vs `kite/prod/*`) |
| Skip SES placeholder cleanup | Update description marking deprecated; future readers know why secret exists empty |

---

## Helper script (shipped — see top of doc)

`scripts/populate-secrets.sh` is the canonical fast-path automation for 5 ASCII secrets (Phase 1 BETA scope; AI keys deferred Phase 2 per ADR-026): `kite/prod/db/password`, `kite/prod/jwt/secret`, `kite/prod/encryption/master-key`, `kite/prod/internal-api/secret`, `kite/prod/ses/configuration-set`. See "Helper script (recommended fast path)" section near the top of this doc.

The runbook procedure above remains the source of truth for the 5 secrets the script does not cover (Resend pivot, RabbitMQ JSON creds, SES SMTP deprecation, Cloudflare deferral) — those require manual flow because values originate outside this repo or pivoted scope.

---

## Related

- `release-1-deploy-runbook.md` §2.4 (parent runbook)
- `release-1-deploy-session-2026-05-07.md` (Stream A pivot context)
- `infrastructure/terraform-aws/secrets.tf` (IaC defining placeholders)
- `infrastructure/terraform-aws/iam.tf` line 40 (EC2 read access)
- ADR-026 (Ollama defer Phase 2 — affects AI key urgency)
