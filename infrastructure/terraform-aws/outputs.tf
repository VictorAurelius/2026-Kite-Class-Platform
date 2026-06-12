# =============================================================================
# Outputs — values needed for deploy + post-apply user actions
# =============================================================================

# --- Compute ---
output "kh_backend_public_ip" {
  description = "KH backend EC2 public IP"
  value       = aws_instance.kh_backend.public_ip
}

output "kh_backend_instance_id" {
  description = "KH backend EC2 instance ID (use for SSM session)"
  value       = aws_instance.kh_backend.id
}

output "kc_app_public_ip" {
  description = "KC app EC2 public IP"
  value       = aws_instance.kc_app.public_ip
}

output "kc_app_instance_id" {
  description = "KC app EC2 instance ID"
  value       = aws_instance.kc_app.id
}

output "alb_dns_name" {
  description = "ALB DNS name (Cloudflare CNAME target). Empty if enable_alb=false."
  value       = var.enable_alb ? aws_lb.main[0].dns_name : ""
}

# --- Database ---
output "rds_endpoint" {
  description = "RDS PostgreSQL endpoint (host:port)"
  value       = aws_db_instance.main.endpoint
}

output "rds_address" {
  description = "RDS PostgreSQL hostname only"
  value       = aws_db_instance.main.address
}

# --- Storage / Registry ---
output "s3_assets_bucket" {
  description = "S3 assets bucket name"
  value       = aws_s3_bucket.assets.id
}

output "ecr_registry_url" {
  description = "ECR registry URL (use for docker login + tag)"
  value       = "${data.aws_caller_identity.current.account_id}.dkr.ecr.${var.aws_region}.amazonaws.com"
}

output "ecr_repositories" {
  description = "Map of ECR repository names to URLs"
  value = {
    for k, v in aws_ecr_repository.services : k => v.repository_url
  }
}

# --- Secrets ---
output "secrets_arns" {
  description = "AWS Secrets Manager ARNs (consumed by EC2 instance profile)"
  value = {
    db_password = aws_secretsmanager_secret.db_password.arn
    jwt         = aws_secretsmanager_secret.jwt.arn
    encryption  = aws_secretsmanager_secret.encryption.arn
    placeholders = {
      for k, v in aws_secretsmanager_secret.placeholders : k => v.arn
    }
  }
}

# --- IAM (CI/CD) ---
output "github_terraform_plan_role_arn" {
  description = "IAM role ARN for terraform-plan workflow OIDC (use in workflow `role-to-assume`)"
  value       = aws_iam_role.github_terraform_plan.arn
}

output "github_terraform_apply_role_arn" {
  value       = aws_iam_role.github_terraform_apply.arn
  description = "ARN of OIDC role assumed by terraform-apply workflow_dispatch. Set as GitHub Variable AWS_TERRAFORM_APPLY_ROLE_ARN post-bootstrap."
}

# --- Convenience ---
output "ssm_kh_backend_command" {
  description = "Command to open SSM session to KH backend EC2"
  value       = "aws ssm start-session --target ${aws_instance.kh_backend.id} --region ${var.aws_region}"
}

output "ssm_kc_app_command" {
  description = "Command to open SSM session to KC app EC2"
  value       = "aws ssm start-session --target ${aws_instance.kc_app.id} --region ${var.aws_region}"
}

output "next_steps" {
  description = "Post-apply user actions"
  value       = <<-EOT
    Phase 1 BETA infrastructure provisioned. Next steps:
      1. Populate placeholder secrets via AWS console:
         aws secretsmanager put-secret-value --secret-id ${var.project_name}/${var.environment}/ses-smtp-credentials --secret-string '{...}'
         (repeat for cloudflare-api-token, ai-openai-api-key, etc.)
      2. SSH/SSM into kh-backend + kc-app to deploy Docker Compose stacks
         Use: aws ssm start-session --target ${aws_instance.kh_backend.id}
      3. Update Cloudflare DNS records:
         - kitehub.me  CNAME -> ${var.enable_alb ? aws_lb.main[0].dns_name : aws_instance.kc_app.public_ip}
      4. Configure ALB ACM cert (post DNS) → re-run terraform with alb_acm_certificate_arn set
      5. Verify GitHub OIDC role in terraform-plan workflow:
         role-to-assume: ${aws_iam_role.github_terraform_plan.arn}
  EOT
}

# --- Audit / Observability (GAP-437) ---
output "cloudtrail_log_bucket" {
  description = "S3 bucket holding CloudTrail audit logs"
  value       = aws_s3_bucket.cloudtrail_logs.bucket
}

output "cloudtrail_arn" {
  description = "CloudTrail trail ARN — for CloudWatch metric filter integration (Phase 2)"
  value       = aws_cloudtrail.main.arn
}
