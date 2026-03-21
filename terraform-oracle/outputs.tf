# =============================================================================
# Outputs
# =============================================================================

output "backend_public_ip" {
  description = "Public IP of backend VM"
  value       = oci_core_instance.backend.public_ip
}

output "frontend_public_ip" {
  description = "Public IP of frontend VM"
  value       = oci_core_instance.frontend.public_ip
}

output "backend_private_ip" {
  description = "Private IP of backend VM (for nginx proxy_pass)"
  value       = oci_core_instance.backend.private_ip
}

output "vcn_id" {
  description = "VCN OCID"
  value       = oci_core_vcn.kitehub_vcn.id
}

output "bucket_name" {
  description = "Object Storage bucket name"
  value       = oci_objectstorage_bucket.kitehub_assets.name
}

output "ssh_command_backend" {
  description = "SSH command for backend VM"
  value       = "ssh opc@${oci_core_instance.backend.public_ip}"
}

output "ssh_command_frontend" {
  description = "SSH command for frontend VM"
  value       = "ssh opc@${oci_core_instance.frontend.public_ip}"
}

output "dns_records" {
  description = "DNS records to configure"
  value = {
    "kiteclass.com"     = oci_core_instance.frontend.public_ip
    "api.kiteclass.com" = oci_core_instance.frontend.public_ip
  }
}

output "next_steps" {
  description = "Steps after terraform apply"
  value       = <<-EOT
    1. SSH to backend: ssh opc@${oci_core_instance.backend.public_ip}
    2. SSH to frontend: ssh opc@${oci_core_instance.frontend.public_ip}
    3. Deploy: see documents/07-guides/PRODUCTION-DEPLOY.md (Option A)
    4. DNS: point kiteclass.com → ${oci_core_instance.frontend.public_ip}
    5. Nginx config: set GATEWAY_HOST=${oci_core_instance.backend.private_ip}
  EOT
}
