# =============================================================================
# CloudWatch P0 Alarms — Wave beta-prep-1 Bucket C item 2 (closes GAP-144 carry)
# =============================================================================
# Phase 1 BETA P0 alarm coverage extending existing SNS topic
# `aws_sns_topic.production_alerts` (production-alerts.tf) to cover the P0
# incident classes user identified in beta-prep-1 plan §3 Bucket C item 2:
#
#   1. RDS CPU > 80% sustained 5min   — RDS exhaustion early signal
#   2. EC2 disk > 90% (kh_backend)    — log file accumulation / image bloat
#   3. EC2 disk > 90% (kc_app)        — same scope, kc_app instance
#   4. EC2 status check failure (kh)  — system-level failure (instance hardware)
#   5. EC2 status check failure (kc)  — same scope, kc_app
#   6. Nginx 5xx rate spike           — ALB substitute (Wave aws-restore-1
#                                       eliminated ALB; nginx access log
#                                       metric filter is the closest proxy).
#   7. Backup failure / DLQ non-empty — outbox dispatcher backlog signal
#   8. CloudTrail root user login     — security finding P0 escalation
#
# Note on ALB: per beta-prep-1 plan §3 Bucket C item 2 — ALB was eliminated
# Wave `aws-restore-1`. Pre-existing nginx access log CloudWatch metric filter
# emits `Nginx5xxCount` to namespace `KiteHub/Nginx`. If filter not yet
# wired post-restore, alarm stays INSUFFICIENT_DATA — useful Phase 1 BETA
# observability gap signal until filter shipped.
#
# Subscribers: inherits `aws_sns_topic.production_alerts` subscriptions
# (support@kitehub.me + vannkite@outlook.com per production-alerts.tf).
# Adding alarms to this SNS topic = automatic email delivery; no separate
# topic + subscription needed (eliminates double-subscribe friction).
#
# Pre-mutation audit: see `documents/04-quality/audits/aws-verification/
# 2026-05-26-wave-beta-prep-1-bucket-c-pre-apply.md` (paired same PR).
# =============================================================================

# ----------------- Alarm 1: RDS CPU > 80% sustained 5min -----------------
resource "aws_cloudwatch_metric_alarm" "rds_cpu_high" {
  alarm_name          = "${var.project_name}-rds-cpu-high"
  alarm_description   = "RDS kitehub-postgres CPU > 80pct sustained 5min - exhaustion early signal. Action: check pg_stat_activity for slow queries; consider instance upsize t3.micro -> t3.small."
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "CPUUtilization"
  namespace           = "AWS/RDS"
  period              = 300
  statistic           = "Average"
  threshold           = 80

  dimensions = {
    DBInstanceIdentifier = "kitehub-postgres"
  }

  alarm_actions             = [aws_sns_topic.production_alerts.arn]
  ok_actions                = [aws_sns_topic.production_alerts.arn]
  insufficient_data_actions = []
  treat_missing_data        = "notBreaching"

  tags = {
    Name = "${var.project_name}-rds-cpu-high"
    Role = "rds-monitoring-p0"
    Gap  = "GAP-144"
  }
}

# ----------------- Alarm 2: EC2 disk > 90% (kh_backend) -----------------
# CWAgent disk plugin emits `disk_used_percent` with InstanceId + path
# dimensions. Threshold 90pct = ~12GB used on 30GB root volume (gp3 baseline).
resource "aws_cloudwatch_metric_alarm" "kh_backend_disk_high" {
  alarm_name          = "${var.project_name}-kh-backend-disk-high"
  alarm_description   = "EC2 kh-backend disk usage > 90pct sustained 10min - log/image accumulation. Action: SSH, docker system prune; check /var/log size; rotate logs; consider EBS expand 30GB -> 50GB."
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "disk_used_percent"
  namespace           = "CWAgent"
  period              = 300
  statistic           = "Average"
  threshold           = 90

  dimensions = {
    InstanceId = aws_instance.kh_backend.id
    path       = "/"
    fstype     = "xfs"
  }

  alarm_actions             = [aws_sns_topic.production_alerts.arn]
  ok_actions                = [aws_sns_topic.production_alerts.arn]
  insufficient_data_actions = []
  treat_missing_data        = "notBreaching"

  tags = {
    Name = "${var.project_name}-kh-backend-disk-high"
    Role = "ec2-disk-monitoring-p0"
    Gap  = "GAP-144"
  }
}

