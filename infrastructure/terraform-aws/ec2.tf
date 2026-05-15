# =============================================================================
# EC2 — Architecture B 2-instance topology
# =============================================================================
# Phase 1 BETA replaces EKS with plain EC2 + Docker Compose for cost
# (free-tier-friendly). 2 instances:
#   - kh-backend: 6 KH services + RabbitMQ + Redis self-host
#   - kc-app:     KC core + frontends
#
# Bring-up: user_data installs Docker + Docker Compose; deploy via SSM run-command
# pulling images from ECR. Detailed bootstrap shipped in Bucket B Dockerfiles
# + GitHub Actions docker-build-push workflow.

# Pinned Amazon Linux 2023 AMI (ap-southeast-1).
#
# Wave 82 Bucket B prep 2026-05-15: changed from `most_recent = true` + name-glob filter
# to specific `image-id` lookup. AWS releases new AL2023 AMIs ~monthly; with `most_recent`
# every `terraform apply` would replace EXISTING `aws_instance.kh_backend` + `aws_instance.kc_app`
# whenever a new AMI lands → 5-10min downtime + force-redeploy. Wave 82 Bucket B adds
# `aws_instance.kc_app_fe` which would cascade the same replacement on first apply.
#
# Pinning to current AMI ID gives explicit "AMI bump = separate wave with planned maintenance"
# semantics. Update value below to bump AMI; replacement is then intentional, not surprise.
data "aws_ami" "al2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "image-id"
    values = ["ami-04a8a2b994a2a7176"]
  }
}

# --- Cloud-init user-data — installs Docker + Docker Compose plugin + CloudWatch agent ---
locals {
  ec2_user_data = <<-USERDATA
    #!/bin/bash
    set -euo pipefail
    dnf update -y
    dnf install -y docker amazon-cloudwatch-agent
    systemctl enable --now docker
    usermod -aG docker ec2-user

    # Docker Compose v2 plugin
    DOCKER_CONFIG=/usr/local/lib/docker
    mkdir -p $DOCKER_CONFIG/cli-plugins
    curl -sSL "https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64" \
      -o $DOCKER_CONFIG/cli-plugins/docker-compose
    chmod +x $DOCKER_CONFIG/cli-plugins/docker-compose

    # ECR login helper (uses instance profile — no static keys)
    cat > /etc/ecr-login.sh <<'ECR'
    #!/bin/bash
    aws ecr get-login-password --region ${var.aws_region} \
      | docker login --username AWS --password-stdin \
        ${data.aws_caller_identity.current.account_id}.dkr.ecr.${var.aws_region}.amazonaws.com
    ECR
    chmod +x /etc/ecr-login.sh

    # GAP-483 Wave 65: git + repo clone for deploy-prod.sh
    # deploy-production.yml SSM RunCommand invokes /opt/kite-prod/scripts/deploy-prod.sh
    # so every EC2 instance MUST boot with the repo already cloned (no manual SSM bootstrap).
    dnf install -y git
    mkdir -p /opt/kite-prod
    chown ec2-user:ec2-user /opt/kite-prod
    sudo -u ec2-user git clone https://github.com/VictorAurelius/2026-Kite-Class-Platform.git /opt/kite-prod
    # Tag pinning: leave on main; deploy-prod.sh checks out KITE_VERSION at deploy time.

    # Mark bootstrap complete
    echo "Phase 1 BETA EC2 ready" > /var/log/kite-bootstrap.log
  USERDATA
}

# --- KH backend cluster (6 KiteHub services) ---
resource "aws_instance" "kh_backend" {
  ami                         = data.aws_ami.al2023.id
  instance_type               = var.kh_backend_instance_type
  subnet_id                   = aws_subnet.public[0].id
  vpc_security_group_ids      = [aws_security_group.ec2_app.id]
  iam_instance_profile        = aws_iam_instance_profile.ec2_app.name
  associate_public_ip_address = true
  key_name                    = var.ec2_key_pair_name

  user_data                   = local.ec2_user_data
  user_data_replace_on_change = false

  root_block_device {
    volume_type           = "gp3"
    volume_size           = 30 # Free tier covers 30GB EBS gp3
    encrypted             = true
    delete_on_termination = true
  }

  metadata_options {
    http_tokens                 = "required" # IMDSv2 only
    http_endpoint               = "enabled"
    http_put_response_hop_limit = 2
  }

  tags = {
    Name = "${var.project_name}-kh-backend"
    Role = "kh-backend-cluster"
  }
}

