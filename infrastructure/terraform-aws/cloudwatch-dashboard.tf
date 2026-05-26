# =============================================================================
# CloudWatch Dashboard — Phase 1 BETA overview (GAP-437 Phase 2)
# =============================================================================
# Single dashboard with widgets covering the live AWS resources for at-a-
# glance health view. CloudWatch dashboards: 3 free per account; this uses 1.
#
# Widgets:
#   1. EC2 CPU + Network (kh_backend + kc_app)
#   2. RDS CPU + DB connections + free storage (db.t3.micro)
#   3. ALB request count + 5xx errors + target health
#   4. S3 bucket sizes (assets + cloudtrail-logs)
#   5. CloudTrail event count (proxy via AWS API event metric — limited free)
#
# All widgets pull from default CloudWatch metrics (free for AWS-managed
# services). No metric filters or custom metrics in Phase 2 — those add cost.
# =============================================================================

resource "aws_cloudwatch_dashboard" "phase_1_overview" {
  dashboard_name = "${var.project_name}-phase-1-overview"

  # Wave 84 Bucket A extension (GAP-437 Phase 2-3):
  #   - Added kc_app_fe EC2 to CPU + Network widgets (3-instance fleet)
  #   - Added ALB target health widget + ALB latency widget
  #   - Added RDS IOPS widget (read + write)
  #   - Added Row 5/6: Security Events (4 metric filter counts from
  #     cloudtrail-metric-filters.tf - KiteHub/Security namespace)

  dashboard_body = jsonencode({
    widgets = [
      # -------------------- Row 1: EC2 --------------------
      {
        type   = "metric"
        x      = 0
        y      = 0
        width  = 12
        height = 6
        properties = {
          title  = "EC2 CPU Utilization"
          region = var.aws_region
          metrics = [
            ["AWS/EC2", "CPUUtilization", "InstanceId", aws_instance.kh_backend.id, { label = "KH Backend" }],
            ["AWS/EC2", "CPUUtilization", "InstanceId", aws_instance.kc_app.id, { label = "KC App" }],
            ["AWS/EC2", "CPUUtilization", "InstanceId", aws_instance.kc_app_fe.id, { label = "KC App FE" }],
          ]
          view    = "timeSeries"
          stacked = false
          period  = 300
          stat    = "Average"
          yAxis = {
            left = { min = 0, max = 100 }
          }
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 0
        width  = 12
        height = 6
        properties = {
          title  = "EC2 Network In/Out (bytes)"
          region = var.aws_region
          metrics = [
            ["AWS/EC2", "NetworkIn", "InstanceId", aws_instance.kh_backend.id, { label = "KH In" }],
            [".", "NetworkOut", ".", ".", { label = "KH Out" }],
            ["AWS/EC2", "NetworkIn", "InstanceId", aws_instance.kc_app.id, { label = "KC In" }],
            [".", "NetworkOut", ".", ".", { label = "KC Out" }],
            ["AWS/EC2", "NetworkIn", "InstanceId", aws_instance.kc_app_fe.id, { label = "KC FE In" }],
            [".", "NetworkOut", ".", ".", { label = "KC FE Out" }],
          ]
          view    = "timeSeries"
          stacked = false
          period  = 300
          stat    = "Sum"
        }
      },

      # -------------------- Row 2: RDS --------------------
      {
        type   = "metric"
        x      = 0
        y      = 6
        width  = 8
        height = 6
        properties = {
          title  = "RDS CPU Utilization"
          region = var.aws_region
          metrics = [
            ["AWS/RDS", "CPUUtilization", "DBInstanceIdentifier", aws_db_instance.main.id],
          ]
          view   = "timeSeries"
          period = 300
          stat   = "Average"
          yAxis = {
            left = { min = 0, max = 100 }
          }
        }
      },
      {
        type   = "metric"
        x      = 8
        y      = 6
        width  = 8
        height = 6
        properties = {
          title  = "RDS Database Connections"
          region = var.aws_region
          metrics = [
            ["AWS/RDS", "DatabaseConnections", "DBInstanceIdentifier", aws_db_instance.main.id],
          ]
          view   = "timeSeries"
          period = 300
          stat   = "Average"
        }
      },
      {
        type   = "metric"
        x      = 16
        y      = 6
        width  = 8
        height = 6
        properties = {
          title  = "RDS Free Storage Space (bytes)"
          region = var.aws_region
          metrics = [
            ["AWS/RDS", "FreeStorageSpace", "DBInstanceIdentifier", aws_db_instance.main.id],
          ]
          view      = "singleValue"
          period    = 300
          stat      = "Average"
          sparkline = true
        }
      },

      # -------------------- Row 3: ALB REMOVED (Wave aws-restore-1, 2026-05-26) --------------------
      # ALB widgets removed per architecture pivot — CF → kc_app_fe EIP → nginx
      # reverse-proxy → kh_backend private VPC. ALB metrics no longer applicable.
      # Replace with nginx access log analytics (CloudWatch Logs Insights) Phase 1.5+.

      # -------------------- Row 4: S3 + CloudTrail --------------------
      {
        type   = "metric"
        x      = 0
        y      = 18
        width  = 12
        height = 6
        properties = {
          title  = "S3 Bucket Sizes (bytes, daily)"
          region = var.aws_region
          metrics = [
            ["AWS/S3", "BucketSizeBytes", "BucketName", aws_s3_bucket.assets.id, "StorageType", "StandardStorage", { label = "Assets" }],
            ["AWS/S3", "BucketSizeBytes", "BucketName", aws_s3_bucket.cloudtrail_logs.id, "StorageType", "StandardStorage", { label = "CloudTrail Logs" }],
          ]
          view   = "timeSeries"
          period = 86400
          stat   = "Average"
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 18
        width  = 12
        height = 6
        properties = {
          title  = "S3 Object Count (daily)"
          region = var.aws_region
          metrics = [
            ["AWS/S3", "NumberOfObjects", "BucketName", aws_s3_bucket.assets.id, "StorageType", "AllStorageTypes", { label = "Assets" }],
            ["AWS/S3", "NumberOfObjects", "BucketName", aws_s3_bucket.cloudtrail_logs.id, "StorageType", "AllStorageTypes", { label = "CloudTrail Logs" }],
          ]
          view   = "timeSeries"
          period = 86400
          stat   = "Average"
        }
      },

      # -------------------- Row 5: ALB Target widgets REMOVED (Wave aws-restore-1, 2026-05-26) --------------------
      # ALB Target Health + Response Time widgets removed per architecture pivot.
      # Future replacement: nginx upstream timing logs + CloudWatch Logs Insights
      # query Phase 1.5+ (kc_app_fe nginx fe_proxy log format includes
      # $upstream_response_time + $request_time).
      {
        type   = "metric"
        x      = 16
        y      = 24
        width  = 8
        height = 6
        properties = {
          title  = "RDS IOPS (Read + Write)"
          region = var.aws_region
          metrics = [
            ["AWS/RDS", "ReadIOPS", "DBInstanceIdentifier", aws_db_instance.main.id, { label = "Read IOPS" }],
            [".", "WriteIOPS", ".", ".", { label = "Write IOPS" }],
          ]
          view   = "timeSeries"
          period = 300
          stat   = "Average"
        }
      },

      # -------------------- Row 6: Security Events (Wave 84 GAP-437 Phase 3) --------------------
      # Metric filters defined in cloudtrail-metric-filters.tf emit to namespace
      # KiteHub/Security. Dashboard surfaces counts side-by-side for at-a-glance
      # security posture view. Alarm thresholds defined in
      # cloudwatch-security-alarms.tf.
      {
        type   = "metric"
        x      = 0
        y      = 30
        width  = 6
        height = 6
        properties = {
          title  = "Failed IAM Auth (5m count)"
          region = var.aws_region
          metrics = [
            ["KiteHub/Security", "FailedIAMAuthCount", { label = "Failed IAM" }],
          ]
          view   = "timeSeries"
          period = 300
          stat   = "Sum"
        }
      },
      {
        type   = "metric"
        x      = 6
        y      = 30
        width  = 6
        height = 6
        properties = {
          title  = "Root Account Use (P0)"
          region = var.aws_region
          metrics = [
            ["KiteHub/Security", "RootAccountUseCount", { label = "Root use" }],
          ]
          view      = "singleValue"
          period    = 300
          stat      = "Sum"
          sparkline = true
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 30
        width  = 6
        height = 6
        properties = {
          title  = "Security Group Changes (5m count)"
          region = var.aws_region
          metrics = [
            ["KiteHub/Security", "SecurityGroupChangeCount", { label = "SG changes" }],
          ]
          view   = "timeSeries"
          period = 300
          stat   = "Sum"
        }
      },
      {
        type   = "metric"
        x      = 18
        y      = 30
        width  = 6
        height = 6
        properties = {
          title  = "Secrets Manager Access (5m count)"
          region = var.aws_region
          metrics = [
            ["KiteHub/Security", "SecretsManagerAccessCount", { label = "Secrets access" }],
          ]
          view   = "timeSeries"
          period = 300
          stat   = "Sum"
        }
      },
    ]
  })
}

output "cloudwatch_dashboard_url" {
  description = "CloudWatch dashboard console URL"
  value       = "https://${var.aws_region}.console.aws.amazon.com/cloudwatch/home?region=${var.aws_region}#dashboards:name=${aws_cloudwatch_dashboard.phase_1_overview.dashboard_name}"
}
