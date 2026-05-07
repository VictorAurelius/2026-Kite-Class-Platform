# =============================================================================
# RDS PostgreSQL — Phase 1 BETA db.t3.micro (free tier 12mo)
# =============================================================================

resource "aws_db_subnet_group" "main" {
  name       = "${var.project_name}-db-subnet"
  subnet_ids = aws_subnet.private[*].id
  tags       = { Name = "${var.project_name}-db-subnet" }
}

resource "random_password" "rds" {
  length  = 32
  special = false
}

resource "aws_db_instance" "main" {
  identifier     = "${var.project_name}-postgres"
  engine         = "postgres"
  engine_version = var.rds_engine_version
  instance_class = var.rds_instance_class

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
}
