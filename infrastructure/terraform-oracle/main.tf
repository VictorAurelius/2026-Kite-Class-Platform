# =============================================================================
# KiteHub Production Infrastructure - Oracle Cloud (Primary)
# =============================================================================
# Usage:
#   cp terraform.tfvars.example terraform.tfvars
#   cp backend.hcl.example backend.hcl   # fill in OCI namespace + credentials
#   terraform init -backend-config=backend.hcl
#   terraform plan
#   terraform apply
#
# Creates:
#   - VCN + Subnet + Security List
#   - 2 ARM VMs (A1.Flex) for backend + frontend/AI
#   - Reserved public IPs
#   - Object Storage bucket (versioning + lifecycle — see compute.tf, GAP-118)
#
# Cost: $0/month (Always Free tier)
# Docs: documents/03-planning/infrastructure/kitehub-oracle-cloud-deployment.md
# DR/Backup: documents/05-guides/disaster-recovery-plan.md (GAP-119)
# =============================================================================

terraform {
  required_version = ">= 1.5"

  required_providers {
    oci = {
      source  = "oracle/oci"
      version = "~> 5.0"
    }
  }

  # Remote state via OCI Object Storage (S3-compatible API)
  # Config supplied at init time: terraform init -backend-config=backend.hcl
  # See backend.hcl.example for required fields
  backend "s3" {}
}

provider "oci" {
  tenancy_ocid     = var.tenancy_ocid
  user_ocid        = var.user_ocid
  fingerprint      = var.fingerprint
  private_key_path = var.private_key_path
  region           = var.region
  # For production on OCI VMs: replace above with instance principal (no key files needed)
  # auth = "InstancePrincipal"
}

# =============================================================================
# DATA SOURCES
# =============================================================================

data "oci_identity_availability_domains" "ads" {
  compartment_id = var.tenancy_ocid
}

# Oracle Linux 9 ARM image
data "oci_core_images" "oracle_linux_arm" {
  compartment_id           = var.compartment_ocid
  operating_system         = "Oracle Linux"
  operating_system_version = "9"
  shape                    = "VM.Standard.A1.Flex"
  sort_by                  = "TIMECREATED"
  sort_order               = "DESC"
}
