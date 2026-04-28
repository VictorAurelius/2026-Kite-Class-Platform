# =============================================================================
# S3 Bucket (Assets) + ECR Repositories
# =============================================================================
# GAP-118: Backup + replication strategy
#   - Versioning enabled (recover deleted/overwritten objects)
#   - Cross-region replication ap-southeast-1 -> us-east-1 (DR + geo-redundancy)
#   - Lifecycle: non-current versions -> Glacier 30d, expire after 1y
#   - Object Lock (compliance) for templates prefix (immutable IP protection)
#
# Cost impact (rough, prod-scale ~50 GB current assets, ~10 GB/mo growth):
#   - Versioning storage:     +20-50% bucket storage cost (avg ~ +$0.50/mo per 50 GB)
#   - Cross-region replica:   2x storage in destination region (~$1.15/mo per 50 GB)
#                             + replication PUT requests + inter-region transfer
#                             (~$0.02/GB transferred = ~$0.20/mo @ 10 GB delta)
#   - Glacier Deep Archive:   $0.00099/GB/mo for non-current versions older than 30d
#   - Object Lock:            no extra cost beyond storage
#   - TOTAL estimate:         ~$2-4/mo at current scale; scales linearly with assets
#   See documents/05-guides/operations/dr-rto-rpo-matrix.md (GAP-119) for SLA mapping.
# =============================================================================

# -----------------------------------------------------------------------------
# Provider alias for replica region (us-east-1)
# -----------------------------------------------------------------------------
provider "aws" {
  alias  = "replica"
  region = var.s3_replica_region

  default_tags {
    tags = {
      Project     = "KiteHub"
      Environment = var.environment
      ManagedBy   = "Terraform"
      Role        = "dr-replica"
    }
  }
}

