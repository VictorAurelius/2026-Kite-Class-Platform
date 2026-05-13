# =============================================================================
# ECR Repositories — Bucket B (Docker) consumers reference these names
# =============================================================================
# Naming convention: kite/<service> per Wave 37 plan §3 Bucket B.
# Region pin: ap-southeast-1 (provider default).

locals {
  ecr_services = [
    # KiteHub backend services
    "kite/kitehub-subscription",
    "kite/kitehub-gateway",
    "kite/kitehub-branding",
    "kite/kitehub-admin",
    "kite/kitehub-email",
    "kite/kitehub-platform",
    "kite/kitehub-frontend",
    # KiteClass services
    "kite/kiteclass-core",
    "kite/kiteclass-gateway",
    "kite/kiteclass-frontend",
  ]
}

resource "aws_ecr_repository" "services" {
  for_each = toset(local.ecr_services)

  name                 = each.key
  image_tag_mutability = "MUTABLE" # Phase 1 BETA — flip IMMUTABLE for GA

  image_scanning_configuration {
    scan_on_push = true # GAP-400 Trivy is more thorough; ECR scan is free supplement
  }

  encryption_configuration {
    encryption_type = "AES256"
  }

  tags = { Name = each.key }
}

# Lifecycle: prefix-targeted retention — version tags (0.x.y, 1.x) kept forever,
# ephemeral tags (sha-, main, test, latest, pr-) capped to avoid Free Tier blowup.
#
# 2026-05-13 redesign: previous "keep last 10 any" rule expired version tags
# when ephemeral churn outpaced version cadence — v0.9.0-beta-staging.10 was
# pushed 2026-05-12 then expired by 2026-05-13 because 10+ ephemeral pushes
# (sha-XXX, test, main, latest) for Wave 66 fixes bumped it out of top-10.
# Deploy of staging.10 failed: "manifest unknown: Requested image not found".
#
# New design: no catch-all "any" rule → version tags (no matching prefix) kept
# forever. Storage growth ~10 services × 10 versions/mo × 500MB = ~50GB/yr.
# Acceptable cost (~$5/mo) for reliable redeploy of older versions.
resource "aws_ecr_lifecycle_policy" "cleanup" {
  for_each   = aws_ecr_repository.services
  repository = each.value.name

  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Expire untagged images after 7 days"
        selection = {
          tagStatus   = "untagged"
          countType   = "sinceImagePushed"
          countUnit   = "days"
          countNumber = 7
        }
        action = { type = "expire" }
      },
      {
        rulePriority = 2
        description  = "Keep last 20 sha-prefixed (commit-pinned) tags"
        selection = {
          tagStatus     = "tagged"
          tagPrefixList = ["sha-"]
          countType     = "imageCountMoreThan"
          countNumber   = 20
        }
        action = { type = "expire" }
      },
      {
        rulePriority = 3
        description  = "Keep last 10 ephemeral branch/PR tags (main/test/latest/pr-)"
        selection = {
          tagStatus     = "tagged"
          tagPrefixList = ["main", "test", "latest", "pr-"]
          countType     = "imageCountMoreThan"
          countNumber   = 10
        }
        action = { type = "expire" }
      },
      # Version tags (0.x.y, 1.x, v* — none of the above prefixes) NOT matched
      # by any expire rule → kept forever. Storage cost accepted per file header.
    ]
  })
}
