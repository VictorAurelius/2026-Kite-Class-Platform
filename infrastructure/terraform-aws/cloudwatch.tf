# =============================================================================
# CloudWatch Alarms — OOM safety net for right-sized EC2 (GAP-447)
# =============================================================================
# Phase 1 BETA right-size m7i-flex.large 8GB → t3.medium 4GB cuts EC2 cost
# ~$60/mo but reduces RAM headroom from ~5GB to ~800MB (KH) / ~1.5GB (KC).
# This file ships the OOM safety net that BẮT BUỘC trước khi downsize:
#
#   1. SNS topic memory_alerts (email subscription vannkite@outlook.com)
#   2. MemoryUtilization >85% alarm per EC2 (5 min × 2 datapoints)
#
# Pre-requisite: CloudWatch agent must be installed on EC2 to emit
# MemoryUtilization metric (procstat / mem statsd plugin). The cloud-init
# `user_data` block in ec2.tf already installs amazon-cloudwatch-agent
# package; agent config payload + start command tracked in
# `documents/05-guides/deploy/right-size-stress-test.md` Phase 1 (manual
# step pre-downsize). Until agent is configured + started, alarms will
# stay in INSUFFICIENT_DATA — that itself is a useful signal.
# =============================================================================

# ----------------- SNS topic + email subscription -----------------
resource "aws_sns_topic" "memory_alerts" {
  name = "${var.project_name}-memory-alerts"

  tags = {
    Name    = "${var.project_name}-memory-alerts"
    Purpose = "OOM safety net for right-sized EC2 (GAP-447)"
  }
}

resource "aws_sns_topic_subscription" "memory_alerts_email" {
  topic_arn = aws_sns_topic.memory_alerts.arn
  protocol  = "email"
  endpoint  = "vannkite@outlook.com"
}

# ----------------- KH backend memory alarm -----------------
# CWAgent namespace is convention emitted by amazon-cloudwatch-agent
# when configured with mem.measurement = ["used_percent"]. Dimension
# `InstanceId` is auto-attached by the agent.
resource "aws_cloudwatch_metric_alarm" "kh_backend_memory_high" {
  alarm_name          = "${var.project_name}-kh-backend-memory-high"
  alarm_description   = "KH backend EC2 memory >85% - OOM risk after right-size t3.medium 4GB (GAP-447). Compose budget ~3.2GB; headroom ~800MB. Action: SSH inspect, JVM heap tune, or upsize t3.large."
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "mem_used_percent"
  namespace           = "CWAgent"
  period              = 300 # 5 min
  statistic           = "Average"
  threshold           = 85

  dimensions = {
    InstanceId = aws_instance.kh_backend.id
  }

  alarm_actions             = [aws_sns_topic.memory_alerts.arn]
  ok_actions                = [aws_sns_topic.memory_alerts.arn]
  insufficient_data_actions = [] # CW agent not configured = quiet, not noisy
  treat_missing_data        = "notBreaching"

  tags = {
    Name = "${var.project_name}-kh-backend-memory-high"
    Role = "kh-backend-cluster"
  }
}

# ----------------- KC app memory alarm -----------------
resource "aws_cloudwatch_metric_alarm" "kc_app_memory_high" {
  alarm_name          = "${var.project_name}-kc-app-memory-high"
  alarm_description   = "KC app EC2 memory >85% - OOM risk after right-size t3.medium 4GB (GAP-447). Compose budget ~2.5GB (backend-only post-Vercel-pivot); headroom ~1.5GB. Action: SSH inspect, JVM heap tune, or upsize t3.large."
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "mem_used_percent"
  namespace           = "CWAgent"
  period              = 300
  statistic           = "Average"
  threshold           = 85

  dimensions = {
    InstanceId = aws_instance.kc_app.id
  }

  alarm_actions             = [aws_sns_topic.memory_alerts.arn]
  ok_actions                = [aws_sns_topic.memory_alerts.arn]
  insufficient_data_actions = []
  treat_missing_data        = "notBreaching"

  tags = {
    Name = "${var.project_name}-kc-app-memory-high"
    Role = "kc-app"
  }
}

output "memory_alerts_sns_topic_arn" {
  description = "SNS topic ARN for EC2 memory alarms (subscribe additional endpoints if needed)"
  value       = aws_sns_topic.memory_alerts.arn
}
