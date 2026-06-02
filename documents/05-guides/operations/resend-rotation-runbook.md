# Resend API Key Rotation Runbook

**Last Updated:** 2026-06-02
**Owner:** Solo dev (Phase 1 BETA)
**Cadence:** Quarterly (per `secrets-rotation-runbook.md` §3.5) OR ad-hoc khi: (a) key leak suspected, (b) team member off-board có access vault, (c) Resend dashboard access changes.
**Related:** GAP-572 (schema-mismatch + key leak rotate), GAP-869 (rotation execution follow-up), GAP-370 (email production E2E parent), GAP-525 (Wave 81 Bucket C rotation pattern), [`credential-rotation-runbook.md`](credential-rotation-runbook.md) §2.3.2 (generic 3rd-party API key shape), [`secrets-rotation-runbook.md`](secrets-rotation-runbook.md) §3.5 (cadence), [`resend-provisioning-runbook.md`](../deploy/resend-provisioning-runbook.md) (first-time setup)

---

## 1. Khi nào dùng runbook này

Runbook chuyên cho **Resend API key rotation**. Khác với generic [`credential-rotation-runbook.md`](credential-rotation-runbook.md) ở chỗ:

- Cover **vendor-specific schema requirement** (`scripts/fetch-secrets.sh` lines 94-105 dual-schema accept) — JSON wrapper `{api_key, from_email, from_name}` HOẶC plain string
- Cover **`kitehub-email` service** consumer (EmailService → ResendClient → Resend HTTP API)
- Cover **smoke test specific** (welcome email signup flow → Resend dashboard Logs verify delivered <30s)
- Cover **AWS Secrets Manager IaC parity** (terraform-aws `secrets.tf` lines 137-167 — `lifecycle ignore_changes` preserves manual real value)

Trigger rotation khi:

| Trigger | Lý do | Priority |
|---|---|---|
| Quarterly cadence | Default policy per `secrets-rotation-runbook.md` §3.5 | P2 routine |
| Key leak (suspected) | Logs / chat / commit lộ first chars vendor key prefix `re_...` | P0 immediate |
| Off-board team member | Người rời có access AWS Secrets Manager OR Resend dashboard | P1 same week |
| Resend account migrate | Account swap / org transfer | P1 coordinated |

**GAP-572 2026-05-15 incident**: tôi (Claude) vô tình leak first 30 chars Resend key vào chat khi diagnose schema → trigger immediate rotate. Per `agent-aws-access.md` §2.2 (banned secret-revealing `get-secret-value`), incident logged + key rotate mandatory.

---

## 2. Pre-rotate checklist

Trước khi bắt đầu rotation, verify:

- [ ] **AWS stack up** — `bash scripts/aws/start-stack.sh` đã chạy, EC2 `running`, RDS `available` (per `pre-flight-aws-lifecycle-check.md` §3)
- [ ] **AWS credentials valid** — `aws sts get-caller-identity --profile dev-admin` returns valid Arn matching account `906286017800` (per `pre-flight-aws-lifecycle-check.md` §3.1)
- [ ] **Resend dashboard access** — login `https://resend.com` với account email; verify thấy API Keys page
- [ ] **Existing key identified** — biết key prefix nào đang chạy production (KHÔNG run `get-secret-value` để check — banned per `agent-aws-access.md` §2.2; check qua `aws secretsmanager describe-secret --secret-id kitehub/production/resend-api-key --query 'LastChangedDate'` để xem timestamp last rotation)
- [ ] **`from_email` domain verified** — Resend dashboard → Domains shows `kitehub.me` status `Verified` (DKIM/SPF/DMARC green)
- [ ] **Maintenance window** — Phase 1 BETA = solo dev có thể rotate bất kỳ lúc; production launch = schedule 10-min low-traffic window
- [ ] **MailHog OR external test inbox available** — để verify smoke email delivery post-rotate

---

## 3. Rotation procedure (5 phases)

### Phase 1 — USER ACTION generate new key + store as JSON wrapper

