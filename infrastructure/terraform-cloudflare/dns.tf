# Cloudflare DNS - Email authentication records for kitehub.me
# Wave 77 Bucket A - SEND foundation (GAP-370 + GAP-533)
#
# Records managed:
#   1. SPF (TXT @)               - authorize Resend (or SES) to send on behalf of kitehub.me
#   2. DKIM CNAME x 3 (resend.*) - Resend signs outbound mail with these selectors
#   3. DMARC (TXT _dmarc)        - receiver policy for SPF/DKIM-fail messages
#
# Provider precedence per ADR-018: Cloudflare is primary DNS.
# Apply procedure: README section 3. DO NOT apply during Bucket A scope.

locals {
  spf_include = var.email_provider == "resend" ? "include:_spf.resend.com" : "include:amazonses.com"
  spf_value   = "v=spf1 ${local.spf_include} ~all"

  dmarc_value = join(" ", [
    "v=DMARC1;",
    "p=${var.dmarc_policy};",
    "pct=${var.dmarc_pct};",
    "rua=mailto:${var.dmarc_rua_email};",
    "ruf=mailto:${var.dmarc_ruf_email};",
    "fo=1;",
    "aspf=r;",
    "adkim=r"
  ])
}

# -----------------------------------------------------------------------------
# 1. SPF - authorize provider to send mail from kitehub.me
# -----------------------------------------------------------------------------
resource "cloudflare_record" "spf" {
  zone_id = var.cloudflare_zone_id
  name    = "@"
  type    = "TXT"
  content = local.spf_value
  ttl     = 3600
  proxied = false

  comment = "Wave 77 Bucket A - SPF authorize ${var.email_provider} (GAP-370 + GAP-533)"
}

# -----------------------------------------------------------------------------
# 2. DKIM CNAME x 3 - Resend selectors
#    Operator MUST replace var.resend_dkim_selector_* with values from
#    Resend dashboard (Domains -> kitehub.me -> Add DNS records).
#    SES path: set var.email_provider = "ses" - DKIM records shape differs;
#    use AWS SES Console "Verify domain" output instead and adapt this file
#    (defer to a follow-up wave; Bucket A scope is Resend-first).
# -----------------------------------------------------------------------------
resource "cloudflare_record" "dkim_1" {
  count   = var.email_provider == "resend" ? 1 : 0
  zone_id = var.cloudflare_zone_id
  name    = "resend._domainkey"
  type    = "CNAME"
  content = var.resend_dkim_selector_1
  ttl     = 3600
  proxied = false

  comment = "Wave 77 Bucket A - Resend DKIM selector #1 (GAP-533)"
}

resource "cloudflare_record" "dkim_2" {
  count   = var.email_provider == "resend" ? 1 : 0
  zone_id = var.cloudflare_zone_id
  name    = "resend2._domainkey"
  type    = "CNAME"
  content = var.resend_dkim_selector_2
  ttl     = 3600
  proxied = false

  comment = "Wave 77 Bucket A - Resend DKIM selector #2 (GAP-533)"
}

resource "cloudflare_record" "dkim_3" {
  count   = var.email_provider == "resend" ? 1 : 0
  zone_id = var.cloudflare_zone_id
  name    = "resend3._domainkey"
  type    = "CNAME"
  content = var.resend_dkim_selector_3
  ttl     = 3600
  proxied = false

  comment = "Wave 77 Bucket A - Resend DKIM selector #3 (GAP-533)"
}

# -----------------------------------------------------------------------------
# 3. DMARC - receiver policy
#    Policy quarantine + pct=100 per email-deliverability-runbook.md section 4.
#    Aggregate reports (rua) help diagnose deliverability - review weekly during warm-up.
# -----------------------------------------------------------------------------
resource "cloudflare_record" "dmarc" {
  zone_id = var.cloudflare_zone_id
  name    = "_dmarc"
  type    = "TXT"
  content = local.dmarc_value
  ttl     = 3600
  proxied = false

  comment = "Wave 77 Bucket A - DMARC policy=${var.dmarc_policy} pct=${var.dmarc_pct} (GAP-533)"
}

# -----------------------------------------------------------------------------
# Wave aws-restore-1 (2026-05-26): api.kitehub.me subdomain routing
# -----------------------------------------------------------------------------
# Architecture pivot post-ALB elimination: api.kitehub.me CNAMEs to apex
# kitehub.me (single proxied EIP = kc_app_fe). nginx on kc_app_fe Host-based
# vhost reverse-proxies api.kitehub.me requests to kh_backend gateway private
# VPC 10.0.0.129:8080. Replaces old ALB-fronted api routing.
#
# Proxied (orange cloud) = TLS termination CF edge + DDoS protection +
# CF rate limiting. nginx terminates internal TLS with wildcard *.kitehub.me
# cert. End-to-end TLS preserved.
#
# NOTE: apex kitehub.me + app.kitehub.me records currently managed MANUALLY
# via Cloudflare dashboard (pre-terraform-cloudflare init). Import to
# terraform tracked Wave alb-terraform-cleanup-1 follow-up.
resource "cloudflare_record" "api" {
  zone_id = var.cloudflare_zone_id
  name    = "api"
  type    = "CNAME"
  content = "kitehub.me"
  ttl     = 1 # 1 = automatic when proxied
  proxied = true

  comment = "Wave aws-restore-1 - api subdomain via kc_app_fe nginx vhost (post ALB elimination)"
}

# -----------------------------------------------------------------------------
# Outputs - surfaces values for runbook verification
# -----------------------------------------------------------------------------
output "spf_record" {
  description = "SPF TXT content applied to apex"
  value       = cloudflare_record.spf.content
}

output "dmarc_record" {
  description = "DMARC TXT content applied to _dmarc.kitehub.me"
  value       = cloudflare_record.dmarc.content
}

output "dkim_records_applied" {
  description = "Count of DKIM CNAME records applied (3 for Resend, 0 for SES path)"
  value = (
    length(cloudflare_record.dkim_1)
    + length(cloudflare_record.dkim_2)
    + length(cloudflare_record.dkim_3)
  )
}
