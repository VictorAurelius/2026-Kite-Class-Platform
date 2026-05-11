# Secrets Seeding Runbook — first-time AWS Secrets Manager provisioning

**Audience:** SRE / DevOps thực hiện first-time secrets provisioning trong release deploy. Đây là một-lần-per-release artifact.
**Sister runbook:** ongoing rotation, audit, emergency-rotate procedures live in `documents/05-guides/operations/secrets-rotation-runbook.md`.
**Closes (PARTIAL):** GAP-379 (provisioning slice). AWS Secrets Manager seed + IAM policy apply là user steps, không auto-runnable từ CI.
**Standards:** AWS Well-Architected (Security pillar) · NIST SP 800-53 Rev 5 (SC-12 Cryptographic Key Management) · Twelve-Factor (config in env) · `.claude/rules/release-deploy-standard.md` §3.1 · Luật An ninh mạng 2018 + ND 53/2022/NĐ-CP (data localization → `ap-southeast-1` Singapore).
**Naming:** Per `.claude/rules/deployment-naming-convention.md` §2 — first-time setup artifacts live in `deploy/`.

---

## 1. Audience + scope

This runbook covers **first-time secrets seeding during release deploy** — pre-launch provisioning của một AWS account environment (production, staging, beta). One-time per environment. Cho ongoing rotation/audit/emergency-rotate procedures, xem **`documents/05-guides/operations/secrets-rotation-runbook.md`**.

Use this runbook when:
- Bootstrapping a fresh AWS account environment
- Provisioning a new secret added to the inventory (§3)
- Re-seeding after disaster recovery (DR) restore on a clean account

Do NOT use this runbook for:
- Routine rotation cadence (→ rotation runbook §5)
- Emergency rotation on compromise (→ rotation runbook §5.3)
- Quarterly audit (→ rotation runbook §6.3)

---

## 2. Architecture overview (summary)

```
┌───────────────┐
│  Spring Boot  │ ─── reads env vars at boot (12-factor)
│   service     │
└───────┬───────┘
        │ IAM role (EKS workload identity) OR IAM user (Phase 1 BETA)
        ▼
┌──────────────────────────────────┐
│  AWS Secrets Manager              │
│  (region: ap-southeast-1)         │
│  Namespacing: kitehub/<env>/<key> │
└──────────────────────────────────┘
```

Full architecture diagram + provider decisions: `operations/secrets-rotation-runbook.md` §1.

| Item | Decision |
|------|----------|
| Provider | **AWS Secrets Manager** (`infrastructure/terraform-aws/secrets.tf`) |
| Region | `ap-southeast-1` (Singapore) — closest VN-compliant region |
| Naming | `kitehub/<environment>/<secret-key>` |
| Seed source | Terraform `aws_secretsmanager_secret` + `_version` resources; manual values via `put-secret-value` |

---

## 3. Secrets inventory (Phase 1 BETA)

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

## 4. Provisioning procedure

### 4.1 Create secrets via Terraform (preferred)

`infrastructure/terraform-aws/secrets.tf` already declares some secrets. Extend per inventory §3:

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

### 4.2 Manual seed of secret values (one-time)

After `terraform apply` creates the empty secret resource, populate value:

```bash
aws secretsmanager put-secret-value \
  --region ap-southeast-1 \
  --secret-id kitehub/production/openai-api-key \
  --secret-string "sk-[USER_INPUT_REQUIRED]"
```

**Never commit real secret values to git.** Use 1Password / Bitwarden / company password manager as offline source-of-truth, sync to Secrets Manager via `put-secret-value`.

### 4.3 Generated random secrets (preferred for DB passwords / JWT)

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

## 5. IAM policy template — service access (apply at seed time)

### 5.1 EKS workload identity (preferred Phase 1.5+)

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

### 5.2 EC2 instance profile (Phase 1 BETA Oracle Cloud)

