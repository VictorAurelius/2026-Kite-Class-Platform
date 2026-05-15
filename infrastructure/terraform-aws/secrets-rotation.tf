# =============================================================================
# Secrets Manager Rotation — Wave 84 Bucket B (GAP-379)
# =============================================================================
# 90-day rotation for 4 in-scope secrets:
#   - db-password         -> AWS-managed SecretsManagerRDSPostgreSQLRotationSingleUser
#   - jwt-secret          -> custom rotate-secret-handler (in-house generator)
#   - encryption-key      -> custom rotate-secret-handler (in-house generator)
#   - seed-admin-password -> custom rotate-secret-handler (in-house generator)
#
# Out of scope (manual quarterly rotation per secrets-rotation-runbook.md):
#   - ses-smtp-credentials   (rotate via AWS SES console; coordinate w/ email module)
#   - cloudflare-api-token   (rotate via Cloudflare dashboard)
#   - resend-api-key         (rotate via Resend dashboard)
#   - ai-openai-api-key      (rotate via OpenAI dashboard)
#   - ai-anthropic-api-key   (rotate via Anthropic console)
#   - rabbitmq-default-creds (in-host config; coordinate w/ EC2 user_data refresh)
#
# References:
#   https://docs.aws.amazon.com/secretsmanager/latest/userguide/rotating-secrets-lambda-function-overview.html
#   .claude/rules/pre-launch-secrets-hardening-checklist.md §2.3 + §2.5
#   documents/05-guides/operations/secrets-rotation-runbook.md §5

# -----------------------------------------------------------------------------
# Lambda package — zip the handler source for upload.
# -----------------------------------------------------------------------------
data "archive_file" "rotate_secret_zip" {
  type        = "zip"
  source_dir  = "${path.module}/lambdas/rotate-secret"
  output_path = "${path.module}/lambdas/rotate-secret.zip"
  excludes    = ["tests", "__pycache__", "tests/__pycache__", ".pytest_cache"]
}

# -----------------------------------------------------------------------------
# IAM role + policy for custom rotation Lambda.
# Scoped to the 3 in-house secrets only (least-privilege per
# pre-launch-secrets-hardening-checklist.md §2.4 / §2.5).
# -----------------------------------------------------------------------------
resource "aws_iam_role" "rotate_secret_lambda" {
  name = "${var.project_name}-${var.environment}-rotate-secret-lambda"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Action    = "sts:AssumeRole"
      Principal = { Service = "lambda.amazonaws.com" }
    }]
  })

  tags = { Name = "${var.project_name}-rotate-secret-lambda" }
}

resource "aws_iam_role_policy_attachment" "rotate_secret_lambda_basic" {
  role       = aws_iam_role.rotate_secret_lambda.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

resource "aws_iam_role_policy" "rotate_secret_inline" {
  name = "${var.project_name}-rotate-secret-inline"
  role = aws_iam_role.rotate_secret_lambda.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "RotateInHouseSecrets"
        Effect = "Allow"
        Action = [
          "secretsmanager:DescribeSecret",
          "secretsmanager:GetSecretValue",
          "secretsmanager:PutSecretValue",
          "secretsmanager:UpdateSecretVersionStage",
        ]
        Resource = [
          aws_secretsmanager_secret.jwt.arn,
          aws_secretsmanager_secret.encryption.arn,
          aws_secretsmanager_secret.seed_admin_password.arn,
        ]
      },
      {
        Sid      = "GetRandomPassword"
        Effect   = "Allow"
        Action   = ["secretsmanager:GetRandomPassword"]
        Resource = "*"
      },
    ]
  })
}

# -----------------------------------------------------------------------------
# Custom rotation Lambda (jwt-secret, encryption-key, seed-admin-password).
# -----------------------------------------------------------------------------
resource "aws_lambda_function" "rotate_secret_handler" {
  function_name    = "${var.project_name}-${var.environment}-rotate-secret-handler"
  role             = aws_iam_role.rotate_secret_lambda.arn
  handler          = "rotate_secret_handler.lambda_handler"
  runtime          = "python3.12"
  timeout          = 30
  filename         = data.archive_file.rotate_secret_zip.output_path
  source_code_hash = data.archive_file.rotate_secret_zip.output_base64sha256

  environment {
    # PROBE_URL optional — leave unset for Phase 1 BETA (no in-process probe).
    # Set per-secret via aws_lambda_function_event_invoke_config if a probe is
    # added later (e.g., gateway /health/jwt verification endpoint).
    variables = {
      LOG_LEVEL = "INFO"
    }
  }

  tags = { Name = "${var.project_name}-rotate-secret-handler" }
}

