# =============================================================================
# Route53 — optional hosted zone (Cloudflare DNS is primary per ADR-018)
# =============================================================================
# Default disabled; flip `manage_route53_zone = true` only if migrating away
# from Cloudflare DNS.

resource "aws_route53_zone" "primary" {
  count = var.manage_route53_zone ? 1 : 0
  name  = var.domain_name

  tags = { Name = "${var.project_name}-${replace(var.domain_name, ".", "-")}" }
}

# Apex A-record → ALB (only if both managed)
resource "aws_route53_record" "apex_alb" {
  count = var.manage_route53_zone && var.enable_alb ? 1 : 0

  zone_id = aws_route53_zone.primary[0].zone_id
  name    = var.domain_name
  type    = "A"

  alias {
    name                   = aws_lb.main[0].dns_name
    zone_id                = aws_lb.main[0].zone_id
    evaluate_target_health = true
  }
}
