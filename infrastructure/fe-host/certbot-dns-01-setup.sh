#!/usr/bin/env bash
# =============================================================================
# Wave 82 Bucket B GAP-567 — Certbot DNS-01 via Cloudflare API token
# =============================================================================
#
# Mục đích: acquire wildcard cert *.kitehub.me + apex kitehub.me bằng DNS-01
# challenge qua Cloudflare API. Tránh HTTP-01 race với nginx port 80
# (nginx hold port 80 cho 301 redirect → HTTPS).
#
# Idempotent: re-run an toàn — certbot detect existing cert, systemd timer
# enable-or-noop, CloudWatch metric script upsert.
#
# Pre-requisites trên EC2:
#   - Amazon Linux 2023 (hoặc Ubuntu 22.04 — adjust dnf → apt nếu cần)
#   - IAM role attached: SSM read + Secrets Manager read + CloudWatch put-metric
#   - SSM Parameter Store có /kitehub/production/cloudflare-api-token
#     (token với scope Zone:DNS:Edit cho kitehub.me zone)
#   - nginx đã install (cho deploy-hook reload)
#
# Triển khai:
#   sudo bash infrastructure/fe-host/certbot-dns-01-setup.sh
#
# Verify post-run:
#   sudo certbot certificates       # list cert + expiry
#   sudo systemctl list-timers      # certbot-renew.timer + cert-days-monitor.timer
#   sudo /usr/local/bin/cert-days-to-expire.sh kitehub.me  # push 1 metric point
#
# Cross-link:
#   - GAP-567 §Proposed Fix Bước 1-6
#   - ADR-031 §Decision (FE self-host trên EC2 t3.small)
#   - .claude/rules/release-deploy-standard.md §3.1 Security pillar
#   - infrastructure/fe-host/nginx-fe.conf (ssl_certificate paths /etc/letsencrypt/live/kitehub.me/)
#
# =============================================================================

set -euo pipefail

# -----------------------------------------------------------------------------
# Configuration variables
# -----------------------------------------------------------------------------
# Domain: wildcard + apex. DNS-01 cho phép wildcard (HTTP-01 không).
readonly DOMAIN_APEX="kitehub.me"
readonly DOMAIN_WILDCARD="*.kitehub.me"

# Admin email Let's Encrypt account — nhận expiry warning từ LE side.
readonly LE_ADMIN_EMAIL="admin@kitehub.me"

# Cloudflare API token location: SSM Parameter Store SecureString.
# Tạo trước qua: aws ssm put-parameter --name /kitehub/production/cloudflare-api-token \
#   --value "$CF_TOKEN" --type SecureString --tier Standard
readonly CF_TOKEN_SSM_PATH="/kitehub/production/cloudflare-api-token"
readonly AWS_REGION="ap-southeast-1"  # Singapore (per ADR-025)

# Local credential file — mode 0600 để certbot đọc, root-only.
readonly CF_CREDENTIALS_FILE="/root/.secrets/cloudflare.ini"

# CloudWatch namespace + metric name (terraform alarm kc_app_cert_expiry
# reference cùng dimension Domain=kitehub.me).
readonly CW_NAMESPACE="KiteHub/EC2/kc-app"
readonly CW_METRIC_NAME="CertDaysToExpire"

# systemd timer schedule cho cert-days-monitor (daily push).
readonly MONITOR_TIMER_SCHEDULE="daily"

# -----------------------------------------------------------------------------
# Helper: log với timestamp UTC để correlate CloudWatch
# -----------------------------------------------------------------------------
log() {
  echo "[$(date -u +'%Y-%m-%dT%H:%M:%SZ')] $*"
}

die() {
  log "ERROR: $*"
  exit 1
}