# Allow Secrets Manager to invoke the Lambda for the 3 in-house secrets.
resource "aws_lambda_permission" "rotate_secret_invoke_jwt" {
  statement_id  = "AllowSecretsManagerInvokeJwt"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.rotate_secret_handler.function_name
  principal     = "secretsmanager.amazonaws.com"
  source_arn    = aws_secretsmanager_secret.jwt.arn
}

resource "aws_lambda_permission" "rotate_secret_invoke_encryption" {
  statement_id  = "AllowSecretsManagerInvokeEncryption"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.rotate_secret_handler.function_name
  principal     = "secretsmanager.amazonaws.com"
  source_arn    = aws_secretsmanager_secret.encryption.arn
}

resource "aws_lambda_permission" "rotate_secret_invoke_seed_admin" {
  statement_id  = "AllowSecretsManagerInvokeSeedAdmin"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.rotate_secret_handler.function_name
  principal     = "secretsmanager.amazonaws.com"
  source_arn    = aws_secretsmanager_secret.seed_admin_password.arn
}

# -----------------------------------------------------------------------------
# Rotation schedule wiring — 90 days for all 4 secrets.
# -----------------------------------------------------------------------------

# 1) DB password -> AWS-managed RDS rotation Lambda.
# The managed Lambda is provisioned by Secrets Manager itself when the
# rotation is configured against an RDS-format secret. We reference it by
# convention; the AWS console auto-creates it. For full IaC, replace this
# block with the Serverless Application Repository deploy of
# AWSSecretsManagerRDSPostgreSQLRotationSingleUser; for solo-dev mode we
# document the managed-Lambda dependency and leave AWS console / `aws
# secretsmanager rotate-secret` to first-bind.
#
# After terraform apply, run ONCE per environment:
#   aws secretsmanager rotate-secret \
#     --secret-id kitehub/production/db-password \
#     --rotation-lambda-arn arn:aws:lambda:ap-southeast-1:<acct>:function:SecretsManagerRDSPostgreSQLRotationSingleUser \
#     --rotation-rules AutomaticallyAfterDays=90
# OR set up rotation via the AWS console which provisions the SAR app
# automatically. Tracked in secrets-rotation-runbook.md §5.2.

resource "aws_secretsmanager_secret_rotation" "jwt" {
  secret_id           = aws_secretsmanager_secret.jwt.id
  rotation_lambda_arn = aws_lambda_function.rotate_secret_handler.arn

  rotation_rules {
    automatically_after_days = 90
  }

  depends_on = [aws_lambda_permission.rotate_secret_invoke_jwt]
}

resource "aws_secretsmanager_secret_rotation" "encryption" {
  secret_id           = aws_secretsmanager_secret.encryption.id
  rotation_lambda_arn = aws_lambda_function.rotate_secret_handler.arn

  rotation_rules {
    automatically_after_days = 90
  }

  depends_on = [aws_lambda_permission.rotate_secret_invoke_encryption]
}

resource "aws_secretsmanager_secret_rotation" "seed_admin" {
  secret_id           = aws_secretsmanager_secret.seed_admin_password.id
  rotation_lambda_arn = aws_lambda_function.rotate_secret_handler.arn

  rotation_rules {
    automatically_after_days = 90
  }

  depends_on = [aws_lambda_permission.rotate_secret_invoke_seed_admin]
}

# -----------------------------------------------------------------------------
# Outputs — used by scripts/test-secret-rotation.sh + audit artifacts.
# -----------------------------------------------------------------------------
output "rotate_secret_lambda_name" {
  description = "Lambda function name for custom rotation (used by test script)"
  value       = aws_lambda_function.rotate_secret_handler.function_name
}

output "rotate_secret_lambda_arn" {
  description = "Lambda function ARN for custom rotation"
  value       = aws_lambda_function.rotate_secret_handler.arn
}

output "rotation_managed_secrets" {
  description = "List of secrets under automated 90-day rotation"
  value = {
    custom_lambda = [
      aws_secretsmanager_secret.jwt.name,
      aws_secretsmanager_secret.encryption.name,
      aws_secretsmanager_secret.seed_admin_password.name,
    ]
    aws_managed_rds = [aws_secretsmanager_secret.db_password.name]
    manual_only = [
      "ses-smtp-credentials",
      "cloudflare-api-token",
      "resend-api-key",
      "ai-openai-api-key",
      "ai-anthropic-api-key",
      "rabbitmq-default-creds",
    ]
  }
}
