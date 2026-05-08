# =============================================================================
# EventBridge Scheduler — Cost-saving stop/start EC2 + RDS off-hours
# =============================================================================
# Per GAP-446: Phase 1 BETA solo-dev mode does not need 24/7 availability.
# Stop EC2 (kh-backend, kc-app) + RDS (main) outside working hours to save
# ~$70/mo (~58% downtime weekday-night + weekend).
#
# Schedule (Asia/Ho_Chi_Minh ICT):
#   - 22:00 Mon-Fri: stop EC2 + RDS (off-hours weekday)
#   - 08:00 Mon-Fri: start EC2 + RDS (start of business day)
#   - 22:00 Friday : stop EC2 + RDS (ensure stopped over weekend)
#   - 08:00 Monday : start EC2 + RDS (start of week)
#
# AWS managed RDS auto-restarts after 7 days stopped — Friday-stop is OK because
# Monday-start fires within 3 days (60h). Phase 1 BETA accepted limitation.
#
# Toggle: set var.enable_cost_scheduling = false to disable all schedules
# (e.g. when promoting to GA / production with paying tenants requiring 24/7).
# =============================================================================

resource "aws_scheduler_schedule_group" "kite_cost_saving" {
  count = var.enable_cost_scheduling ? 1 : 0
  name  = "${var.project_name}-cost-saving"

  tags = { Name = "${var.project_name}-cost-saving" }
}

# --- IAM role assumed by EventBridge Scheduler to call EC2/RDS APIs ---
resource "aws_iam_role" "scheduler_executor" {
  count = var.enable_cost_scheduling ? 1 : 0
  name  = "${var.project_name}-${var.environment}-scheduler-executor"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Action    = "sts:AssumeRole"
      Principal = { Service = "scheduler.amazonaws.com" }
      Condition = {
        StringEquals = {
          "aws:SourceAccount" = data.aws_caller_identity.current.account_id
        }
      }
    }]
  })

  tags = { Name = "${var.project_name}-scheduler-executor" }
}

resource "aws_iam_role_policy" "scheduler_executor_inline" {
  count = var.enable_cost_scheduling ? 1 : 0
  name  = "${var.project_name}-scheduler-executor-inline"
  role  = aws_iam_role.scheduler_executor[0].id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      # EC2 start/stop — scoped to instances tagged Project=Kite + Phase=1-beta
      # (matches default_tags in main.tf — applies to kh_backend + kc_app).
      {
        Effect = "Allow"
        Action = [
          "ec2:StartInstances",
          "ec2:StopInstances",
        ]
        Resource = "*"
        Condition = {
          StringEquals = {
            "aws:ResourceTag/Project" = "Kite"
            "aws:ResourceTag/Phase"   = "1-beta"
          }
        }
      },
      # EC2 describe needed by Scheduler input transformer (no condition support
      # on describe API per AWS docs).
      {
        Effect   = "Allow"
        Action   = "ec2:DescribeInstances"
        Resource = "*"
      },
      # RDS start/stop on the main DB instance (resource ARN — RDS does not
      # support tag-condition on Start/StopDBInstance).
      {
        Effect = "Allow"
        Action = [
          "rds:StartDBInstance",
          "rds:StopDBInstance",
        ]
        Resource = "arn:aws:rds:${var.aws_region}:${data.aws_caller_identity.current.account_id}:db:${var.project_name}-postgres"
      },
    ]
  })
}

# --- Targets (input templates reused across schedules) ---
locals {
  # EC2 instance IDs to stop/start. Tag-based filter resolved at apply time
  # via terraform refs (compile-time list). Phase 1 BETA = 2 instances.
  managed_ec2_instance_ids = [
    aws_instance.kh_backend.id,
    aws_instance.kc_app.id,
  ]
}

# =============================================================================
# Schedule 1 — STOP EC2 + RDS at 22:00 ICT weekday (Mon-Fri)
# =============================================================================

resource "aws_scheduler_schedule" "stop_weekday_evening_ec2" {
  count       = var.enable_cost_scheduling ? 1 : 0
  name        = "stop-weekday-evening-ec2"
  group_name  = aws_scheduler_schedule_group.kite_cost_saving[0].name
  description = "Stop EC2 instances at 22:00 ICT Mon-Fri to save off-hours cost"

  schedule_expression          = "cron(0 22 ? * MON-FRI *)"
  schedule_expression_timezone = "Asia/Ho_Chi_Minh"

  flexible_time_window {
    mode = "OFF"
  }

  target {
    arn      = "arn:aws:scheduler:::aws-sdk:ec2:stopInstances"
    role_arn = aws_iam_role.scheduler_executor[0].arn

    input = jsonencode({
      InstanceIds = local.managed_ec2_instance_ids
    })
  }
}

resource "aws_scheduler_schedule" "stop_weekday_evening_rds" {
  count       = var.enable_cost_scheduling ? 1 : 0
  name        = "stop-weekday-evening-rds"
  group_name  = aws_scheduler_schedule_group.kite_cost_saving[0].name
  description = "Stop RDS at 22:00 ICT Mon-Fri (will auto-restart in 7d if not started; Mon-start fires within 60h)"

  schedule_expression          = "cron(0 22 ? * MON-FRI *)"
  schedule_expression_timezone = "Asia/Ho_Chi_Minh"

  flexible_time_window {
    mode = "OFF"
  }

  target {
    arn      = "arn:aws:scheduler:::aws-sdk:rds:stopDBInstance"
    role_arn = aws_iam_role.scheduler_executor[0].arn

    input = jsonencode({
      DbInstanceIdentifier = "${var.project_name}-postgres"
    })
  }
}

