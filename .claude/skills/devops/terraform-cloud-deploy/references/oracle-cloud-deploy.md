# Oracle Cloud (OCI) Deploy Reference

## OCI vs AWS Mapping — Quick Reference

| Khái niệm | AWS | OCI tương đương |
|-----------|-----|----------------|
| Region / AZ | Region / AZ | Region / Availability Domain (AD) |
| VPC | VPC | Virtual Cloud Network (VCN) |
| EC2 | EC2 | OCI Compute (Shapes) |
| RDS | RDS | Autonomous Database / Base DB System |
| S3 | S3 | Object Storage |
| EKS | EKS | OCI Kubernetes Engine (OKE) |
| IAM | IAM | IAM (Compartments, Groups, Policies) |
| CloudWatch | CloudWatch | OCI Monitoring + Logging |
| Lambda | Lambda | OCI Functions |
| Route53 | Route53 | OCI DNS |
| ALB/NLB | ALB/NLB | Load Balancer / Network Load Balancer |

---

## OCI Đặc thù (phải hiểu trước khi làm Terraform)

### 1. Compartments — Quan trọng nhất

OCI dùng Compartments để phân chia tài nguyên (giống AWS Organizations nhưng trong 1 tenancy):

```hcl
# Tạo compartment structure
resource "oci_identity_compartment" "project" {
  compartment_id = var.tenancy_ocid  # root compartment
  name           = var.project_name
  description    = "Compartment cho project ${var.project_name}"

  freeform_tags = local.common_tags
}

resource "oci_identity_compartment" "networking" {
  compartment_id = oci_identity_compartment.project.id
  name           = "networking"
  description    = "VCN, subnets, gateways"
}

resource "oci_identity_compartment" "compute" {
  compartment_id = oci_identity_compartment.project.id
  name           = "compute"
  description    = "VM instances, OKE"
}

resource "oci_identity_compartment" "database" {
  compartment_id = oci_identity_compartment.project.id
  name           = "database"
  description    = "Autonomous DB, Base DB"
}
```

### 2. OCI Provider Setup

```hcl
# providers.tf
terraform {
  required_version = ">= 1.5.0"
  required_providers {
    oci = {
      source  = "oracle/oci"
      version = ">= 5.0.0"
    }
  }
  backend "s3" {
    # OCI Object Storage compatible với S3 API
    endpoint                    = "https://<namespace>.compat.objectstorage.<region>.oraclecloud.com"
    bucket                      = "terraform-state"
    key                         = "${var.env}/terraform.tfstate"
    region                      = "us-ashburn-1"
    skip_region_validation      = true
    skip_credentials_validation = true
    skip_metadata_api_check     = true
    force_path_style            = true
    access_key = var.oci_access_key  # customer secret key
    secret_key = var.oci_secret_key
  }
}

provider "oci" {
  region       = var.oci_region
  tenancy_ocid = var.tenancy_ocid
  user_ocid    = var.user_ocid        # dùng cho local dev
  fingerprint  = var.fingerprint      # dùng cho local dev
  private_key  = var.private_key_pem  # HOẶC
  # Trong OCI Cloud Shell hoặc instance: dùng instance principal
  # auth = "InstancePrincipal"
}
```

### 3. OCI Shapes — Chọn đúng

```hcl
# Flexible shapes (khuyến nghị — scale CPU/RAM linh hoạt)
resource "oci_core_instance" "web" {
  # AMD Flexible — tốt cho web workload
  shape = "VM.Standard.E4.Flex"
  shape_config {
    ocpus         = 2
    memory_in_gbs = 8
  }

  # HOẶC ARM Ampere A1 — rẻ hơn 3x so với x86, tốt cho general workload
  shape = "VM.Standard.A1.Flex"
  shape_config {
    ocpus         = 4   # 4 OCPU ARM ~ 8 vCPU
    memory_in_gbs = 24
  }
}

# OCI Always Free tier (dùng cho dev/test miễn phí)
resource "oci_core_instance" "dev" {
  shape = "VM.Standard.A1.Flex"
  shape_config {
    ocpus         = 2   # Free: tối đa 4 OCPU ARM total
    memory_in_gbs = 12  # Free: tối đa 24GB total
  }
}
```

