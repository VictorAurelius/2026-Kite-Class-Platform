# =============================================================================
# Variables — Phase 1 BETA (Architecture B)
# =============================================================================

variable "aws_region" {
  description = "AWS region (Singapore per ADR-025)"
  type        = string
  default     = "ap-southeast-1"

  validation {
    condition     = var.aws_region == "ap-southeast-1"
    error_message = "Phase 1 BETA pin region to ap-southeast-1 per ADR-025. Update validation if migrating region (Phase 3)."
  }
}

variable "environment" {
  description = "Environment name (production, staging, beta)"
  type        = string
  default     = "production"
}

variable "project_name" {
  description = "Project name prefix for all resources"
  type        = string
  default     = "kitehub"
}

variable "domain_name" {
  description = "Primary platform domain (used for ALB cert hint + Route53 if managed here). Per GAP-458 (Path C Free GitHub Student Pack 2026-05-09) + GAP-692 Phase 1 STALE mismatch fix (2026-05-21): default flipped from kiteclass.com to kitehub.me to match real apex domain. Multi-env override via terraform.tfvars OR -var flag."
  type        = string
  default     = "kitehub.me"
}

variable "aws_account_id" {
  description = "AWS account ID per environment (Phase 1 BETA Singapore Free Tier per ADR-025). No default - MUST be explicit via terraform.tfvars OR -var flag to prevent cross-env contamination. Per GAP-692 Phase 1 env-reference.yaml integration (Wave 102.8 Bucket B 2026-05-21)."
  type        = string
}

variable "secrets_prefix" {
  description = "AWS Secrets Manager prefix per environment (default kitehub/production for backward compat with fetch-secrets.sh + production-env-config-registry.md scope). Override for test/dev envs via terraform.tfvars. Per GAP-692 Phase 1 env-reference.yaml integration (Wave 102.8 Bucket B 2026-05-21)."
  type        = string
  default     = "kitehub/production"
}

# --- VPC ---
variable "vpc_cidr" {
  description = "VPC CIDR block"
  type        = string
  default     = "10.0.0.0/16"
}

variable "enable_nat_gateway" {
  description = "Provision NAT Gateway for private subnets. Phase 1 BETA = false (cost savings ~$32/mo); private subnets reach internet via VPC endpoints OR EC2 in public subnets only."
  type        = bool
  default     = false
}

# --- EC2 (Architecture B: 2 instances replace EKS) ---
variable "kh_backend_instance_type" {
  description = "EC2 instance type for KiteHub backend cluster (5 KH services + redis + rabbitmq + gateway). Phase 1 BETA: t3.large 8GB (~$60/mo) — UPSIZED 2026-05-13 per GAP-502 RC2 OOM evidence (11 container die/1h on t3.medium; GAP-447 sizing assumption invalidated). Compose budget ~3.0GB peak post Wave 70 Bucket C JVM tune (kitehub-* mem_limit total 2944 MiB) + 1.5GB headroom for GC/spike. Sizing history: t3.micro (OOM cascade #1031) → m7i-flex.large (over-correction PR #1031) → t3.medium (PR #1032 right-size GAP-447) → t3.large (Wave 70 GAP-502 escalation). Post-release downsize evaluation tracked separately (criteria: ≥4 weeks stability + avg MemoryUtilization <60% + zero OOM events → consider t3.large → t3.medium)."
  type        = string
  default     = "t3.large"
}

variable "kc_app_instance_type" {
  description = "EC2 instance type for KiteClass app stack (kiteclass-core + gateway + redis + rabbitmq; frontend on Vercel post-2026-05-07 pivot). Phase 1 BETA: t3.medium 4GB (~$30/mo) — right-sized 2026-05-08 per GAP-447. Compose budget ~2.5GB peak (docker-compose.kc.yml §13-18) → t3.small 2GB insufficient (under 2.5GB peak); t3.medium gives 1.5GB headroom. GAP-411 stale t3.small sizing was based on pre-Vercel-pivot plan (KC frontend ON kc-app)."
  type        = string
  default     = "t3.medium"
}

