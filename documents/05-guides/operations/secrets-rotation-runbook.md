# Secrets Rotation Runbook — AWS Secrets Manager (ongoing operations)

**Audience:** SRE / DevOps thực hiện routine rotation, audit, emergency-rotate procedures. Đây là recurring artifact (cadence-driven hoặc incident-driven).
**Sister runbook:** first-time seeding during release deploy lives in `documents/05-guides/deploy/secrets-seeding-runbook.md`.
**Closes (PARTIAL):** GAP-379 — runbook artifact (rotation/audit slice). Lambda automated rotation tracked separately.
**Standards:** AWS Well-Architected (Security pillar) · NIST SP 800-53 Rev 5 (SC-12 Cryptographic Key Management) · Twelve-Factor (config in env) · `.claude/rules/release-deploy-standard.md` §3.1 · Luật An ninh mạng 2018 + ND 53/2022/NĐ-CP (data localization → `ap-southeast-1` Singapore region).
**Naming:** Per `.claude/rules/deployment-naming-convention.md` §2 — recurring ops artifacts live in `operations/`.

---

## 0. Audience + scope

This runbook covers **ongoing rotation, audit, and emergency-rotate procedures** — recurring per cadence hoặc incident-driven. Cho first-time provisioning trong release deploy, xem **`documents/05-guides/deploy/secrets-seeding-runbook.md`**.

Use this runbook when:
- Quarterly / cadence-driven rotation per inventory (§2)
- Emergency rotation on suspected compromise (§5.3)
- Quarterly audit + compliance review (§6.3)
- Configuring Spring Boot service to consume rotated secrets (§7)

Do NOT use this runbook for:
- First-time secret seeding on a fresh AWS environment (→ seeding runbook)
- New secret added to inventory (→ seeding runbook §4 first, then return here for rotation cadence)

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

## 3. Provisioning (first-time seed) — moved

First-time secrets seeding (Terraform create, manual `put-secret-value`, random_password pattern) is documented in **`documents/05-guides/deploy/secrets-seeding-runbook.md` §4**. Đó là one-time per environment artifact; rotation runbook này focuses on ongoing operations.

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

### 5.2 Lambda automated rotation (Wave 84 Bucket B — landed 2026-05-15)

90-day automated rotation cho 4 in-house secrets đã được wired qua Wave 84 Bucket B (GAP-379 → DONE 100%):

| Secret | Rotation strategy | Provisioned by |
|---|---|---|
| `kitehub/<env>/db-password` | AWS-managed `SecretsManagerRDSPostgreSQLRotationSingleUser` | Bootstrap manual sau first `terraform apply` — xem §5.2.1 |
| `kitehub/<env>/jwt-secret` | Custom Lambda `kitehub-<env>-rotate-secret-handler` | `aws_secretsmanager_secret_rotation.jwt` (terraform-aws/secrets-rotation.tf) |
| `kitehub/<env>/encryption-key` | Custom Lambda (cùng function) | `aws_secretsmanager_secret_rotation.encryption` |
| `kitehub/<env>/seed-admin-password` | Custom Lambda (cùng function) | `aws_secretsmanager_secret_rotation.seed_admin` |

Custom Lambda code: `infrastructure/terraform-aws/lambdas/rotate-secret/rotate_secret_handler.py`. Implement 4-step AWS lifecycle (createSecret → setSecret → testSecret → finishSecret). `setSecret` no-op cho in-house secrets (services reload qua env-var injection at next boot / SSM refresh). `testSecret` skip nếu env var `PROBE_URL` không set.

#### 5.2.1 Bootstrap AWS-managed RDS rotation (one-time per environment)

Sau khi `terraform apply` ship Wave 84 Bucket B, RDS db-password chưa được wire vì AWS-managed Lambda (`SecretsManagerRDSPostgreSQLRotationSingleUser`) cần Serverless Application Repository deploy hoặc AWS console bootstrap. Chạy ONCE per environment:

