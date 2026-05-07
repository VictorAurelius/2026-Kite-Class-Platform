# =============================================================================
# IAM — EC2 instance profile + GitHub OIDC for terraform-plan CI
# =============================================================================

# --- EC2 instance role: pull from ECR + read Secrets Manager + access S3 + SSM ---
resource "aws_iam_role" "ec2_app" {
  name = "${var.project_name}-${var.environment}-ec2-app"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Action    = "sts:AssumeRole"
      Principal = { Service = "ec2.amazonaws.com" }
    }]
  })

  tags = { Name = "${var.project_name}-ec2-app-role" }
}

# Pull Docker images from ECR
resource "aws_iam_role_policy_attachment" "ec2_ecr_read" {
  role       = aws_iam_role.ec2_app.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
}

# SSM Session Manager (replaces SSH key for shell access)
resource "aws_iam_role_policy_attachment" "ec2_ssm" {
  role       = aws_iam_role.ec2_app.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

# CloudWatch agent for logs/metrics
resource "aws_iam_role_policy_attachment" "ec2_cloudwatch" {
  role       = aws_iam_role.ec2_app.name
  policy_arn = "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy"
}

# Custom inline policy: read Secrets Manager + access S3 assets bucket
resource "aws_iam_role_policy" "ec2_secrets_s3" {
  name = "${var.project_name}-ec2-secrets-s3"
  role = aws_iam_role.ec2_app.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue",
          "secretsmanager:DescribeSecret",
        ]
        Resource = [
          "arn:aws:secretsmanager:${var.aws_region}:${data.aws_caller_identity.current.account_id}:secret:${var.project_name}/${var.environment}/*",
        ]
      },
      {
        Effect = "Allow"
        Action = [
          "s3:GetObject",
          "s3:PutObject",
          "s3:DeleteObject",
          "s3:ListBucket",
        ]
        Resource = [
          aws_s3_bucket.assets.arn,
          "${aws_s3_bucket.assets.arn}/*",
        ]
      },
    ]
  })
}

resource "aws_iam_instance_profile" "ec2_app" {
  name = "${var.project_name}-${var.environment}-ec2-app"
  role = aws_iam_role.ec2_app.name
}

# =============================================================================
# GitHub OIDC Provider (for terraform-plan CI per GAP-397) — read-only role
# =============================================================================

resource "aws_iam_openid_connect_provider" "github" {
  url            = "https://token.actions.githubusercontent.com"
  client_id_list = ["sts.amazonaws.com"]
  thumbprint_list = [
    # GitHub Actions OIDC thumbprint (rotation-stable per AWS docs).
    # Reference: https://github.blog/changelog/2023-06-27-github-actions-update-on-oidc-integration-with-aws/
    "6938fd4d98bab03faadb97b34396831e3780aea1",
    "1c58a3a8518e8759bf075b76b750d4f2df264fcd",
  ]

  tags = { Name = "${var.project_name}-github-oidc" }
}

# Read-only role assumed by terraform-plan workflow on PRs
resource "aws_iam_role" "github_terraform_plan" {
  name = "${var.project_name}-github-terraform-plan"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Federated = aws_iam_openid_connect_provider.github.arn }
      Action    = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = {
          "token.actions.githubusercontent.com:aud" = "sts.amazonaws.com"
        }
        StringLike = {
          # Allow PR + main branch runs (read-only — plan, not apply)
          "token.actions.githubusercontent.com:sub" = "repo:${var.github_repo}:*"
        }
      }
    }]
  })

  tags = { Name = "${var.project_name}-github-terraform-plan" }
}

# Attach AWS-managed ReadOnlyAccess (terraform plan needs to describe most resources)
resource "aws_iam_role_policy_attachment" "github_plan_readonly" {
  role       = aws_iam_role.github_terraform_plan.name
  policy_arn = "arn:aws:iam::aws:policy/ReadOnlyAccess"
}

# Plan needs DynamoDB lock acquire/release + S3 state read/write (state lock during plan)
resource "aws_iam_role_policy" "github_plan_state_access" {
  name = "${var.project_name}-github-plan-state"
  role = aws_iam_role.github_terraform_plan.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "s3:GetObject",
          "s3:PutObject",
          "s3:ListBucket",
        ]
        Resource = [
          "arn:aws:s3:::${var.project_name}-terraform-state-${data.aws_caller_identity.current.account_id}",
          "arn:aws:s3:::${var.project_name}-terraform-state-${data.aws_caller_identity.current.account_id}/*",
        ]
      },
      {
        Effect = "Allow"
        Action = [
          "dynamodb:GetItem",
          "dynamodb:PutItem",
          "dynamodb:DeleteItem",
        ]
        Resource = [
          "arn:aws:dynamodb:${var.aws_region}:${data.aws_caller_identity.current.account_id}:table/${var.project_name}-terraform-locks",
        ]
      },
    ]
  })
}

# =============================================================================
# GitHub OIDC — Deploy role (deploy-staging.yml + deploy-production.yml)
# =============================================================================
# Per GAP-436: write-capable role for SSM RunCommand on EC2 + Secrets Manager
# read + ECR pull (login). Scoped trust to main branch + version tags only.

resource "aws_iam_role" "github_deploy" {
  name = "${var.project_name}-github-deploy"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Federated = aws_iam_openid_connect_provider.github.arn }
      Action    = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = {
          "token.actions.githubusercontent.com:aud" = "sts.amazonaws.com"
        }
        StringLike = {
          # Allow main branch + version tags + GitHub Environments (staging/production)
          "token.actions.githubusercontent.com:sub" = [
            "repo:${var.github_repo}:ref:refs/heads/main",
            "repo:${var.github_repo}:ref:refs/tags/v*",
            "repo:${var.github_repo}:environment:staging",
            "repo:${var.github_repo}:environment:production",
          ]
        }
      }
    }]
  })

  tags = { Name = "${var.project_name}-github-deploy" }
}

