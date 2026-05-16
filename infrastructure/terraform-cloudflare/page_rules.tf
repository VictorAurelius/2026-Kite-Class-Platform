# Cloudflare Page Rules — magic-link + invite endpoint cache bypass
# Wave 86 Bucket E-AC4 (GAP-584) — P0 BLOCKER chặn Bucket G invite
#
# Problem:
#   Cloudflare default caches query-string URLs (~2h TTL). Magic-link endpoints
#   include single-use tokens trong URL → tenant A's redirect có thể được CF cache
#   serve cho tenant B → cross-tenant auth bypass + onboarding security breach.
#
# Fix:
#   Page Rule `cache_level = bypass` cho 2 URL patterns:
#     1. *kitehub.me/auth/magic*    (magic-link auth)
#     2. *kitehub.me/auth/invite/*  (invite acceptance flow)
#
# Defense-in-depth:
#   Option B (response header Cache-Control: no-store) tracked GAP-584 AC #2
#   — Spring Boot AuthController follow-up; Page Rule là edge layer hard guard.
#
# Self-test post-apply:
#   curl -sI https://kitehub.me/auth/magic/test-token | grep -i cf-cache-status
#   Expected: CF-Cache-Status: BYPASS hoặc DYNAMIC (never HIT)
#
# References:
#   - GAP-584: documents/04-quality/gaps/GAP-584-magic-link-cloudflare-cache-bypass.md
#   - Audit:   documents/04-quality/audits/cloudflare-verification/2026-05-16-wave-86-magic-link-bypass-page-rule.md
#   - Wave 86 plan §3 Bucket E AC E-AC4 (P0 BLOCKER)

# -----------------------------------------------------------------------------
# Page Rule 1 — magic-link endpoints (`/auth/magic*`)
# -----------------------------------------------------------------------------
resource "cloudflare_page_rule" "magic_link_bypass_cache" {
  zone_id  = var.cloudflare_zone_id
  target   = "*kitehub.me/auth/magic*"
  priority = 1
  status   = "active"

  actions {
    cache_level = "bypass"
  }
}

# -----------------------------------------------------------------------------
# Page Rule 2 — invite endpoints (`/auth/invite/*`)
# -----------------------------------------------------------------------------
resource "cloudflare_page_rule" "invite_bypass_cache" {
  zone_id  = var.cloudflare_zone_id
  target   = "*kitehub.me/auth/invite/*"
  priority = 2
  status   = "active"

  actions {
    cache_level = "bypass"
  }
}

# -----------------------------------------------------------------------------
# Outputs — operator verification reference
# -----------------------------------------------------------------------------
output "magic_link_page_rule_id" {
  description = "Cloudflare Page Rule ID for magic-link cache bypass. Verify via API: curl -H 'Authorization: Bearer $CF_API_TOKEN' https://api.cloudflare.com/client/v4/zones/$ZONE_ID/pagerules/$ID"
  value       = cloudflare_page_rule.magic_link_bypass_cache.id
}

output "invite_page_rule_id" {
  description = "Cloudflare Page Rule ID for invite cache bypass."
  value       = cloudflare_page_rule.invite_bypass_cache.id
}
