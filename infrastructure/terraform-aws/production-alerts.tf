# =============================================================================
# Production Alerts — SNS topic + RDS storage alarm (Wave 86 Bucket H)
# =============================================================================
# Phase 1 BETA AlertManager adaptation: original GAP-144 scope = Helm AlertManager
# Slack/PagerDuty/SMTP receivers. Phase 1 BETA reality = EC2 docker-compose (no
# EKS yet) → Helm AlertManager not deployed live.
#
# This module ships the SNS-direct path that closes the Phase 1 BETA "alerts go
# somewhere observable" outcome:
#   1. Dedicated `kitehub-production-alerts` SNS topic
#   2. Email subscriptions: support@kitehub.me + vannkite@outlook.com (backup)
#   3. CloudWatch alarm `RDSFreeStorageSpace < 5GB` (GAP-583 H-AC2)
#
# Helm AlertManager live-cluster delivery test remains DEFERRED until EKS
# platform deploy (Phase 1.5+). See GAP-144 Log + runbook
# `documents/05-guides/operations/runbooks/alertmanager-mock-fire-runbook.md`.
#
# Pre-mutation audit: `documents/04-quality/audits/aws-verification/
# 2026-05-16-wave-86-h-pre-apply-state.md`
# =============================================================================

# ----------------- SNS topic -----------------
resource "aws_sns_topic" "production_alerts" {
  name = "${var.project_name}-production-alerts"

  tags = {
    Name    = "${var.project_name}-production-alerts"
    Purpose = "Phase 1 BETA production-grade alerts (GAP-144 SNS-direct adaptation + GAP-583 RDS storage)"
  }
}

# ----------------- Email subscriptions -----------------
# Primary support inbox — operational visibility per Wave 86 H-AC4 + H-AC14 SLA doc
resource "aws_sns_topic_subscription" "production_alerts_support" {
  topic_arn = aws_sns_topic.production_alerts.arn
  protocol  = "email"
  endpoint  = "support@kitehub.me"
}

# Backup/personal — ensures no single-point-of-failure on email delivery
resource "aws_sns_topic_subscription" "production_alerts_personal" {
  topic_arn = aws_sns_topic.production_alerts.arn
  protocol  = "email"
  endpoint  = "vannkite@outlook.com"
}

# ----------------- RDS storage alarm (GAP-583 H-AC2) -----------------
# Threshold: free storage < 5GB. RDS allocated 20GB t3.micro gp3 → fires when
# ~15GB used (75% fill). Estimated reach: ~30 days at Phase 1 BETA traffic
# baseline per simulation-3axis audit cell 8. Action runbook:
# `documents/05-guides/operations/rds-storage-runbook.md`.
resource "aws_cloudwatch_metric_alarm" "rds_storage_low" {
  alarm_name          = "${var.project_name}-rds-storage-low"
  alarm_description   = "RDS kitehub-postgres free storage < 5GB. Threshold 75% fill on 20GB t3.micro gp3 (autoscale disabled cost saving). Action: see rds-storage-runbook.md - resize 20GB -> 30GB or enable storage autoscaling."
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = 2
  metric_name         = "FreeStorageSpace"
  namespace           = "AWS/RDS"
  period              = 300 # 5 min
  statistic           = "Average"
  threshold           = 5368709120 # 5 GB in bytes (5 * 1024^3)

  dimensions = {
    DBInstanceIdentifier = "kitehub-postgres"
  }

  alarm_actions             = [aws_sns_topic.production_alerts.arn]
  ok_actions                = [aws_sns_topic.production_alerts.arn]
  insufficient_data_actions = []
  treat_missing_data        = "notBreaching"

  tags = {
    Name = "${var.project_name}-rds-storage-low"
    Role = "rds-storage-monitoring"
    Gap  = "GAP-583"
  }
}

# ----------------- Outputs -----------------
output "production_alerts_sns_topic_arn" {
  description = "SNS topic ARN for production alerts (subscribe additional endpoints if needed)"
  value       = aws_sns_topic.production_alerts.arn
}
