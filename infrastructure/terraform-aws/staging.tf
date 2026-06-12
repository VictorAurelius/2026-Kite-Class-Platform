# =============================================================================
# STAGING ENVIRONMENT — Architecture B revision (Phase 1 BETA)
# =============================================================================
# Per ADR-025 + GAP-380 (Wave 38 Bucket D): staging Phase 1 = single t3.micro
# EC2 host running combined KH+KC stack via docker-compose. NO EKS Phase 1
# (per Architecture B). Phase 2 EKS migration tracked GAP-415 (Wave 37).
#
# Provisioning is gated by `var.enable_staging` so production-only deploys
# stay unaffected when staging is torn down for cost savings.
#
# Cost projection (always-on staging):
#   - 1× t3.micro EC2 (combined KH+KC):           Free tier 12mo, ~$8.5/mo after
#   - 1× RDS db.t3.micro (staging Postgres):      ~$13/mo (free tier exhausted by prod)
#   - 1× S3 bucket (assets):                      ~$0.5/mo
#   - 1× Route53 record (if zone managed):        included in prod hosted zone
#   - Data transfer (Cloudflare proxy front):     minimal
#   - TOTAL Yr1 estimate:                         ~$25-35/mo (within $50/mo gap target)
#
# Tear-down for cost savings:
#   terraform apply -var="enable_staging=false"   # destroys staging-* resources
#
# Phase 2 EKS migration trigger gate (per Wave 37 GAP-415):
#   - 5+ beta tenants live on prod
#   - Phase 1 BETA Quality audit /100 ≥80
#   - Multi-AZ requirement surfaces (HA SLO ≥99.5%)
#   - When trigger fires → migrate staging to EKS first as canary, then prod.
#
# Verification: `terraform fmt -check` + `terraform validate` (no apply by agent
# per GAP-381 Phase 2 BANNED — human runs terraform apply post-PR-merge).
# =============================================================================

# --- Staging public subnet (dedicated, isolates blast radius from prod) ---
resource "aws_subnet" "staging_public" {
  count                   = var.enable_staging ? 1 : 0
  vpc_id                  = aws_vpc.main.id
  cidr_block              = cidrsubnet(var.vpc_cidr, 8, 20) # 10.0.20.0/24 (distinct from prod public/private ranges)
  availability_zone       = data.aws_availability_zones.available.names[0]
  map_public_ip_on_launch = true

  tags = { Name = "${var.project_name}-staging-public" }
}

resource "aws_subnet" "staging_private" {
  count             = var.enable_staging ? 2 : 0 # RDS subnet group requires ≥2 AZ
  vpc_id            = aws_vpc.main.id
  cidr_block        = cidrsubnet(var.vpc_cidr, 8, 30 + count.index) # 10.0.30.0/24, 10.0.31.0/24
  availability_zone = data.aws_availability_zones.available.names[count.index]

  tags = { Name = "${var.project_name}-staging-private-${count.index}" }
}

resource "aws_route_table_association" "staging_public" {
  count          = var.enable_staging ? 1 : 0
  subnet_id      = aws_subnet.staging_public[0].id
  route_table_id = aws_route_table.public.id # Reuse prod public route table (same IGW)
}

resource "aws_route_table_association" "staging_private" {
  count          = var.enable_staging ? 2 : 0
  subnet_id      = aws_subnet.staging_private[count.index].id
  route_table_id = aws_route_table.private.id # Reuse prod private route table
}

# --- Staging Security Group (separate from prod ec2_app) ---
resource "aws_security_group" "staging_ec2" {
  count       = var.enable_staging ? 1 : 0
  name_prefix = "${var.project_name}-staging-ec2-"
  vpc_id      = aws_vpc.main.id
  description = "Staging EC2 ingress - Cloudflare proxy on 80/443; SSM for shell access"

  # Public-but-no-real-data per GAP-380 brainstorm — Cloudflare proxy fronts everything.
  # Real auth + staging banner block accidental access.
  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "HTTP from Cloudflare proxy (staging banner + auth-gated)"
  }

  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "HTTPS from Cloudflare proxy"
  }

  # App ports for direct testing via SSM port-forward (no public exposure)
  ingress {
    from_port   = 8080
    to_port     = 8087
    protocol    = "tcp"
    self        = true
    description = "Internal app ports (SSM port-forward only)"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    description = "All egress (RDS, ECR, Secrets Manager, S3)"
  }

  tags = { Name = "${var.project_name}-staging-ec2-sg" }
}

