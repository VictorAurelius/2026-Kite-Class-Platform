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

# Lifecycle: keep last 10 images per repo (Free tier 500MB limit guard)
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
        description  = "Keep last 10 tagged images"
        selection = {
          tagStatus   = "any"
          countType   = "imageCountMoreThan"
          countNumber = 10
        }
        action = { type = "expire" }
      },
    ]
  })
}