# -----------------------------------------------------------------------------
# Step 1: Install certbot + dns-cloudflare plugin
# -----------------------------------------------------------------------------
# Amazon Linux 2023 đã có dnf + python3 sẵn. certbot + plugin từ EPEL hoặc
# pip3 install (EPEL preferred — package-managed update).
install_certbot() {
  log "Step 1: Install certbot + python3-certbot-dns-cloudflare"

  if command -v certbot &> /dev/null \
     && python3 -c "import certbot_dns_cloudflare" 2>/dev/null; then
    log "  certbot + dns-cloudflare plugin already installed, skipping"
    return 0
  fi

  # Amazon Linux 2023 path
  if command -v dnf &> /dev/null; then
    sudo dnf install -y certbot python3-certbot-dns-cloudflare \
      || die "dnf install certbot failed"

  # Ubuntu 22.04 fallback path
  elif command -v apt-get &> /dev/null; then
    sudo apt-get update
    sudo apt-get install -y certbot python3-certbot-dns-cloudflare \
      || die "apt install certbot failed"

  else
    die "Neither dnf nor apt-get available — unsupported OS"
  fi

  log "  certbot $(certbot --version 2>&1 | head -1) installed"
}

# -----------------------------------------------------------------------------
# Step 2: Fetch Cloudflare API token từ SSM Parameter Store
# -----------------------------------------------------------------------------
# Token là SecureString → SSM tự decrypt khi IAM role có ssm:GetParameter +
# kms:Decrypt cho CMK SSM dùng. Token NEVER chứa trong code/git/env file
# committed — chỉ tồn tại trong AWS-side encrypted store + /root/.secrets/ local.
fetch_cf_token() {
  log "Step 2: Fetch Cloudflare API token từ SSM ($CF_TOKEN_SSM_PATH)"

  local cf_token
  cf_token=$(aws ssm get-parameter \
    --name "$CF_TOKEN_SSM_PATH" \
    --with-decryption \
    --region "$AWS_REGION" \
    --query 'Parameter.Value' \
    --output text 2>&1) \
    || die "Cannot fetch SSM parameter $CF_TOKEN_SSM_PATH — verify IAM role has ssm:GetParameter + kms:Decrypt"

  if [[ -z "$cf_token" || "$cf_token" == "None" ]]; then
    die "SSM parameter $CF_TOKEN_SSM_PATH empty or not found"
  fi

  # Write credentials file mode 0600 (root-only read).
  # certbot dns-cloudflare expects format `dns_cloudflare_api_token = <token>`.
  sudo mkdir -p "$(dirname "$CF_CREDENTIALS_FILE")"
  sudo chmod 700 "$(dirname "$CF_CREDENTIALS_FILE")"

  # heredoc với sudo tee: -- safer than echo > file (atomic + permission preserved).
  sudo tee "$CF_CREDENTIALS_FILE" > /dev/null <<EOF
# Cloudflare API token cho certbot DNS-01 challenge
# Scope: Zone:DNS:Edit on kitehub.me zone only
# Source: AWS SSM Parameter Store $CF_TOKEN_SSM_PATH
# Generated: $(date -u +'%Y-%m-%dT%H:%M:%SZ')
dns_cloudflare_api_token = $cf_token
EOF

  sudo chmod 600 "$CF_CREDENTIALS_FILE"
  sudo chown root:root "$CF_CREDENTIALS_FILE"

  log "  Credentials written to $CF_CREDENTIALS_FILE (mode 0600)"
}

# -----------------------------------------------------------------------------
# Step 3: Issue cert (idempotent — certbot detect existing)
# -----------------------------------------------------------------------------
# --keep-until-expiring: nếu cert hiện tại còn >30 ngày, KHÔNG re-issue.
# Tránh hit LE rate limit (50 cert/domain/week) khi script chạy nhiều lần.
issue_cert() {
  log "Step 3: Issue cert cho $DOMAIN_APEX + $DOMAIN_WILDCARD"

  local cert_path="/etc/letsencrypt/live/$DOMAIN_APEX/fullchain.pem"

  if [[ -f "$cert_path" ]]; then
    # Đã có cert — check expiry, skip nếu >30 ngày.
    local expiry_epoch
    expiry_epoch=$(date -d "$(sudo openssl x509 -enddate -noout -in "$cert_path" | cut -d= -f2)" +%s)
    local now_epoch
    now_epoch=$(date +%s)
    local days_left=$(( (expiry_epoch - now_epoch) / 86400 ))

    if [[ "$days_left" -gt 30 ]]; then
      log "  Cert exists, $days_left days remaining — skip issuance (re-run renew separately)"
      return 0
    fi
    log "  Cert exists but expires in $days_left days — proceed to renew"
  fi

  sudo certbot certonly \
    --dns-cloudflare \
    --dns-cloudflare-credentials "$CF_CREDENTIALS_FILE" \
    --dns-cloudflare-propagation-seconds 60 \
    -d "$DOMAIN_APEX" \
    -d "$DOMAIN_WILDCARD" \
    --non-interactive \
    --agree-tos \
    --email "$LE_ADMIN_EMAIL" \
    --keep-until-expiring \
    || die "certbot certonly failed — check /var/log/letsencrypt/letsencrypt.log"

  log "  Cert issued/renewed: /etc/letsencrypt/live/$DOMAIN_APEX/"
}

