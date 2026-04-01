# AWS Cloud Deploy Reference

## Architecture Patterns — chọn theo workload

| Pattern | Phù hợp khi | Services chính |
|---------|------------|---------------|
| **Simple 3-tier** | Web app truyền thống, team nhỏ | ALB + EC2 ASG + RDS |
| **Serverless** | API không đều, event-driven, ít ops | API Gateway + Lambda + DynamoDB |
| **Container (ECS)** | Microservices, team đã quen Docker | ECS Fargate + ALB + RDS |
| **Kubernetes (EKS)** | Scale lớn, nhiều services, DevOps mature | EKS + ALB Ingress + RDS/ElastiCache |
| **Data/Analytics** | Batch processing, ML, BI | EMR / Glue + S3 + Redshift |

---

## Terraform Modules AWS — Chuẩn

### VPC Foundation (bắt buộc cho mọi dự án)

```hcl
# modules/networking/main.tf
module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "~> 5.0"

  name = "${var.project}-${var.env}-vpc"
  cidr = var.vpc_cidr  # e.g. "10.0.0.0/16"

  azs             = data.aws_availability_zones.available.names
  private_subnets = [for k, v in local.azs : cidrsubnet(var.vpc_cidr, 4, k)]
  public_subnets  = [for k, v in local.azs : cidrsubnet(var.vpc_cidr, 4, k + 4)]
  database_subnets = [for k, v in local.azs : cidrsubnet(var.vpc_cidr, 4, k + 8)]

  enable_nat_gateway   = true
  single_nat_gateway   = var.env != "prod"  # prod dùng NAT/AZ
  enable_dns_hostnames = true
  enable_dns_support   = true

  # VPC Flow Logs
  enable_flow_log                      = true
  create_flow_log_cloudwatch_log_group = true
  create_flow_log_cloudwatch_iam_role  = true
  flow_log_max_aggregation_interval    = 60

  tags = local.common_tags
}
```

### Remote State Backend (setup 1 lần đầu)

```hcl
# bootstrap/main.tf — chạy trước mọi thứ khác
resource "aws_s3_bucket" "terraform_state" {
  bucket = "${var.project}-terraform-state-${data.aws_caller_identity.current.account_id}"

  lifecycle {
    prevent_destroy = true
  }

  tags = local.common_tags
}

resource "aws_s3_bucket_versioning" "state" {
  bucket = aws_s3_bucket.terraform_state.id
  versioning_configuration { status = "Enabled" }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "state" {
  bucket = aws_s3_bucket.terraform_state.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "aws:kms"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "state" {
  bucket                  = aws_s3_bucket.terraform_state.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_dynamodb_table" "terraform_locks" {
  name         = "${var.project}-terraform-locks"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "LockID"
  attribute {
    name = "LockID"
    type = "S"
  }
  tags = local.common_tags
}
```

### RDS Production-Ready

```hcl
# modules/database/main.tf
resource "aws_db_instance" "main" {
  identifier = "${var.project}-${var.env}-db"

  engine         = "postgres"
  engine_version = "15.4"
  instance_class = var.env == "prod" ? "db.r6g.xlarge" : "db.t3.medium"

  allocated_storage     = var.env == "prod" ? 100 : 20
  max_allocated_storage = var.env == "prod" ? 1000 : 100
  storage_type          = "gp3"
  storage_encrypted     = true
  kms_key_id            = aws_kms_key.rds.arn

  db_name  = var.database_name
  username = var.database_username
  password = random_password.db.result  # KHÔNG hardcode

  multi_az               = var.env == "prod"
  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]

  backup_retention_period = var.env == "prod" ? 30 : 7
  backup_window           = "02:00-03:00"
  maintenance_window      = "sun:04:00-sun:05:00"

  deletion_protection       = var.env == "prod"
  skip_final_snapshot       = var.env != "prod"
  final_snapshot_identifier = var.env == "prod" ? "${var.project}-final-snapshot" : null

  performance_insights_enabled = true
  monitoring_interval          = 60
  enabled_cloudwatch_logs_exports = ["postgresql", "upgrade"]

  tags = local.common_tags
}

resource "random_password" "db" {
  length  = 32
  special = false
}

resource "aws_secretsmanager_secret" "db_password" {
  name = "/${var.project}/${var.env}/db-password"
  tags = local.common_tags
}

resource "aws_secretsmanager_secret_version" "db_password" {
  secret_id     = aws_secretsmanager_secret.db_password.id
  secret_string = random_password.db.result
}
```

---

## Deploy Phases — AWS

### Phase 1: Foundation (Tuần 1–2)

**Mục tiêu:** Hạ tầng nền tảng an toàn

