# =============================================================================
# Security Groups — Phase 1 BETA Architecture B
# =============================================================================

# --- ALB SG (internet-facing 80/443) ---
resource "aws_security_group" "alb" {
  count       = var.enable_alb ? 1 : 0
  name_prefix = "${var.project_name}-alb-"
  vpc_id      = aws_vpc.main.id
  description = "ALB ingress 80/443 from internet (Cloudflare proxy)"

  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "HTTPS from internet (Cloudflare proxy front)"
  }

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "HTTP redirect to HTTPS"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${var.project_name}-alb-sg" }
}

# --- EC2 SG (ALB → app ports; SSH restricted) ---
resource "aws_security_group" "ec2_app" {
  name_prefix = "${var.project_name}-ec2-app-"
  vpc_id      = aws_vpc.main.id
  description = "EC2 app ingress from ALB on app ports + SSM"

  # Allow ALB → EC2 on common app ports (80, 8080, 3000, 8081-8087)
  dynamic "ingress" {
    for_each = var.enable_alb ? [80, 8080, 3000, 8081, 8082, 8083, 8084, 8085, 8086, 8087] : []
    content {
      from_port       = ingress.value
      to_port         = ingress.value
      protocol        = "tcp"
      security_groups = [aws_security_group.alb[0].id]
      description     = "Port ${ingress.value} from ALB"
    }
  }

  # Wave aws-restore-1 (2026-05-26): if ALB disabled, allow kc_app_fe nginx
  # reverse-proxy to private VPC ports 80, 443, 8080 (gateway). NO 0.0.0.0/0
  # fallback — DB-bearing EC2 must stay private. Per architecture pivot:
  # CF → kc_app_fe EIP → nginx reverse_proxy → kh_backend private IP.
  dynamic "ingress" {
    for_each = var.enable_alb ? [] : [80, 443, 8080]
    content {
      from_port       = ingress.value
      to_port         = ingress.value
      protocol        = "tcp"
      security_groups = [aws_security_group.kc_app_fe.id]
      description     = "Port ${ingress.value} from kc_app_fe nginx reverse-proxy (no ALB mode)"
    }
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    description = "All egress (RDS, Secrets Manager, ECR, S3, internet)"
  }

  tags = { Name = "${var.project_name}-ec2-app-sg" }
}

# --- RDS SG (5432 from EC2 only) ---
resource "aws_security_group" "rds" {
  name_prefix = "${var.project_name}-rds-"
  vpc_id      = aws_vpc.main.id
  description = "RDS PostgreSQL - 5432 from EC2 app SG only"

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.ec2_app.id]
    description     = "PostgreSQL from EC2 app instances"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${var.project_name}-rds-sg" }
}
