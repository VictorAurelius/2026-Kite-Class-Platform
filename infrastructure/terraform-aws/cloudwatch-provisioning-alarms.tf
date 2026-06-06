# =============================================================================
# Tenant Provisioning Alarms — Wave provisioning-1 Bucket D (GAP-952)
# =============================================================================
# Closes GAP-952: saga compensation failure + provisioning-stuck instances were
# only logged (BR-PROV-005 "logged but never rethrown") so admins never learned
# to clean up orphaned tenant instances. This file extracts two app-emitted
# alert signals from the kiteclass-core CloudWatch Logs stream -> metrics ->
# alarms -> existing SNS topic `aws_sns_topic.production_alerts`
# (production-alerts.tf, subscribers support@kitehub.me + vannkite@outlook.com).
#
# App-level vs IaC split (see PR body):
#   - App-level (kiteclass-core): TenantProvisioningSaga.compensate() + the
#     ProvisioningStuckSweep @Scheduled cron emit a Micrometer counter AND a
#     structured log line carrying a stable token. This stack has NO CloudWatch
#     meter registry, so the alarm is driven by the LOG TOKEN, not the Micrometer
#     metric (Micrometer serves /actuator + Prometheus only).
#   - IaC (this file): log-metric-filter parses the tokens into CloudWatch
#     metrics in namespace `KiteClass/Provisioning`, alarmed at >0.
#
# Alert tokens (keep verbatim — must match the Java constants):
#   - TENANT_PROVISIONING_COMPENSATION_FAILED
#       (TenantProvisioningSaga.ALERT_COMPENSATION_FAILED)
#   - TENANT_PROVISIONING_STUCK
#       (ProvisioningStuckSweep.ALERT_STUCK)
#
# Cost: log group storage <100MB/mo ~ within Free Tier 5GB CloudWatch Logs;
# SNS email 1k/mo free. Same cost envelope as cloudtrail-metric-filters.tf.
#
# LIVE-APPLY DEFERRED: the kiteclass-core CloudWatch agent must ship application
# logs to the log group below before these filters produce data. AWS account is
# in flux (GAP-612 history); `terraform import` may be required if the log group
# was auto-created by the CloudWatch agent. Until applied, alarms sit in
# INSUFFICIENT_DATA — itself a useful observability-gap signal. Follow-up: live
# verify + fault-injection (manual compensation fail) per GAP-952 AC #3.
# =============================================================================

# --- CloudWatch log group for kiteclass-core application logs ---
# Mirrors cloudtrail-metric-filters.tf pattern (terraform-managed group so
# retention + tags stay consistent). If the CloudWatch agent already created
# this group, run `terraform import aws_cloudwatch_log_group.kc_core_app <name>`
# before apply.
resource "aws_cloudwatch_log_group" "kc_core_app" {
  name              = "/kite/kiteclass-core/app"
  retention_in_days = 30

  tags = {
    Name    = "${var.project_name}-kc-core-app-logs"
    Purpose = "kiteclass-core app log stream for provisioning metric filters GAP-952"
  }
}

# =============================================================================
# Metric filter 1: saga compensation failure (instance stuck pre-FAILED)
# =============================================================================
resource "aws_cloudwatch_log_metric_filter" "provisioning_compensation_failed" {
  name           = "${var.project_name}-provisioning-compensation-failed"
  log_group_name = aws_cloudwatch_log_group.kc_core_app.name
  pattern        = "TENANT_PROVISIONING_COMPENSATION_FAILED"

  metric_transformation {
    name          = "tenant_provisioning_compensation_failed"
    namespace     = "KiteClass/Provisioning"
    value         = "1"
    default_value = "0"
    unit          = "Count"
  }
}

resource "aws_cloudwatch_metric_alarm" "provisioning_compensation_failed" {
  alarm_name          = "${var.project_name}-provisioning-compensation-failed"
  alarm_description   = "Tenant provisioning saga compensation FAILED (markFailed itself threw) - instance stuck pre-FAILED. Action: GET /api/v1/admin/tenants stuck list; POST retry-provisioning (GAP-953) or manual DB cleanup. See provisioning compensation in TenantProvisioningSaga + provisioning-stuck-sweep."
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "tenant_provisioning_compensation_failed"
  namespace           = "KiteClass/Provisioning"
  period              = 300
  statistic           = "Sum"
  threshold           = 0

  alarm_actions             = [aws_sns_topic.production_alerts.arn]
  ok_actions                = [aws_sns_topic.production_alerts.arn]
  insufficient_data_actions = []
  treat_missing_data        = "notBreaching"

  tags = {
    Name = "${var.project_name}-provisioning-compensation-failed"
    Role = "provisioning-observability-p0"
    Gap  = "GAP-952"
  }
}

# =============================================================================
# Metric filter 2: provisioning-stuck instance auto-failed by the sweep
# =============================================================================
resource "aws_cloudwatch_log_metric_filter" "provisioning_stuck" {
  name           = "${var.project_name}-provisioning-stuck"
  log_group_name = aws_cloudwatch_log_group.kc_core_app.name
  pattern        = "TENANT_PROVISIONING_STUCK"

  metric_transformation {
    name          = "tenant_provisioning_stuck"
    namespace     = "KiteClass/Provisioning"
    value         = "1"
    default_value = "0"
    unit          = "Count"
  }
}

resource "aws_cloudwatch_metric_alarm" "provisioning_stuck" {
  alarm_name          = "${var.project_name}-provisioning-stuck"
  alarm_description   = "Tenant provisioning instance stuck in INITIALIZING/GENERATING > threshold (default 10min) - the @Scheduled provisioning-stuck-sweep auto-marked it FAILED. Signals a saga that died before reaching DEPLOYED. Action: inspect failure_reason; admin retry (GAP-953)."
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "tenant_provisioning_stuck"
  namespace           = "KiteClass/Provisioning"
  period              = 300
  statistic           = "Sum"
  threshold           = 0

  alarm_actions             = [aws_sns_topic.production_alerts.arn]
  ok_actions                = [aws_sns_topic.production_alerts.arn]
  insufficient_data_actions = []
  treat_missing_data        = "notBreaching"

  tags = {
    Name = "${var.project_name}-provisioning-stuck"
    Role = "provisioning-observability-p1"
    Gap  = "GAP-952"
  }
}

output "provisioning_compensation_failed_alarm_arn" {
  description = "ARN of the tenant-provisioning compensation-failed alarm (GAP-952)"
  value       = aws_cloudwatch_metric_alarm.provisioning_compensation_failed.arn
}

output "provisioning_stuck_alarm_arn" {
  description = "ARN of the tenant-provisioning stuck-sweep alarm (GAP-952)"
  value       = aws_cloudwatch_metric_alarm.provisioning_stuck.arn
}