variable "ec2_key_pair_name" {
  description = "EC2 key pair name for SSH access (must exist in AWS account before apply). Set to null to skip SSH (use SSM instead)."
  type        = string
  default     = null
}

# --- RDS ---
variable "rds_instance_class" {
  description = "RDS instance class. Phase 1 default db.t3.micro (free tier 12mo)."
  type        = string
  default     = "db.t3.micro"
}

variable "rds_allocated_storage" {
  description = "RDS storage in GB (free tier covers 20GB)"
  type        = number
  default     = 20
}

variable "rds_multi_az" {
  description = "Multi-AZ for RDS. Phase 1 BETA = false (single AZ cost savings)."
  type        = bool
  default     = false
}

variable "rds_db_name" {
  description = "Initial database name"
  type        = string
  default     = "kitehub"
}

variable "rds_engine_version" {
  description = "PostgreSQL engine version"
  type        = string
  default     = "15"
}

# Wave aws-restore-1 (2026-05-26): support restore from snapshot post-GAP-612
# Day 8 UNBLOCK. Default empty = fresh create; non-empty = restore from snapshot.
# When set, aws_db_instance.main.snapshot_identifier flips RDS into restore mode;
# RDS preserves master password from snapshot (overrides random_password.rds.result
# — handled via lifecycle.ignore_changes = [password] in rds.tf).
variable "rds_restore_from_snapshot" {
  description = "Snapshot identifier to restore RDS from. Empty = create fresh empty DB."
  type        = string
  default     = ""
}

# --- ALB ---
variable "enable_alb" {
  description = "Provision Application Load Balancer in front of EC2. Default true; set false if Cloudflare proxy connects directly to EC2 public IP."
  type        = bool
  default     = true
}

variable "alb_acm_certificate_arn" {
  description = "ACM certificate ARN for ALB HTTPS listener. Set after cert validation in ACM console; null to skip HTTPS listener (HTTP-only for initial bring-up)."
  type        = string
  default     = null
}

# --- Route53 (optional — Cloudflare DNS is primary per ADR-018) ---
variable "manage_route53_zone" {
  description = "Create Route53 hosted zone for domain_name. Default false (Cloudflare DNS is primary)."
  type        = bool
  default     = false
}

# --- Staging (per GAP-380 / Wave 38 Bucket D — Architecture B: EC2 + docker-compose) ---
variable "enable_staging" {
  description = "Provision staging environment (single t3.micro EC2 + db.t3.micro RDS + S3 + Route53). Default false; set true for always-on staging. Tear down via enable_staging=false to save cost."
  type        = bool
  default     = false
}

variable "staging_instance_type" {
  description = "EC2 instance type for staging combined KH+KC docker-compose stack. Phase 1 default t3.micro (free tier 12mo)."
  type        = string
  default     = "t3.micro"
}

variable "staging_rds_instance_class" {
  description = "RDS instance class for staging. Phase 1 default db.t3.micro (~$13/mo since prod consumes free tier)."
  type        = string
  default     = "db.t3.micro"
}

# --- Cost-saving scheduler (per GAP-446 / Wave 43 Bucket A) ---
variable "enable_cost_scheduling" {
  description = "Provision EventBridge Scheduler to stop/start EC2 + RDS off-hours (Asia/Ho_Chi_Minh). Disabled 2026-05-13 — manual stop/start via scripts/aws/{stop,start}-stack.sh preferred during active dev; scheduler auto-restart conflicted with manual stops. Re-enable when promoting to GA or when consistent off-hours pattern resumes."
  type        = bool
  default     = false
}

# --- GitHub OIDC (for terraform-plan CI workflow per GAP-397) ---
variable "github_repo" {
  description = "GitHub repo in 'owner/name' format for OIDC trust policy."
  type        = string
  default     = "VictorAurelius/2026-Kite-Class-Platform"
}
