# =============================================================================
# KiteHub Terraform State Bootstrap — AWS
# =============================================================================
# Run this ONCE before running the main Terraform in ../
#
# Usage:
#   cd infrastructure/terraform-aws/bootstrap
#   terraform init
#   terraform apply
#
# This creates:
#   - S3 bucket for Terraform state (versioning + encryption + public access block)
#   - DynamoDB table for state locking
#
# After applying, uncomment the backend "s3" block in ../main.tf and run:
#   cd ..
#   terraform init   (will migrate local state to S3)
#   terraform plan
# =============================================================================

terraform {
  required_version = ">= 1.5"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
  # Bootstrap uses local state intentionally — it creates the remote state backend
}

provider "aws" {
  region = var.aws_region
}

# --- S3 Bucket for Terraform State ---

resource "aws_s3_bucket" "terraform_state" {
  bucket = "kitehub-terraform-state-${data.aws_caller_identity.current.account_id}"

  lifecycle {
    prevent_destroy = true
  }

  tags = {
    Project   = "KiteHub"
    ManagedBy = "Terraform"
    Purpose   = "terraform-state"
  }
}

resource "aws_s3_bucket_versioning" "state" {
  bucket = aws_s3_bucket.terraform_state.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "state" {
  bucket = aws_s3_bucket.terraform_state.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "aws:kms"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "state" {
  bucket                  = aws_s3_bucket.terraform_state.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# Lifecycle: expire old non-current state versions after 90 days (cost hygiene)
resource "aws_s3_bucket_lifecycle_configuration" "state" {
  bucket     = aws_s3_bucket.terraform_state.id
  depends_on = [aws_s3_bucket_versioning.state]

  rule {
    id     = "expire-old-state-versions"
    status = "Enabled"
    filter {}

    abort_incomplete_multipart_upload {
      days_after_initiation = 7
    }

    noncurrent_version_expiration {
      noncurrent_days = 90
    }
  }
}

# --- DynamoDB Table for State Locking ---

resource "aws_dynamodb_table" "terraform_locks" {
  name         = "kitehub-terraform-locks"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "LockID"

  attribute {
    name = "LockID"
    type = "S"
  }

  tags = {
    Project   = "KiteHub"
    ManagedBy = "Terraform"
    Purpose   = "terraform-state-lock"
  }
}

# --- Data Sources ---

data "aws_caller_identity" "current" {}

variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "ap-southeast-1"
}

# --- Outputs ---

output "state_bucket_name" {
  description = "S3 bucket name for Terraform state — use this in ../main.tf backend block"
  value       = aws_s3_bucket.terraform_state.bucket
}

output "dynamodb_table_name" {
  description = "DynamoDB table name for state locking"
  value       = aws_dynamodb_table.terraform_locks.name
}

output "next_steps" {
  description = "Instructions after bootstrap"
  value       = <<-EOT
    Bootstrap complete! Next steps:
    1. Copy bucket name: ${aws_s3_bucket.terraform_state.bucket}
    2. Edit ../main.tf → uncomment backend "s3" block → update bucket name
    3. Run: cd .. && terraform init
    4. Confirm state migration when prompted
  EOT
}