# =============================================================================
# Schedule 2 — START EC2 + RDS at 08:00 ICT weekday (Mon-Fri)
# =============================================================================

resource "aws_scheduler_schedule" "start_weekday_morning_ec2" {
  count       = var.enable_cost_scheduling ? 1 : 0
  name        = "start-weekday-morning-ec2"
  group_name  = aws_scheduler_schedule_group.kite_cost_saving[0].name
  description = "Start EC2 instances at 08:00 ICT Mon-Fri (start of business day)"

  schedule_expression          = "cron(0 8 ? * MON-FRI *)"
  schedule_expression_timezone = "Asia/Ho_Chi_Minh"

  flexible_time_window {
    mode = "OFF"
  }

  target {
    arn      = "arn:aws:scheduler:::aws-sdk:ec2:startInstances"
    role_arn = aws_iam_role.scheduler_executor[0].arn

    input = jsonencode({
      InstanceIds = local.managed_ec2_instance_ids
    })
  }
}

resource "aws_scheduler_schedule" "start_weekday_morning_rds" {
  count       = var.enable_cost_scheduling ? 1 : 0
  name        = "start-weekday-morning-rds"
  group_name  = aws_scheduler_schedule_group.kite_cost_saving[0].name
  description = "Start RDS at 08:00 ICT Mon-Fri (DB ready before EC2 boot completes)"

  schedule_expression          = "cron(0 8 ? * MON-FRI *)"
  schedule_expression_timezone = "Asia/Ho_Chi_Minh"

  flexible_time_window {
    mode = "OFF"
  }

  target {
    arn      = "arn:aws:scheduler:::aws-sdk:rds:startDBInstance"
    role_arn = aws_iam_role.scheduler_executor[0].arn

    input = jsonencode({
      DbInstanceIdentifier = "${var.project_name}-postgres"
    })
  }
}

# =============================================================================
# Schedule 3 — STOP everything at 22:00 ICT Friday (defensive — covers weekend)
# =============================================================================
# Same time as stop-weekday-evening but explicit Friday entry guards against
# accidental weekend-running if MON-FRI rule changed. Idempotent: stopping
# already-stopped instances is no-op.

resource "aws_scheduler_schedule" "stop_friday_evening_ec2" {
  count       = var.enable_cost_scheduling ? 1 : 0
  name        = "stop-friday-evening-ec2"
  group_name  = aws_scheduler_schedule_group.kite_cost_saving[0].name
  description = "Defensive stop EC2 22:00 ICT Friday to ensure weekend off"

  schedule_expression          = "cron(0 22 ? * FRI *)"
  schedule_expression_timezone = "Asia/Ho_Chi_Minh"

  flexible_time_window {
    mode = "OFF"
  }

  target {
    arn      = "arn:aws:scheduler:::aws-sdk:ec2:stopInstances"
    role_arn = aws_iam_role.scheduler_executor[0].arn

    input = jsonencode({
      InstanceIds = local.managed_ec2_instance_ids
    })
  }
}

resource "aws_scheduler_schedule" "stop_friday_evening_rds" {
  count       = var.enable_cost_scheduling ? 1 : 0
  name        = "stop-friday-evening-rds"
  group_name  = aws_scheduler_schedule_group.kite_cost_saving[0].name
  description = "Defensive stop RDS 22:00 ICT Friday — Mon-start fires within 60h (RDS 7d auto-restart limit safe)"

  schedule_expression          = "cron(0 22 ? * FRI *)"
  schedule_expression_timezone = "Asia/Ho_Chi_Minh"

  flexible_time_window {
    mode = "OFF"
  }

  target {
    arn      = "arn:aws:scheduler:::aws-sdk:rds:stopDBInstance"
    role_arn = aws_iam_role.scheduler_executor[0].arn

    input = jsonencode({
      DbInstanceIdentifier = "${var.project_name}-postgres"
    })
  }
}

# =============================================================================
# Schedule 4 — START everything at 08:00 ICT Monday (start of week)
# =============================================================================

resource "aws_scheduler_schedule" "start_monday_morning_ec2" {
  count       = var.enable_cost_scheduling ? 1 : 0
  name        = "start-monday-morning-ec2"
  group_name  = aws_scheduler_schedule_group.kite_cost_saving[0].name
  description = "Start EC2 08:00 ICT Monday (start of week, post-weekend)"

  schedule_expression          = "cron(0 8 ? * MON *)"
  schedule_expression_timezone = "Asia/Ho_Chi_Minh"

  flexible_time_window {
    mode = "OFF"
  }

  target {
    arn      = "arn:aws:scheduler:::aws-sdk:ec2:startInstances"
    role_arn = aws_iam_role.scheduler_executor[0].arn

    input = jsonencode({
      InstanceIds = local.managed_ec2_instance_ids
    })
  }
}

resource "aws_scheduler_schedule" "start_monday_morning_rds" {
  count       = var.enable_cost_scheduling ? 1 : 0
  name        = "start-monday-morning-rds"
  group_name  = aws_scheduler_schedule_group.kite_cost_saving[0].name
  description = "Start RDS 08:00 ICT Monday (DB ready before EC2 boot)"

  schedule_expression          = "cron(0 8 ? * MON *)"
  schedule_expression_timezone = "Asia/Ho_Chi_Minh"

  flexible_time_window {
    mode = "OFF"
  }

  target {
    arn      = "arn:aws:scheduler:::aws-sdk:rds:startDBInstance"
    role_arn = aws_iam_role.scheduler_executor[0].arn

    input = jsonencode({
      DbInstanceIdentifier = "${var.project_name}-postgres"
    })
  }
}
