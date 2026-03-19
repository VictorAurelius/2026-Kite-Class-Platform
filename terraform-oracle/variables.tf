# =============================================================================
# Variables for Oracle Cloud Infrastructure
# =============================================================================

# --- OCI Authentication ---
variable "tenancy_ocid" {
  description = "OCID of the OCI tenancy"
  type        = string
}

variable "user_ocid" {
  description = "OCID of the OCI user"
  type        = string
}

variable "fingerprint" {
  description = "Fingerprint of the OCI API key"
  type        = string
}

variable "private_key_path" {
  description = "Path to the OCI API private key"
  type        = string
  default     = "~/.oci/oci_api_key.pem"
}

variable "compartment_ocid" {
  description = "OCID of the compartment (use tenancy OCID for root)"
  type        = string
}

variable "region" {
  description = "OCI region (choose carefully - cannot change for Always Free)"
  type        = string
  default     = "ap-singapore-1"
}

# --- Compute ---
variable "ssh_public_key" {
  description = "SSH public key for VM access"
  type        = string
}

variable "vm_backend_ocpus" {
  description = "OCPUs for backend VM (max 4 total across all free VMs)"
  type        = number
  default     = 2
}

variable "vm_backend_memory_gb" {
  description = "Memory (GB) for backend VM (max 24 total across all free VMs)"
  type        = number
  default     = 12
}

variable "vm_frontend_ocpus" {
  description = "OCPUs for frontend+AI VM"
  type        = number
  default     = 2
}

variable "vm_frontend_memory_gb" {
  description = "Memory (GB) for frontend+AI VM"
  type        = number
  default     = 12
}

# --- Network ---
variable "vcn_cidr" {
  description = "CIDR block for VCN"
  type        = string
  default     = "10.0.0.0/16"
}

variable "subnet_cidr" {
  description = "CIDR block for public subnet"
  type        = string
  default     = "10.0.1.0/24"
}

# --- Tags ---
variable "project_name" {
  description = "Project name for tagging"
  type        = string
  default     = "KiteHub"
}