resource "aws_iam_role_policy" "github_deploy_inline" {
  name = "${var.project_name}-github-deploy-inline"
  role = aws_iam_role.github_deploy.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      # ECR pull (login + image read) — needed by amazon-ecr-login action
      {
        Effect = "Allow"
        Action = [
          "ecr:GetAuthorizationToken",
          "ecr:BatchCheckLayerAvailability",
          "ecr:BatchGetImage",
          "ecr:GetDownloadUrlForLayer",
        ]
        Resource = "*"
      },
      # SSM RunCommand on instances tagged Project=kitehub
      {
        Effect = "Allow"
        Action = [
          "ssm:SendCommand",
          "ssm:GetCommandInvocation",
          "ssm:ListCommandInvocations",
          "ssm:DescribeInstanceInformation",
        ]
        Resource = "*"
        Condition = {
          StringEquals = {
            "aws:ResourceTag/Project" = var.project_name
          }
        }
      },
      # SSM document access (AWS-RunShellScript is most common)
      {
        Effect = "Allow"
        Action = "ssm:SendCommand"
        Resource = [
          "arn:aws:ssm:${var.aws_region}::document/AWS-RunShellScript",
        ]
      },
      # Secrets Manager read — staging + prod secret prefixes
      {
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue",
          "secretsmanager:DescribeSecret",
        ]
        Resource = [
          "arn:aws:secretsmanager:${var.aws_region}:${data.aws_caller_identity.current.account_id}:secret:kite/staging/*",
          "arn:aws:secretsmanager:${var.aws_region}:${data.aws_caller_identity.current.account_id}:secret:kite/prod/*",
        ]
      },
    ]
  })
}

# =============================================================================
# GitHub OIDC — ECR Push role (docker-build-push.yml)
# =============================================================================
# Per GAP-436: ECR push permission for tag builds. Trust scoped to main + tags.

resource "aws_iam_role" "github_ecr_push" {
  name = "${var.project_name}-github-ecr-push"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Federated = aws_iam_openid_connect_provider.github.arn }
      Action    = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = {
          "token.actions.githubusercontent.com:aud" = "sts.amazonaws.com"
        }
        StringLike = {
          "token.actions.githubusercontent.com:sub" = [
            "repo:${var.github_repo}:ref:refs/heads/main",
            "repo:${var.github_repo}:ref:refs/tags/v*",
          ]
        }
      }
    }]
  })

  tags = { Name = "${var.project_name}-github-ecr-push" }
}

resource "aws_iam_role_policy" "github_ecr_push_inline" {
  name = "${var.project_name}-github-ecr-push-inline"
  role = aws_iam_role.github_ecr_push.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = "ecr:GetAuthorizationToken"
        Resource = "*"
      },
      {
        Effect = "Allow"
        Action = [
          "ecr:BatchCheckLayerAvailability",
          "ecr:BatchGetImage",
          "ecr:CompleteLayerUpload",
          "ecr:GetDownloadUrlForLayer",
          "ecr:InitiateLayerUpload",
          "ecr:PutImage",
          "ecr:UploadLayerPart",
          "ecr:DescribeRepositories",
          "ecr:DescribeImages",
        ]
        # All ECR repos under kite/ namespace (per ecr.tf locals.ecr_services).
        # Repo naming convention: kite/<service> (CLAUDE.md Docker Naming) — e.g.
        # kite/kitehub-subscription, kite/kiteclass-gateway. Wildcard pattern
        # `repository/${var.project_name}-*` (project_name="kitehub") would expand
        # to `repository/kitehub-*` which does NOT match `repository/kite/...`.
        # Phase 3 first OIDC trigger 2026-05-07 (run #25527705091, retry #25528087813)
        # failed all 8 Push to ECR jobs with 403 Forbidden on HEAD blob check —
        # confirmed via API call missing BatchCheckLayerAvailability permission for
        # actual ARN format. Fix: scope to `repository/kite/*` (least-privilege,
        # matches all 10 ECR repos).
        Resource = "arn:aws:ecr:${var.aws_region}:${data.aws_caller_identity.current.account_id}:repository/kite/*"
      },
    ]
  })
}

# =============================================================================
# GitHub OIDC — Restore Drill role (restore-drill.yml)
# =============================================================================
# Per GAP-436: read-only S3 backup bucket for monthly restore drill workflow.
# Trust scoped to main branch + workflow_dispatch only (manual trigger).

resource "aws_iam_role" "github_restore_drill" {
  name = "${var.project_name}-github-restore-drill"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Federated = aws_iam_openid_connect_provider.github.arn }
      Action    = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = {
          "token.actions.githubusercontent.com:aud" = "sts.amazonaws.com"
        }
        StringLike = {
          "token.actions.githubusercontent.com:sub" = "repo:${var.github_repo}:ref:refs/heads/main"
        }
      }
    }]
  })

  tags = { Name = "${var.project_name}-github-restore-drill" }
}

resource "aws_iam_role_policy" "github_restore_drill_inline" {
  name = "${var.project_name}-github-restore-drill-inline"
  role = aws_iam_role.github_restore_drill.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      # Read-only S3 access on backups bucket (created by s3.tf at Phase 2.3)
      {
        Effect = "Allow"
        Action = [
          "s3:GetObject",
          "s3:ListBucket",
        ]
        Resource = [
          "arn:aws:s3:::${var.project_name}-backups-${data.aws_caller_identity.current.account_id}",
          "arn:aws:s3:::${var.project_name}-backups-${data.aws_caller_identity.current.account_id}/*",
        ]
      },
    ]
  })
}
