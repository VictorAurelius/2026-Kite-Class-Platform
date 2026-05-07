# Secrets Management Runbook — AWS Secrets Manager + Rotation

**Audience:** SRE / DevOps provisioning secrets cho production / beta deploy.
**Closes (PARTIAL):** GAP-379 — runbook artifact. AWS Secrets Manager provisioning + rotation Lambda setup are user steps, not auto-runnable.
**Standards:** AWS Well-Architected (Security pillar) · NIST SP 800-53 Rev 5 (SC-12 Cryptographic Key Management) · Twelve-Factor (config in env) · `.claude/rules/release-deploy-standard.md` §3.1 · Luật An ninh mạng 2018 + ND 53/2022/NĐ-CP (data localization → choose `ap-southeast-1` Singapore region).

---

## 1. Architecture overview

```
┌───────────────┐
│  Spring Boot  │ ─── reads env vars at boot (12-factor)
│   service     │
└───────┬───────┘
        │ IAM role (EKS workload identity)
        ▼
┌──────────────────────────────────┐
│  AWS Secrets Manager              │
│  (region: ap-southeast-1)         │
│  Namespacing: kitehub/<env>/<key> │
└──────────┬───────────────────────┘
           │ access logged
           ▼
   ┌─────────────────┐
   │  CloudTrail     │ ─── audit trail
   │  (kms encrypted)│
   └─────────────────┘
```

| Item | Decision |
|------|----------|
| Provider | **AWS Secrets Manager** (confirmed Phase 1 — already used in `infrastructure/terraform-aws/secrets.tf`) |
| Region | `ap-southeast-1` (Singapore) — closest VN-compliant region |
| Naming convention | `kitehub/<environment>/<secret-key>` (matches existing terraform) |
| Rotation | Manual Phase 1 (runbook §4); Lambda rotation Wave 34 |
| Audit | CloudTrail event `GetSecretValue` per access |
| Alt provider | HashiCorp Vault — **defer post-Release 2** (cost concerns) |

---

## 2. Secrets inventory (Phase 1 BETA)

| Secret key | Description | Used by | Rotation cadence |
|-----------|-------------|---------|------------------|
| `kitehub/<env>/db-password` | RDS Postgres master password | core, subscription, branding, all kitehub-* services | 90 days |
| `kitehub/<env>/redis-auth-token` | ElastiCache Redis AUTH token | core, gateway | 90 days |
| `kitehub/<env>/jwt-secret` | JWT signing key (HS256/RS256) | gateway, core | 180 days (versioned `kid`) |
| `kitehub/<env>/encryption-key` | At-rest encryption master key | core (PII fields) | 365 days (rotation = re-encrypt) |
| `kitehub/<env>/ses-smtp-credentials` | SES SMTP user / password (per GAP-370) | email module | 180 days |
| `kitehub/<env>/cloudflare-api-token` | Cloudflare API for cache purge / WAF management (per GAP-371) | gateway | 180 days |
| `kitehub/<env>/openai-api-key` | OpenAI API for AI Branding fallback | branding | 180 days OR on suspected compromise |
| `kitehub/<env>/admin-default-password` | Initial admin seed password (per GAP-376) | seed migration | One-time → rotate immediately post-seed |
| `kitehub/<env>/sentry-dsn` | Sentry DSN (error tracking) | all services | Annual |
| `kitehub/<env>/dmarc-report-mail` | dmarc@kitehub.vn IMAP password (DMARC report ingestion) | reporting | 180 days |

Phase 1.5 PAID adds:
| `kitehub/<env>/vnpay-merchant-key` | VNPay payment gateway | subscription | 180 days |
| `kitehub/<env>/momo-partner-code` | MoMo partner secret | subscription | 180 days |

**AWS access keys NEVER stored as secrets.** Use IAM roles (EKS workload identity / EC2 instance profile) for service-to-AWS auth.

---

## 3. Provisioning — first-time setup

### 3.1 Create secrets via Terraform (preferred)

