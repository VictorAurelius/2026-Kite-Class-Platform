# Cloudflare provider — Phase 1 BETA DNS for kitehub.me
# Wave 77 Bucket A — email SEND foundation
#
# Apply via: terraform init + terraform plan -out=tfplan + terraform apply tfplan
# DO NOT run apply during Wave 77 Bucket A — operator action only after Resend DKIM CNAME values are ready.

terraform {
  required_version = ">= 1.5.0"

  required_providers {
    cloudflare = {
      source  = "cloudflare/cloudflare"
      version = "~> 4.40"
    }
  }

  # Same S3 remote backend as terraform-aws (different state key)
  # backend "s3" {
  #   bucket         = "kitehub-tfstate-906286017800"
  #   key            = "infrastructure/terraform-cloudflare/state.tfstate"
  #   region         = "ap-southeast-1"
  #   dynamodb_table = "kitehub-tfstate-lock"
  #   encrypt        = true
  # }
  # NOTE: backend block commented until operator confirms init flow during Bucket A apply step.
}

provider "cloudflare" {
  api_token = var.cloudflare_api_token
}