# --- KC app (KiteClass core + frontends) ---
resource "aws_instance" "kc_app" {
  ami                         = data.aws_ami.al2023.id
  instance_type               = var.kc_app_instance_type
  subnet_id                   = aws_subnet.public[0].id # Same AZ as kh-backend Phase 1
  vpc_security_group_ids      = [aws_security_group.ec2_app.id]
  iam_instance_profile        = aws_iam_instance_profile.ec2_app.name
  associate_public_ip_address = true
  key_name                    = var.ec2_key_pair_name

  user_data                   = local.ec2_user_data
  user_data_replace_on_change = false

  root_block_device {
    volume_type           = "gp3"
    volume_size           = 30
    encrypted             = true
    delete_on_termination = true
  }

  metadata_options {
    http_tokens                 = "required"
    http_endpoint               = "enabled"
    http_put_response_hop_limit = 2
  }

  tags = {
    Name = "${var.project_name}-kc-app"
    Role = "kc-app"
  }
}

# =============================================================================
# Application Load Balancer (optional — Cloudflare proxy can hit EC2 directly)
# =============================================================================

resource "aws_lb" "main" {
  count              = var.enable_alb ? 1 : 0
  name               = "${var.project_name}-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb[0].id]
  subnets            = aws_subnet.public[*].id

  enable_http2               = true
  enable_deletion_protection = false # Phase 1 BETA — flip true for GA

  tags = { Name = "${var.project_name}-alb" }
}

# Target group: KH backend on port 8080 (gateway entry)
resource "aws_lb_target_group" "kh_backend" {
  count       = var.enable_alb ? 1 : 0
  name        = "${var.project_name}-kh-backend-tg"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = aws_vpc.main.id
  target_type = "instance"

  health_check {
    path                = "/actuator/health"
    matcher             = "200"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }

  tags = { Name = "${var.project_name}-kh-backend-tg" }
}

resource "aws_lb_target_group_attachment" "kh_backend" {
  count            = var.enable_alb ? 1 : 0
  target_group_arn = aws_lb_target_group.kh_backend[0].arn
  target_id        = aws_instance.kh_backend.id
  port             = 8080
}

# NOTE: kc_app TG + attachment + listener rule removed 2026-05-13 (GAP-501).
# Reason: post-Vercel pivot (2026-05-07) — FE served by Vercel CDN at kitehub.me apex;
# nothing listens on kc_app EC2 :3000 anymore. ALB priority-100 rule was returning
# HTTP 502 on /, /auth/*, /dashboard/* via api.kitehub.me. Default action of HTTPS
# listener forwards to kh_backend TG, which is the only TG api.kitehub.me needs.
# kc_app EC2 itself stays (runs BE Java services per GAP-447 right-sizing).

# HTTP listener (redirects to HTTPS if cert provided, else forwards)
resource "aws_lb_listener" "http" {
  count             = var.enable_alb ? 1 : 0
  load_balancer_arn = aws_lb.main[0].arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type = var.alb_acm_certificate_arn != null ? "redirect" : "forward"

    dynamic "redirect" {
      for_each = var.alb_acm_certificate_arn != null ? [1] : []
      content {
        port        = "443"
        protocol    = "HTTPS"
        status_code = "HTTP_301"
      }
    }

    dynamic "forward" {
      for_each = var.alb_acm_certificate_arn == null ? [1] : []
      content {
        target_group {
          arn = aws_lb_target_group.kh_backend[0].arn
        }
      }
    }
  }
}

# HTTPS listener (only if cert provided)
resource "aws_lb_listener" "https" {
  count             = var.enable_alb && var.alb_acm_certificate_arn != null ? 1 : 0
  load_balancer_arn = aws_lb.main[0].arn
  port              = 443
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS13-1-2-2021-06"
  certificate_arn   = var.alb_acm_certificate_arn

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.kh_backend[0].arn
  }
}

# Listener rule kc_app_default removed 2026-05-13 (GAP-501) — see header note above.
# All paths now fall through HTTPS listener default action → kh_backend TG.
