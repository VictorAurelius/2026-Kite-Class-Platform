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

# Custom inline policy: SES send permissions (GAP-608 — Wave beta-readiness-5 Bucket B)
# Phase 1 BETA fix — kitehub-email SESEmailService uses SDK with EC2 instance profile
# credentials; without this policy every outbound email fails HTTP 500 with
# `AccessDenied: User ... is not authorized to perform ses:SendEmail`.
#
# Action scope: SendEmail (v1) + SendRawEmail (defensive — some SDK paths may
# fall back to raw send). SendTemplatedEmail + GetSendQuota added per gap proposal
# §Phase 1 (likely future-use for SES templates + quota monitoring).
#
# Resource scope: identity ARNs in ap-southeast-1 account 906286017800 + SES
# configuration sets. Wildcard `identity/*` acceptable Phase 1 BETA because:
#   - SES sandbox enforces verified-recipient check at API level
#   - Account is single-tenant production (no cross-tenant risk)
#   - Post-sandbox exit (GAP-NEW follow-up) can narrow to specific verified
#     identities if least-privilege audit demands it
resource "aws_iam_role_policy" "ec2_ses_send" {
  name = "${var.project_name}-ec2-ses-send"
  role = aws_iam_role.ec2_app.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "SesSendEmail"
        Effect = "Allow"
        Action = [
          "ses:SendEmail",
          "ses:SendRawEmail",
          "ses:SendTemplatedEmail",
          "ses:GetSendQuota",
        ]
        Resource = [
          "arn:aws:ses:${var.aws_region}:${data.aws_caller_identity.current.account_id}:identity/*",
          "arn:aws:ses:${var.aws_region}:${data.aws_caller_identity.current.account_id}:configuration-set/*",
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
# GitHub OIDC — Apply role (terraform-apply.yml workflow_dispatch)
# =============================================================================
# Per release-deploy-standard.md §9 (revised) + GAP-449: workflow_dispatch +
# confirm input + human-click = permitted; OIDC ephemeral creds preferred over
# local admin key. Trust scoped to GitHub Environment 'production' for stricter
# review-gate vs the read-only plan role.

resource "aws_iam_role" "github_terraform_apply" {
  name = "${var.project_name}-github-terraform-apply"

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
          # Restrict to GitHub Environment 'production' for stricter gate
          "token.actions.githubusercontent.com:sub" = "repo:${var.github_repo}:environment:production"
        }
      }
    }]
  })

  tags = { Name = "${var.project_name}-github-terraform-apply" }
}

resource "aws_iam_role_policy_attachment" "github_apply_poweruser" {
  role       = aws_iam_role.github_terraform_apply.name
  policy_arn = "arn:aws:iam::aws:policy/PowerUserAccess"
}

# IAM management perms (terraform manages IAM resources — PowerUserAccess excludes IAM)
resource "aws_iam_role_policy" "github_apply_iam_management" {
  name = "${var.project_name}-github-apply-iam"
  role = aws_iam_role.github_terraform_apply.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = [
        "iam:CreateRole", "iam:DeleteRole", "iam:GetRole", "iam:UpdateRole",
        "iam:UpdateAssumeRolePolicy",
        "iam:AttachRolePolicy", "iam:DetachRolePolicy", "iam:ListAttachedRolePolicies",
        "iam:CreatePolicy", "iam:DeletePolicy", "iam:GetPolicy", "iam:ListPolicies",
        "iam:CreatePolicyVersion", "iam:DeletePolicyVersion", "iam:ListPolicyVersions",
        "iam:GetPolicyVersion",
        "iam:PutRolePolicy", "iam:DeleteRolePolicy", "iam:GetRolePolicy", "iam:ListRolePolicies",
        "iam:PassRole", "iam:TagRole", "iam:UntagRole", "iam:ListRoleTags",
        "iam:CreateInstanceProfile", "iam:DeleteInstanceProfile", "iam:GetInstanceProfile",
        "iam:AddRoleToInstanceProfile", "iam:RemoveRoleFromInstanceProfile",
        "iam:ListInstanceProfilesForRole",
        # Wave 82 Bucket B fix — apply failed 2026-05-15 11:08 UTC at
        # aws_iam_instance_profile.kc_app_fe creation: default_tags require
        # iam:TagInstanceProfile not in the original policy. Parallel pattern
        # to TagRole/UntagRole/ListRoleTags above.
        "iam:TagInstanceProfile", "iam:UntagInstanceProfile", "iam:ListInstanceProfileTags",
        "iam:CreateOpenIDConnectProvider", "iam:DeleteOpenIDConnectProvider",
        "iam:GetOpenIDConnectProvider", "iam:UpdateOpenIDConnectProviderThumbprint",
        "iam:TagOpenIDConnectProvider", "iam:ListOpenIDConnectProviderTags"
      ]
      Resource = "*"
    }]
  })
}

