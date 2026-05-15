#!/usr/bin/env bash
# =============================================================================
# GAP-573 backfill — wire cert-days-monitor systemd timer + publisher lên live
# EC2 i-05cfda7c6c60b683f (Wave 84 launch 2026-05-15) WITHOUT replacing instance.
# =============================================================================
#
# Bối cảnh: `ec2-kc-app.tf` user_data đã extend (GAP-573 fix) với systemd setup,
# NHƯNG `user_data_replace_on_change = false` → live EC2 không tự re-run user_data.
# Script này chạy thủ công qua SSM SendCommand để backfill lên instance hiện tại.
#
# Future EC2 replacement (Wave 85+ instance_type bump v.v.) sẽ tự fresh-run
# user_data — KHÔNG cần script này.
#
# Cách chạy (từ workstation, KHÔNG SSH):
#
#   aws ssm send-command \
#     --instance-ids i-05cfda7c6c60b683f \
#     --document-name AWS-RunShellScript \
#     --parameters "commands=[\"$(base64 -w0 infrastructure/fe-host/cert-days-monitor-backfill.sh | sed 's/.*/echo & | base64 -d | sudo bash/')\"]" \
#     --profile dev-admin --region ap-southeast-1
#
# Hoặc đơn giản hơn — paste content qua AWS Console SSM Run Command UI.
#
# Verify post-backfill:
#   systemctl is-active cert-days-monitor.timer    # active
#   journalctl -u cert-days-monitor.service --no-pager -n 20
#   aws cloudwatch get-metric-statistics --namespace KiteHub/FE \
#     --metric-name CertDaysToExpire \
#     --dimensions Name=InstanceId,Value=i-05cfda7c6c60b683f \
#     --start-time $(date -u -d '1 hour ago' +%FT%TZ) \
#     --end-time $(date -u +%FT%TZ) --period 3600 --statistics Maximum
#
# Cross-link: GAP-573, GAP-567 (Wave 82), ec2-kc-app.tf line 197+
# =============================================================================

set -euo pipefail

log() { echo "[$(date -u +'%Y-%m-%dT%H:%M:%SZ')] $*"; }

log "==== GAP-573 cert-days-monitor backfill BEGIN ===="

# Step 1: Publisher script
cat > /usr/local/bin/cert-days-to-expire.sh <<'PUBLISHER_EOF'
#!/usr/bin/env bash
# CertDaysToExpire publisher — GAP-573 (Wave 82 GAP-567 follow-up)
set -euo pipefail

DOMAIN="${1:-kitehub.me}"
CERT_FILE="/etc/letsencrypt/live/$DOMAIN/cert.pem"
NAMESPACE="KiteHub/FE"
METRIC_NAME="CertDaysToExpire"
REGION="ap-southeast-1"

TOKEN=$(curl -s -X PUT 'http://169.254.169.254/latest/api/token' \
  -H 'X-aws-ec2-metadata-token-ttl-seconds: 300')
INSTANCE_ID=$(curl -s -H "X-aws-ec2-metadata-token: $TOKEN" \
  http://169.254.169.254/latest/meta-data/instance-id)

if [[ -z "$INSTANCE_ID" ]]; then
  echo "[$(date -u +'%Y-%m-%dT%H:%M:%SZ')] ERROR: Cannot fetch InstanceId via IMDSv2" >&2
  exit 1
fi

if [[ ! -f "$CERT_FILE" ]]; then
  echo "[$(date -u +'%Y-%m-%dT%H:%M:%SZ')] WARN: Cert not found ($CERT_FILE) — skip publish"
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
log "Step 1 OK: /usr/local/bin/cert-days-to-expire.sh installed"

# Step 2: systemd service
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
log "Step 2 OK: cert-days-monitor.service installed"

# Step 3: systemd timer (daily, randomized delay 5min)
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
log "Step 3 OK: cert-days-monitor.timer installed"

# Step 4: Enable + start
systemctl daemon-reload
systemctl enable --now cert-days-monitor.timer
log "Step 4 OK: timer enabled — state=$(systemctl is-active cert-days-monitor.timer)"

# Step 5: Initial push baseline data point (avoid 24h wait for first timer fire)
log "Step 5: Initial metric push"
/usr/local/bin/cert-days-to-expire.sh kitehub.me || log "WARN: initial push failed — timer will retry daily"

log "==== GAP-573 backfill DONE ===="
log ""
log "Verify next 5 minutes:"
log "  systemctl list-timers cert-days-monitor.timer"
log "  journalctl -u cert-days-monitor.service --no-pager -n 20"
