# =============================================================================
# EC2 kc-app FE self-host — Wave 82 Bucket B
# =============================================================================
# Phase 1 BETA FE self-host migration (ADR-031): cấp phát instance t3.small mới
# để chạy 2 Next.js standalone server (kitehub-frontend port 4701 + kiteclass-
# frontend port 4700) + nginx reverse proxy + PM2 process manager + certbot
# DNS-01 cho TLS auto-renewal qua Cloudflare API.
#
# P0 mitigations áp dụng trong file này (failure-mode matrix Wave 82 Bucket A):
#   - F6 / GAP-565: SG description ASCII trên MỌI rule; port 4701/4700 chỉ
#     internal self-reference (KHÔNG public 0.0.0.0/0); SSH restrict admin CIDR.
#   - F7 / GAP-566: t3.small 2GB RAM tight cho 2 Next standalone + nginx + PM2.
#     EBS 20GB gp3 + swapfile 2GB cấp tại /swapfile + CloudWatch alarm >85% mem.
#   - F10 / GAP-567: Certbot DNS-01 via Cloudflare API token đọc từ SSM
#     Parameter Store; custom metric KiteHub/FE/CertDaysToExpire + alarm <30d.
#
# F11 (BE CORS) handled ở separate agent / scope, không cover file này.
#
# Deploy flow: terraform apply tạo instance + SG + IAM. App deploy qua SSM
# SendCommand (npm ci + pnpm build + pm2 reload) — KHÔNG bake vào user_data
# để tách concern provisioning vs deployment.
# =============================================================================

# --- Variables (top-of-file defaults; can be moved to variables.tf if needed) ---
# ASCII-only description values per terraform-aws hook policy

variable "admin_ssh_cidr" {
  description = "CIDR block allowed to SSH into kc-app-fe instance. NEVER 0.0.0.0/0. Example: 113.161.0.0/16 or personal IP /32."
  type        = string
  default     = null

  validation {
    condition     = var.admin_ssh_cidr == null ? true : var.admin_ssh_cidr != "0.0.0.0/0"
    error_message = "admin_ssh_cidr must not be 0.0.0.0/0 - public SSH is banned per GAP-565."
  }
}

variable "alarm_sns_topic_arn_fe" {
  description = "SNS topic ARN for FE alarms (memory + cert expiry). Default null = no alarm action. Can reuse aws_sns_topic.memory_alerts.arn to share notification channel with BE."
  type        = string
  default     = null
}

variable "kc_app_fe_instance_type" {
  description = "Instance type for FE self-host. Default t3.small (2GB RAM, 2 vCPU) per ADR-031 plus GAP-566 swap mitigation."
  type        = string
  default     = "t3.small"
}

# --- Security Group cho FE instance (P0 GAP-565) ---
# Mỗi rule có description ASCII rõ ràng per .claude/rules/aws-sg-description-ascii.md
# Port 4701/4700 chỉ self-reference (KHÔNG public) — buộc traffic đi qua nginx.
resource "aws_security_group" "kc_app_fe" {
  name_prefix = "${var.project_name}-kc-app-fe-"
  vpc_id      = aws_vpc.main.id
  description = "FE self-host instance ingress - 22 admin only, 80/443 public, 4700/4701 internal nginx-only"

  # SSH admin chỉ từ admin CIDR (KHÔNG public)
  dynamic "ingress" {
    for_each = var.admin_ssh_cidr != null ? [var.admin_ssh_cidr] : []
    content {
      from_port   = 22
      to_port     = 22
      protocol    = "tcp"
      cidr_blocks = [ingress.value]
      description = "SSH admin access from approved admin CIDR only"
    }
  }

  # HTTP public cho certbot HTTP-01 fallback challenge + redirect to HTTPS
  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "HTTP for certbot HTTP-01 fallback challenge and redirect to HTTPS"
  }

  # HTTPS public cho FE traffic
  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "HTTPS public FE traffic to nginx reverse proxy"
  }

  # Port 4701 chỉ self-reference (nginx local proxy_pass)
  ingress {
    from_port   = 4701
    to_port     = 4701
    protocol    = "tcp"
    self        = true
    description = "Internal nginx to Next.js standalone for kitehub-frontend - self-reference only no public"
  }

  # Port 4700 chỉ self-reference (nginx local proxy_pass)
  ingress {
    from_port   = 4700
    to_port     = 4700
    protocol    = "tcp"
    self        = true
    description = "Internal nginx to Next.js standalone for kiteclass-frontend - self-reference only no public"
  }

  # Egress toàn bộ (BE API calls, certbot renewal, npm/pnpm registry, SSM, CloudWatch)
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    description = "All egress for BE API calls cert renewal npm fetch SSM CloudWatch"
  }

  tags = {
    Name = "${var.project_name}-kc-app-fe-sg"
    Role = "fe-self-host"
  }
}