# State backend access (S3 + DynamoDB) — same shape as plan role
resource "aws_iam_role_policy" "github_apply_state_access" {
  name = "${var.project_name}-github-apply-state"
  role = aws_iam_role.github_terraform_apply.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = ["s3:GetObject", "s3:PutObject", "s3:ListBucket"]
        Resource = [
          "arn:aws:s3:::${var.project_name}-terraform-state-${data.aws_caller_identity.current.account_id}",
          "arn:aws:s3:::${var.project_name}-terraform-state-${data.aws_caller_identity.current.account_id}/*",
        ]
      },
      {
        Effect   = "Allow"
        Action   = ["dynamodb:GetItem", "dynamodb:PutItem", "dynamodb:DeleteItem"]
        Resource = "arn:aws:dynamodb:${var.aws_region}:${data.aws_caller_identity.current.account_id}:table/${var.project_name}-terraform-locks"
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
            "aws:ResourceTag/Project" = "Kite"
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
      # EC2 describe (read-only) — needed for dynamic instance lookup by tag
      # in deploy-production.yml workflow (GAP-482 fix: survives EC2 replacement
      # via AMI bump). Read-only — no instance lifecycle perms.
      {
        Sid    = "Ec2DescribeForDeployLookup"
        Effect = "Allow"
        Action = [
          "ec2:DescribeInstances",
          "ec2:DescribeTags",
        ]
        Resource = "*"
      },
      # RDS describe (read-only) — needed for preflight job in
      # deploy-production.yml (GAP-493 Path B). Workflow fails fast if
      # kitehub-postgres is stopped (cost-saving scheduler.tf can stop RDS
      # off-hours) instead of letting containers crash-restart for 8 min.
      # RDS DescribeDBInstances does NOT support tag-based Condition scoping
      # on the action (per AWS docs); Resource="*" is the only valid form
      # for this verb. Read-only — no lifecycle perms (start/stop/modify).
      {
        Sid    = "RdsDescribeForPreflight"
        Effect = "Allow"
        Action = [
          "rds:DescribeDBInstances",
        ]
        Resource = "*"
      },
      # Secrets Manager read — production + staging secret prefixes.
      # GAP-482 retry #3: actual secrets named ${var.project_name}/${var.environment}/*
      # (= kitehub/production/* per secrets.tf), NOT hardcoded "kite/staging/*"
      # or "kite/prod/*" which never matched any real secret ARN.
      {
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue",
          "secretsmanager:DescribeSecret",
        ]
        Resource = [
          "arn:aws:secretsmanager:${var.aws_region}:${data.aws_caller_identity.current.account_id}:secret:${var.project_name}/${var.environment}/*",
          "arn:aws:secretsmanager:${var.aws_region}:${data.aws_caller_identity.current.account_id}:secret:${var.project_name}/staging/*",
        ]
      },
      # ALB target health read — needed by Poll step (GAP-498 redesign).
      # Replaces SSM Status field polling (false-InProgress 8min false-failure)
      # with independent ALB target health + smoke verification. Read-only.
      {
        Sid    = "AlbTargetHealthForDeployPoll"
        Effect = "Allow"
        Action = [
          "elasticloadbalancing:DescribeTargetGroups",
          "elasticloadbalancing:DescribeTargetHealth",
        ]
        Resource = "*"
      },
      # CloudWatch Logs read on SSM deploy log group (GAP-491 Phase 2).
      # deploy-production.yml poll loop calls filter-log-events every 10s
      # to interleave live stdout/stderr with status polling — closes the
      # 2026-05-12 visibility gap (SSM Failed at 7s shown as InProgress
      # for 15 polls before noticing).
      {
        Sid    = "CloudWatchLogsReadDeployStream"
        Effect = "Allow"
        Action = [
          "logs:FilterLogEvents",
          "logs:DescribeLogStreams",
        ]
        Resource = [
          "arn:aws:logs:${var.aws_region}:${data.aws_caller_identity.current.account_id}:log-group:/aws/ssm/kite-deploy",
          "arn:aws:logs:${var.aws_region}:${data.aws_caller_identity.current.account_id}:log-group:/aws/ssm/kite-deploy:*",
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

# =============================================================================
# GitHub OIDC — Tier 3 cutover role (.github/workflows/tier-3-cutover.yml)
# =============================================================================
# Per GAP-459 + release-deploy-standard.md §9 (workflow_dispatch + confirm-input
# narrow OIDC role). Scoped to operational state-changes for Tier 3 cutover:
#   - EC2 lifecycle: Start/Stop/DescribeInstances
#   - RDS lifecycle: Start/Stop/DescribeDBInstances
#   - ACM:           ImportCertificate, ListCertificates, DescribeCertificate
#   - ELBv2 listener: Create/Modify/Delete, AddListenerCertificates, Describe*
#
# NO IAM management (those are in github_terraform_apply role). NO write to S3
# beyond logs. Trust: GitHub Environment 'production' only — same gate as
# terraform-apply role (requires environment protection rules in repo settings).

resource "aws_iam_role" "github_tier_3_cutover" {
  name = "${var.project_name}-github-tier-3-cutover"

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
          "token.actions.githubusercontent.com:sub" = "repo:${var.github_repo}:environment:production"
        }
      }
    }]
  })

  tags = { Name = "${var.project_name}-github-tier-3-cutover" }
}

