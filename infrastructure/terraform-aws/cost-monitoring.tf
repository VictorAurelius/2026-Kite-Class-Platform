# =============================================================================
# Cost Monitoring — EC2 right-sizing automation (GAP-414, Wave 84 Bucket G)
# =============================================================================
# Monthly cost report Lambda + per-EC2 low-CPU alarms flag downsize candidates.
#
# Architecture:
#   1. SNS topic `kitehub-cost-alerts` — destination for both alarms + Lambda digest
#   2. Per-EC2 CloudWatch alarm: avg CPU <=20% over 7 days → SNS notify
#   3. Lambda `kitehub-ec2-cost-report` (Python 3.12)
#      - Triggered by EventBridge cron `cron(0 8 1 * ? *)` (1st of month 08:00 UTC = 15:00 ICT)
#      - Calls Cost Explorer API (us-east-1 endpoint) for last-month EC2 cost breakdown
#      - Reads CloudWatch CPU/Memory metrics for 30 days
#      - Emits HTML digest to SNS
#
# Apply order: this file is additive (zero existing-resource modifications).
# Per `pre-mutation-state-check.md` §1.5 — Lambda IAM cross-reference matrix:
#   ce:GetCostAndUsage          → Cost Explorer  (Resource: *)
#   cloudwatch:GetMetricStatistics → CloudWatch   (Resource: *)
#   sns:Publish                 → kitehub-cost-alerts SNS topic
#   logs:CreateLogGroup/Stream/PutLogEvents → /aws/lambda/kitehub-ec2-cost-report
# =============================================================================

# ----------------- SNS topic cost-alerts -----------------
resource "aws_sns_topic" "cost_alerts" {
  name = "${var.project_name}-cost-alerts"

  tags = {
    Name    = "${var.project_name}-cost-alerts"
    Purpose = "EC2 right-sizing alarms + monthly cost digest GAP-414"
  }
}

resource "aws_sns_topic_subscription" "cost_alerts_email" {
  topic_arn = aws_sns_topic.cost_alerts.arn
  protocol  = "email"
  endpoint  = "vannkite@outlook.com"
}

# ----------------- Per-EC2 low-CPU alarms (downsize candidate signal) -----------------
# Period: 86400s (1 day) × 7 evaluation_periods = 7-day rolling window.
# Threshold: avg CPU <=20% → sustained low utilization → downsize candidate.
# treat_missing_data = notBreaching → instance stopped during window does NOT
# spuriously fire alarm.

resource "aws_cloudwatch_metric_alarm" "kh_backend_low_cpu" {
  alarm_name          = "${var.project_name}-kh-backend-low-cpu-7d"
  alarm_description   = "kh-backend EC2 sustained avg CPU <=20% over 7 days - downsize candidate (GAP-414). Review: SSM inspect compose workloads, run `bash scripts/aws/start-stack.sh` checks, then consider terraform var change t3.medium -> t3.small."
  comparison_operator = "LessThanOrEqualToThreshold"
  evaluation_periods  = 7
  metric_name         = "CPUUtilization"
  namespace           = "AWS/EC2"
  period              = 86400 # 1 day
  statistic           = "Average"
  threshold           = 20

  dimensions = {
    InstanceId = aws_instance.kh_backend.id
  }

  alarm_actions      = [aws_sns_topic.cost_alerts.arn]
  ok_actions         = [aws_sns_topic.cost_alerts.arn]
  treat_missing_data = "notBreaching"

  tags = {
    Name = "${var.project_name}-kh-backend-low-cpu-7d"
    Role = "kh-backend"
  }
}

