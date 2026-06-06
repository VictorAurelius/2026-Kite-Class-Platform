# =============================================================================
# Secrets Manager — 8-10 secrets per Architecture B
# =============================================================================
# All secrets consumed by EC2 instance profile (see iam.tf ec2_secrets_s3 policy).
# NO secret values inline; passwords generated with random_password (rotation
# defers to manual or AWS-managed rotation post-Phase 1).

# --- DB password (sourced from rds.tf random_password.rds) ---
resource "aws_secretsmanager_secret" "db_password" {
  name                    = "${var.project_name}/${var.environment}/db-password"
  description             = "RDS PostgreSQL master password"
  recovery_window_in_days = 7

  tags = { Name = "${var.project_name}-db-password" }
}

resource "aws_secretsmanager_secret_version" "db_password" {
  secret_id = aws_secretsmanager_secret.db_password.id
  secret_string = jsonencode({
    username = aws_db_instance.main.username
    password = random_password.rds.result
    host     = aws_db_instance.main.address
    port     = aws_db_instance.main.port
    dbname   = aws_db_instance.main.db_name
  })
}

# --- JWT signing secret ---
# Consumed by HS512 JWT mint (kiteclass-core AuthTokenService + kitehub gateway JWT
# verify). length=64 bytes = 512 bits = HS512 minimum key size (ZERO margin: the
# AuthTokenService boot guard fail-fasts if JWT_SECRET < 64 bytes, so 64 exactly passes
# but has no buffer). DO NOT change `length` here — it is in the lifecycle ignore_changes
# list below, so a length bump would NOT regenerate the live secret anyway; a real rotation
# to a longer key must go through the manual secrets-rotation-runbook (terraform state rm +
# re-create) to avoid silently desyncing state vs Secrets Manager. If margin is desired,
# rotate to length 88 via that runbook, not by editing this attribute.
#
# GAP-450 Option B: lifecycle ignore_changes prevents recurring drift on `result`
# attribute (state shows id="none" while Secrets Manager has live value from Phase 2.3
# apply 2026-05-07). Rotation is manual per documents/05-guides/operations/secrets-rotation-runbook.md;
# auto-regenerate-on-plan would silently rotate production secret = unwanted.
# Option A (terraform state rm + import current value) tracked in
# documents/05-guides/operations/terraform-state-import-runbook.md as user-executed
# follow-up requiring secretsmanager:GetSecretValue (Tier 2 per agent-aws-access.md §3)
# and terraform state rm/import (Tier 3 BANNED for agent per §4.3) — solo-dev runs manually.
resource "random_password" "jwt" {
  length  = 64
  special = false
  lifecycle {
    ignore_changes = [result, length, special, lower, upper, numeric, min_lower, min_upper, min_numeric, min_special, override_special, keepers]
  }
}

resource "aws_secretsmanager_secret" "jwt" {
  name                    = "${var.project_name}/${var.environment}/jwt-secret"
  description             = "JWT signing secret (HS512, 64-byte/512-bit minimum, zero margin)"
  recovery_window_in_days = 7
  tags                    = { Name = "${var.project_name}-jwt-secret" }
}

resource "aws_secretsmanager_secret_version" "jwt" {
  secret_id     = aws_secretsmanager_secret.jwt.id
  secret_string = random_password.jwt.result
}

# --- JWT challenge token signing secret (2FA enrollment flow) ---
# Wave 105 Bucket E0 GAP-717 — declare IaC for secret originally created manually
# via Wave 81 jwt-secret-fix-runbook 2026-05-15. Secret already exists in AWS
# Secrets Manager; terraform did not previously declare it → IaC drift. This
# resource closes drift forward; post AWS account 906286017800 restore (GAP-612
# unblock), run:
#   terraform import aws_secretsmanager_secret.jwt_challenge \
#     kitehub/production/jwt-challenge-secret
# to bind existing secret to terraform state (vs trying to re-create →
# AWS rejects "already exists"). Document live verify procedure inline in
# GAP-717 §"Post-AWS-restore live verify" section.
#
# Wave 79 Bucket C `ChallengeTokenService.@PostConstruct` fail-fast guard
# enforces non-dev-default in production profile. Wave 81 Bucket F PR #1388
# wired `scripts/fetch-secrets.sh` to fetch + write to /etc/kite/.env. IAM
# grant via wildcard `${var.project_name}/${var.environment}/*` pattern in
# iam.tf (ec2_app + deploy roles already covered, no edit needed).
#
# GAP-450 Option B: lifecycle ignore_changes prevents recurring drift on
# `result` attribute — same rationale as random_password.jwt / encryption.
# Rotation manual per documents/05-guides/operations/secrets-rotation-runbook.md.
resource "random_password" "jwt_challenge" {
  length  = 64
  special = false
  lifecycle {
    ignore_changes = [result, length, special, lower, upper, numeric, min_lower, min_upper, min_numeric, min_special, override_special, keepers]
  }
}