# --- IAM role cho FE instance (P0 GAP-567 cert + memory metrics) ---
# Tách role riêng kc_app_fe (NOT reuse ec2_app role) để principle of least
# privilege: FE chỉ cần đọc SSM cloudflare token + put cert/memory metrics.
resource "aws_iam_role" "kc_app_fe" {
  name = "${var.project_name}-${var.environment}-kc-app-fe"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Action    = "sts:AssumeRole"
      Principal = { Service = "ec2.amazonaws.com" }
    }]
  })

  tags = { Name = "${var.project_name}-kc-app-fe-role" }
}

# SSM Session Manager (replace SSH cho ad-hoc shell access + SendCommand deploy)
resource "aws_iam_role_policy_attachment" "kc_app_fe_ssm" {
  role       = aws_iam_role.kc_app_fe.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

# CloudWatch agent (memory + custom CertDaysToExpire metric publishing)
resource "aws_iam_role_policy_attachment" "kc_app_fe_cloudwatch" {
  role       = aws_iam_role.kc_app_fe.name
  policy_arn = "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy"
}

# Inline policy: read Cloudflare API token cho certbot DNS-01 + put cert metric
resource "aws_iam_role_policy" "kc_app_fe_certbot_metrics" {
  name = "${var.project_name}-kc-app-fe-certbot-metrics"
  role = aws_iam_role.kc_app_fe.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        # Certbot DNS-01: đọc Cloudflare API token từ AWS Secrets Manager.
        # Token được quản lý qua `aws_secretsmanager_secret.placeholders["cloudflare-api-token"]`
        # trong secrets.tf (placeholder pattern — user populate manually post-apply).
        # Wave 82 Bucket B fix 2026-05-15: switched from SSM Parameter Store sang
        # Secrets Manager để match existing user setup (token đã set trong Secrets Manager,
        # không phải Parameter Store như Agent 1+2 designs giả định).
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue",
        ]
        Resource = [
          aws_secretsmanager_secret.placeholders["cloudflare-api-token"].arn,
        ]
      },
      {
        # CertDaysToExpire custom metric publish (namespace scoped via condition)
        Effect   = "Allow"
        Action   = ["cloudwatch:PutMetricData"]
        Resource = "*"
        Condition = {
          StringEquals = {
            "cloudwatch:namespace" = ["KiteHub/FE", "CWAgent"]
          }
        }
      },
    ]
  })
}

resource "aws_iam_instance_profile" "kc_app_fe" {
  name = "${var.project_name}-${var.environment}-kc-app-fe"
  role = aws_iam_role.kc_app_fe.name
}