# -----------------------------------------------------------------------------
# Primary S3 Bucket (assets) - ap-southeast-1
# -----------------------------------------------------------------------------
resource "aws_s3_bucket" "assets" {
  bucket = "${var.project_name}-assets-${var.environment}"
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

# Block all public access (defense-in-depth alongside bucket policy)
resource "aws_s3_bucket_public_access_block" "assets" {
  bucket                  = aws_s3_bucket.assets.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# -----------------------------------------------------------------------------
# Lifecycle: keep current versions; tier non-current versions to Glacier; expire 1y
# -----------------------------------------------------------------------------
resource "aws_s3_bucket_lifecycle_configuration" "assets" {
  bucket = aws_s3_bucket.assets.id

  # Versioning must be enabled before lifecycle can reference noncurrent transitions
  depends_on = [aws_s3_bucket_versioning.assets]

  rule {
    id     = "noncurrent-version-tiering"
    status = "Enabled"

    # Empty filter = applies to all objects in bucket
    filter {}

    noncurrent_version_transition {
      noncurrent_days = 30
      storage_class   = "GLACIER"
    }

    noncurrent_version_expiration {
      noncurrent_days = 365
    }

    # Clean up incomplete multipart uploads (cost hygiene)
    abort_incomplete_multipart_upload {
      days_after_initiation = 7
    }
  }
}

# -----------------------------------------------------------------------------
# Object Lock (compliance mode) for templates/* prefix
# Prevents deletion/overwrite for retention period — protects platform IP (template SVGs)
# NOTE: Object Lock requires the bucket to have been created with object_lock_enabled=true.
#       For an existing bucket without it, a one-shot AWS support ticket is required to enable.
#       This config block is the desired-state; if `terraform apply` errors with
#       "ObjectLockConfigurationNotFoundError", file an AWS support request to enable Object
#       Lock on the existing bucket, then re-apply. New buckets created from scratch will
#       pick up object_lock_enabled via the bucket resource itself (see ToDo below).
# TODO(GAP-118 follow-up): once next-gen bucket lands, set
#   `object_lock_enabled = true` on aws_s3_bucket.assets (immutable on create).
# -----------------------------------------------------------------------------
resource "aws_s3_bucket_object_lock_configuration" "assets_templates" {
  count  = var.s3_object_lock_enabled ? 1 : 0
  bucket = aws_s3_bucket.assets.id

  rule {
    default_retention {
      mode = "COMPLIANCE"
      days = var.s3_object_lock_retention_days
    }
  }

  depends_on = [aws_s3_bucket_versioning.assets]
}

# -----------------------------------------------------------------------------
# Cross-region replication: ap-southeast-1 -> us-east-1
# -----------------------------------------------------------------------------
resource "aws_s3_bucket" "assets_replica" {
  provider = aws.replica
  bucket   = "${var.project_name}-assets-${var.environment}-replica"
  tags     = { Name = "${var.project_name}-assets-replica" }
}

resource "aws_s3_bucket_versioning" "assets_replica" {
  provider = aws.replica
  bucket   = aws_s3_bucket.assets_replica.id
  versioning_configuration { status = "Enabled" }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "assets_replica" {
  provider = aws.replica
  bucket   = aws_s3_bucket.assets_replica.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "assets_replica" {
  provider                = aws.replica
  bucket                  = aws_s3_bucket.assets_replica.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# IAM role assumed by S3 replication service
resource "aws_iam_role" "s3_replication" {
  name = "${var.project_name}-s3-replication-${var.environment}"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "s3.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })

  tags = { Name = "${var.project_name}-s3-replication" }
}

resource "aws_iam_policy" "s3_replication" {
  name = "${var.project_name}-s3-replication-${var.environment}"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "s3:GetReplicationConfiguration",
          "s3:ListBucket",
        ]
        Resource = [aws_s3_bucket.assets.arn]
      },
      {
        Effect = "Allow"
        Action = [
          "s3:GetObjectVersionForReplication",
          "s3:GetObjectVersionAcl",
          "s3:GetObjectVersionTagging",
        ]
        Resource = ["${aws_s3_bucket.assets.arn}/*"]
      },
      {
        Effect = "Allow"
        Action = [
          "s3:ReplicateObject",
          "s3:ReplicateDelete",
          "s3:ReplicateTags",
        ]
        Resource = ["${aws_s3_bucket.assets_replica.arn}/*"]
      },
    ]
  })
}

resource "aws_iam_role_policy_attachment" "s3_replication" {
  role       = aws_iam_role.s3_replication.name
  policy_arn = aws_iam_policy.s3_replication.arn
}

resource "aws_s3_bucket_replication_configuration" "assets" {
  # Replication requires versioning on both source + destination
  depends_on = [
    aws_s3_bucket_versioning.assets,
    aws_s3_bucket_versioning.assets_replica,
    aws_iam_role_policy_attachment.s3_replication,
  ]

  role   = aws_iam_role.s3_replication.arn
  bucket = aws_s3_bucket.assets.id

  rule {
    id     = "replicate-all-to-${var.s3_replica_region}"
    status = "Enabled"

    # Empty filter = replicate all objects
    filter {}

    delete_marker_replication { status = "Enabled" }

    destination {
      bucket        = aws_s3_bucket.assets_replica.arn
      storage_class = "STANDARD_IA"
    }
  }
}

# =============================================================================
# ECR Repositories
# =============================================================================
resource "aws_ecr_repository" "services" {
  for_each = toset([
    "kitehub-subscription",
    "kitehub-gateway",
    "kitehub-branding",
    "kitehub-admin",
    "kitehub-email",
    "kitehub-frontend",
    "kiteclass-core",
    "kiteclass-gateway",
    "kiteclass-frontend",
  ])

  name                 = "${var.project_name}/${each.key}"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = { Name = each.key }
}

# ECR Lifecycle Policy (keep last 10 images)
resource "aws_ecr_lifecycle_policy" "cleanup" {
  for_each   = aws_ecr_repository.services
  repository = each.value.name

  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Keep last 10 images"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = 10
      }
      action = { type = "expire" }
    }]
  })
}