resource "aws_iam_role_policy" "github_tier_3_cutover_inline" {
  name = "${var.project_name}-github-tier-3-cutover-inline"
  role = aws_iam_role.github_tier_3_cutover.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      # EC2 lifecycle — Start/Stop/Describe scoped to Project=kitehub-tagged instances
      {
        Sid    = "Ec2Lifecycle"
        Effect = "Allow"
        Action = [
          "ec2:StartInstances",
          "ec2:StopInstances",
        ]
        Resource = "*"
        Condition = {
          StringEquals = {
            "aws:ResourceTag/Project" = "Kite"
          }
        }
      },
      {
        Sid    = "Ec2Describe"
        Effect = "Allow"
        Action = [
          "ec2:DescribeInstances",
          "ec2:DescribeInstanceStatus",
        ]
        Resource = "*"
      },
      # RDS lifecycle — Start/Stop/Describe (RDS doesn't support tag-based condition for Start/Stop)
      {
        Sid    = "RdsLifecycle"
        Effect = "Allow"
        Action = [
          "rds:StartDBInstance",
          "rds:StopDBInstance",
          "rds:DescribeDBInstances",
        ]
        Resource = "*"
      },
      # ACM — Import + Describe (limited to specific account)
      {
        Sid    = "AcmCertManagement"
        Effect = "Allow"
        Action = [
          "acm:ImportCertificate",
          "acm:ListCertificates",
          "acm:DescribeCertificate",
          "acm:GetCertificate",
          "acm:AddTagsToCertificate",
        ]
        Resource = "*"
      },
      # ELBv2 — Listener + cert binding for ALB HTTPS
      {
        Sid    = "ElbV2ListenerOps"
        Effect = "Allow"
        Action = [
          "elasticloadbalancing:DescribeLoadBalancers",
          "elasticloadbalancing:DescribeListeners",
          "elasticloadbalancing:DescribeListenerCertificates",
          "elasticloadbalancing:DescribeTargetGroups",
          "elasticloadbalancing:DescribeTargetHealth",
          "elasticloadbalancing:CreateListener",
          "elasticloadbalancing:ModifyListener",
          "elasticloadbalancing:AddListenerCertificates",
          "elasticloadbalancing:RemoveListenerCertificates",
        ]
        Resource = "*"
      },
      # CloudWatch logs (for workflow output debugging if needed)
      {
        Sid    = "LogsRead"
        Effect = "Allow"
        Action = [
          "logs:DescribeLogGroups",
          "logs:DescribeLogStreams",
          "logs:GetLogEvents",
        ]
        Resource = "*"
      },
    ]
  })
}