```bash
# Step 1: Resend dashboard
# Login https://resend.com/api-keys
# → Click "Create API Key"
# → Name: "kitehub-production-YYYY-MM-DD"
# → Permission: "Sending access" (NOT full access — least privilege)
# → Copy key value (re_...) — VENDOR SHOWS ONCE only

# Step 2: Store as JSON wrapper trong AWS Secrets Manager
# WHY JSON wrapper: scripts/fetch-secrets.sh lines 94-105 accepts BOTH schemas;
# JSON wrapper allows override from_email / from_name without redeploy.
# Plain string also works (per dual-schema accept) — defaults to noreply@kitehub.me + KiteHub Beta.
read -s NEW_KEY  # paste key value, press Enter (won't echo to terminal)
echo "{\"api_key\":\"$NEW_KEY\",\"from_email\":\"noreply@kitehub.me\",\"from_name\":\"KiteHub Beta\"}" | \
  aws secretsmanager put-secret-value \
    --secret-id kitehub/production/resend-api-key \
    --secret-string file:///dev/stdin \
    --profile dev-admin \
    --region ap-southeast-1 \
    --query '[Name,VersionId]' \
    --output text
unset NEW_KEY  # clear shell history

# Expected output:
# kitehub/production/resend-api-key	<new-version-uuid>
```

**Tại sao stdin pipe**: tránh key value xuất hiện trong shell history / process list. Per `pre-launch-secrets-hardening-checklist.md` §2.1 (zero hardcoded secrets) + `agent-aws-access.md` §2.2 banned secret reveal.

### Phase 2 — Double-write window (overlap old + new key)

Để tránh in-flight emails fail trong rotation window:

```bash
# Step 1: Verify new version is AWSCURRENT, old version moved to AWSPREVIOUS
aws secretsmanager list-secret-version-ids \
  --secret-id kitehub/production/resend-api-key \
  --include-deprecated \
  --profile dev-admin --region ap-southeast-1 \
  --query 'Versions[].[VersionId,VersionStages]' \
  --output table

# Expected: new version có ["AWSCURRENT"], old version có ["AWSPREVIOUS"]

# Step 2: Trigger kitehub-email service to re-fetch secret + reload
EC2_ID="i-05d7af46d01436b96"  # kh-backend EC2 (verify via aws ec2 describe-instances --filters Name=tag:Name,Values=kitehub-kh-backend)
aws ssm send-command \
  --instance-ids "$EC2_ID" \
  --document-name AWS-RunShellScript \
  --parameters 'commands=["sudo bash /opt/kite-prod/scripts/fetch-secrets.sh 2>&1 | grep -iE \"resend|INFO|WARN\" | tail -5","sudo grep ^RESEND_API_KEY= /etc/kite/.env | awk -F= \"{print length(\\$2)}\"","cd /opt/kite-prod && sudo docker compose --env-file /etc/kite/.env -f docker-compose.production.yml up -d --force-recreate --no-deps kitehub-email 2>&1 | tail -5"]' \
  --profile dev-admin --region ap-southeast-1 \
  --query 'Command.CommandId' --output text

# Wait ~10 seconds for SSM command to complete
sleep 10

# Step 3: Get SSM command output
COMMAND_ID="<paste from above>"
aws ssm get-command-invocation \
  --command-id "$COMMAND_ID" \
  --instance-id "$EC2_ID" \
  --profile dev-admin --region ap-southeast-1 \
  --query 'StandardOutputContent' \
  --output text

# Expected:
# - INFO: Resend secret stored as JSON wrapper OR plain string detected
# - RESEND_API_KEY length > 0 (vd 40+ chars)
# - kitehub-email container recreated với new env
```

**Tại sao force-recreate**: `kitehub-email` Spring Boot service reads env vars at JVM startup; SIGHUP không reload `@Value` annotations. Per `production-env-config-registry.md` §3 — runtime env-var refresh requires container restart.

### Phase 3 — Smoke test live email delivery

```bash
# Step 1: Trigger welcome email signup flow via API
# (chọn email test address: noreply+test@kitehub.me OR project-internal inbox)
curl -X POST \
  -H 'Content-Type: application/json' \
  -d '{"email":"smoke-test+rotate@kitehub.me","name":"Smoke Test Rotate","orgName":"test-rotation","persona":"P2_CENTER_OWNER","consentGiven":true,"consentAccepted":true}' \
  https://api.kitehub.me/api/v1/auth/request-beta-access \
  -w "\nHTTP: %{http_code}\n"

# Expected: HTTP 201 + JSON {"requestId":...,"email":"smoke-test+rotate@kitehub.me","status":"PENDING"}

# Step 2: Verify Resend dashboard log within 30s
# → Open https://resend.com/emails
# → Filter "smoke-test+rotate@kitehub.me"
# → Status should be "Delivered" (NOT "Bounced" / "Pending" / "Failed")
# → Click row → verify: From: noreply@kitehub.me, Subject: <welcome>, body renders

# Step 3 (alternative — MailHog dev path):
# Nếu test trên local dev stack:
curl -s http://localhost:8025/api/v2/messages | jq '.items[] | select(.Content.Headers.To[0] | contains("smoke-test+rotate"))'
# Expected: 1 message với From: noreply@kitehub.me
```