resource "aws_cloudwatch_metric_alarm" "kc_app_low_cpu" {
  alarm_name          = "${var.project_name}-kc-app-low-cpu-7d"
  alarm_description   = "kc-app EC2 sustained avg CPU <=20% over 7 days - downsize candidate (GAP-414). Review per ec2-cost-review.md runbook."
  comparison_operator = "LessThanOrEqualToThreshold"
  evaluation_periods  = 7
  metric_name         = "CPUUtilization"
  namespace           = "AWS/EC2"
  period              = 86400
  statistic           = "Average"
  threshold           = 20

  dimensions = {
    InstanceId = aws_instance.kc_app.id
  }

  alarm_actions      = [aws_sns_topic.cost_alerts.arn]
  ok_actions         = [aws_sns_topic.cost_alerts.arn]
  treat_missing_data = "notBreaching"

  tags = {
    Name = "${var.project_name}-kc-app-low-cpu-7d"
    Role = "kc-app"
  }
}

resource "aws_cloudwatch_metric_alarm" "kc_app_fe_low_cpu" {
  alarm_name          = "${var.project_name}-kc-app-fe-low-cpu-7d"
  alarm_description   = "kc-app-fe EC2 sustained avg CPU <=20% over 7 days - downsize candidate (GAP-414). Review per ec2-cost-review.md runbook."
  comparison_operator = "LessThanOrEqualToThreshold"
  evaluation_periods  = 7
  metric_name         = "CPUUtilization"
  namespace           = "AWS/EC2"
  period              = 86400
  statistic           = "Average"
  threshold           = 20

  dimensions = {
    InstanceId = aws_instance.kc_app_fe.id
  }

  alarm_actions      = [aws_sns_topic.cost_alerts.arn]
  ok_actions         = [aws_sns_topic.cost_alerts.arn]
  treat_missing_data = "notBreaching"

  tags = {
    Name = "${var.project_name}-kc-app-fe-low-cpu-7d"
    Role = "kc-app-fe"
  }
}

# ----------------- Lambda IAM role (least-privilege) -----------------
data "aws_iam_policy_document" "ec2_cost_report_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["lambda.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "ec2_cost_report" {
  name               = "${var.project_name}-ec2-cost-report-role"
  assume_role_policy = data.aws_iam_policy_document.ec2_cost_report_assume.json

  tags = {
    Name    = "${var.project_name}-ec2-cost-report-role"
    Purpose = "Lambda exec role for monthly EC2 cost report GAP-414"
  }
}

# Least-privilege policy:
# - ce:GetCostAndUsage on * (Cost Explorer scope is account-wide; no ARN-level restriction available)
# - cloudwatch:GetMetricStatistics on * (CloudWatch metric scope is account-wide)
# - sns:Publish narrowed to the cost-alerts topic ARN
# - logs:* narrowed to the function's log group
data "aws_iam_policy_document" "ec2_cost_report" {
  statement {
    sid = "CostExplorerRead"
    actions = [
      "ce:GetCostAndUsage",
      "ce:GetCostAndUsageWithResources",
    ]
    resources = ["*"]
  }

  statement {
    sid = "CloudWatchMetricsRead"
    actions = [
      "cloudwatch:GetMetricStatistics",
      "cloudwatch:ListMetrics",
    ]
    resources = ["*"]
  }

  statement {
    sid       = "EC2DescribeForInstanceList"
    actions   = ["ec2:DescribeInstances"]
    resources = ["*"]
  }

  statement {
    sid       = "SNSPublishCostAlerts"
    actions   = ["sns:Publish"]
    resources = [aws_sns_topic.cost_alerts.arn]
  }

  statement {
    sid = "LambdaLogs"
    actions = [
      "logs:CreateLogGroup",
      "logs:CreateLogStream",
      "logs:PutLogEvents",
    ]
    resources = [
      "arn:aws:logs:${var.aws_region}:*:log-group:/aws/lambda/${var.project_name}-ec2-cost-report:*",
    ]
  }
}

resource "aws_iam_role_policy" "ec2_cost_report" {
  name   = "${var.project_name}-ec2-cost-report-policy"
  role   = aws_iam_role.ec2_cost_report.id
  policy = data.aws_iam_policy_document.ec2_cost_report.json
}

