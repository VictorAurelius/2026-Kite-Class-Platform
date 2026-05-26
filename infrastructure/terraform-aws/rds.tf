# =============================================================================
# RDS PostgreSQL — Phase 1 BETA db.t3.micro (free tier 12mo)
# =============================================================================

resource "aws_db_subnet_group" "main" {
  name       = "${var.project_name}-db-subnet"
  subnet_ids = aws_subnet.private[*].id
  tags       = { Name = "${var.project_name}-db-subnet" }
}

# GAP-450 Option B: lifecycle ignore_changes prevents recurring drift on `result`
# attribute. Auto-regenerate-on-plan would silently rotate RDS master password =
# kh_backend kết nối DB hỏng. Rotation manual qua documents/05-guides/operations/secrets-rotation-runbook.md §5.1.
# Option A (state rm + import current value) tracked in
# documents/05-guides/operations/terraform-state-import-runbook.md.
resource "random_password" "rds" {
  length  = 32
  special = false
  lifecycle {
    ignore_changes = [result, length, special, lower, upper, numeric, min_lower, min_upper, min_numeric, min_special, override_special, keepers]
  }
}

resource "aws_db_instance" "main" {
  identifier     = "${var.project_name}-postgres"
  engine         = "postgres"
  engine_version = var.rds_engine_version
  instance_class = var.rds_instance_class

  # Wave aws-restore-1 (2026-05-26): support restore from snapshot post-GAP-612
  # Day 8 UNBLOCK. When var.rds_restore_from_snapshot is non-empty, RDS restored
  # from snapshot instead of fresh create. RDS preserves master password from
  # snapshot — handled via lifecycle.ignore_changes = [password] below.
  snapshot_identifier = var.rds_restore_from_snapshot != "" ? var.rds_restore_from_snapshot : null

  allocated_storage     = var.rds_allocated_storage
  max_allocated_storage = var.rds_allocated_storage * 2
  storage_type          = "gp3"
  storage_encrypted     = true

  db_name  = var.rds_db_name
  username = "kitehub"
  password = random_password.rds.result

  multi_az               = var.rds_multi_az
  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  publicly_accessible    = false

  backup_retention_period = 7
  backup_window           = "17:00-18:00" # 00:00-01:00 ICT (UTC+7)
  maintenance_window      = "sun:18:00-sun:19:00"

  performance_insights_enabled = false # Free tier guard
  monitoring_interval          = 0     # Disable enhanced monitoring (free tier)

  skip_final_snapshot       = false
  final_snapshot_identifier = "${var.project_name}-final-snapshot-${var.environment}"

  deletion_protection = false # Phase 1 BETA — flip true for GA per release-deploy-standard.md §3.4

  tags = { Name = "${var.project_name}-postgres" }

  # Wave aws-restore-1 (2026-05-26):
  # - snapshot_identifier: ignored post-create so future plans don't try to re-restore
  # - password: RDS preserves master password from snapshot (overrides random_password.rds)
  lifecycle {
    ignore_changes = [snapshot_identifier, password]
  }
}
