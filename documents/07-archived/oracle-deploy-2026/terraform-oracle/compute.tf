# =============================================================================
# Compute - 2 ARM VMs (Always Free: 4 OCPU + 24GB total)
# =============================================================================

# VM 1: Backend (Spring Boot, PostgreSQL, Redis, RabbitMQ)
resource "oci_core_instance" "backend" {
  compartment_id      = var.compartment_ocid
  availability_domain = data.oci_identity_availability_domains.ads.availability_domains[0].name
  display_name        = "${var.project_name}-backend"
  shape               = "VM.Standard.A1.Flex"

  shape_config {
    ocpus         = var.vm_backend_ocpus
    memory_in_gbs = var.vm_backend_memory_gb
  }

  source_details {
    source_type             = "image"
    source_id               = data.oci_core_images.oracle_linux_arm.images[0].id
    boot_volume_size_in_gbs = 100
  }

  create_vnic_details {
    subnet_id        = oci_core_subnet.kitehub_subnet.id
    assign_public_ip = true
    display_name     = "${var.project_name}-backend-vnic"
  }

  metadata = {
    ssh_authorized_keys = var.ssh_public_key
    user_data = base64encode(<<-EOF
      #!/bin/bash
      # Install Docker
      dnf install -y docker
      systemctl enable --now docker
      usermod -aG docker opc

      # Install Docker Compose (ARM)
      curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-linux-aarch64" \
        -o /usr/local/bin/docker-compose
      chmod +x /usr/local/bin/docker-compose

      # Open firewall ports (instance-level - 2nd layer)
      firewall-cmd --permanent --add-port=80/tcp
      firewall-cmd --permanent --add-port=443/tcp
      firewall-cmd --permanent --add-port=9000/tcp
      firewall-cmd --reload

      echo "Backend VM setup complete"
    EOF
    )
  }

  freeform_tags = {
    Project   = var.project_name
    Role      = "backend"
    ManagedBy = "Terraform"
  }
}

# VM 2: Frontend + AI (Next.js, Ollama, Nginx)
resource "oci_core_instance" "frontend" {
  compartment_id      = var.compartment_ocid
  availability_domain = data.oci_identity_availability_domains.ads.availability_domains[0].name
  display_name        = "${var.project_name}-frontend"
  shape               = "VM.Standard.A1.Flex"

  shape_config {
    ocpus         = var.vm_frontend_ocpus
    memory_in_gbs = var.vm_frontend_memory_gb
  }

  source_details {
    source_type             = "image"
    source_id               = data.oci_core_images.oracle_linux_arm.images[0].id
    boot_volume_size_in_gbs = 100
  }

  create_vnic_details {
    subnet_id        = oci_core_subnet.kitehub_subnet.id
    assign_public_ip = true
    display_name     = "${var.project_name}-frontend-vnic"
  }

  metadata = {
    ssh_authorized_keys = var.ssh_public_key
    user_data = base64encode(<<-EOF
      #!/bin/bash
      # Install Docker
      dnf install -y docker
      systemctl enable --now docker
      usermod -aG docker opc

      # Install Docker Compose (ARM)
      curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-linux-aarch64" \
        -o /usr/local/bin/docker-compose
      chmod +x /usr/local/bin/docker-compose

      # Open firewall ports (instance-level - 2nd layer)
      firewall-cmd --permanent --add-port=80/tcp
      firewall-cmd --permanent --add-port=443/tcp
      firewall-cmd --reload

      echo "Frontend VM setup complete"
    EOF
    )
  }

  freeform_tags = {
    Project   = var.project_name
    Role      = "frontend-ai"
    ManagedBy = "Terraform"
  }
}

# Object Storage bucket for backups + assets
# GAP-118: versioning + lifecycle (cross-region replication noted below — OCI limitation)
resource "oci_objectstorage_bucket" "kitehub_assets" {
  compartment_id = var.compartment_ocid
  namespace      = data.oci_objectstorage_namespace.ns.namespace
  name           = "kitehub-assets"
  access_type    = "NoPublicAccess"

  # GAP-118 §Production (Oracle) AC #1: enable object versioning
  # Recovers deleted/overwritten objects (AI assets + template SVGs).
  # Cost: storage doubles for objects with active versions; mitigated by lifecycle below.
  versioning = "Enabled"

  # Auto-tier large infrequently-accessed objects to OCI Archive Storage
  # (analogous to S3 Glacier). Saves ~80% storage cost on cold AI assets.
  auto_tiering = "InfrequentAccess"

  freeform_tags = {
    Project   = var.project_name
    ManagedBy = "Terraform"
  }
}

# GAP-118 §Production (Oracle) AC #2: lifecycle policy
# - Delete previous (non-current) versions older than 365 days
# - Abort multipart uploads older than 7 days (cost hygiene)
#
# NOTE on cross-region replication (GAP-118 §Production Oracle AC #2 alt):
#   OCI Object Storage SUPPORTS cross-region replication via
#   `oci_objectstorage_replication_policy`, but the destination region requires
#   a pre-existing bucket in that region + IAM policy granting replication.
#   For Always Free tier (single region — Singapore ap-singapore-1), cross-region
#   replication is NOT applicable; multi-region requires paid tier.
#   The recommended GAP-118 §Production Oracle path is therefore:
#     - In-region versioning + lifecycle (this file)
#     - Cross-CLOUD backup: scheduled `oci os object sync` -> S3 secondary bucket
#       (out-of-band cron / GitHub Actions workflow — not Terraform-managed)
#   Tracked in restore-procedure.md (GAP-117) and dr-rto-rpo-matrix.md (GAP-119).
resource "oci_objectstorage_object_lifecycle_policy" "kitehub_assets" {
  namespace = data.oci_objectstorage_namespace.ns.namespace
  bucket    = oci_objectstorage_bucket.kitehub_assets.name

  rules {
    name        = "delete-noncurrent-versions-1y"
    action      = "DELETE"
    target      = "previous-object-versions"
    is_enabled  = true
    time_amount = 365
    time_unit   = "DAYS"
  }

  rules {
    name        = "abort-incomplete-multipart-7d"
    action      = "ABORT"
    target      = "multipart-uploads"
    is_enabled  = true
    time_amount = 7
    time_unit   = "DAYS"
  }
}

data "oci_objectstorage_namespace" "ns" {
  compartment_id = var.compartment_ocid
}