# --- Staging RDS Security Group (5432 from staging EC2 only) ---
resource "aws_security_group" "staging_rds" {
  count       = var.enable_staging ? 1 : 0
  name_prefix = "${var.project_name}-staging-rds-"
  vpc_id      = aws_vpc.main.id
  description = "Staging RDS - 5432 from staging EC2 SG only"

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.staging_ec2[0].id]
    description     = "PostgreSQL from staging EC2"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${var.project_name}-staging-rds-sg" }
}

# --- Staging EC2 instance (combined KH + KC docker-compose) ---
resource "aws_instance" "staging" {
  count                       = var.enable_staging ? 1 : 0
  ami                         = data.aws_ami.al2023.id
  instance_type               = var.staging_instance_type
  subnet_id                   = aws_subnet.staging_public[0].id
  vpc_security_group_ids      = [aws_security_group.staging_ec2[0].id]
  iam_instance_profile        = aws_iam_instance_profile.ec2_app.name # Reuse prod app profile (same ECR/Secrets/SSM perms)
  associate_public_ip_address = true
  key_name                    = var.ec2_key_pair_name # Optional; SSM is primary access path

  user_data                   = local.ec2_user_data
  user_data_replace_on_change = false

  root_block_device {
    volume_type           = "gp3"
    volume_size           = 30
    encrypted             = true
    delete_on_termination = true
  }

  metadata_options {
    http_tokens                 = "required" # IMDSv2 only
    http_endpoint               = "enabled"
    http_put_response_hop_limit = 2
  }

  tags = {
    Name        = "${var.project_name}-staging"
    Role        = "staging-combined-stack"
    Environment = "staging"
  }
}

# --- Staging RDS subnet group ---
resource "aws_db_subnet_group" "staging" {
  count      = var.enable_staging ? 1 : 0
  name       = "${var.project_name}-staging-db-subnet"
  subnet_ids = aws_subnet.staging_private[*].id
  tags       = { Name = "${var.project_name}-staging-db-subnet" }
}

# --- Staging RDS password (separate from prod — never share secrets) ---
resource "random_password" "staging_rds" {
  count   = var.enable_staging ? 1 : 0
  length  = 32
  special = false
}

# --- Staging RDS PostgreSQL ---
resource "aws_db_instance" "staging" {
  count          = var.enable_staging ? 1 : 0
  identifier     = "${var.project_name}-staging-postgres"
  engine         = "postgres"
  engine_version = var.rds_engine_version
  instance_class = var.staging_rds_instance_class

  allocated_storage     = 20 # Free tier baseline; staging never grows large
  max_allocated_storage = 40
  storage_type          = "gp3"
  storage_encrypted     = true

  db_name  = "${var.rds_db_name}_staging"
  username = "kitehub_staging"
  password = random_password.staging_rds[0].result

  multi_az               = false # Staging never multi-AZ (cost)
  db_subnet_group_name   = aws_db_subnet_group.staging[0].name
  vpc_security_group_ids = [aws_security_group.staging_rds[0].id]
  publicly_accessible    = false

  backup_retention_period = 1 # Minimal backups for staging
  backup_window           = "16:00-17:00"
  maintenance_window      = "sun:17:00-sun:18:00"

  performance_insights_enabled = false
  monitoring_interval          = 0

  skip_final_snapshot = true # Staging is disposable
  deletion_protection = false

  tags = {
    Name        = "${var.project_name}-staging-postgres"
    Environment = "staging"
  }
}

# --- Staging S3 assets bucket ---
resource "aws_s3_bucket" "staging_assets" {
  count  = var.enable_staging ? 1 : 0
  bucket = "${var.project_name}-assets-staging-${data.aws_caller_identity.current.account_id}"
  tags = {
    Name        = "${var.project_name}-staging-assets"
    Environment = "staging"
  }
}