`infrastructure/terraform-aws/secrets.tf` already declares some secrets. Extend per inventory §2:

```hcl
# Example — extend secrets.tf
resource "aws_secretsmanager_secret" "ses_smtp" {
  name                    = "${var.project_name}/${var.environment}/ses-smtp-credentials"
  recovery_window_in_days = 7
  tags                    = { Name = "SES SMTP Credentials" }
}

resource "aws_secretsmanager_secret_version" "ses_smtp" {
  secret_id     = aws_secretsmanager_secret.ses_smtp.id
  secret_string = jsonencode({
    smtp_username = "[USER_INPUT_REQUIRED: SES_SMTP_USERNAME]"
    smtp_password = "[USER_INPUT_REQUIRED: SES_SMTP_PASSWORD]"
  })
  lifecycle {
    ignore_changes = [secret_string]  # Manual updates after first apply
  }
}
```

Apply:
```bash
cd infrastructure/terraform-aws
terraform plan -out tfplan
# Review tfplan → confirm only "create" not "destroy" for any existing secret
terraform apply tfplan
```

### 3.2 Manual seed of secret values (one-time)

After `terraform apply` creates the empty secret resource, populate value:

```bash
aws secretsmanager put-secret-value \
  --region ap-southeast-1 \
  --secret-id kitehub/production/openai-api-key \
  --secret-string "sk-[USER_INPUT_REQUIRED]"
```

**Never commit real secret values to git.** Use 1Password / Bitwarden / company password manager as offline source-of-truth, sync to Secrets Manager via `put-secret-value`.

### 3.3 Generated random secrets (preferred for DB passwords / JWT)

`secrets.tf` already has the pattern:

```hcl
resource "random_password" "jwt" {
  length  = 64
  special = false
}

resource "aws_secretsmanager_secret_version" "jwt" {
  secret_id     = aws_secretsmanager_secret.jwt.id
  secret_string = random_password.jwt.result
}
```

Reuse for new secrets where the value can be machine-generated (DB master password, encryption key).

---

## 4. IAM policy template — service access

### 4.1 EKS workload identity (preferred Phase 1.5+)

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "ReadOwnSecrets",
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue",
        "secretsmanager:DescribeSecret"
      ],
      "Resource": [
        "arn:aws:secretsmanager:ap-southeast-1:[USER_INPUT_REQUIRED: AWS_ACCOUNT_ID]:secret:kitehub/production/db-password-*",
        "arn:aws:secretsmanager:ap-southeast-1:[USER_INPUT_REQUIRED: AWS_ACCOUNT_ID]:secret:kitehub/production/redis-auth-token-*"
      ]
    }
  ]
}
```

**Least-privilege rule:** each service IAM role lists ONLY the secrets it needs. Never `Resource: "*"`.

Per-service mapping (Phase 1):
| Service | Allowed secrets |
|---------|----------------|
| `kiteclass-core` | `db-password`, `redis-auth-token`, `jwt-secret`, `encryption-key` |
| `kitehub-subscription` | `db-password`, `redis-auth-token`, `jwt-secret` |
| `kitehub-branding` | `db-password`, `openai-api-key` (optional), `redis-auth-token` |
| `email` (kitehub-email module) | `ses-smtp-credentials`, `db-password` |
| `kitehub-gateway` | `jwt-secret`, `redis-auth-token`, `cloudflare-api-token` |
| `seed-migration` (one-time) | `admin-default-password`, `db-password` |

### 4.2 EC2 instance profile (Phase 1 BETA Oracle Cloud)

Phase 1 BETA chạy trên Oracle Cloud, không có IAM workload identity. Workaround:
1. Tạo dedicated IAM user `kitehub-prod-secrets-reader` với access keys.
2. Gắn policy như §4.1 (least-privilege per service — hoặc 1 policy gộp cho beta).
3. Lưu access keys trong env vars trên Oracle VM (`/etc/kitehub/secrets.env`, mode 600, owned by service user).
4. **Migrate to EKS workload identity Phase 1.5** — eliminate static keys.

---

## 5. Rotation procedures

### 5.1 Manual rotation — DB password (Phase 1, until Lambda lands)

```bash
# Step 1: Generate new password
NEW_PASSWORD=$(openssl rand -base64 48 | tr -d '/+=' | head -c 32)

