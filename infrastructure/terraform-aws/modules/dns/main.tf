# DNS module — Cloudflare zones + records for KiteHub platform.
# Status: SKELETON. See README.md for full intended scope.
# Full resources land post-registrar procurement (GAP-191 follow-up).
#
# This file intentionally declares provider requirements + inputs + outputs
# but creates no live resources. Safe to `terraform init` and `validate`.

terraform {
  required_version = ">= 1.6.0"
  required_providers {
    cloudflare = {
      source  = "cloudflare/cloudflare"
      version = "~> 4.0"
    }
  }
}

# ---- Inputs -----------------------------------------------------------------

variable "cloudflare_api_token" {
  description = "Cloudflare API token (scoped: Zone:Edit, DNS:Edit for managed zones)."
  type        = string
  sensitive   = true
  default     = null
}

variable "alb_ipv4" {
  description = "Primary ALB IPv4 for apex + wildcard records."
  type        = string
  default     = null
}

variable "alb_ipv6" {
  description = "Primary ALB IPv6 for AAAA records."
  type        = string
  default     = null
}

variable "kitehub_vn_enabled" {
  description = "Enable kitehub.vn zone (flip to true only after Matbao registration completes)."
  type        = bool
  default     = false
}

variable "kiteclass_com_enabled" {
  description = "Enable kitehub.me zone (flip to true after Cloudflare Registrar purchase completes)."
  type        = bool
  default     = false
}

# ---- Resources (skeleton) ---------------------------------------------------
#
# Once credentials + registration complete, uncomment + expand per README.md:
#
# resource "cloudflare_zone" "kitehub_vn" {
#   count   = var.kitehub_vn_enabled ? 1 : 0
#   account = var.cloudflare_account_id
#   zone    = "kitehub.vn"
#   plan    = "free"
# }
#
# resource "cloudflare_zone" "kiteclass_com" {
#   count   = var.kiteclass_com_enabled ? 1 : 0
#   account = var.cloudflare_account_id
#   zone    = "kitehub.me"
#   plan    = "business" # Custom Hostnames requires Business+
# }
#
# resource "cloudflare_record" "kitehub_vn_apex_a" { ... }
# resource "cloudflare_record" "kiteclass_com_wildcard_a" { ... }

# ---- Outputs ----------------------------------------------------------------

output "kiteclass_com_zone_id" {
  description = "Zone ID for kitehub.me — consumed by runtime provisioning adapter."
  value       = null # replace with cloudflare_zone.kiteclass_com[0].id once enabled
}

output "kitehub_vn_zone_id" {
  description = "Zone ID for kitehub.vn."
  value       = null # replace with cloudflare_zone.kitehub_vn[0].id once enabled
}

output "managed_zones" {
  description = "List of zone names actually managed by this module."
  value = compact([
    var.kitehub_vn_enabled ? "kitehub.vn" : null,
    var.kiteclass_com_enabled ? "kitehub.me" : null,
  ])
}