# --- Cloud-init user_data: nginx + Node 20 LTS + PM2 + certbot + swapfile ---
# (P0 GAP-566 swap mitigation cho t3.small 2GB tight RAM)
#
# GAP-573 fix 2026-05-15: extend user_data wire systemd timer + publisher script
# cho CertDaysToExpire metric. Align namespace+dimension (KiteHub/FE +
# InstanceId) voi terraform alarm kc_app_fe_cert_expiry (line 305) — fixes
# Wave 82 GAP-567 gap khi setup script ship only as standalone file, not wired
# into user_data. Idempotent — re-run safe; publisher exit 0 khi cert chua
# co (avoid timer-failure trong window pre-issuance).
locals {
  kc_app_fe_user_data = <<-USERDATA
    #!/bin/bash
    set -euo pipefail
    dnf update -y

    # GAP-566: swapfile 2GB tai /swapfile mode 0600 + /etc/fstab persist
    # Cap swap TRUOC khi cai Node/nginx de build process tranh OOM.
    if [ ! -f /swapfile ]; then
      fallocate -l 2G /swapfile
      chmod 0600 /swapfile
      mkswap /swapfile
      swapon /swapfile
      echo '/swapfile none swap sw 0 0' >> /etc/fstab
    fi

    # Cai runtime: nginx + Node 20 LTS + amazon-cloudwatch-agent + certbot
    dnf install -y nginx amazon-cloudwatch-agent python3-pip
    # Node 20 LTS via NodeSource (Amazon Linux 2023 default Node version older)
    curl -fsSL https://rpm.nodesource.com/setup_20.x | bash -
    dnf install -y nodejs
    npm install -g pm2 pnpm

    # Certbot + Cloudflare DNS plugin (GAP-567)
    pip3 install --upgrade certbot certbot-dns-cloudflare

    systemctl enable --now nginx

    # Repo clone (deploy script invoke qua SSM SendCommand se checkout tag)
    dnf install -y git
    mkdir -p /opt/kite-fe
    chown ec2-user:ec2-user /opt/kite-fe
    sudo -u ec2-user git clone https://github.com/VictorAurelius/2026-Kite-Class-Platform.git /opt/kite-fe

    # =========================================================================
    # GAP-573: cert-days-to-expire publisher + systemd timer
    # =========================================================================
    # Publish CertDaysToExpire metric vao CloudWatch namespace KiteHub/FE
    # dimension InstanceId=<self>. Match terraform alarm kc_app_fe_cert_expiry
    # (line 305 cua file nay). Timer chay daily — publisher idempotent + tolerant
    # khi cert chua issued (exit 0 + WARN log de timer khong fail).

    cat > /usr/local/bin/cert-days-to-expire.sh <<'PUBLISHER_EOF'
    #!/usr/bin/env bash
    # CertDaysToExpire publisher — GAP-573 wire (Wave 82 GAP-567 follow-up)
    # Cross-link: ec2-kc-app.tf alarm kc_app_fe_cert_expiry namespace KiteHub/FE
    set -euo pipefail

    DOMAIN="$${1:-kitehub.me}"
    CERT_FILE="/etc/letsencrypt/live/$DOMAIN/cert.pem"
    NAMESPACE="KiteHub/FE"
    METRIC_NAME="CertDaysToExpire"
    REGION="ap-southeast-1"

    # Fetch instance ID via IMDSv2 (http_tokens=required per ec2-kc-app.tf)
    TOKEN=$(curl -s -X PUT 'http://169.254.169.254/latest/api/token' \
      -H 'X-aws-ec2-metadata-token-ttl-seconds: 300')
    INSTANCE_ID=$(curl -s -H "X-aws-ec2-metadata-token: $TOKEN" \
      http://169.254.169.254/latest/meta-data/instance-id)

    if [[ -z "$INSTANCE_ID" ]]; then
      echo "[$(date -u +'%Y-%m-%dT%H:%M:%SZ')] ERROR: Cannot fetch InstanceId from IMDSv2" >&2
      exit 1
    fi

    # Tolerant of pre-issuance window — exit 0 + log WARN khi cert chua co.
    # Timer chay daily; first run sau cert issuance se push baseline data point.
    if [[ ! -f "$CERT_FILE" ]]; then
      echo "[$(date -u +'%Y-%m-%dT%H:%M:%SZ')] WARN: Cert not found ($CERT_FILE) — skip metric publish (pre-issuance window)"
      exit 0
    fi

    EXPIRY_EPOCH=$(date -d "$(openssl x509 -enddate -noout -in "$CERT_FILE" | cut -d= -f2)" +%s)
    NOW_EPOCH=$(date +%s)
    DAYS=$(( (EXPIRY_EPOCH - NOW_EPOCH) / 86400 ))

    echo "[$(date -u +'%Y-%m-%dT%H:%M:%SZ')] $DOMAIN cert expires in $DAYS days (InstanceId=$INSTANCE_ID)"

    aws cloudwatch put-metric-data \
      --namespace "$NAMESPACE" \
      --metric-name "$METRIC_NAME" \
      --dimensions "InstanceId=$INSTANCE_ID" \
      --value "$DAYS" \
      --unit Count \
      --region "$REGION"
    PUBLISHER_EOF
    chmod 755 /usr/local/bin/cert-days-to-expire.sh

    # systemd service oneshot — run publisher once per timer trigger
    cat > /etc/systemd/system/cert-days-monitor.service <<'SVC_EOF'
    [Unit]
    Description=Publish cert days-to-expire metric to CloudWatch KiteHub/FE
    After=network-online.target
    Wants=network-online.target

    [Service]
    Type=oneshot
    ExecStart=/usr/local/bin/cert-days-to-expire.sh kitehub.me
    User=root
    StandardOutput=journal
    StandardError=journal
    SVC_EOF

    # systemd timer daily fire — provides datapoint vao alarm period 86400s
    cat > /etc/systemd/system/cert-days-monitor.timer <<'TMR_EOF'
    [Unit]
    Description=Daily cert days-to-expire push to CloudWatch KiteHub/FE
    After=network-online.target

    [Timer]
    OnCalendar=daily
    Persistent=true
    RandomizedDelaySec=300

    [Install]
    WantedBy=timers.target
    TMR_EOF

    systemctl daemon-reload
    systemctl enable --now cert-days-monitor.timer

    # First-run push 1 baseline data point ngay (avoid 24h wait cho first timer fire).
    # Tolerant if cert chua co — publisher exit 0 + log WARN, timer se retry daily.
    /usr/local/bin/cert-days-to-expire.sh kitehub.me || true

    echo "Wave 82 Bucket B FE self-host bootstrap complete (GAP-573 cert-monitor wired $(date -u +%FT%TZ))" > /var/log/kite-fe-bootstrap.log
  USERDATA
}

# --- FE EC2 instance (t3.small per ADR-031) ---
resource "aws_instance" "kc_app_fe" {
  ami                         = data.aws_ami.al2023.id
  instance_type               = var.kc_app_fe_instance_type
  subnet_id                   = aws_subnet.public[0].id
  vpc_security_group_ids      = [aws_security_group.kc_app_fe.id]
  iam_instance_profile        = aws_iam_instance_profile.kc_app_fe.name
  associate_public_ip_address = true
  key_name                    = var.ec2_key_pair_name

  user_data                   = local.kc_app_fe_user_data
  user_data_replace_on_change = false

  root_block_device {
    volume_type           = "gp3"
    volume_size           = 20 # Next standalone images + node_modules + swapfile 2GB headroom
    encrypted             = true
    delete_on_termination = true
    tags = {
      Name = "${var.project_name}-kc-app-fe-root"
    }
  }

  metadata_options {
    http_tokens                 = "required" # IMDSv2 only
    http_endpoint               = "enabled"
    http_put_response_hop_limit = 2
  }

  tags = {
    Name = "${var.project_name}-kc-app-fe"
    Role = "fe-self-host"
  }
}

# --- CloudWatch alarm: memory >85% 5min (P0 GAP-566) ---
# t3.small 2GB RAM tight cho 2 Next standalone + nginx + PM2; swap mitigation
# van can CW alarm de canh bao som khi headroom can (swap dung nhieu = degraded
# latency). Threshold 85% align voi existing kh_backend/kc_app alarm pattern.
resource "aws_cloudwatch_metric_alarm" "kc_app_fe_memory_high" {
  alarm_name          = "${var.project_name}-kc-app-fe-memory-high"
  alarm_description   = "kc-app-fe EC2 memory >85% 5min - OOM risk on t3.small 2GB tight (GAP-566). Action: SSM inspect Next/nginx, pm2 restart, consider upsize t3.medium."
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "mem_used_percent"
  namespace           = "CWAgent"
  period              = 300
  statistic           = "Average"
  threshold           = 85

  dimensions = {
    InstanceId = aws_instance.kc_app_fe.id
  }

  alarm_actions             = var.alarm_sns_topic_arn_fe != null ? [var.alarm_sns_topic_arn_fe] : []
  ok_actions                = var.alarm_sns_topic_arn_fe != null ? [var.alarm_sns_topic_arn_fe] : []
  insufficient_data_actions = [] # CW agent chua cau hinh = quiet, khong noisy
  treat_missing_data        = "notBreaching"

  tags = {
    Name = "${var.project_name}-kc-app-fe-memory-high"
    Role = "fe-self-host"
  }
}

# --- CloudWatch alarm: cert expiry <30d (P0 GAP-567) ---
# Custom metric KiteHub/FE/CertDaysToExpire publish boi cron script chay daily
# tren kc-app-fe (cron + Python/bash inspect /etc/letsencrypt/live/<domain>/
# cert.pem expiry, put-metric-data dimension Domain=<domain>). Alarm <30d cho
# heads-up truoc Let's Encrypt 90d expiry; auto-renewal certbot timer default
# renew tai 30d truoc expiry, alarm nhu fail-safe neu renew failed.
resource "aws_cloudwatch_metric_alarm" "kc_app_fe_cert_expiry" {
  alarm_name          = "${var.project_name}-kc-app-fe-cert-expiry"
  alarm_description   = "kc-app-fe TLS cert <30 days to expire (GAP-567). Certbot auto-renew may have failed. Action: SSM inspect /var/log/letsencrypt/, run certbot renew --dry-run, check Cloudflare API token SSM."
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = 1
  metric_name         = "CertDaysToExpire"
  namespace           = "KiteHub/FE"
  period              = 86400 # 24h - cert metric publish daily
  statistic           = "Minimum"
  threshold           = 30

  dimensions = {
    InstanceId = aws_instance.kc_app_fe.id
  }

  alarm_actions             = var.alarm_sns_topic_arn_fe != null ? [var.alarm_sns_topic_arn_fe] : []
  ok_actions                = var.alarm_sns_topic_arn_fe != null ? [var.alarm_sns_topic_arn_fe] : []
  insufficient_data_actions = []          # Metric chua publish (pre-cert-install) = quiet
  treat_missing_data        = "breaching" # Sau khi metric publish baseline, missing = problem

  tags = {
    Name = "${var.project_name}-kc-app-fe-cert-expiry"
    Role = "fe-self-host"
  }
}

# --- Outputs ---
output "kc_app_fe_instance_id" {
  description = "EC2 instance ID of FE self-host instance"
  value       = aws_instance.kc_app_fe.id
}

output "kc_app_fe_public_ip" {
  description = "Public IP for DNS A record kitehub.me cutover"
  value       = aws_instance.kc_app_fe.public_ip
}

output "kc_app_fe_private_ip" {
  description = "Private IP for internal probe and monitoring"
  value       = aws_instance.kc_app_fe.private_ip
}

output "kc_app_fe_sg_id" {
  description = "Security group ID for FE instance (cross-reference from RDS SG to whitelist FE-to-DB if needed)"
  value       = aws_security_group.kc_app_fe.id
}