```
Day 1–2: Bootstrap
  ✓ Tạo AWS Organizations structure (nếu multi-account)
  ✓ Setup S3 + DynamoDB cho Terraform state
  ✓ Configure AWS SSO / IAM Identity Center
  ✓ Enable AWS CloudTrail, Config, Security Hub

Day 3–5: Networking
  ✓ Deploy VPC module (prod + staging)
  ✓ Setup Transit Gateway (nếu multi-VPC)
  ✓ Configure Route53 hosted zones
  ✓ ACM certificates

Day 6–10: Security baseline
  ✓ IAM roles/policies theo least privilege
  ✓ KMS keys cho các services
  ✓ GuardDuty, Config rules
  ✓ WAF rules (nếu có public endpoint)
```

### Phase 2: Application Layer (Tuần 3–4)

```
Compute:
  ✓ ECS Fargate task definitions / EKS node groups
  ✓ ALB + target groups + listener rules
  ✓ Auto Scaling policies

Database:
  ✓ RDS (Multi-AZ cho prod)
  ✓ ElastiCache (Redis) nếu cần caching
  ✓ Secrets Manager cho credentials

Storage:
  ✓ S3 buckets với proper policies
  ✓ EFS nếu cần shared filesystem

CI/CD:
  ✓ CodePipeline / GitHub Actions
  ✓ ECR cho container images
  ✓ Terraform Cloud / Atlantis cho IaC workflow
```

### Phase 3: Production Hardening (Tuần 5–6)

```
Observability:
  ✓ CloudWatch dashboards + alarms
  ✓ X-Ray tracing
  ✓ Log aggregation (CloudWatch Logs Insights)
  ✓ Grafana / Datadog integration (optional)

Reliability:
  ✓ Route53 health checks + failover
  ✓ RDS read replicas
  ✓ Backup plans (AWS Backup)
  ✓ Disaster Recovery runbook

Cost:
  ✓ Cost Explorer budgets & alerts
  ✓ Trusted Advisor review
  ✓ Savings Plans / Reserved Instances plan
```

---

## Cost Estimation Template

### Scenario: Web App (1000 MAU)

| Service | Dev/month | Staging/month | Prod/month |
|---------|-----------|---------------|------------|
| EC2/ECS (t3.medium × 2) | $30 | $60 | $150 (prod: m6i.large × 3) |
| RDS PostgreSQL | $25 | $50 | $400 (Multi-AZ, r6g.large) |
| ALB | $20 | $20 | $30 |
| S3 (100GB) | $5 | $5 | $10 |
| NAT Gateway | $35 | $35 | $70 (2 AZ) |
| CloudWatch | $5 | $10 | $30 |
| **Total** | **~$120** | **~$180** | **~$690** |

### Cost Optimization Rules
- Dev: dùng t3/t4g instances, single-AZ, không Nat Gateway (dùng VPC Endpoints)
- Staging: scale down ngoài giờ với Terraform schedules
- Prod: Compute Savings Plans 1yr = 30–40% tiết kiệm

---

## AWS Accounts Strategy

### Multi-Account (khuyến nghị cho prod)

```
Root (management)
├── Security Account (GuardDuty, Security Hub, Audit logs)
├── Shared Services (ECR, Transit Gateway, DNS)
├── Dev Account
├── Staging Account
└── Prod Account
```

```hcl
# Terraform switch account bằng assume_role
provider "aws" {
  alias  = "prod"
  region = "ap-southeast-1"
  assume_role {
    role_arn = "arn:aws:iam::PROD_ACCOUNT_ID:role/TerraformDeployRole"
  }
}
```

---

## Pre-Launch Checklist AWS

### Security
- [ ] Không có IAM users với long-term credentials (dùng roles)
- [ ] MFA bật cho root account
- [ ] S3 Block Public Access bật toàn account
- [ ] Security Hub findings < 10 HIGH, 0 CRITICAL
- [ ] Pen test hoặc AWS Inspector scan

### Reliability
- [ ] RDS Multi-AZ confirmed
- [ ] ASG min = 2 instances (prod)
- [ ] ALB health checks passing
- [ ] Route53 health checks configured
- [ ] Backup restore tested

### Operations
- [ ] CloudWatch alarms cho CPU > 80%, Error rate > 1%
- [ ] On-call runbook documented
- [ ] Terraform state backup verified
- [ ] Rollback plan documented
- [ ] Cost budget alert at 80% of monthly budget

### Compliance
- [ ] CloudTrail enabled và log to S3
- [ ] Config rules passing
- [ ] Data encryption at rest và in transit
- [ ] GDPR/data retention policy configured (nếu áp dụng)
