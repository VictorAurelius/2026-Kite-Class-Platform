# =============================================================================
# CloudTrail — multi-region API audit log (GAP-437 Phase 1)
# =============================================================================
# Captures all AWS API management events to a dedicated S3 bucket. Audit
# baseline for security incident investigation + compliance (PDPL 2023,
# ISO27001, SOC2 readiness). Free tier: management events first copy = $0.
#
# Phase 1 BETA scope: management events only (no data events — those add up
# fast at $0.10/100k). Multi-region so ap-southeast-1 + us-east-1 (default
# console fallback) both captured.
# =============================================================================

# --- Audit log bucket ---

resource "aws_s3_bucket" "cloudtrail_logs" {
  bucket = "${var.project_name}-cloudtrail-logs-${data.aws_caller_identity.current.account_id}"

  lifecycle {
    prevent_destroy = true
  }

  tags = {
    Project   = var.project_name
    ManagedBy = "Terraform"
    Purpose   = "cloudtrail-audit-logs"
  }
}

resource "aws_s3_bucket_versioning" "cloudtrail_logs" {
  bucket = aws_s3_bucket.cloudtrail_logs.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "cloudtrail_logs" {
  bucket = aws_s3_bucket.cloudtrail_logs.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "cloudtrail_logs" {
  bucket                  = aws_s3_bucket.cloudtrail_logs.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# Lifecycle: expire raw logs after 90 days (cost hygiene). Adjust to 7 years
# if compliance/legal hold required for financial events (per logs-format-standard.md §4).
resource "aws_s3_bucket_lifecycle_configuration" "cloudtrail_logs" {
  bucket     = aws_s3_bucket.cloudtrail_logs.id
  depends_on = [aws_s3_bucket_versioning.cloudtrail_logs]

  rule {
    id     = "expire-old-cloudtrail-logs"
    status = "Enabled"
    filter {}

    abort_incomplete_multipart_upload {
      days_after_initiation = 7
    }

    transition {
      days          = 30
      storage_class = "STANDARD_IA"
    }

    expiration {
      days = 90
    }

    noncurrent_version_expiration {
      noncurrent_days = 30
    }
  }
}

# --- Bucket policy: allow CloudTrail service principal to write ---

resource "aws_s3_bucket_policy" "cloudtrail_logs" {
  bucket = aws_s3_bucket.cloudtrail_logs.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "AWSCloudTrailAclCheck"
        Effect = "Allow"
        Principal = {
          Service = "cloudtrail.amazonaws.com"
        }
        Action   = "s3:GetBucketAcl"
        Resource = aws_s3_bucket.cloudtrail_logs.arn
        Condition = {
          StringEquals = {
            "aws:SourceArn" = "arn:aws:cloudtrail:${var.aws_region}:${data.aws_caller_identity.current.account_id}:trail/${var.project_name}-main"
          }
        }
      },
      {
        Sid    = "AWSCloudTrailWrite"
        Effect = "Allow"
        Principal = {
          Service = "cloudtrail.amazonaws.com"
        }
        Action   = "s3:PutObject"
        Resource = "${aws_s3_bucket.cloudtrail_logs.arn}/AWSLogs/${data.aws_caller_identity.current.account_id}/*"
        Condition = {
          StringEquals = {
            "s3:x-amz-acl"  = "bucket-owner-full-control"
            "aws:SourceArn" = "arn:aws:cloudtrail:${var.aws_region}:${data.aws_caller_identity.current.account_id}:trail/${var.project_name}-main"
          }
        }
      },
    ]
  })
}

# --- CloudTrail trail ---

resource "aws_cloudtrail" "main" {
  name           = "${var.project_name}-main"
  s3_bucket_name = aws_s3_bucket.cloudtrail_logs.id

  # Multi-region: capture API calls from any AWS region (console fallbacks
  # often hit us-east-1 even when working in ap-southeast-1).
  is_multi_region_trail = true

  # Global service events (IAM, STS, CloudFront, Route53) included once.
  include_global_service_events = true

  # Log file integrity: SHA-256 + RSA-signed digest for tamper detection.
  enable_log_file_validation = true

  # Management events default = ReadOnly + WriteOnly (all). No data events
  # in Phase 1 BETA (they cost $0.10/100k and add little value at low scale).
  event_selector {
    read_write_type           = "All"
    include_management_events = true
  }

  # CloudWatch Logs delivery (Wave 84 Bucket A - GAP-437 Phase 2).
  # Enables metric filters in cloudtrail-metric-filters.tf to extract security
  # signals from CloudTrail event stream. Role + log group are defined in
  # cloudtrail-metric-filters.tf.
  cloud_watch_logs_group_arn = "${aws_cloudwatch_log_group.cloudtrail_events.arn}:*"
  cloud_watch_logs_role_arn  = aws_iam_role.cloudtrail_logs_delivery.arn

  tags = {
    Project   = var.project_name
    ManagedBy = "Terraform"
    Purpose   = "audit-trail"
  }

  depends_on = [
    aws_s3_bucket_policy.cloudtrail_logs,
    aws_iam_role_policy.cloudtrail_logs_delivery,
  ]
}
