# =============================================================================
# ACM - Tenant Custom Domain Certificates (scaffold for GAP-812 Phase B)
# =============================================================================
# Per-tenant ACM certificate provisioning with DNS validation for verified
# custom domains. Scaffold in Wave tenant-domain-1 Bucket D - APPLY DEFERRED
# per release-deploy-standard.md section 9 (terraform apply human-only).
#
# Scope (per GAP-812 Phase B):
# - Phase B v1 (deferred): use Cloudflare for SaaS (preferred per ADR-018 +
#   GAP-812 outside-in audit). ACM here = fallback / alternate provider if
#   migrating away from Cloudflare DNS.
# - Phase B v2 (future): automate cert request on Instance.domainStatus
#   transition PENDING_VERIFY -> VERIFIED via Lambda + EventBridge subscriber
#   to outbox event domain.verified.
#
# References:
# - documents/01-business/kitehub/custom-domain/rules.md (DOM-11 SSL issuance)
# - documents/05-guides/operations/custom-domain-verify-runbook.md
# - documents/02-architecture/adr/ADR-018-domain-registrar-dns.md
# =============================================================================

variable "tenant_custom_domains" {
  description = "List of verified tenant custom domains for ACM cert provisioning. Populated manually for now; future automation via Lambda on domain verify success."
  type = list(object({
    tenant_id = string # Instance UUID (string form)
    domain    = string # Fully-qualified custom domain (e.g., lop.skyedu.vn)
  }))
  default = []
  # Example after manual fill:
  # default = [
  #   { tenant_id = "550e8400-e29b-41d4-a716-446655440000", domain = "lop.skyedu.vn" }
  # ]
}

# -----------------------------------------------------------------------------
# Per-tenant ACM certificate with DNS validation.
# Note: DNS validation requires CNAME records at tenant DNS provider -
# tenant follows runbook to add CNAME after this resource creates pending cert.
# -----------------------------------------------------------------------------
resource "aws_acm_certificate" "tenant_domain" {
  for_each = { for d in var.tenant_custom_domains : d.tenant_id => d }

  domain_name       = each.value.domain
  validation_method = "DNS"

  lifecycle {
    create_before_destroy = true
  }

  tags = {
    Name      = "${var.project_name}-tenant-${each.value.tenant_id}"
    TenantId  = each.value.tenant_id
    Domain    = each.value.domain
    ManagedBy = "terraform"
    Module    = "kitehub-subscription/custom-domain"
  }
}

# -----------------------------------------------------------------------------
# Outputs - ACM cert ARN per tenant (consumed by Lambda automation Phase B v2)
# and DNS validation records (consumed by runbook tenant-facing instructions).
# -----------------------------------------------------------------------------
output "tenant_acm_cert_arns" {
  description = "Map of tenant_id to ACM certificate ARN for verified custom domains"
  value = {
    for tenant_id, cert in aws_acm_certificate.tenant_domain :
    tenant_id => cert.arn
  }
}

output "tenant_acm_validation_records" {
  description = "Map of tenant_id to required CNAME validation records (name, value) for tenant DNS provider setup"
  value = {
    for tenant_id, cert in aws_acm_certificate.tenant_domain :
    tenant_id => [
      for dvo in cert.domain_validation_options : {
        name  = dvo.resource_record_name
        type  = dvo.resource_record_type
        value = dvo.resource_record_value
      }
    ]
  }
}