**Verdict criteria**: Resend dashboard shows `Delivered` trong 30s OR email arrives test inbox. Nếu FAIL → ROLLBACK (per Phase 5 below) trước khi revoke old key.

### Phase 4 — Revoke old key trên Resend dashboard

```
# Step 1: Resend dashboard
# Login https://resend.com/api-keys
# → Find old key (name = "kitehub-production-<prev-date>")
# → Click "Revoke" → confirm

# Step 2: Verify revoke
# → Resend dashboard → API Keys list
# → Old key disappears (revoked keys không show trong list)
# → Last activity timestamp = pre-rotate timestamp (no usage post-revoke)

# Step 3: Verify production still healthy
curl -s -X POST \
  -H 'Content-Type: application/json' \
  -d '{"email":"smoke-post-revoke@kitehub.me","name":"Post Revoke","orgName":"test","persona":"P2_CENTER_OWNER","consentGiven":true,"consentAccepted":true}' \
  https://api.kitehub.me/api/v1/auth/request-beta-access \
  -w "\nHTTP: %{http_code}\n"

# Expected: HTTP 201 + email delivered Resend dashboard < 30s (new key still works)
```

**Tại sao revoke last**: nếu Phase 3 smoke FAIL, old key vẫn active để rollback nhanh. Revoke only after Phase 3 PASS.

### Phase 5 — Rollback procedure (nếu Phase 3 smoke FAIL)

Nếu Phase 3 smoke email KHÔNG deliver / Resend dashboard shows error:

```bash
# Step 1: Restore previous secret version
# AWSPREVIOUS version stage point to old key (still active vì chưa Phase 4 revoke)
aws secretsmanager update-secret-version-stage \
  --secret-id kitehub/production/resend-api-key \
  --version-stage AWSCURRENT \
  --move-to-version-id "<old-version-uuid-from-Phase-2-Step-1-output>" \
  --remove-from-version-id "<new-version-uuid-from-Phase-1-Step-2-output>" \
  --profile dev-admin --region ap-southeast-1

# Step 2: Trigger kitehub-email re-fetch + restart
EC2_ID="i-05d7af46d01436b96"
aws ssm send-command \
  --instance-ids "$EC2_ID" \
  --document-name AWS-RunShellScript \
  --parameters 'commands=["sudo bash /opt/kite-prod/scripts/fetch-secrets.sh","cd /opt/kite-prod && sudo docker compose --env-file /etc/kite/.env -f docker-compose.production.yml up -d --force-recreate --no-deps kitehub-email"]' \
  --profile dev-admin --region ap-southeast-1 \
  --query 'Command.CommandId' --output text

# Wait 15 sec, verify kitehub-email healthy
sleep 15
curl -s https://api.kitehub.me/api/health
# Expected: HTTP 200 + {"status":"UP"}

# Step 3: Re-smoke welcome email (per Phase 3) — verify old key path works
# Nếu PASS → rollback successful; investigate Phase 3 fail root cause separately
# Nếu FAIL → escalate; production email broken regardless of rotation
```

---

## 4. Post-rotate verification + documentation

### 4.1 Update rotation log

Append entry to `documents/05-guides/operations/credential-rotation-2026-XX-XX.md` (create new dated file per rotation):

```markdown
# Credential Rotation 2026-XX-XX

## Resend API key

- **Trigger:** [Quarterly cadence | Leak suspected | Off-board <name> | Account migrate]
- **Old key prefix:** re_xxxxxx (last 4 chars + first 7 chars only — KHÔNG full key)
- **New key prefix:** re_yyyyyy
- **Pre-rotate version-id:** <uuid>
- **Post-rotate version-id:** <uuid>
- **Smoke email recipient:** smoke-test+rotate-<date>@kitehub.me
- **Smoke delivery time:** XX seconds
- **Resend dashboard log URL:** https://resend.com/emails/<email-uuid>
- **kitehub-email container restart time:** YYYY-MM-DDTHH:MM:SSZ
- **Old key revoke time:** YYYY-MM-DDTHH:MM:SSZ
- **Next rotation due:** YYYY-MM-DD (+90 days)

## Notes

<any anomalies, durations, learnings>
```

### 4.2 Cross-link sync

- Update `documents/05-guides/operations/secrets-rotation-runbook.md` §3.5 last-rotated date for `resend-api-key` row
- Update 1Password / vault note với new rotation date + next-due date (+90 days)
- Nếu account migrate scope: update `documents/05-guides/deploy/resend-provisioning-runbook.md` với new account email

