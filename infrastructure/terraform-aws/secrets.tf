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
