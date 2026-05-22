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
  description             = "JWT signing secret (HS256)"
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
