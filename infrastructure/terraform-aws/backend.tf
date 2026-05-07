# =============================================================================
# Remote State Backend — S3 + DynamoDB lock
# =============================================================================
# Per GAP-396: production Terraform state lives in S3 with DynamoDB locking.
#
# Bootstrap (in `bootstrap/`) creates these resources BEFORE this backend can
# initialize. Migration sequence:
#
#   1. cd bootstrap && terraform init && terraform apply
#   2. Note the `state_bucket_name` output (e.g. kitehub-terraform-state-123456789012)
#   3. Update the `bucket` value below with that exact name
#   4. cd .. && terraform init -migrate-state
#   5. Confirm migration prompt — local state will be uploaded to S3
#
# IMPORTANT: This backend block CANNOT use variables or interpolation
# (HCL limitation). Update the `bucket` value manually after bootstrap.
# =============================================================================

terraform {
  backend "s3" {
    # NOTE: Replace <ACCOUNT_ID> with actual AWS account ID after bootstrap apply.
    # Bootstrap output `state_bucket_name` provides the exact value.
    bucket         = "kitehub-terraform-state-906286017800"
    key            = "phase-1-beta/terraform.tfstate"
    region         = "ap-southeast-1"
    dynamodb_table = "kitehub-terraform-locks"
    encrypt        = true
  }
}