# -----------------------------------------------------------------------------
# Step 4: Setup auto-renewal via certbot-renew.timer
# -----------------------------------------------------------------------------
# certbot package mặc định ship `certbot-renew.timer` (weekly check, only
# renews if <30 days remaining). Verify enable + deploy-hook reload nginx.
setup_renewal_timer() {
  log "Step 4: Setup certbot-renew.timer + nginx deploy-hook"

  # Deploy hook: certbot chạy script này sau khi renew thành công.
  # Reload nginx pickup cert mới mà không drop connection (graceful).
  local hook_dir="/etc/letsencrypt/renewal-hooks/deploy"
  local hook_script="$hook_dir/reload-nginx.sh"

  sudo mkdir -p "$hook_dir"
  sudo tee "$hook_script" > /dev/null <<'EOF'
#!/usr/bin/env bash
# Reload nginx sau cert renewal — pickup fullchain.pem mới
# Generated by certbot-dns-01-setup.sh
set -e
echo "[$(date -u +'%Y-%m-%dT%H:%M:%SZ')] Cert renewed, reloading nginx"
systemctl reload nginx
EOF
  sudo chmod 755 "$hook_script"

  # Enable + start certbot-renew.timer (idempotent — systemctl OK với re-enable).
  sudo systemctl enable --now certbot-renew.timer \
    || die "Cannot enable certbot-renew.timer"

  log "  certbot-renew.timer: $(systemctl is-active certbot-renew.timer)"

  # Dry-run verify: simulate renewal, không actually re-issue.
  if sudo certbot renew --dry-run 2>&1 | grep -q "Congratulations"; then
    log "  certbot renew --dry-run: PASS"
  else
    log "  WARN: certbot renew --dry-run output unexpected — verify manually"
  fi
}