resource "aws_s3_bucket_versioning" "staging_assets" {
  count  = var.enable_staging ? 1 : 0
  bucket = aws_s3_bucket.staging_assets[0].id
  versioning_configuration { status = "Enabled" }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "staging_assets" {
  count  = var.enable_staging ? 1 : 0
  bucket = aws_s3_bucket.staging_assets[0].id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "staging_assets" {
  count                   = var.enable_staging ? 1 : 0
  bucket                  = aws_s3_bucket.staging_assets[0].id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_lifecycle_configuration" "staging_assets" {
  count      = var.enable_staging ? 1 : 0
  bucket     = aws_s3_bucket.staging_assets[0].id
  depends_on = [aws_s3_bucket_versioning.staging_assets]

  rule {
    id     = "staging-cost-hygiene"
    status = "Enabled"
    filter {}

    abort_incomplete_multipart_upload {
      days_after_initiation = 3
    }

    noncurrent_version_expiration {
      noncurrent_days = 14 # Aggressive cleanup — staging artifacts ephemeral
    }
  }
}

# --- Route53 records (only when manage_route53_zone=true) ---
resource "aws_route53_record" "staging_kitehub" {
  count   = var.enable_staging && var.manage_route53_zone ? 1 : 0
  zone_id = aws_route53_zone.primary[0].zone_id
  name    = "staging.kitehub.${var.domain_name}"
  type    = "A"
  ttl     = 300
  records = [aws_instance.staging[0].public_ip]
}

resource "aws_route53_record" "staging_kiteclass" {
  count   = var.enable_staging && var.manage_route53_zone ? 1 : 0
  zone_id = aws_route53_zone.primary[0].zone_id
  name    = "staging.kiteclass.${var.domain_name}"
  type    = "A"
  ttl     = 300
  records = [aws_instance.staging[0].public_ip]
}

# =============================================================================
# Outputs (gated — empty when staging disabled)
# =============================================================================

output "staging_instance_id" {
  description = "Staging EC2 instance ID (use for SSM session). Empty if enable_staging=false."
  value       = var.enable_staging ? aws_instance.staging[0].id : ""
}

output "staging_public_ip" {
  description = "Staging EC2 public IP (Cloudflare CNAME target)."
  value       = var.enable_staging ? aws_instance.staging[0].public_ip : ""
}

output "staging_rds_endpoint" {
  description = "Staging RDS endpoint (host:port)."
  value       = var.enable_staging ? aws_db_instance.staging[0].endpoint : ""
}

output "staging_s3_bucket" {
  description = "Staging S3 assets bucket name."
  value       = var.enable_staging ? aws_s3_bucket.staging_assets[0].id : ""
}

output "staging_ssm_command" {
  description = "Command to open SSM session to staging EC2."
  value       = var.enable_staging ? "aws ssm start-session --target ${aws_instance.staging[0].id} --region ${var.aws_region}" : ""
}

locals {
  staging_next_steps_template = var.enable_staging ? format(
    "Staging environment provisioned. Next steps:\n  1. SSM into staging EC2:\n     aws ssm start-session --target %s --region %s\n  2. Configure docker-compose.staging.yml + ECR pull:\n     /etc/ecr-login.sh && docker compose -f docker-compose.staging.yml up -d\n  3. Apply Flyway migrations:\n     psql %s (use kitehub_staging user)\n  4. Seed synthetic fixtures:\n     bash scripts/seed-staging-fixtures.sh\n  5. Update Cloudflare DNS (proxied):\n     staging.kitehub.vn  A  %s\n     staging.kitehub.me A  %s\n  6. Run smoke test:\n     ./scripts/smoke-test.sh https://staging.kitehub.vn https://staging.kitehub.me",
    try(aws_instance.staging[0].id, ""),
    var.aws_region,
    try(aws_db_instance.staging[0].endpoint, ""),
    try(aws_instance.staging[0].public_ip, ""),
    try(aws_instance.staging[0].public_ip, "")
  ) : "Staging disabled — set enable_staging=true to provision."
}

output "staging_next_steps" {
  description = "Post-apply staging activation steps."
  value       = local.staging_next_steps_template
}
