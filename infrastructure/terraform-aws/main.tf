# =============================================================================
# KiteHub Production Infrastructure - AWS
# =============================================================================
# Usage:
#   cp terraform.tfvars.example terraform.tfvars
#   terraform init
#   terraform plan
#   terraform apply
# =============================================================================

terraform {
  required_version = ">= 1.5"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # Remote state — run bootstrap/ first to create S3 bucket + DynamoDB table
  # Update bucket name with output from: cd bootstrap && terraform output state_bucket_name
  backend "s3" {
    bucket         = "kitehub-terraform-state-<ACCOUNT_ID>"
    key            = "production/terraform.tfstate"
    region         = "ap-southeast-1"
    dynamodb_table = "kitehub-terraform-locks"
    encrypt        = true
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "KiteHub"
      Environment = var.environment
      ManagedBy   = "Terraform"
    }
  }
}

# =============================================================================
# DATA SOURCES
# =============================================================================

data "aws_availability_zones" "available" {
  state = "available"
}

data "aws_caller_identity" "current" {}