# ----------------- Alarm 3: EC2 disk > 90% (kc_app) -----------------
resource "aws_cloudwatch_metric_alarm" "kc_app_disk_high" {
  alarm_name          = "${var.project_name}-kc-app-disk-high"
  alarm_description   = "EC2 kc-app disk usage > 90pct sustained 10min - log/image accumulation. Action: SSH, docker system prune; check /var/log size; rotate logs; consider EBS expand 30GB -> 50GB."
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "disk_used_percent"
  namespace           = "CWAgent"
  period              = 300
  statistic           = "Average"
  threshold           = 90

  dimensions = {
    InstanceId = aws_instance.kc_app.id
    path       = "/"
    fstype     = "xfs"
  }

  alarm_actions             = [aws_sns_topic.production_alerts.arn]
  ok_actions                = [aws_sns_topic.production_alerts.arn]
  insufficient_data_actions = []
  treat_missing_data        = "notBreaching"

  tags = {
    Name = "${var.project_name}-kc-app-disk-high"
    Role = "ec2-disk-monitoring-p0"
    Gap  = "GAP-144"
  }
}

# ----------------- Alarm 4: EC2 system status check failure (kh_backend) -----------------
# AWS/EC2 namespace StatusCheckFailed_System = AWS-side infrastructure failure
# (host hardware, network reachability). Auto-recovery typically fires but
# alert tells solo dev to acknowledge + verify.
resource "aws_cloudwatch_metric_alarm" "kh_backend_status_check_failed" {
  alarm_name          = "${var.project_name}-kh-backend-status-check-failed"
  alarm_description   = "EC2 kh-backend system status check FAILED - AWS hardware/network failure. Auto-recovery typically fires within 5min. Action: verify instance is back via aws ec2 describe-instances; check CloudTrail for auto-recovery event."
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "StatusCheckFailed_System"
  namespace           = "AWS/EC2"
  period              = 60
  statistic           = "Maximum"
  threshold           = 0

  dimensions = {
    InstanceId = aws_instance.kh_backend.id
  }

  alarm_actions             = [aws_sns_topic.production_alerts.arn]
  ok_actions                = [aws_sns_topic.production_alerts.arn]
  insufficient_data_actions = []
  treat_missing_data        = "notBreaching"

  tags = {
    Name = "${var.project_name}-kh-backend-status-check-failed"
    Role = "ec2-system-monitoring-p0"
    Gap  = "GAP-144"
  }
}

# ----------------- Alarm 5: EC2 system status check failure (kc_app) -----------------
resource "aws_cloudwatch_metric_alarm" "kc_app_status_check_failed" {
  alarm_name          = "${var.project_name}-kc-app-status-check-failed"
  alarm_description   = "EC2 kc-app system status check FAILED - AWS hardware/network failure. Auto-recovery typically fires within 5min. Action: verify instance back via aws ec2 describe-instances; check CloudTrail for auto-recovery event."
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "StatusCheckFailed_System"
  namespace           = "AWS/EC2"
  period              = 60
  statistic           = "Maximum"
  threshold           = 0

  dimensions = {
    InstanceId = aws_instance.kc_app.id
  }

  alarm_actions             = [aws_sns_topic.production_alerts.arn]
  ok_actions                = [aws_sns_topic.production_alerts.arn]
  insufficient_data_actions = []
  treat_missing_data        = "notBreaching"

  tags = {
    Name = "${var.project_name}-kc-app-status-check-failed"
    Role = "ec2-system-monitoring-p0"
    Gap  = "GAP-144"
  }
}