Phase 1 BETA chạy trên Oracle Cloud, không có IAM workload identity. Workaround:
1. Tạo dedicated IAM user `kitehub-prod-secrets-reader` với access keys.
2. Gắn policy như §5.1 (least-privilege per service — hoặc 1 policy gộp cho beta).
3. Lưu access keys trong env vars trên Oracle VM (`/etc/kitehub/secrets.env`, mode 600, owned by service user).
4. **Migrate to EKS workload identity Phase 1.5** — eliminate static keys.

---

## 6. Spring Boot integration at boot time

`application*.yml` **KHÔNG hardcode secret values**. Service đọc env vars từ Secrets Manager seed.

**Phase 1 BETA (Oracle VM) — env var injection:**
```bash
# /etc/kitehub/secrets.env (mode 600 on Oracle VM)
KITE_DB_PASSWORD=$(aws secretsmanager get-secret-value --secret-id kitehub/production/db-password --query SecretString --output text)
KITE_JWT_SECRET=$(aws secretsmanager get-secret-value --secret-id kitehub/production/jwt-secret --query SecretString --output text)
# ... per service-needed secret
```

```bash
# systemd unit
EnvironmentFile=/etc/kitehub/secrets.env
```

**Phase 1.5+ (EKS) — Spring Cloud AWS:**
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

Full Spring Boot integration patterns (config-refresh on rotation): `operations/secrets-rotation-runbook.md` §7.

---

## 7. Acceptance — first-time seeding

- [ ] Inventory §3 secrets all created via Terraform on production AWS account
- [ ] `terraform plan` shows ONLY `create` actions, không `destroy` resources hiện có
- [ ] IAM policy §5 applied per service (least-privilege verified)
- [ ] CloudTrail logs `CreateSecret` event timestamps khớp với terraform apply window
- [ ] First production service start → CloudTrail logs first `GetSecretValue` per service
- [ ] All `[USER_INPUT_REQUIRED]` placeholders replaced với real values via `put-secret-value`
- [ ] Spring Boot service boot test: service starts cleanly với env vars từ Secrets Manager (no `[USER_INPUT_REQUIRED]` leak)

After seeding verified → handoff sang **`operations/secrets-rotation-runbook.md`** cho ongoing rotation cadence.

---

## 8. Anti-patterns

| ❌ Don't | ✅ Do |
|---------|------|
| Commit `.env.production` to git | `.env.production.template` only; real values via Secrets Manager |
| Use 1 IAM role for all services | Per-service role, list ONLY needed secrets |
| Use `Resource: "*"` for "convenience" | List explicit secret ARNs |
| Hardcode `[USER_INPUT_REQUIRED]` placeholders into `application*.yml` | Service reads from env var; placeholder replaced during seed step §4.2 |
| Skip random_password Terraform pattern cho machine-generatable secrets | Use `random_password` resource — eliminates human handling |
| Apply seed runbook on existing populated environment | Seed is one-time; subsequent operations use rotation runbook |

---

## 9. Related

- **Sister runbook:** `documents/05-guides/operations/secrets-rotation-runbook.md` (rotation, audit, emergency-rotate, cost, full Spring Boot integration)
- `documents/05-guides/deploy/dns-setup-runbook.md`
- `documents/03-planning/roadmap/release-1-deploy-plan.md` (parent)
- `infrastructure/terraform-aws/secrets.tf` (existing IaC)
- `.env.production.template` (env vars consumed at boot)
- `.claude/rules/logs-format-standard.md` (PII scrubbing — never log secret values)
- `.claude/rules/release-deploy-standard.md` §3.1 Security
- `.claude/rules/deployment-naming-convention.md` §2 + §8 (split mandate)
- GAP-376 production seed admin password
- GAP-370 SES setup (consumes `ses-smtp-credentials`)
- GAP-371 Cloudflare setup (consumes `cloudflare-api-token`)
- GAP-452 (split rationale + AC)

---

## 10. Log

- **2026-05-11** Extracted from `operations/secrets-management-runbook.md` per `.claude/rules/deployment-naming-convention.md` §8 split mandate. Phase 1 (§3 Provisioning, §4 IAM, §9 first-time AC) lives here; ongoing rotation/audit lives in `operations/secrets-rotation-runbook.md`. Closes GAP-452.