# -----------------------------------------------------------------------------
# Step 5: CloudWatch custom metric — cert days-to-expire
# -----------------------------------------------------------------------------
# Daily push metric → terraform alarm kc_app_cert_expiry fires khi <30 ngày.
# Defense in depth vs Let's Encrypt email warning (LE email có thể spam/missed).
setup_cert_days_monitor() {
  log "Step 5: Setup cert-days-to-expire CloudWatch metric publisher"

  local monitor_script="/usr/local/bin/cert-days-to-expire.sh"

  # Write monitor script (idempotent — sudo tee overwrites).
  sudo tee "$monitor_script" > /dev/null <<'EOF'
#!/usr/bin/env bash
# Push cert days-to-expire metric → CloudWatch namespace KiteHub/EC2/kc-app
# Generated by certbot-dns-01-setup.sh
# Cross-link: GAP-567 §Bước 4 + terraform alarm kc_app_cert_expiry
set -euo pipefail

DOMAIN="${1:-kitehub.me}"
CERT_FILE="/etc/letsencrypt/live/$DOMAIN/cert.pem"
NAMESPACE="KiteHub/EC2/kc-app"
METRIC_NAME="CertDaysToExpire"
REGION="ap-southeast-1"

if [[ ! -f "$CERT_FILE" ]]; then
  echo "ERROR: Cert not found: $CERT_FILE" >&2
  exit 1
fi

# Parse cert expiry date (openssl output: notAfter=Aug  6 12:34:56 2026 GMT).
EXPIRY_EPOCH=$(date -d "$(openssl x509 -enddate -noout -in "$CERT_FILE" | cut -d= -f2)" +%s)
NOW_EPOCH=$(date +%s)
DAYS=$(( (EXPIRY_EPOCH - NOW_EPOCH) / 86400 ))

echo "[$(date -u +'%Y-%m-%dT%H:%M:%SZ')] $DOMAIN cert expires in $DAYS days"

# Push metric → terraform alarm threshold=30 fires when DAYS < 30.
aws cloudwatch put-metric-data \
  --namespace "$NAMESPACE" \
  --metric-name "$METRIC_NAME" \
  --dimensions "Domain=$DOMAIN" \
  --value "$DAYS" \
  --unit Count \
  --region "$REGION"
EOF
  sudo chmod 755 "$monitor_script"

  # systemd service + timer (oneshot pattern — service runs once per timer trigger).
  sudo tee /etc/systemd/system/cert-days-monitor.service > /dev/null <<EOF
[Unit]
Description=Push cert days-to-expire metric to CloudWatch
After=network-online.target
Wants=network-online.target

[Service]
Type=oneshot
ExecStart=$monitor_script $DOMAIN_APEX
User=root
StandardOutput=journal
StandardError=journal
EOF

  sudo tee /etc/systemd/system/cert-days-monitor.timer > /dev/null <<EOF
[Unit]
Description=Daily cert days-to-expire push to CloudWatch
After=certbot-renew.timer

[Timer]
OnCalendar=$MONITOR_TIMER_SCHEDULE
Persistent=true
RandomizedDelaySec=300

[Install]
WantedBy=timers.target
EOF

  sudo systemctl daemon-reload
  sudo systemctl enable --now cert-days-monitor.timer \
    || die "Cannot enable cert-days-monitor.timer"

  log "  cert-days-monitor.timer: $(systemctl is-active cert-days-monitor.timer)"

  # Push 1 metric point ngay để verify wiring (terraform alarm có data ngay
  # post-deploy, không phải đợi 24h cho timer fire lần đầu).
  if sudo "$monitor_script" "$DOMAIN_APEX"; then
    log "  Initial metric pushed cho $DOMAIN_APEX"
  else
    log "  WARN: initial metric push failed — verify IAM role có cloudwatch:PutMetricData"
  fi
}

# -----------------------------------------------------------------------------
# Step 6: Reload nginx pickup cert mới (initial issue case)
# -----------------------------------------------------------------------------
reload_nginx() {
  log "Step 6: Reload nginx"

  if ! sudo nginx -t 2>&1; then
    log "  WARN: nginx config test failed — skip reload, fix config first"
    return 0
  fi

  sudo systemctl reload nginx \
    || die "nginx reload failed"

  log "  nginx reloaded — cert active"
}

# -----------------------------------------------------------------------------
# Main
# -----------------------------------------------------------------------------
main() {
  log "==== Certbot DNS-01 setup BEGIN ===="
  log "  Domain: $DOMAIN_APEX + $DOMAIN_WILDCARD"
  log "  Region: $AWS_REGION"

  if [[ "$EUID" -ne 0 ]] && ! sudo -n true 2>/dev/null; then
    die "Script needs sudo. Run: sudo bash $0"
  fi

  install_certbot
  fetch_cf_token
  issue_cert
  setup_renewal_timer
  setup_cert_days_monitor
  reload_nginx

  log "==== Certbot DNS-01 setup DONE ===="
  log ""
  log "Verify post-run:"
  log "  sudo certbot certificates"
  log "  sudo systemctl list-timers certbot-renew.timer cert-days-monitor.timer"
  log "  aws cloudwatch get-metric-statistics --namespace $CW_NAMESPACE \\"
  log "    --metric-name $CW_METRIC_NAME --dimensions Name=Domain,Value=$DOMAIN_APEX \\"
  log "    --start-time \$(date -u -d '1 hour ago' +%FT%TZ) \\"
  log "    --end-time \$(date -u +%FT%TZ) --period 3600 --statistics Maximum \\"
  log "    --region $AWS_REGION"
}

main "$@"