# ----------------- Alarm 6: Nginx 5xx rate spike (ALB substitute) -----------------
# Wave aws-restore-1 eliminated ALB. Nginx access logs piped to CloudWatch
# Logs (log group `/kite/nginx/access`); metric filter `Nginx5xxCount`
# extracts `status >= 500` events to namespace `KiteHub/Nginx`.
#
# If metric filter not yet wired post Wave aws-restore-1, alarm stays
# INSUFFICIENT_DATA - itself a useful Phase 1 BETA observability gap signal
# until log-pipeline shipped (track follow-up gap).
#
# Threshold: > 10 5xx responses in 5min - moderate burst tolerance for
# Phase 1 BETA traffic baseline; tune after first week of beta cohort live.
resource "aws_cloudwatch_metric_alarm" "nginx_5xx_rate_high" {
  alarm_name          = "${var.project_name}-nginx-5xx-rate-high"
  alarm_description   = "Nginx 5xx rate > 10 events / 5min - backend error spike (ALB substitute post Wave aws-restore-1). Action: SSH check upstream Java service logs; verify DB connectivity; check Java OOM."
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "Nginx5xxCount"
  namespace           = "KiteHub/Nginx"
  period              = 300
  statistic           = "Sum"
  threshold           = 10

  alarm_actions             = [aws_sns_topic.production_alerts.arn]
  ok_actions                = [aws_sns_topic.production_alerts.arn]
  insufficient_data_actions = []
  treat_missing_data        = "notBreaching"

  tags = {
    Name = "${var.project_name}-nginx-5xx-rate-high"
    Role = "edge-error-monitoring-p0"
    Gap  = "GAP-144"
  }
}

# ----------------- Alarm 7: Outbox DLQ non-empty -----------------
# Outbox dispatcher (Wave 91 Bucket C ship) emits message to RabbitMQ.
# Dead-letter queue `kitehub.outbox.dlq` accumulates failed dispatches
# (retry exhausted). DLQ depth > 0 = stuck messages requiring manual
# inspection per `pre-handoff-self-test-completeness.md` §2.9 row (d).
#
# Metric source: RabbitMQ Prometheus exporter emits queue depth to
# namespace `KiteHub/RabbitMQ` dimension `Queue`. If exporter not yet
# scraped by CWAgent, alarm INSUFFICIENT_DATA (gap signal).
resource "aws_cloudwatch_metric_alarm" "outbox_dlq_non_empty" {
  alarm_name          = "${var.project_name}-outbox-dlq-non-empty"
  alarm_description   = "RabbitMQ outbox DLQ depth > 0 - dispatcher retries exhausted. Action: SSH check rabbitmqctl list_queues; inspect message payload; manual replay or discard."
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "QueueDepth"
  namespace           = "KiteHub/RabbitMQ"
  period              = 300
  statistic           = "Maximum"
  threshold           = 0

  dimensions = {
    Queue = "kitehub.outbox.dlq"
  }

  alarm_actions             = [aws_sns_topic.production_alerts.arn]
  ok_actions                = [aws_sns_topic.production_alerts.arn]
  insufficient_data_actions = []
  treat_missing_data        = "notBreaching"

  tags = {
    Name = "${var.project_name}-outbox-dlq-non-empty"
    Role = "async-job-monitoring-p0"
    Gap  = "GAP-144"
  }
}

# ----------------- Alarm 8: CloudTrail root user login -----------------
# Root user login = high-severity security finding per
# `pre-launch-infra-hardening-checklist.md` §2.5 + AWS Well-Architected
# Security Pillar. CloudTrail metric filter `RootUserLogin` extracts
# events where `userIdentity.type = Root` AND eventName matches
# `ConsoleLogin` to namespace `KiteHub/Security`.
#
# Pre-existing per cloudtrail-metric-filters.tf if filter wired; if not,
# alarm INSUFFICIENT_DATA (track follow-up gap to wire filter).
resource "aws_cloudwatch_metric_alarm" "cloudtrail_root_login" {
  alarm_name          = "${var.project_name}-cloudtrail-root-login"
  alarm_description   = "CloudTrail detected root user console login - P0 security finding. Action: verify if intentional (rotate billing alarm config etc.); if NOT intentional, rotate root credentials immediately per pre-launch-secrets-hardening-checklist.md."
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "RootUserLogin"
  namespace           = "KiteHub/Security"
  period              = 300
  statistic           = "Sum"
  threshold           = 0

  alarm_actions             = [aws_sns_topic.production_alerts.arn]
  ok_actions                = [aws_sns_topic.production_alerts.arn]
  insufficient_data_actions = []
  treat_missing_data        = "notBreaching"

  tags = {
    Name = "${var.project_name}-cloudtrail-root-login"
    Role = "security-monitoring-p0"
    Gap  = "GAP-144"
  }
}

# ----------------- Outputs -----------------
output "p0_alarm_count" {
  description = "Count of Wave beta-prep-1 Bucket C P0 alarms (excluding pre-existing rds_storage_low)"
  value       = 8
}
