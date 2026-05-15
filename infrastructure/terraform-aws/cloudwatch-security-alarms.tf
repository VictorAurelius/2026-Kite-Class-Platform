# =============================================================================
# CloudWatch Security Alarms + SNS - Wave 84 Bucket A (GAP-437 Phase 3)
# =============================================================================
# Wire 4 metric filters (cloudtrail-metric-filters.tf) -> 4 alarms -> 1 SNS
# topic kitehub-security-alerts with email subscription.
#
# Threshold strategy: >=1 event in 5-minute window triggers alarm. Tuning
# expected post-baseline (security_group changes will fire on every terraform
# apply -> expected noise; root_account_use should fire ZERO times
# post-bootstrap).
#
# Subscription: variable security_alert_email allows override per-env
# (default vannkite@outlook.com matching memory_alerts pattern in cloudwatch.tf).
# =============================================================================

variable "security_alert_email" {
  description = "Email address for CloudTrail security alerts (failed IAM auth, root use, SG changes, secrets access)"
  type        = string
  default     = "vannkite@outlook.com"
}

# --- SNS topic ---

resource "aws_sns_topic" "security_alerts" {
  name = "${var.project_name}-security-alerts"

  tags = {
    Name    = "${var.project_name}-security-alerts"
    Purpose = "CloudTrail metric-filter alerts GAP-437"
  }
}

resource "aws_sns_topic_subscription" "security_alerts_email" {
  topic_arn = aws_sns_topic.security_alerts.arn
  protocol  = "email"
  endpoint  = var.security_alert_email
}

# =============================================================================
# Alarms - 1 per metric filter
# =============================================================================

# --- Alarm 1: Failed IAM auth (P1 severity - credential probe signal) ---

resource "aws_cloudwatch_metric_alarm" "failed_iam_auth" {
  alarm_name          = "${var.project_name}-failed-iam-auth"
  alarm_description   = "Failed IAM authentication detected (UnauthorizedOperation OR AccessDenied). Investigate: credential leak probe, role misconfigured, OR legitimate dev typo. Source: CloudTrail metric filter."
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 1
  metric_name         = "FailedIAMAuthCount"
  namespace           = "KiteHub/Security"
  period              = 300 # 5 min window
  statistic           = "Sum"
  threshold           = 1
  treat_missing_data  = "notBreaching"

  alarm_actions = [aws_sns_topic.security_alerts.arn]
  ok_actions    = [] # silent OK - only alert on breach

  tags = {
    Name     = "${var.project_name}-failed-iam-auth"
    Severity = "P1"
  }
}

# --- Alarm 2: Root account use (P0 severity - should NEVER fire post-bootstrap) ---

resource "aws_cloudwatch_metric_alarm" "root_account_use" {
  alarm_name          = "${var.project_name}-root-account-use"
  alarm_description   = "Root account API call detected. Post-bootstrap (Wave 43+) this should fire ZERO times. If alarm triggers: assume credential compromise + rotate root password + audit IAM immediately."
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 1
  metric_name         = "RootAccountUseCount"
  namespace           = "KiteHub/Security"
  period              = 300
  statistic           = "Sum"
  threshold           = 1
  treat_missing_data  = "notBreaching"

  alarm_actions = [aws_sns_topic.security_alerts.arn]
  ok_actions    = []

  tags = {
    Name     = "${var.project_name}-root-account-use"
    Severity = "P0"
  }
}

# --- Alarm 3: Security group changes (P2 severity - expected noise during deploys) ---
# Threshold higher (>=5 in 5 min) to reduce noise from terraform apply touching
# multiple SG rules. Single SG change = legitimate ops; burst of SG changes
# outside deploy window = signal.

resource "aws_cloudwatch_metric_alarm" "sg_changes" {
  alarm_name          = "${var.project_name}-sg-changes-burst"
  alarm_description   = ">=5 security group modifications in 5 min - burst pattern indicating either large terraform apply OR attacker network surface manipulation. Cross-reference with recent deploy timeline."
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 1
  metric_name         = "SecurityGroupChangeCount"
  namespace           = "KiteHub/Security"
  period              = 300
  statistic           = "Sum"
  threshold           = 5
  treat_missing_data  = "notBreaching"

  alarm_actions = [aws_sns_topic.security_alerts.arn]
  ok_actions    = []

  tags = {
    Name     = "${var.project_name}-sg-changes-burst"
    Severity = "P2"
  }
}

# --- Alarm 4: Secrets Manager access (P2 severity - burst threshold) ---
# Normal workload: backend services fetch JWT/DB secrets once per pod start
# (~5-10 reads/hour). Burst >20 reads/5min = anomaly (mass exfil OR runaway loop).

resource "aws_cloudwatch_metric_alarm" "secrets_access_burst" {
  alarm_name          = "${var.project_name}-secrets-access-burst"
  alarm_description   = ">20 Secrets Manager GetSecretValue/PutSecretValue in 5 min - anomalous read pattern. Normal baseline ~5-10/hour from backend pod starts. Investigate caller identity via CloudTrail logs."
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "SecretsManagerAccessCount"
  namespace           = "KiteHub/Security"
  period              = 300
  statistic           = "Sum"
  threshold           = 20
  treat_missing_data  = "notBreaching"

  alarm_actions = [aws_sns_topic.security_alerts.arn]
  ok_actions    = []

  tags = {
    Name     = "${var.project_name}-secrets-access-burst"
    Severity = "P2"
  }
}

# =============================================================================
# Outputs
# =============================================================================

output "security_alerts_sns_topic_arn" {
  description = "SNS topic ARN for security alerts (subscribe additional endpoints if needed)"
  value       = aws_sns_topic.security_alerts.arn
}