# Step 2: Update RDS master password
aws rds modify-db-instance \
  --region ap-southeast-1 \
  --db-instance-identifier kitehub-production \
  --master-user-password "$NEW_PASSWORD" \
  --apply-immediately

# Step 3: Wait 5-10 min for RDS to apply
aws rds wait db-instance-available \
  --region ap-southeast-1 \
  --db-instance-identifier kitehub-production

# Step 4: Update Secrets Manager — clients fetch new value on next reconnect
aws secretsmanager put-secret-value \
  --region ap-southeast-1 \
  --secret-id kitehub/production/db-password \
  --secret-string "$NEW_PASSWORD"

# Step 5: Trigger pod rollout to force reconnect with new secret
kubectl rollout restart deployment -n kitehub
```

**Downtime expected:** zero (Hikari connection pool retries with refreshed secret on connection failure). Verify in Grafana — error rate spike should subside <1 min.

### 5.2 Lambda automated rotation (Wave 34 scope)

AWS provides built-in rotation Lambda template `SecretsManagerRDSPostgreSQLRotationSingleUser`. Setup tracked separately — not Phase 1 BETA.

### 5.3 Emergency rotation (suspected compromise)

Trigger: leaked secret in logs, GitHub push, ex-employee access, etc.

```bash
# 1. Rotate IMMEDIATELY (no maintenance window)
bash scripts/emergency-rotate-secret.sh kitehub/production/<secret-key>

# 2. Identify all consumers + restart pods
kubectl rollout restart deployment -n kitehub --all

# 3. Investigate leak source
# - Search git log for plaintext: `git log -p -S "<partial-secret>"`
# - Search CloudTrail for unauthorized GetSecretValue: AWS Console → CloudTrail → filter event=GetSecretValue, time=incident window
# - File post-mortem within 48h
```

(`emergency-rotate-secret.sh` not yet implemented — file follow-up gap if needed Phase 1.5.)

---

## 6. Audit + compliance

### 6.1 CloudTrail — Secrets Manager events

CloudTrail logs every `GetSecretValue` call. Default ON nếu account đã enable CloudTrail (most do).

Verify:
```bash
aws cloudtrail lookup-events \
  --region ap-southeast-1 \
  --lookup-attributes AttributeKey=EventName,AttributeValue=GetSecretValue \
  --max-results 50 \
  --start-time "$(date -u -d '24 hours ago' +%Y-%m-%dT%H:%M:%SZ)"
```

Expected event volume: 1 per service per pod per ~15 min (cache TTL). Anomaly = unauthorized access attempt.

### 6.2 Compliance mapping

| Standard | Coverage |
|----------|----------|
| **PDPL 2023** Art 27 | Personal data encryption — `encryption-key` rotation 365 days |
| **Luật An ninh mạng 2018** Art 26 | Data localization — Secrets stored in `ap-southeast-1` (closest VN-compliant region; verify with counsel before Phase 3 K-12) |
| **NIST SP 800-53 SC-12** | Cryptographic key management — IAM-controlled access, CloudTrail audit, rotation cadence documented |
| **OWASP Top 10 A07:2021** | Identification + Authentication failures — JWT signing key rotated 180d, encryption key 365d |

### 6.3 Quarterly audit

- Review IAM policy on each service role — verify still least-privilege
- Review CloudTrail Secrets Manager events — anomaly detection
- Review rotation cadence compliance — any secret past `Next rotate` date?
- Test emergency rotation procedure on staging (dry-run §5.3)

---

## 7. Spring Boot integration

### 7.1 Configuration approach (12-factor)

**Option A — env var injection (Phase 1 BETA, simplest):**
```bash
# /etc/kitehub/secrets.env (mode 600 on Oracle VM)
KITE_DB_PASSWORD=$(aws secretsmanager get-secret-value --secret-id kitehub/production/db-password --query SecretString --output text)
KITE_JWT_SECRET=$(aws secretsmanager get-secret-value --secret-id kitehub/production/jwt-secret --query SecretString --output text)
# ... per service-needed secret

