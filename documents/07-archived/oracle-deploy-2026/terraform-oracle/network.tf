# =============================================================================
# Networking - VCN, Subnet, Security List, Internet Gateway
# =============================================================================

resource "oci_core_vcn" "kitehub_vcn" {
  compartment_id = var.compartment_ocid
  cidr_blocks    = [var.vcn_cidr]
  display_name   = "${var.project_name}-vcn"
  dns_label      = "kitehub"

  freeform_tags = {
    Project   = var.project_name
    ManagedBy = "Terraform"
  }
}

resource "oci_core_internet_gateway" "kitehub_igw" {
  compartment_id = var.compartment_ocid
  vcn_id         = oci_core_vcn.kitehub_vcn.id
  display_name   = "${var.project_name}-igw"
  enabled        = true
}

resource "oci_core_route_table" "kitehub_rt" {
  compartment_id = var.compartment_ocid
  vcn_id         = oci_core_vcn.kitehub_vcn.id
  display_name   = "${var.project_name}-rt"

  route_rules {
    destination       = "0.0.0.0/0"
    network_entity_id = oci_core_internet_gateway.kitehub_igw.id
  }
}

# Security List: 2 layers - this is cloud-level.
# Instance-level iptables must also be configured (see deployment docs).
resource "oci_core_security_list" "kitehub_sl" {
  compartment_id = var.compartment_ocid
  vcn_id         = oci_core_vcn.kitehub_vcn.id
  display_name   = "${var.project_name}-sl"

  # Egress: allow all outbound
  egress_security_rules {
    destination = "0.0.0.0/0"
    protocol    = "all"
    stateless   = false
  }

  # SSH
  ingress_security_rules {
    protocol  = "6" # TCP
    source    = "0.0.0.0/0"
    stateless = false
    tcp_options {
      min = 22
      max = 22
    }
  }

  # HTTP
  ingress_security_rules {
    protocol  = "6"
    source    = "0.0.0.0/0"
    stateless = false
    tcp_options {
      min = 80
      max = 80
    }
  }

  # HTTPS
  ingress_security_rules {
    protocol  = "6"
    source    = "0.0.0.0/0"
    stateless = false
    tcp_options {
      min = 443
      max = 443
    }
  }

  # ICMP (ping)
  ingress_security_rules {
    protocol  = "1" # ICMP
    source    = "0.0.0.0/0"
    stateless = false
  }
}

resource "oci_core_subnet" "kitehub_subnet" {
  compartment_id    = var.compartment_ocid
  vcn_id            = oci_core_vcn.kitehub_vcn.id
  cidr_block        = var.subnet_cidr
  display_name      = "${var.project_name}-public-subnet"
  dns_label         = "public"
  route_table_id    = oci_core_route_table.kitehub_rt.id
  security_list_ids = [oci_core_security_list.kitehub_sl.id]

  # Public subnet (VMs get public IPs)
  prohibit_public_ip_on_vnic = false
}
