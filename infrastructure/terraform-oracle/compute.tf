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
resource "oci_objectstorage_bucket" "kitehub_assets" {
  compartment_id = var.compartment_ocid
  namespace      = data.oci_objectstorage_namespace.ns.namespace
  name           = "kitehub-assets"
  access_type    = "NoPublicAccess"

  freeform_tags = {
    Project   = var.project_name
    ManagedBy = "Terraform"
  }
}

data "oci_objectstorage_namespace" "ns" {
  compartment_id = var.compartment_ocid
}
