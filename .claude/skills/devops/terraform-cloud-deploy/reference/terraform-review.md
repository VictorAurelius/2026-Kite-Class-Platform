# Terraform Review Reference

## Scoring Rubric (100 điểm tổng)

### 1. Security (20 điểm)

| Check | Điểm | Tiêu chí |
|-------|------|----------|
| Không hardcode credentials | 5 | Không có `access_key`, `secret_key`, password trong .tf |
| IAM/Role least privilege | 4 | Không dùng `*` trong policy actions vô tội vạ |
| Encryption at rest | 4 | S3/RDS/EBS bật encryption |
| Network segmentation | 4 | VPC/subnet phân tách public/private đúng |
| Security groups strict | 3 | Không mở `0.0.0.0/0` cho SSH/RDP |

**Scoring:**
- 18–20: Excellent — production-ready security
- 14–17: Good — minor issues
- 10–13: Acceptable — cần fix trước deploy
- <10: Poor — nguy hiểm, không deploy

---

### 2. Code Structure (20 điểm)

| Check | Điểm | Tiêu chí |
|-------|------|----------|
| Module hóa hợp lý | 5 | Resources nhóm thành modules có ý nghĩa |
| File organization | 4 | Tách main.tf / variables.tf / outputs.tf / providers.tf |
| Naming convention | 4 | Tên resource nhất quán (snake_case, có prefix env) |
| DRY — không lặp code | 4 | Dùng `for_each`, `count`, modules thay vì copy-paste |
| Provider version pinned | 3 | `required_providers` có version constraint |

---

### 3. Best Practices (20 điểm)

| Check | Điểm | Tiêu chí |
|-------|------|----------|
| Remote state backend | 6 | S3+DynamoDB (AWS) hoặc OCI Object Storage — KHÔNG dùng local |
| State locking | 4 | DynamoDB (AWS) / OCI locking enabled |
| `terraform.required_version` có | 3 | Version constraint rõ ràng |
| Tags/Labels đầy đủ | 4 | env, project, owner, cost-center tags |
| Outputs có description | 3 | Output values có mô tả rõ ràng |

---

### 4. Cost Awareness (20 điểm)

| Check | Điểm | Tiêu chí |
|-------|------|----------|
| Instance sizing hợp lý | 6 | Không over-provision dev/staging |
| Auto-scaling configured | 5 | ASG/EKS node groups có min/max |
| Lifecycle rules cho S3/storage | 5 | Transition to cheaper tiers |
| Reserved/Spot instances | 4 | Production dùng Reserved, batch dùng Spot |

---

### 5. Reliability (20 điểm)

| Check | Điểm | Tiêu chí |
|-------|------|----------|
| Multi-AZ / Multi-AD | 6 | RDS, ElastiCache, OCI DB có multi-AZ |
| Backup configured | 5 | Automated backups bật, retention ≥7 ngày |
| Health checks | 5 | ALB/NLB target group health check đúng |
| `prevent_destroy` cho critical resources | 4 | DB, S3 buckets quan trọng có lifecycle protect |

---

## Common Issues — Quick Reference

### 🔴 Critical (fix trước khi deploy)

```hcl
# BAD — hardcoded credentials
provider "aws" {
  access_key = "AKIAIOSFODNN7EXAMPLE"
  secret_key = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"
}
# FIX — dùng environment variables hoặc IAM role
provider "aws" {
  region = var.aws_region
  # credentials từ env AWS_ACCESS_KEY_ID / instance profile
}

# BAD — port 22 mở toàn thế giới
resource "aws_security_group_rule" "ssh" {
  cidr_blocks = ["0.0.0.0/0"]
  from_port   = 22
  to_port     = 22
  protocol    = "tcp"
}
# FIX — chỉ cho phép IP cụ thể hoặc dùng SSM Session Manager
resource "aws_security_group_rule" "ssh" {
  cidr_blocks = [var.bastion_cidr]
  from_port   = 22
  to_port     = 22
  protocol    = "tcp"
}

# BAD — local state
terraform {
  backend "local" {}
}
# FIX — S3 remote state
terraform {
  backend "s3" {
    bucket         = "my-tf-state"
    key            = "prod/terraform.tfstate"
    region         = "ap-southeast-1"
    dynamodb_table = "terraform-lock"
    encrypt        = true
  }
}
```