resource "aws_secretsmanager_secret" "jwt_challenge" {
  name                    = "${var.project_name}/${var.environment}/jwt-challenge-secret"
  description             = "HS256 secret for 2FA challenge token verify (Wave 79 GAP-509 / Wave 81 manual creation / Wave 105 IaC declaration)"
  recovery_window_in_days = 7
  tags                    = { Name = "${var.project_name}-jwt-challenge-secret" }
}

resource "aws_secretsmanager_secret_version" "jwt_challenge" {
  secret_id     = aws_secretsmanager_secret.jwt_challenge.id
  secret_string = random_password.jwt_challenge.result

  # Wave aws-restore-1 (2026-05-26): preserve Wave 81 manually-set secret value
  # post terraform import — random_password.jwt_challenge.result is initial
  # seed only; once imported, ignore_changes prevents overwrite.
  lifecycle {
    ignore_changes = [secret_string]
  }
}

# Wave aws-restore-1 (2026-05-26): import existing secret created manually
# Wave 81 (GAP-509 unblock) into terraform state. Per GAP-717 v1.0.0
# "Post-AWS-restore live verify" inline procedure.
import {
  to = aws_secretsmanager_secret.jwt_challenge
  id = "kitehub/production/jwt-challenge-secret"
}

# --- Resend API key (Stream A per ADR-025 — HTTP API replaces SMTP path) ---
# Wave br-4 Bucket A GAP-508 Phase 2/3 — declare IaC for secret to be created
# manually via AWS console post Resend account verified (kitehub.me DNS DKIM/SPF/DMARC).
# Pattern mirrors jwt-challenge precedent Wave 81 GAP-509 / Wave 105 GAP-717 — IaC
# ships first, real API key set manually qua AWS console once Resend domain verified.
#
# Secret payload schema (per scripts/fetch-secrets.sh GAP-572):
#   JSON wrapper: {"api_key":"re_...","from_email":"noreply@kitehub.me","from_name":"KiteHub Beta"}
#   OR plain string: "re_..." (from_email/from_name use defaults)
#
# Post AWS account 906286017800 restore (GAP-612 unblock) workflow:
#   1. Run `terraform apply` — creates empty placeholder secret (random_password version)
#   2. Manual override via AWS console: Secrets Manager → kitehub/production/resend-api-key
#      → Retrieve secret value → Set new value → JSON `{"api_key":"re_<real>","from_email":"noreply@kitehub.me","from_name":"KiteHub Beta"}`
#   3. lifecycle ignore_changes preserves manual real value across subsequent terraform apply
#
# IAM grant via wildcard `${var.project_name}/${var.environment}/*` pattern in
# iam.tf:54 (ec2_app role) — no edit needed; wildcard covers resend-api-key.
#
# fetch-secrets.sh:88-113 (Wave 81+) đã wire to pull this secret on EC2 boot.
# Phase 1 BETA scope — GAP-508 Phase 2 closure (Resend account provisioning Wave br-5+).
#
# GAP-450 Option B: lifecycle ignore_changes prevents recurring drift on
# `result` attribute — same rationale as jwt-challenge.
resource "random_password" "resend_api_key_placeholder" {
  length  = 32
  special = false
  lifecycle {
    ignore_changes = [result, length, special, lower, upper, numeric, min_lower, min_upper, min_numeric, min_special, override_special, keepers]
  }
}

resource "aws_secretsmanager_secret" "resend_api_key" {
  name                    = "${var.project_name}/${var.environment}/resend-api-key"
  description             = "Resend HTTP API key for transactional email (Phase 1 BETA Stream A per ADR-025); JSON wrapper schema {api_key, from_email, from_name} OR plain string; Wave br-4 Bucket A GAP-508 Phase 2/3"
  recovery_window_in_days = 7
  tags                    = { Name = "${var.project_name}-resend-api-key" }
}

resource "aws_secretsmanager_secret_version" "resend_api_key" {
  secret_id     = aws_secretsmanager_secret.resend_api_key.id
  secret_string = random_password.resend_api_key_placeholder.result

  lifecycle {
    ignore_changes = [secret_string]
  }
}

# Wave aws-restore-1 (2026-05-26): import existing secret created manually
# Wave 71b GAP-513 (Resend pivot post-ADR-025 Stream A) into terraform state.
# Pattern mirrors jwt-challenge import.
import {
  to = aws_secretsmanager_secret.resend_api_key
  id = "kitehub/production/resend-api-key"
}