---

## VCN (Virtual Cloud Network) Setup

```hcl
# modules/networking/main.tf

resource "oci_core_vcn" "main" {
  compartment_id = var.compartment_id
  cidr_blocks    = [var.vcn_cidr]  # e.g. "10.0.0.0/16"
  display_name   = "${var.project}-${var.env}-vcn"
  dns_label      = "${var.project}${var.env}"

  freeform_tags = local.common_tags
}

# Internet Gateway (cho public subnet)
resource "oci_core_internet_gateway" "main" {
  compartment_id = var.compartment_id
  vcn_id         = oci_core_vcn.main.id
  enabled        = true
  display_name   = "${var.project}-${var.env}-igw"
}

# NAT Gateway (cho private subnet ra internet)
resource "oci_core_nat_gateway" "main" {
  compartment_id = var.compartment_id
  vcn_id         = oci_core_vcn.main.id
  display_name   = "${var.project}-${var.env}-nat"
}

# Service Gateway (cho OCI services — Object Storage, etc.)
data "oci_core_services" "all" {}
resource "oci_core_service_gateway" "main" {
  compartment_id = var.compartment_id
  vcn_id         = oci_core_vcn.main.id
  display_name   = "${var.project}-${var.env}-sgw"
  services {
    service_id = data.oci_core_services.all.services[0].id  # All OCI Services
  }
}

# Public Subnet
resource "oci_core_subnet" "public" {
  for_each = toset(var.public_subnet_cidrs)

  compartment_id    = var.compartment_id
  vcn_id            = oci_core_vcn.main.id
  cidr_block        = each.value
  display_name      = "${var.project}-${var.env}-public-${index(var.public_subnet_cidrs, each.value) + 1}"
  route_table_id    = oci_core_route_table.public.id
  security_list_ids = [oci_core_security_list.public.id]
  dns_label         = "public${index(var.public_subnet_cidrs, each.value) + 1}"
}

# Private Subnet
resource "oci_core_subnet" "private" {
  for_each = toset(var.private_subnet_cidrs)

  compartment_id             = var.compartment_id
  vcn_id                     = oci_core_vcn.main.id
  cidr_block                 = each.value
  display_name               = "${var.project}-${var.env}-private-${index(var.private_subnet_cidrs, each.value) + 1}"
  prohibit_public_ip_on_vnic = true  # Private subnet không có public IP
  route_table_id             = oci_core_route_table.private.id
  security_list_ids          = [oci_core_security_list.private.id]
}
```

---

## Autonomous Database (ATP/ADW)

```hcl
# modules/database/main.tf

resource "oci_database_autonomous_database" "main" {
  compartment_id = var.compartment_id
  db_name        = "${var.project}${var.env}db"
  display_name   = "${var.project}-${var.env}-adb"

  # ATP (Transaction Processing) hoặc ADW (Data Warehouse)
  db_workload = "OLTP"  # hoặc "DW"

  # CPU và storage
  cpu_core_count      = var.env == "prod" ? 4 : 1
  data_storage_size_in_tbs = var.env == "prod" ? 1 : 0  # 0 = minimum 20GB

  # Auto scaling
  is_auto_scaling_enabled              = var.env == "prod"
  is_auto_scaling_for_storage_enabled  = var.env == "prod"

  # License
  license_model = "LICENSE_INCLUDED"  # hoặc "BRING_YOUR_OWN_LICENSE"

  # Security
  admin_password                       = var.db_admin_password  # dùng OCI Vault
  is_mtls_connection_required          = true
  whitelisted_ips                      = var.env == "prod" ? null : [var.dev_ip]
  subnet_id                            = var.private_subnet_id  # private access
  nsg_ids                              = [oci_core_network_security_group.db.id]

  # Backup
  backup_retention_period_in_days = var.env == "prod" ? 60 : 7

  freeform_tags = local.common_tags
}

# OCI Vault cho password
resource "oci_vault_secret" "db_password" {
  compartment_id = var.compartment_id
  vault_id       = oci_kms_vault.main.id
  key_id         = oci_kms_key.main.id
  secret_name    = "${var.project}/${var.env}/db-admin-password"

  secret_content {
    content_type = "BASE64"
    content      = base64encode(var.db_admin_password)
  }
}
```

