# =============================================================================
# Remote State Backend — S3 + DynamoDB lock (PARTIAL CONFIG)
# =============================================================================
# Per GAP-396: production Terraform state lives in S3 with DynamoDB locking.
#
# Bucket name (which embeds AWS account ID) is intentionally OMITTED from this
# committed file because the repo is PUBLIC. The bucket name is provided at
# init time via `-backend-config=backend.config` (gitignored).
#
# Bootstrap (in `bootstrap/`) creates the state-backing resources BEFORE this
# backend can initialize. Migration sequence:
#
#   1. cd bootstrap && terraform init && terraform apply
#   2. Note the `state_bucket_name` output (e.g. kitehub-terraform-state-XXXXXX)
#   3. cd .. && cp backend.config.example backend.config
#   4. Edit backend.config → set `bucket = "<output from step 2>"`
#   5. terraform init -backend-config=backend.config -migrate-state
#   6. Confirm migration prompt — local state uploaded to S3
#
# IMPORTANT: This backend block CANNOT use variables or interpolation
# (HCL limitation). Sensitive values must be supplied via -backend-config.
# Non-sensitive values (key path, region, lock table) stay inline for clarity.
# =============================================================================

terraform {
  backend "s3" {
    # bucket — supplied via backend.config (gitignored, contains account ID)
    key            = "phase-1-beta/terraform.tfstate"
    region         = "ap-southeast-1"
    dynamodb_table = "kitehub-terraform-locks"
    encrypt        = true
  }
}
