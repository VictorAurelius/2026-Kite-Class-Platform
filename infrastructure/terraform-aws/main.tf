# =============================================================================
# Kite Platform — Phase 1 BETA Production Infrastructure (AWS Singapore)
# =============================================================================
# Architecture B per ADR-025 (AWS-only deploy, Free Tier, ap-southeast-1)
#
# Cost projection (Phase 1 BETA, ~$72/mo Yr1 — AWS Activate $1k cover ~13.9 mo):
#   - 1× t3.micro EC2 (KH backend cluster):        Free tier 12mo, ~$8.5/mo after
#   - 1× t3.small  EC2 (KC + frontends):           ~$15/mo (no free tier for small)
#   - 1× RDS db.t3.micro Postgres:                 Free tier 12mo, ~$13/mo after
#   - 1× ALB (internet-facing):                    ~$16/mo
#   - S3 (assets):                                 Free tier 5GB, ~$0.5/mo
#   - ECR:                                         Free tier 500MB, ~$0.5/mo
#   - Secrets Manager (8 secrets × $0.40):         ~$3.2/mo
#   - Route53 hosted zone (optional):              ~$0.5/mo
#   - Data transfer (Cloudflare proxy front):      Free tier 100GB → minimal
#   - TOTAL Yr1 estimate:                          ~$25-40/mo within free tier; ~$72/mo after
#
# Region pin: ap-southeast-1 (Singapore) per ADR-025 — VN data localization debt
# acknowledged + risk-managed via Phase 1 invite-only (~10-20 tenants) + consent flow.
#
# Usage (after bootstrap/ has been applied to create state backend):
#   1. cd bootstrap && terraform init && terraform apply  # ONE-TIME
#   2. cd .. && cp terraform.tfvars.example terraform.tfvars
#   3. terraform init   (migrates state to S3)
#   4. terraform plan
#   5. HUMAN runs terraform apply (per GAP-381 Phase 2 BANNED for agent)
# =============================================================================

terraform {
  required_version = ">= 1.5"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.5"
    }
  }

  # Remote state backend — see backend.tf for actual config.
  # Bootstrap (`bootstrap/main.tf`) creates the S3 bucket + DynamoDB lock table
  # BEFORE this backend can be initialized.
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "Kite"
      Environment = var.environment
      ManagedBy   = "Terraform"
      Phase       = "1-beta"
      Region      = var.aws_region
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