# ----------------- Lambda packaging -----------------
# Archive the Python source from lambdas/ec2-cost-report/. Source-driven hash
# means the Lambda updates only when handler.py changes (idempotent apply).
data "archive_file" "ec2_cost_report" {
  type        = "zip"
  source_dir  = "${path.module}/lambdas/ec2-cost-report"
  output_path = "${path.module}/lambdas/ec2-cost-report.zip"
  excludes    = ["__pycache__", "test_handler.py", ".pytest_cache"]
}

resource "aws_cloudwatch_log_group" "ec2_cost_report" {
  name              = "/aws/lambda/${var.project_name}-ec2-cost-report"
  retention_in_days = 30

  tags = {
    Name    = "${var.project_name}-ec2-cost-report-logs"
    Purpose = "Monthly cost report Lambda logs GAP-414"
  }
}

resource "aws_lambda_function" "ec2_cost_report" {
  function_name = "${var.project_name}-ec2-cost-report"
  description   = "Monthly EC2 cost + utilization digest -> SNS (GAP-414 Wave 84)"
  role          = aws_iam_role.ec2_cost_report.arn

  filename         = data.archive_file.ec2_cost_report.output_path
  source_code_hash = data.archive_file.ec2_cost_report.output_base64sha256

  runtime     = "python3.12"
  handler     = "handler.lambda_handler"
  timeout     = 120 # 2 min — Cost Explorer + CW Metrics across 3 EC2 instances
  memory_size = 256

  environment {
    variables = {
      SNS_TOPIC_ARN  = aws_sns_topic.cost_alerts.arn
      AWS_REGION_CE  = "us-east-1" # Cost Explorer endpoint pin per AWS docs
      PROJECT_NAME   = var.project_name
      ENVIRONMENT    = var.environment
      TARGET_TAG_KEY = "Project"
      TARGET_TAG_VAL = "Kite"
    }
  }

  depends_on = [
    aws_iam_role_policy.ec2_cost_report,
    aws_cloudwatch_log_group.ec2_cost_report,
  ]

  tags = {
    Name    = "${var.project_name}-ec2-cost-report"
    Purpose = "Monthly cost digest GAP-414"
  }
}

# ----------------- EventBridge schedule: 1st of each month, 08:00 UTC -----------------
resource "aws_cloudwatch_event_rule" "ec2_cost_report_monthly" {
  name                = "${var.project_name}-ec2-cost-report-monthly"
  description         = "Trigger ec2-cost-report Lambda on 1st of each month at 08:00 UTC (15:00 ICT)"
  schedule_expression = "cron(0 8 1 * ? *)"

  tags = {
    Name    = "${var.project_name}-ec2-cost-report-monthly"
    Purpose = "Monthly schedule GAP-414"
  }
}

resource "aws_cloudwatch_event_target" "ec2_cost_report" {
  rule      = aws_cloudwatch_event_rule.ec2_cost_report_monthly.name
  target_id = "ec2-cost-report-lambda"
  arn       = aws_lambda_function.ec2_cost_report.arn
}

resource "aws_lambda_permission" "allow_eventbridge" {
  statement_id  = "AllowEventBridgeInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.ec2_cost_report.function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.ec2_cost_report_monthly.arn
}

# ----------------- Outputs -----------------
output "cost_alerts_sns_topic_arn" {
  description = "SNS topic ARN for cost monitoring alarms + monthly digest"
  value       = aws_sns_topic.cost_alerts.arn
}

output "ec2_cost_report_lambda_arn" {
  description = "Lambda ARN for monthly EC2 cost report"
  value       = aws_lambda_function.ec2_cost_report.arn
}

output "ec2_cost_report_schedule" {
  description = "EventBridge cron schedule for monthly report"
  value       = aws_cloudwatch_event_rule.ec2_cost_report_monthly.schedule_expression
}
