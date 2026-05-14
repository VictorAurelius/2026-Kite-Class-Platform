# Input variables for Cloudflare DNS - Phase 1 BETA email SEND foundation
# Wave 77 Bucket A

variable "cloudflare_api_token" {
  description = "Cloudflare API token with Zone:Read + DNS:Edit on kitehub.me zone. Provided via TF_VAR_cloudflare_api_token or terraform.tfvars (NOT committed)."
  type        = string
  sensitive   = true
}

variable "cloudflare_zone_id" {
  description = "Cloudflare zone ID for kitehub.me. Fetched once via curl https://api.cloudflare.com/client/v4/zones?name=kitehub.me with the API token."
  type        = string
}

variable "domain" {
  description = "Apex domain managed by this config."
  type        = string
  default     = "kitehub.me"
}

# ---------------------------------------------------------------------------
# Email provider - Resend (Phase 1 BETA primary per ADR-025 Stream A)
# ---------------------------------------------------------------------------

variable "email_provider" {
  description = "Email provider - 'resend' (primary Phase 1 BETA) or 'ses' (fallback if SES production approved). Determines SPF include value + DKIM record shape."
  type        = string
  default     = "resend"

  validation {
    condition     = contains(["resend", "ses"], var.email_provider)
    error_message = "email_provider must be 'resend' or 'ses'."
  }
}

variable "resend_dkim_selector_1" {
  description = "Resend DKIM CNAME target #1 - fetched from Resend dashboard after domain add. Placeholder default; MUST replace before apply."
  type        = string
  default     = "PLACEHOLDER-FROM-RESEND-DASHBOARD-1"
}

variable "resend_dkim_selector_2" {
  description = "Resend DKIM CNAME target #2. Placeholder default; MUST replace before apply."
  type        = string
  default     = "PLACEHOLDER-FROM-RESEND-DASHBOARD-2"
}

variable "resend_dkim_selector_3" {
  description = "Resend DKIM CNAME target #3. Placeholder default; MUST replace before apply."
  type        = string
  default     = "PLACEHOLDER-FROM-RESEND-DASHBOARD-3"
}

# ---------------------------------------------------------------------------
# DMARC reporting endpoints
# ---------------------------------------------------------------------------

variable "dmarc_rua_email" {
  description = "DMARC aggregate report destination (mailto:). Use a real inbox monitored weekly during warm-up."
  type        = string
  default     = "dmarc-reports@kitehub.me"
}

variable "dmarc_ruf_email" {
  description = "DMARC forensic report destination (mailto:). Same as rua acceptable for solo-dev."
  type        = string
  default     = "dmarc-reports@kitehub.me"
}

variable "dmarc_policy" {
  description = "DMARC policy - 'none' (warm-up monitor only), 'quarantine' (post warm-up, mark suspicious), or 'reject' (production mature). Phase 1 BETA defaults quarantine per email-deliverability-runbook.md sec 4."
  type        = string
  default     = "quarantine"

  validation {
    condition     = contains(["none", "quarantine", "reject"], var.dmarc_policy)
    error_message = "dmarc_policy must be 'none', 'quarantine', or 'reject'."
  }
}

variable "dmarc_pct" {
  description = "DMARC percentage of messages to apply policy to (0-100). Phase 1 BETA = 100 because volume is low."
  type        = number
  default     = 100
}