output "github_tier_3_cutover_role_arn" {
  description = "ARN of GitHub OIDC role for tier-3-cutover.yml workflow_dispatch"
  value       = aws_iam_role.github_tier_3_cutover.arn
}

# =============================================================================
# GitHub OIDC — Rollback role (.github/workflows/rollback.yml)
# =============================================================================
# Per GAP-477 (Wave 63 Bucket A) + release-deploy-standard.md §3.1 / §4.3 +
# agent-aws-access.md §4.3 (workflow_dispatch carve-out with confirm-input gate).
#
# Stack: EC2 + SSM RunCommand pattern (mirrors github_deploy role). Phase 1 BETA
# is EC2-based, NOT ECS — verified via state-check 2026-05-11:
#   grep -rnE "aws_ecs|aws_ec2_instance" infrastructure/terraform-aws/*.tf
#   → only EC2 resources found (ec2.tf), no aws_ecs_* anywhere.
#
# Least-priv vs github_deploy: rollback role has the same SSM + ECR-pull scope
# but adds CloudWatch PutMetricData (TTR metric per release-deploy-standard.md
# §4.3) and is trust-scoped to GitHub Environment 'production' for stricter
# reviewer-gate (matches github_terraform_apply pattern). NO IAM management,
# NO ALB modify, NO Secrets write — purely image-rollback + redeploy.

resource "aws_iam_role" "github_rollback" {
  name = "${var.project_name}-github-rollback"

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
          # Restrict to GitHub Environment 'production' — matches apply role
          # pattern. Requires repo settings: Environments → production →
          # Required reviewers + Deployment branches=main.
          "token.actions.githubusercontent.com:sub" = "repo:${var.github_repo}:environment:production"
        }
      }
    }]
  })

  tags = { Name = "${var.project_name}-github-rollback" }
}

