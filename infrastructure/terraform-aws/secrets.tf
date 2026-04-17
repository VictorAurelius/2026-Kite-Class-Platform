# =============================================================================
# AWS Secrets Manager
# =============================================================================

resource "aws_secretsmanager_secret" "jwt" {
  name                    = "${var.project_name}/${var.environment}/jwt-secret"
  recovery_window_in_days = 7
  tags                    = { Name = "JWT Secret" }
}

resource "aws_secretsmanager_secret_version" "jwt" {
  secret_id     = aws_secretsmanager_secret.jwt.id
  secret_string = random_password.jwt.result
}

resource "random_password" "jwt" {
  length  = 64
  special = false
}

resource "aws_secretsmanager_secret" "encryption" {
  name                    = "${var.project_name}/${var.environment}/encryption-key"
  recovery_window_in_days = 7
  tags                    = { Name = "Encryption Master Key" }
}

resource "aws_secretsmanager_secret_version" "encryption" {
  secret_id     = aws_secretsmanager_secret.encryption.id
  secret_string = base64encode(random_password.encryption_raw.result)
}

resource "random_password" "encryption_raw" {
  length  = 32
  special = false
}

resource "aws_secretsmanager_secret" "db_password" {
  name                    = "${var.project_name}/${var.environment}/db-password"
  recovery_window_in_days = 7
  tags                    = { Name = "Database Password" }
}

resource "aws_secretsmanager_secret_version" "db_password" {
  secret_id     = aws_secretsmanager_secret.db_password.id
  secret_string = random_password.rds.result
}

# Output secret ARNs for External Secrets Operator
output "secret_arns" {
  description = "AWS Secrets Manager ARNs"
  value = {
    jwt        = aws_secretsmanager_secret.jwt.arn
    encryption = aws_secretsmanager_secret.encryption.arn
    db         = aws_secretsmanager_secret.db_password.arn
  }
}