# --- SePay API key (Wave flow-kh3-3 — payment webhook Apikey auth) ---
# Wave flow-kh3-2 added the SePay payment webhook (POST /api/platform/webhooks/payment)
# authenticated via `Authorization: Apikey <key>` where <key> = kitehub.payment.sepay.api-key.
# The key is VENDOR-PROVIDED (configured in the SePay dashboard at https://sepay.vn,
# Free 50tx/month tier covers Phase 1 BETA) — NOT random-generated. Pattern mirrors the
# resend-api-key precedent: IaC ships a placeholder version first; the real key is set
# manually via AWS console post-apply once the SePay merchant account is provisioned.
#
# Post AWS account 906286017800 restore (GAP-612 unblock) workflow:
#   1. Run `terraform apply` — creates placeholder secret (random_password version)
#   2. SePay dashboard → configure webhook URL https://kitehub.me/api/platform/webhooks/payment
#      + copy the generated API key
#   3. Manual override via AWS console: Secrets Manager → kitehub/production/sepay-api-key
#      → Retrieve secret value → Set new value → plain string "<sepay-api-key>"
#   4. lifecycle ignore_changes = [secret_string] preserves the manual real value across
#      subsequent terraform apply
#
# IAM grant via wildcard `${var.project_name}/${var.environment}/*` pattern in iam.tf
# (ec2_app + deploy roles already covered, no edit needed — same as jwt-challenge/resend).
#
# scripts/fetch-secrets.sh pulls this secret on EC2 boot into /etc/kite/.env as SEPAY_API_KEY.
#
# GAP-450 Option B: lifecycle ignore_changes prevents recurring drift on `result` attribute
# — same rationale as jwt-challenge / resend.
resource "random_password" "sepay_api_key_placeholder" {
  length  = 32
  special = false
  lifecycle {
    ignore_changes = [result, length, special, lower, upper, numeric, min_lower, min_upper, min_numeric, min_special, override_special, keepers]
  }
}

resource "aws_secretsmanager_secret" "sepay_api_key" {
  name                    = "${var.project_name}/${var.environment}/sepay-api-key"
  description             = "SePay merchant gateway API key for payment webhook Apikey auth (Phase 1 BETA); plain string; vendor-set manually via AWS console post-apply; Wave flow-kh3-3"
  recovery_window_in_days = 7
  tags                    = { Name = "${var.project_name}-sepay-api-key" }
}

resource "aws_secretsmanager_secret_version" "sepay_api_key" {
  secret_id     = aws_secretsmanager_secret.sepay_api_key.id
  secret_string = random_password.sepay_api_key_placeholder.result

  lifecycle {
    ignore_changes = [secret_string]
  }
}

# --- Encryption master key (32 bytes base64) ---
# GAP-450 Option B: lifecycle ignore_changes — same rationale as random_password.jwt above.
resource "random_password" "encryption_raw" {
  length  = 32
  special = false
  lifecycle {
    ignore_changes = [result, length, special, lower, upper, numeric, min_lower, min_upper, min_numeric, min_special, override_special, keepers]
  }
}

resource "aws_secretsmanager_secret" "encryption" {
  name                    = "${var.project_name}/${var.environment}/encryption-key"
  description             = "Application-level encryption master key (PII column encryption)"
  recovery_window_in_days = 7
  tags                    = { Name = "${var.project_name}-encryption-key" }
}

resource "aws_secretsmanager_secret_version" "encryption" {
  secret_id     = aws_secretsmanager_secret.encryption.id
  secret_string = base64encode(random_password.encryption_raw.result)
}

# --- Seed admin password (GAP-499: Wave 67 production seed prerequisite) ---
# Generated random password for PLATFORM_ADMIN user inserted by ProductionSeedRunner.
# User rotates manually post-cutover per secrets-rotation-runbook.md.
# GAP-450 Option B: ignore_changes — same rationale as jwt/encryption above.
resource "random_password" "seed_admin" {
  length  = 32
  special = true
  lifecycle {
    ignore_changes = [result, length, special, lower, upper, numeric, min_lower, min_upper, min_numeric, min_special, override_special, keepers]
  }
}

resource "aws_secretsmanager_secret" "seed_admin_password" {
  name                    = "${var.project_name}/${var.environment}/seed-admin-password"
  description             = "Initial password for PLATFORM_ADMIN user seeded by ProductionSeedRunner (rotate post-cutover)"
  recovery_window_in_days = 7
  tags                    = { Name = "${var.project_name}-seed-admin-password" }
}

resource "aws_secretsmanager_secret_version" "seed_admin_password" {
  secret_id     = aws_secretsmanager_secret.seed_admin_password.id
  secret_string = random_password.seed_admin.result
}

# --- Placeholder secrets (filled by user post-apply via AWS console) ---
# These are CREATED but EMPTY — user populates with real values.
locals {
  placeholder_secrets = {
    "ses-smtp-credentials"   = "AWS SES SMTP username + password (per GAP-370)"
    "cloudflare-api-token"   = "Cloudflare DNS edit token (per GAP-369)"
    "ai-openai-api-key"      = "OpenAI API key (Phase 2 fallback per ADR-026)"
    "ai-anthropic-api-key"   = "Anthropic API key (Phase 2 fallback)"
    "rabbitmq-default-creds" = "RabbitMQ default user/pass (self-host on EC2)"
  }
}

resource "aws_secretsmanager_secret" "placeholders" {
  for_each = local.placeholder_secrets

  name                    = "${var.project_name}/${var.environment}/${each.key}"
  description             = each.value
  recovery_window_in_days = 7

  tags = { Name = "${var.project_name}-${each.key}" }
}

# Note: deliberately NO `aws_secretsmanager_secret_version` for placeholders —
# user populates manually via:
#   aws secretsmanager put-secret-value --secret-id kitehub/production/ses-smtp-credentials \
#     --secret-string '{"username":"...","password":"..."}'