### 🟡 Warning (cải thiện trong sprint tới)

```hcl
# BAD — không có provider version
terraform {
  required_providers {
    aws = { source = "hashicorp/aws" }
  }
}
# FIX
terraform {
  required_version = ">= 1.5.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

# BAD — không có tags
resource "aws_instance" "web" {
  ami           = "ami-0c55b159cbfafe1f0"
  instance_type = "t3.medium"
}
# FIX
resource "aws_instance" "web" {
  ami           = var.ami_id
  instance_type = var.instance_type

  tags = merge(local.common_tags, {
    Name = "${var.env}-web-server"
    Role = "webserver"
  })
}

locals {
  common_tags = {
    Environment = var.env
    Project     = var.project_name
    ManagedBy   = "terraform"
    Owner       = var.team_email
    CostCenter  = var.cost_center
  }
}

# BAD — không encryption RDS
resource "aws_db_instance" "main" {
  storage_encrypted = false  # hoặc thiếu dòng này
}
# FIX
resource "aws_db_instance" "main" {
  storage_encrypted = true
  kms_key_id        = aws_kms_key.rds.arn
}
```

### 🟢 Recommendations (nice-to-have)

```hcl
# Thêm prevent_destroy cho production resources
resource "aws_s3_bucket" "data" {
  bucket = "${var.env}-data-${random_id.suffix.hex}"

  lifecycle {
    prevent_destroy = true
  }
}

# Dùng for_each thay vì count cho stability
# BAD
resource "aws_iam_user" "devs" {
  count = length(var.dev_names)
  name  = var.dev_names[count.index]
}
# FIX — for_each không bị index shift khi xóa 1 phần tử
resource "aws_iam_user" "devs" {
  for_each = toset(var.dev_names)
  name     = each.value
}
```

---

## Module Structure Chuẩn

```
project/
├── environments/
│   ├── dev/
│   │   ├── main.tf          # gọi modules
│   │   ├── variables.tf
│   │   ├── outputs.tf
│   │   └── terraform.tfvars
│   ├── staging/
│   └── prod/
├── modules/
│   ├── networking/          # VPC, subnets, route tables, NAT
│   │   ├── main.tf
│   │   ├── variables.tf
│   │   └── outputs.tf
│   ├── compute/             # EC2, ASG, Launch Template
│   ├── database/            # RDS, ElastiCache
│   ├── security/            # IAM, KMS, Security Groups
│   └── monitoring/          # CloudWatch, alerting
├── .terraform-version       # tfenv version pin
├── .tflint.hcl
└── README.md
```

---

## Security Checklist nhanh

- [ ] Không có credentials/secrets trong `.tf` files
- [ ] `.tfvars` files trong `.gitignore`
- [ ] Remote state được encrypt
- [ ] Security groups: không có `0.0.0.0/0` cho port 22, 3389, DB ports
- [ ] S3 buckets: `block_public_acls = true`, versioning bật
- [ ] RDS: `storage_encrypted = true`, `deletion_protection = true`
- [ ] IAM: không dùng `Action = "*"` trừ khi thực sự cần
- [ ] KMS key cho encrypted resources
- [ ] VPC Flow Logs bật
- [ ] CloudTrail bật ở management account

---

## Terraform Commands Cheat Sheet

```bash
# Validate syntax
terraform validate

# Format code
terraform fmt -recursive

# Plan với output file
terraform plan -out=tfplan -var-file=prod.tfvars

# Apply từ plan file
terraform apply tfplan

# Xem state
terraform state list
terraform state show aws_instance.web

# Import existing resource
terraform import aws_instance.web i-1234567890abcdef0

# Destroy specific resource
terraform destroy -target=aws_instance.test

# Workspace management
terraform workspace new staging
terraform workspace select prod
```