---

## OKE (Oracle Kubernetes Engine)

```hcl
# modules/kubernetes/main.tf

resource "oci_containerengine_cluster" "main" {
  compartment_id     = var.compartment_id
  name               = "${var.project}-${var.env}-oke"
  kubernetes_version = "v1.28.2"
  vcn_id             = var.vcn_id

  endpoint_config {
    is_public_ip_enabled = false  # private endpoint
    subnet_id            = var.private_subnet_id
    nsg_ids              = [oci_core_network_security_group.oke_api.id]
  }

  options {
    service_lb_subnet_ids = [var.public_subnet_id]
    kubernetes_network_config {
      pods_cidr     = "10.244.0.0/16"
      services_cidr = "10.96.0.0/16"
    }
    add_ons {
      is_kubernetes_dashboard_enabled = false
      is_tiller_enabled               = false
    }
  }

  cluster_pod_network_options {
    cni_type = "OCI_VCN_IP_NATIVE"  # khuyến nghị cho performance
  }
}

# Node Pool
resource "oci_containerengine_node_pool" "main" {
  cluster_id         = oci_containerengine_cluster.main.id
  compartment_id     = var.compartment_id
  name               = "${var.project}-${var.env}-nodepool"
  kubernetes_version = "v1.28.2"

  node_shape = "VM.Standard.A1.Flex"  # ARM — giá tốt hơn x86
  node_shape_config {
    ocpus         = 4
    memory_in_gbs = 24
  }

  node_source_details {
    image_id    = data.oci_core_images.oke_node.images[0].id
    source_type = "IMAGE"
    boot_volume_size_in_gbs = 100
  }

  node_config_details {
    size = var.env == "prod" ? 3 : 1

    placement_configs {
      availability_domain = data.oci_identity_availability_domains.ads.availability_domains[0].name
      subnet_id           = var.private_subnet_ids[0]
    }
    # Multi-AD cho prod
    dynamic "placement_configs" {
      for_each = var.env == "prod" ? [1, 2] : []
      content {
        availability_domain = data.oci_identity_availability_domains.ads.availability_domains[placement_configs.value].name
        subnet_id           = var.private_subnet_ids[placement_configs.value]
      }
    }

    is_pv_encryption_in_transit_enabled = true
  }
}
```

---

## OCI IAM — Policies

```hcl
# OCI Policies dùng ngôn ngữ tự nhiên — khác AWS

resource "oci_identity_policy" "app_policy" {
  compartment_id = var.compartment_id
  name           = "${var.project}-app-policy"
  description    = "Policy cho application compute instances"

  statements = [
    # Instances đọc secrets từ Vault
    "Allow dynamic-group ${oci_identity_dynamic_group.app_instances.name} to read secret-bundles in compartment ${var.compartment_name}",
    # Instances ghi logs
    "Allow dynamic-group ${oci_identity_dynamic_group.app_instances.name} to use log-content in compartment ${var.compartment_name}",
    # Instances đọc Object Storage
    "Allow dynamic-group ${oci_identity_dynamic_group.app_instances.name} to read objects in compartment ${var.compartment_name}",
  ]
}

# Dynamic Group (giống AWS instance profile)
resource "oci_identity_dynamic_group" "app_instances" {
  compartment_id = var.tenancy_ocid  # dynamic groups phải ở tenancy level
  name           = "${var.project}-${var.env}-app-instances"
  description    = "Compute instances thuộc project ${var.project}"
  matching_rule  = "All {instance.compartment.id = '${oci_identity_compartment.compute.id}'}"
}
```

---

## Deploy Phases — OCI

### Phase 1: Foundation (Tuần 1–2)