resource "aws_iam_role_policy" "github_rollback_inline" {
  name = "${var.project_name}-github-rollback-inline"
  role = aws_iam_role.github_rollback.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      # ECR — read + retag images (rollback retags a previous SHA's image to :latest)
      {
        Sid    = "EcrRead"
        Effect = "Allow"
        Action = [
          "ecr:GetAuthorizationToken",
          "ecr:BatchCheckLayerAvailability",
          "ecr:BatchGetImage",
          "ecr:GetDownloadUrlForLayer",
          "ecr:DescribeImages",
          "ecr:ListImages",
        ]
        Resource = "*"
      },
      # ECR retag scoped to kite/ namespace (matches github_ecr_push pattern)
      {
        Sid    = "EcrRetag"
        Effect = "Allow"
        Action = [
          "ecr:PutImage",
        ]
        Resource = "arn:aws:ecr:${var.aws_region}:${data.aws_caller_identity.current.account_id}:repository/kite/*"
      },
      # SSM RunCommand on instances tagged Project=kitehub (mirrors github_deploy
      # pattern — Phase 1 BETA EC2 stack uses SSM to pull-and-restart containers)
      {
        Sid    = "SsmRollback"
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
            "aws:ResourceTag/Project" = "Kite"
          }
        }
      },
      {
        Sid      = "SsmDocument"
        Effect   = "Allow"
        Action   = "ssm:SendCommand"
        Resource = "arn:aws:ssm:${var.aws_region}::document/AWS-RunShellScript"
      },
      # EC2 describe (read-only) — needed to verify target instances post-rollback
      {
        Sid    = "Ec2DescribeRead"
        Effect = "Allow"
        Action = [
          "ec2:DescribeInstances",
          "ec2:DescribeInstanceStatus",
        ]
        Resource = "*"
      },
      # CloudWatch PutMetricData scoped to KiteHub/Rollback namespace (TTR metric
      # per release-deploy-standard.md §4.3 DORA baseline measurement)
      {
        Sid      = "CloudWatchRollbackMetrics"
        Effect   = "Allow"
        Action   = "cloudwatch:PutMetricData"
        Resource = "*"
        Condition = {
          StringEquals = {
            "cloudwatch:namespace" = "KiteHub/Rollback"
          }
        }
      },
    ]
  })
}

output "github_rollback_role_arn" {
  description = "ARN of GitHub OIDC role for rollback.yml workflow_dispatch (GAP-477)"
  value       = aws_iam_role.github_rollback.arn
}

# =============================================================================
# GitHub OIDC - Cloudflare DNS apex cutover role (cloudflare-apex-cutover.yml)
# =============================================================================
# Per release-deploy-standard.md §9 + agent-aws-access.md §4.3 (workflow_dispatch
# carve-out) + pre-launch-infra-hardening-checklist.md §2.5 (least-priv).
#
# Stack: workflow fetches CF API token from Secrets Manager, optionally reads
# kc-app-fe EIP via ec2:DescribeAddresses, calls Cloudflare REST API to flip
# apex A record. NO terraform apply, NO AWS mutation. Trust scoped to
# Environment production for reviewer-approval gate.
#
# Cross-reference matrix per pre-mutation-state-check.md §1.5:
#   secretsmanager:GetSecretValue -> kitehub/production/cloudflare-api-token-*
#       (resource pattern matches aws_secretsmanager_secret.placeholders key)
#   ec2:DescribeAddresses          -> all (no tag-condition support on read)
#   ec2:DescribeInstances          -> all (Tier 1 read-only per agent-aws-access)

resource "aws_iam_role" "github_cloudflare_cutover" {
  name = "${var.project_name}-github-cloudflare-cutover"

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
          "token.actions.githubusercontent.com:sub" = "repo:${var.github_repo}:environment:production"
        }
      }
    }]
  })

  tags = { Name = "${var.project_name}-github-cloudflare-cutover" }
}

resource "aws_iam_role_policy" "github_cloudflare_cutover_inline" {
  name = "${var.project_name}-github-cloudflare-cutover-inline"
  role = aws_iam_role.github_cloudflare_cutover.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      # Read CF API token from Secrets Manager (scoped to single secret ARN)
      {
        Sid      = "ReadCloudflareToken"
        Effect   = "Allow"
        Action   = "secretsmanager:GetSecretValue"
        Resource = aws_secretsmanager_secret.placeholders["cloudflare-api-token"].arn
      },
      # Read EIP allocation (lookup public IP for apex A record content)
      {
        Sid    = "DescribeEipAndInstance"
        Effect = "Allow"
        Action = [
          "ec2:DescribeAddresses",
          "ec2:DescribeInstances",
        ]
        Resource = "*"
      },
    ]
  })
}

output "github_cloudflare_cutover_role_arn" {
  description = "ARN of GitHub OIDC role for cloudflare-apex-cutover.yml workflow_dispatch"
  value       = aws_iam_role.github_cloudflare_cutover.arn
}