```bash
# Option A — AWS console (recommended cho solo-dev):
# 1. Console > Secrets Manager > kitehub/production/db-password
# 2. "Edit rotation" > "Use AWS-managed rotation"
# 3. Single-user rotation strategy, 90 days
# 4. Console auto-creates Lambda + IAM + invocation permission

# Option B — CLI (advanced, nếu cần IaC purity):
aws secretsmanager rotate-secret \
  --secret-id kitehub/production/db-password \
  --rotation-lambda-arn arn:aws:lambda:ap-southeast-1:<acct-id>:function:SecretsManagerRDSPostgreSQLRotationSingleUser \
  --rotation-rules AutomaticallyAfterDays=90
```

#### 5.2.2 Test rotation manually (post-apply verify)

```bash
bash scripts/test-secret-rotation.sh jwt-secret
# expect: PASS — AWSCURRENT advanced, AWSPREVIOUS == pre-rotation version
```

Script supports `jwt-secret` / `encryption-key` / `seed-admin-password`. Refuses to rotate `db-password` (AWS-managed) hoặc vendor API keys.

#### 5.2.3 Service reload after rotation

Spring Boot services đọc secrets qua env-var injection at boot (`fetch-secrets.sh` chạy trong EC2 user_data). Sau rotation, services chưa pick up new value cho đến lần restart kế tiếp. Phase 1 BETA: trigger restart qua SSM SendCommand sau rotation alarm fire (CloudWatch alarm on `Secrets Manager RotationOccurred` event → SNS → Lambda → SSM). Phase 1.5+: implement Spring Cloud AWS auto-refresh via `RefreshScope`.

### 5.2.B External API keys — manual quarterly rotation

Vendor API keys (Cloudflare, Resend, OpenAI, Anthropic, SES SMTP) KHÔNG auto-rotate (vendor portal là source of truth, không cho remote API rotate). Quarterly cadence:

| Secret | Rotation procedure |
|---|---|
| `cloudflare-api-token` | Cloudflare dashboard → My Profile → API Tokens → rotate → `aws secretsmanager put-secret-value` |
| `resend-api-key` | Resend dashboard → API Keys → revoke + create → `aws secretsmanager put-secret-value` |
| `ai-openai-api-key` | OpenAI dashboard → API keys → rotate → `aws secretsmanager put-secret-value` |
| `ai-anthropic-api-key` | Anthropic console → API keys → rotate → `aws secretsmanager put-secret-value` |
| `ses-smtp-credentials` | AWS console → IAM → smtp user → access keys → rotate → `aws secretsmanager put-secret-value` |

Track next-rotate date trong 1Password vault note. After rotation, trigger EC2 service restart (`kc-app` cho Cloudflare, `kh-backend` cho Resend / AI / SES) qua SSM SendCommand để pick up new value.

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

## 9. Acceptance — rotation cadence verification

- [ ] Per-secret `Next rotate` date tracked (1Password vault note or terraform var) ≤ inventory §2 cadence
- [ ] Manual DB password rotation §5.1 dry-run on staging — zero downtime confirmed at least quarterly
- [ ] Emergency rotation §5.3 procedure tested annually on staging (table-top simulation OK)
- [ ] CloudTrail `GetSecretValue` audit query §6.1 runs cleanly on production weekly
- [ ] Quarterly audit §6.3 checklist completed; findings filed as gaps per `audit-to-gap-pipeline.md`
- [ ] No secret past `Next rotate` date trong inventory

First-time seeding AC lives in `deploy/secrets-seeding-runbook.md` §7.

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

- **Sister runbook:** `documents/05-guides/deploy/secrets-seeding-runbook.md` (first-time provisioning)
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

## 12. Log

- **2026-05-11** Renamed from `secrets-management-runbook.md` per `.claude/rules/deployment-naming-convention.md` §8 split mandate. §3 Provisioning + first-time AC moved to `deploy/secrets-seeding-runbook.md`; §9 reframed as rotation cadence AC. Closes GAP-452.
- **2026-05-07** Wave 33 Bucket D — runbook + IAM policy templates + rotation procedures shipped. GAP-379 stays 🟡 PARTIAL: AWS Secrets Manager populate + IAM policy apply on production AWS account are user steps (cannot run from CI).
