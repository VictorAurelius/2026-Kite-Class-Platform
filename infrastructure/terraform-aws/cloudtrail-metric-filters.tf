# =============================================================================
# CloudTrail Metric Filters - security signal extraction (GAP-437 Phase 2-3)
# =============================================================================
# Wave 84 Bucket A: extract 4 high-signal security events from CloudTrail logs
# -> CloudWatch metrics -> alarms -> SNS. This file does NOT modify the trail
# itself (trail shipped Wave 81 - see cloudtrail.tf); only ADDS:
#
#   1. CloudWatch log group for CloudTrail event delivery (terraform-managed
#      so retention + tags consistent)
#   2. IAM role + policy for trail -> CloudWatch logs delivery
#   3. CloudTrail trail update: cloud_watch_logs_group_arn + role_arn (attached
#      in cloudtrail.tf via Wave 84 edit)
#   4. 4 metric filters: failed IAM auth, root account use, SG changes, secrets
#      access
#
# Cost: management events FREE (first copy); log group storage <100MB/month is
# approximately $0.03/mo within Free Tier 5GB CloudWatch Logs. SNS notifications:
# 1k email/mo free.
#
# Per .claude/rules/pre-mutation-state-check.md section 1.5 - IAM resources
# pre-apply cross-reference matrix documented in audit artifact
# documents/04-quality/audits/aws-verification/2026-05-15-wave-84-bucket-a-cloudtrail-observability-plan.md
# =============================================================================

# --- CloudWatch log group for CloudTrail event stream ---

resource "aws_cloudwatch_log_group" "cloudtrail_events" {
  name              = "/aws/cloudtrail/${var.project_name}-main"
  retention_in_days = 30

  tags = {
    Name    = "${var.project_name}-cloudtrail-events"
    Purpose = "CloudTrail to CloudWatch event delivery for metric filters GAP-437"
  }
}

# --- IAM role for CloudTrail -> CloudWatch logs delivery ---

resource "aws_iam_role" "cloudtrail_logs_delivery" {
  name        = "${var.project_name}-cloudtrail-cwl-role"
  description = "Allow CloudTrail to deliver events to CloudWatch Logs for metric filters GAP-437"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "cloudtrail.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })

  tags = {
    Name = "${var.project_name}-cloudtrail-cwl-role"
  }
}

resource "aws_iam_role_policy" "cloudtrail_logs_delivery" {
  name = "${var.project_name}-cloudtrail-cwl-policy"
  role = aws_iam_role.cloudtrail_logs_delivery.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "AWSCloudTrailCreateLogStream"
        Effect = "Allow"
        Action = [
          "logs:CreateLogStream",
          "logs:PutLogEvents",
        ]
        Resource = "${aws_cloudwatch_log_group.cloudtrail_events.arn}:log-stream:*"
      }
    ]
  })
}

# =============================================================================
# Metric filters - 4 high-signal security events
# =============================================================================
# Each filter parses CloudTrail JSON event records; matching events emit a
# count metric to namespace KiteHub/Security. Alarms (cloudwatch-security-alarms.tf)
# trigger when count >=1 over 5 minutes -> SNS security topic.
# =============================================================================

# --- Filter 1: Failed IAM authentication ---
# Captures Unauthorized API calls + AccessDenied - leading indicator of
# brute-force credential probe OR privilege misconfiguration.

resource "aws_cloudwatch_log_metric_filter" "failed_iam_auth" {
  name           = "${var.project_name}-failed-iam-auth"
  log_group_name = aws_cloudwatch_log_group.cloudtrail_events.name

  pattern = "{ ($.errorCode = \"*UnauthorizedOperation\") || ($.errorCode = \"AccessDenied*\") }"

  metric_transformation {
    name      = "FailedIAMAuthCount"
    namespace = "KiteHub/Security"
    value     = "1"
    unit      = "Count"
  }
}

# --- Filter 2: Root account use ---
# Captures any API call made by root identity (excluding AWS service events).
# Root use is anomalous post-bootstrap; must investigate immediately.

resource "aws_cloudwatch_log_metric_filter" "root_account_use" {
  name           = "${var.project_name}-root-account-use"
  log_group_name = aws_cloudwatch_log_group.cloudtrail_events.name

  pattern = "{ $.userIdentity.type = \"Root\" && $.userIdentity.invokedBy NOT EXISTS && $.eventType != \"AwsServiceEvent\" }"

  metric_transformation {
    name      = "RootAccountUseCount"
    namespace = "KiteHub/Security"
    value     = "1"
    unit      = "Count"
  }
}

# --- Filter 3: Security group changes ---
# Network surface modifications - authorize/revoke ingress, create/delete SG.
# High-signal for both legitimate ops (terraform apply touches SG) AND attack
# (attacker opens ingress to pivot). Tune threshold post-baseline.

resource "aws_cloudwatch_log_metric_filter" "sg_changes" {
  name           = "${var.project_name}-sg-changes"
  log_group_name = aws_cloudwatch_log_group.cloudtrail_events.name

  pattern = "{ ($.eventName = AuthorizeSecurityGroupIngress) || ($.eventName = RevokeSecurityGroupIngress) || ($.eventName = CreateSecurityGroup) || ($.eventName = DeleteSecurityGroup) }"

  metric_transformation {
    name      = "SecurityGroupChangeCount"
    namespace = "KiteHub/Security"
    value     = "1"
    unit      = "Count"
  }
}

# --- Filter 4: Secrets Manager access ---
# GetSecretValue + PutSecretValue capture both reads + writes. Read = JWT
# secret rotation cycle (legitimate) OR credential exfil (attack). Per
# agent-aws-access.md section 2.2, agent never calls GetSecretValue -> all
# reads are workload OR ops team OR attacker.

resource "aws_cloudwatch_log_metric_filter" "secrets_access" {
  name           = "${var.project_name}-secrets-access"
  log_group_name = aws_cloudwatch_log_group.cloudtrail_events.name

  pattern = "{ ($.eventName = GetSecretValue) || ($.eventName = PutSecretValue) }"

  metric_transformation {
    name      = "SecretsManagerAccessCount"
    namespace = "KiteHub/Security"
    value     = "1"
    unit      = "Count"
  }
}

# =============================================================================
# Outputs
# =============================================================================

output "cloudtrail_events_log_group" {
  description = "CloudWatch log group receiving CloudTrail events (input for metric filters)"
  value       = aws_cloudwatch_log_group.cloudtrail_events.name
}

output "cloudtrail_logs_delivery_role_arn" {
  description = "IAM role ARN granting CloudTrail permission to deliver events to CloudWatch Logs"
  value       = aws_iam_role.cloudtrail_logs_delivery.arn
}
