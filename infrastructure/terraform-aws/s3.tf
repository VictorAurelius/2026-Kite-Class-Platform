# =============================================================================
# S3 — Assets bucket (replaces MinIO Phase 1 BETA per ADR-025)
# =============================================================================
# Phase 1 simplified — single bucket, versioning enabled, encryption.
# Cross-region replication + Object Lock deferred to GA per cost guard.

resource "aws_s3_bucket" "assets" {
  bucket = "${var.project_name}-assets-${var.environment}-${data.aws_caller_identity.current.account_id}"
  tags   = { Name = "${var.project_name}-assets" }
}

resource "aws_s3_bucket_versioning" "assets" {
  bucket = aws_s3_bucket.assets.id
  versioning_configuration { status = "Enabled" }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "assets" {
  bucket = aws_s3_bucket.assets.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "assets" {
  bucket                  = aws_s3_bucket.assets.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# Lifecycle: clean incomplete multipart + non-current version expiry (cost hygiene)
resource "aws_s3_bucket_lifecycle_configuration" "assets" {
  bucket     = aws_s3_bucket.assets.id
  depends_on = [aws_s3_bucket_versioning.assets]

  rule {
    id     = "cost-hygiene"
    status = "Enabled"
    filter {}

    abort_incomplete_multipart_upload {
      days_after_initiation = 7
    }

    noncurrent_version_expiration {
      noncurrent_days = 90 # Phase 1 BETA shorter retention for free tier
    }
  }
}

# =============================================================================
# S3 — KiteClass files bucket (MinIO->S3 production parity)
# =============================================================================
# kiteclass-core file storage (uploads, branding assets, vetting evidence).
# Local dev uses MinIO (storage.s3.endpoint=http://localhost:9000); production
# uses this bucket via the EC2 instance role (S3Config DefaultCredentialsProvider
# when STORAGE_S3_ACCESS_KEY blank). Separate from `assets` (kitehub branding) so
# RBAC + lifecycle stay independent.
#
# NOTE: created out-of-band via CLI 2026-06-18 for internal test (instance role
# already had S3 perms extended in iam.tf). `terraform import` required to bring
# under state (see follow-up gap). Bucket name matches CLI-created name exactly.

resource "aws_s3_bucket" "kiteclass_files" {
  bucket = "kiteclass-files-${var.environment}-${data.aws_caller_identity.current.account_id}"
  tags   = { Name = "kiteclass-files" }
}

resource "aws_s3_bucket_versioning" "kiteclass_files" {
  bucket = aws_s3_bucket.kiteclass_files.id
  versioning_configuration { status = "Enabled" }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "kiteclass_files" {
  bucket = aws_s3_bucket.kiteclass_files.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "kiteclass_files" {
  bucket                  = aws_s3_bucket.kiteclass_files.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# CORS: presigned browser uploads/downloads. Restricted to production app origins.
resource "aws_s3_bucket_cors_configuration" "kiteclass_files" {
  bucket = aws_s3_bucket.kiteclass_files.id

  cors_rule {
    allowed_headers = ["*"]
    allowed_methods = ["GET", "PUT", "POST", "HEAD"]
    allowed_origins = [
      "https://kitehub.me",
      "https://app.kitehub.me",
      "https://*.kitehub.me",
    ]
    expose_headers  = ["ETag"]
    max_age_seconds = 3000
  }
}

# Lifecycle: clean incomplete multipart + non-current version expiry (cost hygiene)
resource "aws_s3_bucket_lifecycle_configuration" "kiteclass_files" {
  bucket     = aws_s3_bucket.kiteclass_files.id
  depends_on = [aws_s3_bucket_versioning.kiteclass_files]

  rule {
    id     = "cost-hygiene"
    status = "Enabled"
    filter {}

    abort_incomplete_multipart_upload {
      days_after_initiation = 7
    }

    noncurrent_version_expiration {
      noncurrent_days = 90 # Phase 1 BETA shorter retention for free tier
    }
  }
}