### 4.3 Live verification quarterly health

Mỗi quarter (aligned với rotation cadence), verify Resend domain health:

```bash
# DNS records verify
dig +short kitehub.me TXT | grep -E "v=spf1|resend"
dig +short resend._domainkey.kitehub.me CNAME
dig +short _dmarc.kitehub.me TXT
# Expected: all records present + matching Resend dashboard config

# Quota check
# Resend dashboard → Usage → verify monthly send quota OK (Free 3,000/mo Phase 1 BETA scope)
```

---

## 5. Banned shortcuts

| ❌ Don't | ✅ Do |
|---|---|
| Run `aws secretsmanager get-secret-value --secret-id kitehub/production/resend-api-key` để check current value | Per `agent-aws-access.md` §2.2 — BANNED (reveals secret). Use `describe-secret` cho metadata only |
| Skip Phase 2 SSM command "vì secret update auto-propagate" | Spring `@Value` annotation requires container restart; secret update KHÔNG tự reload |
| Revoke old key TRƯỚC Phase 3 smoke test | Phase 3 fail = no rollback path; revoke last per Phase 4 |
| Store key as plain string "vì fetch-secrets accept both schemas" | JSON wrapper preferred — allows override `from_email` / `from_name` without redeploy. Plain string acceptable nhưng inferior |
| Type key value plaintext trong terminal (`echo "re_..." \| aws...`) | Use `read -s NEW_KEY` để paste invisible + `unset NEW_KEY` post-use |
| Skip rotation log update | Per `output-review-mandate.md` §3 — audit artifact mandatory |
| Test smoke email với production user real email | Use `smoke-test+rotate@kitehub.me` OR project-internal test inbox to avoid spam real user |
| Rotate without `pre-flight-aws-lifecycle-check.md` cred check | Stale creds → SSM SendCommand fail mid-rotation → broken state |
| Run rotation khi AWS stack stopped | Per `pre-flight-aws-lifecycle-check.md` §3.2 — verify EC2 running + RDS available first |

---

## 6. Schema reference (canonical)

### 6.1 AWS Secrets Manager schema

Secret ID: `kitehub/production/resend-api-key` (region: `ap-southeast-1`)

Payload schema (per `scripts/fetch-secrets.sh` lines 94-105 dual-schema accept):

```json
// Preferred — JSON wrapper
{
  "api_key": "re_xxxxxxxxxxxxxxxxxxxx",
  "from_email": "noreply@kitehub.me",
  "from_name": "KiteHub Beta"
}

// Acceptable — plain string (from_email/from_name use defaults)
"re_xxxxxxxxxxxxxxxxxxxx"
```

### 6.2 Spring Boot env-var mapping

`kitehub-email/application.yml`:

```yaml
spring:
  mail:
    resend:
      api-key: ${RESEND_API_KEY:}  # empty default = service warn + skip send
      from-email: ${AWS_SES_FROM_EMAIL:noreply@kitehub.local}  # local dev default
      from-name: ${AWS_SES_FROM_NAME:KiteHub Dev}  # local dev default
```

Production override per `/etc/kite/.env` (written by `scripts/fetch-secrets.sh`):

```bash
RESEND_API_KEY=re_xxxxxxxxxxxxxxxxxxxx
AWS_SES_FROM_EMAIL=noreply@kitehub.me
AWS_SES_FROM_NAME=KiteHub Beta
```

### 6.3 Terraform IaC

Per `infrastructure/terraform-aws/secrets.tf` lines 137-167:

```hcl
resource "random_password" "resend_api_key_placeholder" {
  length  = 32
  special = false
  lifecycle {
    ignore_changes = [result, length, ...]  # preserve manual real value
  }
}

resource "aws_secretsmanager_secret" "resend_api_key" {
  name = "${var.project_name}/${var.environment}/resend-api-key"
  description = "Resend HTTP API key for transactional email; JSON wrapper schema {api_key, from_email, from_name} OR plain string"
  recovery_window_in_days = 7
}

resource "aws_secretsmanager_secret_version" "resend_api_key" {
  secret_id     = aws_secretsmanager_secret.resend_api_key.id
  secret_string = random_password.resend_api_key_placeholder.result
  lifecycle {
    ignore_changes = [secret_string]  # preserve manual real value
  }
}

import {
  to = aws_secretsmanager_secret.resend_api_key
  id = "kitehub/production/resend-api-key"
}
```