```
Day 1–2: Tenancy Setup
  ✓ Tạo compartment hierarchy
  ✓ Setup OCI Vault + KMS key
  ✓ Configure admin users / groups / policies
  ✓ Enable Cloud Guard (security posture)
  ✓ Setup Object Storage bucket cho Terraform state

Day 3–5: Networking
  ✓ Deploy VCN + subnets (public/private/database)
  ✓ Internet Gateway, NAT Gateway, Service Gateway
  ✓ Route Tables và Security Lists/NSGs
  ✓ OCI DNS zones

Day 6–10: Identity & Security
  ✓ Dynamic Groups cho instance principals
  ✓ IAM Policies theo least privilege
  ✓ OCI Bastion Service (thay SSH trực tiếp)
  ✓ Vulnerability Scanning enable
```

### Phase 2: Application Layer (Tuần 3–4)

```
Compute:
  ✓ Compute instances hoặc OKE cluster
  ✓ Load Balancer (Layer 7) + Listeners
  ✓ Instance Pool + Autoscaling Config

Database:
  ✓ Autonomous Database ATP (production)
  ✓ OCI Vault secrets cho DB credentials
  ✓ Wallet download và distribute

Storage:
  ✓ Object Storage buckets với Lifecycle Rules
  ✓ File Storage (NFS) nếu cần
  ✓ Block Volumes cho persistent storage

Monitoring:
  ✓ OCI Monitoring alarms
  ✓ Logging Analytics
  ✓ Notification topics (email/PagerDuty)
```

### Phase 3: Production Hardening (Tuần 5–6)

```
Reliability:
  ✓ Autonomous Data Guard (Multi-AD)
  ✓ Load Balancer health checks tuned
  ✓ Backup policies configured

Security:
  ✓ Cloud Guard detector rules
  ✓ Security Zones (cho prod compartment)
  ✓ WAF policies cho public endpoints
  ✓ Data Safe cho database auditing

Cost:
  ✓ Usage thresholds và budget alerts
  ✓ Autoscaling schedules (scale down ngoài giờ)
  ✓ ARM (A1.Flex) migration plan
```

---

## OCI Cost Optimization

| Tip | Savings |
|-----|---------|
| Dùng ARM (A1.Flex) thay x86 | ~67% rẻ hơn |
| Always Free tier cho dev | $0 cho 4 OCPU + 24GB RAM |
| Autonomous DB — Serverless tier | Chỉ trả khi dùng |
| Pre-emptible Instances cho batch | ~50% rẻ hơn |
| Object Storage Infrequent Access | ~60% rẻ hơn Standard |

---

## OCI Cost Estimation (Web App, 1000 MAU)

| Service | Dev/month | Prod/month |
|---------|-----------|------------|
| Compute VM.Standard.A1.Flex (4 OCPU) | $0 (Always Free) | $40 (8 OCPU) |
| Autonomous DB ATP (1 OCPU) | $0 (Always Free) | $175 (2 OCPU) |
| Load Balancer (10 Mbps) | $10 | $20 |
| Object Storage (1TB) | $0 (10GB free) | $26 |
| VCN/NAT | $0 | $30 |
| **Total** | **~$10** | **~$291** |

OCI thường rẻ hơn AWS **20–40%** cho cùng workload.

---

## Pre-Launch Checklist OCI

### Security
- [ ] Cloud Guard không có CRITICAL findings
- [ ] Security Zones bật cho prod compartment
- [ ] Tất cả passwords/secrets trong OCI Vault
- [ ] Instance Principal thay user credentials
- [ ] Bastion Service thay SSH trực tiếp
- [ ] WAF rules active cho public endpoints

### Reliability
- [ ] Autonomous Data Guard (multi-AD) bật
- [ ] Load Balancer health checks < 5s
- [ ] Autoscaling min = 2 instances (prod)
- [ ] Object Storage versioning bật
- [ ] Backup policy verified với restore test

### Operations
- [ ] OCI Monitoring alarms cho CPU, memory, error rates
- [ ] Notifications đến email/Slack/PagerDuty
- [ ] Logging Analytics queries cho troubleshooting
- [ ] Cost budget alert tại 80%
- [ ] Terraform state backup verified