# systemd unit
EnvironmentFile=/etc/kitehub/secrets.env
```

**Option B — Spring Cloud AWS (Phase 1.5+, EKS):**
```xml
<dependency>
  <groupId>io.awspring.cloud</groupId>
  <artifactId>spring-cloud-aws-starter-secrets-manager</artifactId>
</dependency>
```

```yaml
# application-production.yml
spring:
  config:
    import: aws-secretsmanager:kitehub/production/db-credentials
```

**KHÔNG hardcode secrets vào `application*.yml`.** Always reference env var hoặc Spring Cloud AWS lookup.

### 7.2 Local dev — KHÔNG dùng AWS Secrets Manager

Dev profile (`application-dev.yml`) reads from `.env.local` (gitignored) hoặc hardcoded dev defaults. KHÔNG share AWS account between dev + production.

---

## 8. Cost estimate

| Item | Cost (Phase 1 BETA) |
|------|---------------------|
| 12 secrets × $0.40/mo | $4.80/mo |
| API calls (~100k/mo) | $0.50/mo |
| **Total** | **~$5.30/mo** |

Free tier: 30 days × N secrets — first month $0. Phase 1.5 + Phase 2 expansion ≤$20/mo.

Vault self-hosted alternative: $0 software cost + ~$30/mo Oracle VM = $30/mo + maintenance overhead. Defer post-Release 2.

---

## 9. Acceptance — when this runbook is "verified"

- [ ] Inventory §2 secrets all created via Terraform on production AWS account
- [ ] IAM policy §4 applied per service (least-privilege verified)
- [ ] CloudTrail logs `GetSecretValue` for at least 1 production service start
- [ ] Manual DB password rotation §5.1 dry-run on staging — zero downtime confirmed
- [ ] Documented `[USER_INPUT_REQUIRED]` placeholders all replaced với real values

---

## 10. Anti-patterns

| ❌ Don't | ✅ Do |
|---------|------|
| Commit `.env.production` to git | `.env.production.template` only; real values via Secrets Manager |
| Use 1 IAM role for all services | Per-service role, list ONLY needed secrets |
| Use AWS access keys long-term | EKS workload identity Phase 1.5+; rotate Phase 1 keys ≤90d |
| Skip rotation because "we're small" | Rotate per cadence — silent compromise window grows linearly with key age |
| Log secret values for debugging | Logs scrubber masks per `.claude/rules/logs-format-standard.md` §3.1 |
| Use `Resource: "*"` for "convenience" | List explicit secret ARNs |
| Store recovery passwords in plaintext doc | 1Password / Bitwarden vault, not git |

---

## 11. Related

- `documents/05-guides/operations/dns-setup-runbook.md` (sister runbook)
- `documents/03-planning/roadmap/release-1-deploy-plan.md` (parent)
- `infrastructure/terraform-aws/secrets.tf` (existing IaC)
- `.env.production.template` (env vars consumed at boot)
- `.claude/rules/logs-format-standard.md` (PII scrubbing — never log secret values)
- `.claude/rules/release-deploy-standard.md` §3.1 Security
- GAP-376 production seed admin password
- GAP-370 SES setup (consumes `ses-smtp-credentials`)
- GAP-371 Cloudflare setup (consumes `cloudflare-api-token`)

---

## 12. Log

- **2026-05-07** Wave 33 Bucket D — runbook + IAM policy templates + rotation procedures shipped. GAP-379 stays 🟡 PARTIAL: AWS Secrets Manager populate + IAM policy apply on production AWS account are user steps (cannot run from CI).