**Tại sao `random_password` placeholder + `lifecycle ignore_changes`**: terraform IaC ships first cho repeatable infra; real key set manually qua AWS console OR `aws secretsmanager put-secret-value` post-Resend account verified. `ignore_changes` prevents terraform overwriting manual real value trong subsequent `terraform apply` runs.

---

## 7. Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| `fetch-secrets.sh` log "WARN: resend-api-key not found" | IAM role thiếu `secretsmanager:GetSecretValue` cho `kitehub/production/*` | Verify `infrastructure/terraform-aws/iam.tf` wildcard grant for `ec2_app` role |
| Phase 2 SSM command timeout | EC2 stopped OR SSM agent down | Per `pre-flight-aws-lifecycle-check.md` §3.2 verify EC2 running; SSM agent: `aws ssm describe-instance-information` |
| Phase 3 smoke email "Bounced" Resend dashboard | `from_email` domain NOT verified Resend | Re-verify domain Resend dashboard → Domains → kitehub.me → DKIM/SPF/DMARC green |
| `kitehub-email` container OOM after restart | JVM memory limit too low | Per `pre-launch-infra-hardening-checklist.md` §2.4 — bump `-Xmx` in container env OR Helm values |
| Phase 5 rollback `update-secret-version-stage` fail | Old version-id wrong | `aws secretsmanager list-secret-version-ids --include-deprecated` để find correct AWSPREVIOUS id |
| Resend dashboard 401 / "Invalid API key" | Old key revoked before new key propagated | Restore AWSPREVIOUS per Phase 5 OR re-generate Phase 1 new key |
| `RESEND_API_KEY=` length 0 trong `/etc/kite/.env` post fetch | JSON parse fail (malformed payload) | Verify Secrets Manager payload valid JSON: `aws secretsmanager describe-secret` returns metadata only — never `get-secret-value` per `agent-aws-access.md` §2.2 |

---

## 8. Related runbooks + rules

- [`resend-provisioning-runbook.md`](../deploy/resend-provisioning-runbook.md) — first-time Resend account + domain verification (run TRƯỚC khi first rotation)
- [`credential-rotation-runbook.md`](credential-rotation-runbook.md) §2.3.2 — generic 3rd-party API key rotation shape (Resend = instance)
- [`secrets-rotation-runbook.md`](secrets-rotation-runbook.md) §3.5 — overall rotation cadence table (Resend = row)
- [`secrets-seeding-runbook.md`](../deploy/secrets-seeding-runbook.md) — first-time secret seed (different scope; rotation = update existing)
- `.claude/rules/agent-aws-access.md` §2.2 — BANNED `get-secret-value` (this runbook NEVER reveals secret)
- `.claude/rules/pre-flight-aws-lifecycle-check.md` §3 — cred + state check (Phase 2 mandatory pre-requisite)
- `.claude/rules/pre-launch-secrets-hardening-checklist.md` §2.1 — zero hardcoded secrets (Phase 1 stdin pipe pattern)
- `.claude/rules/production-env-config-registry.md` §3 — runtime env-var refresh requires container restart (Phase 2 force-recreate rationale)
- `.claude/rules/agent-action-bias.md` §1 Part B — command over UI (this runbook = CLI path)

---

## 9. Log

- **2026-06-02 (v1.0.0):** Runbook created. Wave local-doable-7 Bucket E (GAP-572 PARTIAL 75% — schema mismatch already fixed Phase 4 Wave email-finalize-1; IaC parity DONE Wave aws-restore-1 import block; runbook ships now). Paired same-PR với GAP-869 follow-up (rotation execution dev trigger). Reference: GAP-572 leak incident 2026-05-15 (Claude vô tình ran `get-secret-value | head -c 30` để diagnose schema → leaked first 30 chars — meta-incident logged → `agent-aws-access.md` §2.2 enforcement strengthened). Runbook narrow scope (Resend-specific) thay vì extend generic `credential-rotation-runbook.md` §2.3.2 — Resend dual-schema requirement + Spring `@Value` reload + 90-day cadence rationale specific enough to warrant standalone file per `docs-folder-structure.md` § Edge cases "Runbook covers BOTH initial setup + recurring rotation → Split into 2 files" pattern (Phase 1 first-time = `resend-provisioning-runbook.md` ở `deploy/`; Phase 1.5+ rotation cadence = this runbook ở `operations/`). Reviewer: @nguyenvankiet (solo-dev — operations runbook, no rule-creation scope, standard runbook ship per `deployment-naming-convention.md` §3 `<topic>-<action>-runbook.md` pattern).
