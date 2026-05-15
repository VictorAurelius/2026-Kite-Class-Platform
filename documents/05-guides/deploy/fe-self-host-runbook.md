---
title: FE Self-Host Runbook — Vercel → AWS EC2 t3.small cutover
status: draft
created: 2026-05-15
phase: phase-1-beta
wave: 82
risk: MEDIUM (FE migration touches user-facing, DNS cutover)
prerequisites:
  - GAP-565 (SG description + port restriction) DONE
  - GAP-566 (t3.small RAM tuning + PM2 + swapfile + memory alarm) DONE
  - GAP-567 (certbot DNS-01 + 30d expiry monitor) DONE
  - GAP-568 (BE CORS allowlist sweep pre-flip) DONE
  - infrastructure/terraform-aws/ec2-kc-app.tf merged
  - infrastructure/fe-host/{nginx-fe.conf, pm2-ecosystem.config.js, certbot-dns-01-setup.sh} merged
  - scripts/sweep-be-cors-origins.sh merged
last-updated: 2026-05-15
---

# FE Self-Host Runbook — Vercel → AWS EC2 t3.small cutover (Wave 82 Bucket B-D)

**Audience:** SRE / solo-dev thực hiện FE cutover từ Vercel Free Tier sang AWS EC2 t3.small self-host trong Wave 82.
**Sister artifacts:**
- ADR: [`documents/02-architecture/adr/ADR-031-fe-self-host-aws-ec2.md`](../../02-architecture/adr/ADR-031-fe-self-host-aws-ec2.md)
- Wave plan: [`documents/03-planning/waves/wave-2026-05-15-82-fe-self-host.md`](../../03-planning/waves/wave-2026-05-15-82-fe-self-host.md)
- Terraform: `infrastructure/terraform-aws/ec2-kc-app.tf`
- Server configs: `infrastructure/fe-host/nginx-fe.conf` + `pm2-ecosystem.config.js` + `certbot-dns-01-setup.sh`
- BE CORS sweep: `scripts/sweep-be-cors-origins.sh`

**Standards:** AWS Well-Architected (Security + Reliability) · Twelve-Factor (config in env) · `.claude/rules/release-deploy-standard.md` §4 · `.claude/rules/concurrent-production-mutation-ops.md` (serialize) · `.claude/rules/pre-mutation-state-check.md` (audit artifact pre-mutation) · `.claude/rules/agent-aws-access.md` §4.3 (Tier 3 mutations user-executed).

---

## 1. Mục tiêu

Wave 82 Bucket B-D migration scope:

- **Bucket B:** Provision EC2 `kc-app` (t3.small, ap-southeast-1) + bootstrap server stack (nginx + Node.js 22 + PM2 + certbot DNS-01) + sweep BE CORS allowlist trước DNS flip
- **Bucket C:** Build + deploy `kitehub-frontend` + `kiteclass-frontend` lên EC2 với Wave 78-81 contract changes (Beta Status / Onboarding / Staff Invitation / 2FA)
- **Bucket D:** Cutover Cloudflare DNS `kitehub.me` từ Vercel sang EC2 IP (TTL drop 24h pre-flip, gradual rollout)

Sequential mandate per `concurrent-production-mutation-ops.md`: B → C → D, KHÔNG parallel. Mỗi bucket có acceptance gate trước khi sang bucket tiếp theo.

---

## 2. Tiền điều kiện

### 2.1 4 P0 mitigation gaps DONE

- [ ] **GAP-565** — Security Group description + port restriction (F6 mitigation per ADR-031) — verify SG rule descriptions không rỗng + port 22 restricted về `admin_ssh_cidr` cụ thể
- [ ] **GAP-566** — t3.small RAM tuning (F7 mitigation) — PM2 cluster mode + 1GB swapfile + CloudWatch memory alarm @ 80% defined
- [ ] **GAP-567** — certbot DNS-01 + 30d expiry monitor (F10 mitigation) — Cloudflare DNS API token issued + cert renewal cron + expiry alarm wired
- [ ] **GAP-568** — BE CORS allowlist sweep script (F11 mitigation) — `scripts/sweep-be-cors-origins.sh` shipped với `--audit` + `--preflight` modes

### 2.2 Sister artifacts merged

- [ ] `infrastructure/terraform-aws/ec2-kc-app.tf` (Agent 1 output) — review verified
- [ ] `infrastructure/fe-host/nginx-fe.conf` — reverse proxy + TLS termination
- [ ] `infrastructure/fe-host/pm2-ecosystem.config.js` — 2 process: kitehub-frontend:4701 + kiteclass-frontend:4700
- [ ] `infrastructure/fe-host/certbot-dns-01-setup.sh` — wildcard cert bootstrap
- [ ] `scripts/sweep-be-cors-origins.sh` + audit artifact baseline

### 2.3 Tools sẵn sàng (local workstation)

- [ ] `terraform` >= 1.5 (verify: `terraform version`)
- [ ] `aws` CLI v2 với profile `dev-admin` cấu hình (verify: `aws sts get-caller-identity --profile dev-admin`)
- [ ] `gh` CLI authenticated (verify: `gh auth status`)
- [ ] `ssh` key cho `kc-app` EC2 (cùng pattern với `kh-backend`)
- [ ] `dig` để verify DNS propagation
- [ ] Cloudflare API token có quyền edit DNS zone `kitehub.me` (lưu trong AWS Secrets Manager `kitehub/production/cloudflare-api-token`)

### 2.4 Pre-deploy snapshot

- [ ] RDS backup taken trước Bucket B (per `release-deploy-standard.md` §3.1) — chạy `aws rds create-db-snapshot --db-instance-identifier kite-postgres --db-snapshot-identifier pre-wave-82-$(date -u +%Y%m%d-%H%M%S)`
- [ ] Wave 82 closure PR draft mở (placeholder cho rollback evidence)

---

## 3. Bucket B — EC2 provisioning (USER ACTION)

### 3.1 Pre-flight state-check (USER ACTION — mandatory per `pre-mutation-state-check.md` §3)

Trước khi `terraform apply`, USER tạo audit artifact `documents/04-quality/audits/aws-verification/2026-05-15-wave-82-bucket-b-pre-apply.md`:

```bash
# Verify current AWS state — Tier 1 read-only commands per agent-aws-access.md §2.1
aws ec2 describe-instances \
  --profile dev-admin --region ap-southeast-1 \
  --filters "Name=tag:Project,Values=Kite" \
  --query 'Reservations[].Instances[].[InstanceId,Tags[?Key==`Name`]|[0].Value,State.Name,PrivateIpAddress,PublicIpAddress]' \
  --output table

# Verify no orphan kc-app instance
aws ec2 describe-instances \
  --profile dev-admin --region ap-southeast-1 \
  --filters "Name=tag:Name,Values=kc-app" \
  --query 'Reservations[].Instances[].InstanceId' --output text
# Expected: empty (chưa tồn tại)

# Verify CloudTrail logging active (per aws-observability-first.md)
aws cloudtrail get-trail-status --profile dev-admin --region ap-southeast-1 \
  --name kitehub-main --query 'IsLogging' --output text
# Expected: True
```

Audit artifact PHẢI include: scope + commands run + findings (real-vs-phantom planned changes) + verdict. KHÔNG được apply nếu artifact thiếu.

### 3.2 Set terraform variables

Edit `infrastructure/terraform-aws/terraform.tfvars` (hoặc set qua workflow_dispatch inputs):

```hcl
# Wave 82 Bucket B — kc-app FE EC2
kc_app_admin_ssh_cidr      = "203.0.113.0/32"   # Solo-dev workstation IP — USER FILL với IP thật
kc_app_alarm_sns_topic_arn = "arn:aws:sns:ap-southeast-1:906286017800:kitehub-ops-alerts"
kc_app_instance_type       = "t3.small"
kc_app_swapfile_size_mb    = 1024
```

CHÚ Ý: `kc_app_admin_ssh_cidr` PHẢI là `/32` IP cụ thể, KHÔNG dùng `0.0.0.0/0` (per GAP-565 F6 mitigation).

### 3.3 Terraform plan — review against expectations

USER chạy plan qua workflow_dispatch (per `release-deploy-standard.md` §9 — human-triggered):

```bash
gh workflow run terraform-apply.yml \
  -f dry_run=true \
  -f confirm=PLAN
```

Wait CI complete, download plan output:

```bash
gh run download <run-id> --name terraform-plan-output -D /tmp/plan-wave-82/
cat /tmp/plan-wave-82/plan.txt | grep -E "Plan:|will be created|must be replaced|will be destroyed"
```

Expected diff (verify per `pre-mutation-state-check.md` real-vs-phantom matrix):

| # | Resource | Action | Notes |
|---|---|---|---|
| 1 | `aws_instance.kc_app` | create | t3.small Singapore |
| 2 | `aws_security_group.kc_app_fe` | create | port 80/443/22 với description đầy đủ per GAP-565 |
| 3 | `aws_iam_role.kc_app_ssm` + `aws_iam_role_policy_attachment.kc_app_ssm` | create | SSM access cho bootstrap |
| 4 | `aws_cloudwatch_metric_alarm.kc_app_memory_high` | create | memory @ 80% threshold per GAP-566 |
| 5 | `aws_cloudwatch_metric_alarm.kc_app_cert_expiry` | create | cert expire 30d ahead per GAP-567 |

`Plan: 5 to add, 0 to change, 0 to destroy` (hoặc tương đương — verify count khớp expectation).

❌ NẾU plan có `to change` hoặc `to destroy` items → STOP. Investigate drift, không apply.

### 3.4 Terraform apply (USER ACTION — workflow_dispatch)

```bash
gh workflow run terraform-apply.yml \
  -f dry_run=false \
  -f confirm=APPLY
```

`confirm=APPLY` verbatim case-sensitive (cognitive checkpoint per `release-deploy-standard.md` §9). Workflow yêu cầu manual approver gate trên GitHub Environment `production`.

Monitor:

```bash
gh run watch <run-id>
```

Apply ~3-5 phút. Verify exit success.

### 3.5 Post-apply verification (USER ACTION)

```bash
# 1. EC2 instance running + public IP assigned
aws ec2 describe-instances \
  --profile dev-admin --region ap-southeast-1 \
  --filters "Name=tag:Name,Values=kc-app" \
  --query 'Reservations[0].Instances[0].[InstanceId,State.Name,PublicIpAddress,PrivateIpAddress]' \
  --output table
# Expected: running + IP assigned

# Save EC2 IP for next steps
KC_APP_IP=$(aws ec2 describe-instances --profile dev-admin --region ap-southeast-1 \
  --filters "Name=tag:Name,Values=kc-app" \
  --query 'Reservations[0].Instances[0].PublicIpAddress' --output text)
echo "kc-app IP: $KC_APP_IP"

# 2. SG audit — verify descriptions present (per GAP-565)
aws ec2 describe-security-groups \
  --profile dev-admin --region ap-southeast-1 \
  --filters "Name=group-name,Values=kc-app-fe" \
  --query 'SecurityGroups[0].IpPermissions[].[FromPort,ToPort,IpRanges[0].CidrIp,IpRanges[0].Description]' \
  --output table
# Expected: 3 rules (80, 443, 22) all với description non-empty + port 22 restricted về admin CIDR

# 3. IAM role attached
aws ec2 describe-instances \
  --profile dev-admin --region ap-southeast-1 \
  --instance-id $(aws ec2 describe-instances --profile dev-admin --region ap-southeast-1 \
    --filters "Name=tag:Name,Values=kc-app" --query 'Reservations[0].Instances[0].InstanceId' --output text) \
  --query 'Reservations[0].Instances[0].IamInstanceProfile.Arn' --output text
# Expected: arn:aws:iam::906286017800:instance-profile/kc-app-ssm-profile

# 4. CloudWatch alarms armed
aws cloudwatch describe-alarms \
  --profile dev-admin --region ap-southeast-1 \
  --alarm-names kc-app-memory-high kc-app-cert-expiry-30d \
  --query 'MetricAlarms[].[AlarmName,StateValue]' --output table
# Expected: INSUFFICIENT_DATA (chưa có metric) hoặc OK
```

❌ NẾU bất kỳ check nào fail → STOP, rollback per §10.

✅ Gate 3.5 PASS → tiếp tục §4 server bootstrap.

---

## 4. Bucket B — Server bootstrap (USER ACTION via SSM)

KHÔNG dùng SSH cho bootstrap — dùng SSM SendCommand để có audit trail CloudTrail.

### 4.1 Install base stack

```bash
KC_APP_INSTANCE_ID=$(aws ec2 describe-instances --profile dev-admin --region ap-southeast-1 \
  --filters "Name=tag:Name,Values=kc-app" --query 'Reservations[0].Instances[0].InstanceId' --output text)

# Install nginx + Node.js 22 + PM2 + certbot
aws ssm send-command \
  --profile dev-admin --region ap-southeast-1 \
  --instance-ids $KC_APP_INSTANCE_ID \
  --document-name "AWS-RunShellScript" \
  --comment "Wave 82 Bucket B — base stack install" \
  --parameters 'commands=[
    "sudo apt-get update -y",
    "sudo apt-get install -y nginx curl ca-certificates",
    "curl -fsSL https://deb.nodesource.com/setup_22.x | sudo -E bash -",
    "sudo apt-get install -y nodejs",
    "sudo npm install -g pm2",
    "sudo snap install --classic certbot",
    "sudo ln -sf /snap/bin/certbot /usr/bin/certbot",
    "node --version && pm2 --version && nginx -v && certbot --version"
  ]' \
  --output text --query 'Command.CommandId'

# Save command-id, wait ~2-3 phút, verify
aws ssm get-command-invocation \
  --profile dev-admin --region ap-southeast-1 \
  --command-id <cmd-id> --instance-id $KC_APP_INSTANCE_ID \
  --query '[Status,StandardOutputContent]' --output text
# Expected: Success
```

### 4.2 Configure swapfile + verify memory alarm (F7 mitigation)

```bash
aws ssm send-command \
  --profile dev-admin --region ap-southeast-1 \
  --instance-ids $KC_APP_INSTANCE_ID \
  --document-name "AWS-RunShellScript" \
  --comment "Wave 82 Bucket B — swapfile 1GB" \
  --parameters 'commands=[
    "sudo fallocate -l 1G /swapfile",
    "sudo chmod 600 /swapfile",
    "sudo mkswap /swapfile",
    "sudo swapon /swapfile",
    "echo \"/swapfile none swap sw 0 0\" | sudo tee -a /etc/fstab",
    "free -h"
  ]'
# Verify output: Swap line shows 1.0Gi total
```

### 4.3 Bootstrap nginx + PM2 + certbot DNS-01 (F10 mitigation)

Upload sister artifacts từ `infrastructure/fe-host/`:

```bash
# Copy nginx config
scp -i ~/.ssh/kite-keypair.pem \
  infrastructure/fe-host/nginx-fe.conf \
  ubuntu@$KC_APP_IP:/tmp/nginx-fe.conf

scp -i ~/.ssh/kite-keypair.pem \
  infrastructure/fe-host/pm2-ecosystem.config.js \
  ubuntu@$KC_APP_IP:/tmp/pm2-ecosystem.config.js

scp -i ~/.ssh/kite-keypair.pem \
  infrastructure/fe-host/certbot-dns-01-setup.sh \
  ubuntu@$KC_APP_IP:/tmp/certbot-dns-01-setup.sh
```

Run certbot DNS-01 bootstrap (cần Cloudflare API token).

**Lưu ý:** `certbot-dns-01-setup.sh` fetch token từ **SSM Parameter Store** (`/kitehub/production/cloudflare-api-token` per terraform `ec2-kc-app.tf` IAM scope). User PHẢI put parameter trước:

```bash
# One-time setup (user, local từ máy có dev-admin profile):
aws ssm put-parameter \
  --profile dev-admin --region ap-southeast-1 \
  --name /kitehub/production/cloudflare-api-token \
  --type SecureString \
  --value '<CLOUDFLARE_TOKEN_VALUE>' \
  --description 'Wave 82 GAP-567 — Certbot DNS-01 challenge token'

# Generate token tại https://dash.cloudflare.com/profile/api-tokens
# Template: "Edit zone DNS" → Zone Resources: include kitehub.me
```

After SSM parameter set, cert script reads it directly via IAM role attached to EC2:

```bash
# Inside EC2 (post-SSM bootstrap):

aws ssm send-command \
  --profile dev-admin --region ap-southeast-1 \
  --instance-ids $KC_APP_INSTANCE_ID \
  --document-name "AWS-RunShellScript" \
  --comment "Wave 82 Bucket B — certbot DNS-01 wildcard cert" \
  --parameters "commands=[
    \"sudo chmod +x /tmp/certbot-dns-01-setup.sh\",
    \"CLOUDFLARE_API_TOKEN='$CF_TOKEN' sudo /tmp/certbot-dns-01-setup.sh kitehub.me\"
  ]"
# Verify: /etc/letsencrypt/live/kitehub.me/fullchain.pem tồn tại
```

Cài nginx config:

```bash
aws ssm send-command \
  --profile dev-admin --region ap-southeast-1 \
  --instance-ids $KC_APP_INSTANCE_ID \
  --document-name "AWS-RunShellScript" \
  --comment "Wave 82 Bucket B — nginx reverse proxy" \
  --parameters 'commands=[
    "sudo cp /tmp/nginx-fe.conf /etc/nginx/sites-available/kite-fe",
    "sudo ln -sf /etc/nginx/sites-available/kite-fe /etc/nginx/sites-enabled/kite-fe",
    "sudo rm -f /etc/nginx/sites-enabled/default",
    "sudo nginx -t",
    "sudo systemctl reload nginx"
  ]'
# Expected: nginx -t báo syntax OK
```

✅ Gate 4 PASS = nginx reload OK + cert files tồn tại + `free -h` show swap 1GB.

---

## 5. Bucket C — FE build + deploy (USER ACTION)

### 5.1 Build FE local hoặc CI

Option A — Build local (faster cho lần đầu):

```bash
# kitehub-frontend
cd kitehub/kitehub-frontend
pnpm install --frozen-lockfile
pnpm build
# Output: .next/standalone/ + .next/static/ + public/

# kiteclass-frontend
cd ../../kiteclass/kiteclass-frontend
pnpm install --frozen-lockfile
pnpm build
```

Option B — Build CI (preferred ongoing):

```bash
gh workflow run frontend-build.yml -f deploy_target=kc-app
# Workflow upload artifact tarball
```

### 5.2 Upload build artifacts qua rsync

```bash
# kitehub-frontend
rsync -avz --delete \
  -e "ssh -i ~/.ssh/kite-keypair.pem" \
  kitehub/kitehub-frontend/.next/standalone/ \
  ubuntu@$KC_APP_IP:/var/www/kitehub-frontend/

rsync -avz --delete \
  -e "ssh -i ~/.ssh/kite-keypair.pem" \
  kitehub/kitehub-frontend/.next/static/ \
  ubuntu@$KC_APP_IP:/var/www/kitehub-frontend/.next/static/

rsync -avz --delete \
  -e "ssh -i ~/.ssh/kite-keypair.pem" \
  kitehub/kitehub-frontend/public/ \
  ubuntu@$KC_APP_IP:/var/www/kitehub-frontend/public/

# kiteclass-frontend — tương tự
rsync -avz --delete \
  -e "ssh -i ~/.ssh/kite-keypair.pem" \
  kiteclass/kiteclass-frontend/.next/standalone/ \
  ubuntu@$KC_APP_IP:/var/www/kiteclass-frontend/
# (lặp .next/static + public)
```

### 5.3 Start PM2 + persist

```bash
# Upload ecosystem config đã làm trong §4.3
aws ssm send-command \
  --profile dev-admin --region ap-southeast-1 \
  --instance-ids $KC_APP_INSTANCE_ID \
  --document-name "AWS-RunShellScript" \
  --comment "Wave 82 Bucket C — PM2 start" \
  --parameters 'commands=[
    "sudo cp /tmp/pm2-ecosystem.config.js /var/www/ecosystem.config.js",
    "cd /var/www && sudo pm2 start ecosystem.config.js",
    "sudo pm2 save",
    "sudo pm2 startup systemd -u ubuntu --hp /home/ubuntu",
    "sudo pm2 list"
  ]'
# Expected: 2 processes (kitehub-frontend:4701 + kiteclass-frontend:4700) status=online
```

### 5.4 Verify nginx reverse proxy

```bash
# Direct EC2 IP test (HTTP — bypass DNS)
aws ssm send-command \
  --profile dev-admin --region ap-southeast-1 \
  --instance-ids $KC_APP_INSTANCE_ID \
  --document-name "AWS-RunShellScript" \
  --comment "Wave 82 Bucket C — verify nginx proxy" \
  --parameters 'commands=[
    "curl -sI http://localhost:4701/ | head -1",
    "curl -sI http://localhost:4700/ | head -1",
    "curl -sI -H \"Host: kitehub.me\" http://localhost/ | head -1"
  ]'
# Expected: HTTP/1.1 200 OK cho cả 3 calls
```

✅ Gate 5 PASS = PM2 2 processes online + nginx proxy 200 OK.

---

## 6. Bucket B — BE CORS sweep (USER ACTION — trước DNS flip)

### 6.1 Audit current CORS state

```bash
bash scripts/sweep-be-cors-origins.sh --audit
```

Script output liệt kê các BE services (kitehub-gateway, kiteclass-gateway, các module BE) + current `CORS_ALLOWED_ORIGINS` env var values. Save output as `documents/04-quality/audits/aws-verification/2026-05-15-wave-82-be-cors-audit-pre.md`.

### 6.2 Add `https://kitehub.me` vào BE CORS allowlist

Edit `kitehub/docker-compose.production.yml` (và các tier deploy file khác):

```yaml
# kitehub-gateway service
environment:
  CORS_ALLOWED_ORIGINS: "https://kitehub.me,https://kiteclass.kitehub.me,https://app.kitehub.me"
  # Wave 82: add kitehub.me (new self-host) — KEEP Vercel preview hostnames during 24h dual-serve
```

Tương tự cho `kiteclass-gateway` + bất kỳ BE service nào có CORS config riêng (xem audit output §6.1 cho list đầy đủ).

### 6.3 Restart BE services

```bash
# SSM vào kh-backend EC2
KH_BACKEND_ID=$(aws ec2 describe-instances --profile dev-admin --region ap-southeast-1 \
  --filters "Name=tag:Name,Values=kh-backend" --query 'Reservations[0].Instances[0].InstanceId' --output text)

aws ssm send-command \
  --profile dev-admin --region ap-southeast-1 \
  --instance-ids $KH_BACKEND_ID \
  --document-name "AWS-RunShellScript" \
  --comment "Wave 82 Bucket B — BE CORS reload" \
  --parameters 'commands=[
    "cd /opt/kitehub && docker-compose -f docker-compose.production.yml up -d --force-recreate kitehub-gateway",
    "cd /opt/kitehub && docker-compose -f docker-compose.production.yml ps"
  ]'
```

### 6.4 Verify CORS preflight cho new origin

```bash
bash scripts/sweep-be-cors-origins.sh --preflight https://kitehub.me
```

Expected ≥6 endpoints return `Access-Control-Allow-Origin: https://kitehub.me`. NẾU <6 → BE chưa pick up env, restart lại.

✅ Gate 6 PASS = preflight ≥6 endpoints OK + audit artifact saved.

---

## 7. Bucket D — DNS cutover (USER ACTION)

### 7.1 T-24h: Lower TTL (rollback fast path)

```bash
# Get CF zone ID
ZID=$(curl -s -H "Authorization: Bearer $CF_TOKEN" \
  "https://api.cloudflare.com/client/v4/zones?name=kitehub.me" \
  | jq -r '.result[0].id')

# Get current A record ID for apex kitehub.me
RID=$(curl -s -H "Authorization: Bearer $CF_TOKEN" \
  "https://api.cloudflare.com/client/v4/zones/$ZID/dns_records?type=A&name=kitehub.me" \
  | jq -r '.result[0].id')

# PATCH TTL 300 → 60s
curl -X PATCH \
  -H "Authorization: Bearer $CF_TOKEN" \
  -H "Content-Type: application/json" \
  "https://api.cloudflare.com/client/v4/zones/$ZID/dns_records/$RID" \
  -d '{"ttl": 60}'

# Verify
curl -s -H "Authorization: Bearer $CF_TOKEN" \
  "https://api.cloudflare.com/client/v4/zones/$ZID/dns_records/$RID" \
  | jq '.result.ttl'
# Expected: 60
```

Wait 24h cho TTL cũ (300s) hết toàn cầu. Trong 24h này KHÔNG flip — chỉ TTL thay đổi.

### 7.2 T-0: Flip A record → EC2 IP

```bash
# PATCH A record content = $KC_APP_IP, keep proxied=true (orange cloud)
curl -X PATCH \
  -H "Authorization: Bearer $CF_TOKEN" \
  -H "Content-Type: application/json" \
  "https://api.cloudflare.com/client/v4/zones/$ZID/dns_records/$RID" \
  -d "{\"type\":\"A\",\"name\":\"kitehub.me\",\"content\":\"$KC_APP_IP\",\"ttl\":60,\"proxied\":true}"
```

### 7.3 Verify propagation (within 5 phút)

```bash
# Check authoritative CF DNS
dig +short @1.1.1.1 kitehub.me
# Expected: trả về IP CF proxy (NOT Vercel cname.vercel-dns.com)

# Check curl từ public
curl -sI https://kitehub.me/ | head -5
# Expected: HTTP/2 200 + Server header chỉ CF/nginx (NOT vercel)
```

### 7.4 Monitor 1h post-flip

```bash
# CF analytics: tỷ lệ 503/525 (SSL errors) — qua dashboard hoặc API
curl -s -H "Authorization: Bearer $CF_TOKEN" \
  "https://api.cloudflare.com/client/v4/zones/$ZID/analytics/dashboard?since=-3600&until=0" \
  | jq '.result.totals.requests.http_status'

# CloudWatch memory + cert alarms
aws cloudwatch describe-alarms --profile dev-admin --region ap-southeast-1 \
  --alarm-names kc-app-memory-high kc-app-cert-expiry-30d \
  --query 'MetricAlarms[].[AlarmName,StateValue]' --output table
# Expected: OK / INSUFFICIENT_DATA, KHÔNG được ALARM

# PM2 health
aws ssm send-command \
  --profile dev-admin --region ap-southeast-1 \
  --instance-ids $KC_APP_INSTANCE_ID \
  --document-name "AWS-RunShellScript" \
  --parameters 'commands=["sudo pm2 list","free -h"]'
# Expected: 2 online + restart count = 0 + swap usage <50%
```

✅ Gate 7 PASS = `dig` → new IP + HTTP 200 + <1% 5xx errors + PM2 stable + memory <80%.

---

## 8. Acceptance gate (Wave 82 Bucket B-D closure)

Per Wave 82 plan §5 Verification Gates:

| Bucket | Gate | Verify command | Status |
|---|---|---|---|
| B (terraform) | EC2 + SG + IAM + alarms created | §3.5 | [ ] |
| B (server) | nginx reload + cert + swap 1GB | §4 | [ ] |
| B (CORS) | sweep --preflight ≥6 endpoints PASS | §6.4 | [ ] |
| C (deploy) | PM2 2 processes online + nginx 200 OK | §5.4 | [ ] |
| D (DNS) | `dig kitehub.me` → new IP + curl 200 + <1% 5xx 1h | §7.4 | [ ] |

Audit artifacts required (per `pre-mutation-state-check.md`):
- [ ] `documents/04-quality/audits/aws-verification/2026-05-15-wave-82-bucket-b-pre-apply.md`
- [ ] `documents/04-quality/audits/aws-verification/2026-05-15-wave-82-be-cors-audit-pre.md`
- [ ] `documents/04-quality/audits/aws-verification/2026-05-15-wave-82-dns-cutover-post.md`

---

## 9. Rollback plan

### 9.1 Bucket B fail (terraform / server bootstrap)

Nếu apply fail giữa chừng hoặc post-apply check fail:

```bash
# Destroy only kc-app resources (cẩn thận, KHÔNG destroy kh-backend)
cd infrastructure/terraform-aws
terraform destroy \
  -target=aws_instance.kc_app \
  -target=aws_security_group.kc_app_fe \
  -target=aws_iam_role.kc_app_ssm \
  -target=aws_cloudwatch_metric_alarm.kc_app_memory_high \
  -target=aws_cloudwatch_metric_alarm.kc_app_cert_expiry
```

Vercel vẫn serve `kitehub.me` → public không bị ảnh hưởng. Investigate offline, re-apply khi fix xong.

### 9.2 Bucket C fail (PM2 / build)

```bash
# Revert symlink to previous working build snapshot
aws ssm send-command --profile dev-admin --region ap-southeast-1 \
  --instance-ids $KC_APP_INSTANCE_ID \
  --document-name "AWS-RunShellScript" \
  --parameters 'commands=[
    "sudo pm2 stop all",
    "sudo cp -r /var/www/_backup_last_known_good/* /var/www/",
    "sudo pm2 start /var/www/ecosystem.config.js"
  ]'
```

Nếu không có backup → trang maintenance HTML tĩnh:

```bash
aws ssm send-command --profile dev-admin --region ap-southeast-1 \
  --instance-ids $KC_APP_INSTANCE_ID \
  --document-name "AWS-RunShellScript" \
  --parameters 'commands=[
    "sudo pm2 stop all",
    "echo \"<h1>Bảo trì hệ thống — vui lòng quay lại sau\" | sudo tee /var/www/maintenance.html",
    "sudo nginx -s reload"
  ]'
```

### 9.3 Bucket D fail (DNS flip + 5xx errors)

CRITICAL — flip ngược về Vercel trong 5 phút (TTL 60s):

```bash
# Set A record back to Vercel CNAME target IP (hoặc switch record type về CNAME)
# Vercel cũ: kitehub.me CNAME cname.vercel-dns.com
# Cách nhanh nhất — delete A record + re-create CNAME

# Delete current A record
curl -X DELETE \
  -H "Authorization: Bearer $CF_TOKEN" \
  "https://api.cloudflare.com/client/v4/zones/$ZID/dns_records/$RID"

# Create CNAME → Vercel
curl -X POST \
  -H "Authorization: Bearer $CF_TOKEN" \
  -H "Content-Type: application/json" \
  "https://api.cloudflare.com/client/v4/zones/$ZID/dns_records" \
  -d '{"type":"CNAME","name":"kitehub.me","content":"cname.vercel-dns.com","ttl":60,"proxied":true}'

# Verify propagation
dig +short @1.1.1.1 kitehub.me
```

Wait 5 phút (TTL 60s) cho propagation. Investigate EC2 offline.

---

## 10. Troubleshooting matrix

| Triệu chứng | Nguyên nhân khả nghi | Fix |
|---|---|---|
| `certbot` fail DNS-01 challenge timeout | Cloudflare API token thiếu quyền edit DNS hoặc token expired | Verify token scope `Zone:DNS:Edit` cho zone `kitehub.me`; rotate token nếu expired |
| `certbot` renewal cron fail silent | IAM role thiếu quyền đọc Secrets Manager hoặc SSM agent down | Check CloudWatch alarm `kc-app-cert-expiry-30d` ALARM state; SSM `sudo certbot renew --dry-run` để repro |
| PM2 process restart loop (>5 restart trong 10 phút) | OOM — t3.small RAM tight + ISR regen | Verify swapfile active (`swapon --show`); bump `max_memory_restart` trong ecosystem.config.js; xem CloudWatch `kc-app-memory-high` |
| `curl https://kitehub.me` → 525 SSL handshake fail | Cert chưa install / nginx config sai SSL block | SSM verify `/etc/letsencrypt/live/kitehub.me/fullchain.pem` tồn tại; `sudo nginx -t` check syntax; reload |
| CORS reject sau DNS flip — FE call BE bị block | BE chưa pick up `CORS_ALLOWED_ORIGINS` env mới | Re-run `bash scripts/sweep-be-cors-origins.sh --preflight https://kitehub.me`; identify endpoint thiếu; force-recreate container BE |
| `dig kitehub.me` vẫn trả Vercel sau 30 phút | DNS cache local resolver stale | Test với `dig @1.1.1.1` (CF authoritative) hoặc `dig @8.8.8.8` (Google); flush local: `sudo systemctl restart systemd-resolved` |
| PM2 startup không persist sau reboot | `pm2 startup` script chưa run với sudo | SSM: `sudo pm2 startup systemd -u ubuntu --hp /home/ubuntu` + `sudo pm2 save` |
| nginx 502 Bad Gateway | PM2 process không listen port hoặc nginx upstream sai | SSM `sudo pm2 list` xem status; verify `ecosystem.config.js` ports 4700/4701 khớp `nginx-fe.conf` proxy_pass |
| CloudWatch memory alarm INSUFFICIENT_DATA >2h | CloudWatch agent chưa install hoặc IAM role thiếu permission | SSM: `sudo systemctl status amazon-cloudwatch-agent`; verify `kc-app-ssm-profile` có policy `CloudWatchAgentServerPolicy` |
| BE CORS sweep `--audit` báo conflict origin | Cấu hình docker-compose tier khác có CORS riêng | Audit từng tier file (production / staging / beta); align CORS_ALLOWED_ORIGINS đồng nhất |

---

## 11. References

- ADR: [`ADR-031: FE Self-Host trên AWS EC2 (t3.small) — Phase 1 BETA`](../../02-architecture/adr/ADR-031-fe-self-host-aws-ec2.md)
- Wave plan: [`wave-2026-05-15-82-fe-self-host.md`](../../03-planning/waves/wave-2026-05-15-82-fe-self-host.md)
- Gap: [`GAP-565 — EC2 Security Group description + port restriction`](../../04-quality/gaps/GAP-565-wave-82-ec2-security-group-description-port-restriction.md)
- Gap: [`GAP-566 — t3.small RAM tuning (PM2 + swapfile + memory alarm)`](../../04-quality/gaps/GAP-566-wave-82-t3-small-ram-tuning-pm2-swapfile-memory-alarm.md)
- Gap: [`GAP-567 — certbot DNS-01 cert renewal + 30d expiry monitor`](../../04-quality/gaps/GAP-567-wave-82-certbot-dns-01-cert-renewal-30d-expiry-monitor.md)
- Gap: [`GAP-568 — BE CORS allowlist sweep pre-DNS-flip`](../../04-quality/gaps/GAP-568-wave-82-be-cors-allowlist-sweep-pre-dns-flip.md)
- Sister runbook: [`secrets-seeding-runbook.md`](secrets-seeding-runbook.md)
- Sister runbook: [`dns-setup-runbook.md`](dns-setup-runbook.md)
- Rule: [`release-deploy-standard.md`](../../../.claude/rules/release-deploy-standard.md) §4 deploy execution
- Rule: [`concurrent-production-mutation-ops.md`](../../../.claude/rules/concurrent-production-mutation-ops.md) — sequential B→C→D mandate
- Rule: [`pre-mutation-state-check.md`](../../../.claude/rules/pre-mutation-state-check.md) — audit artifact pre-apply
- Rule: [`agent-aws-access.md`](../../../.claude/rules/agent-aws-access.md) §4.3 — Tier 3 mutations user-executed
- Rule: [`aws-observability-first.md`](../../../.claude/rules/aws-observability-first.md) — CloudTrail logging baseline
- Rule: [`aws-sg-description-ascii.md`](../../../.claude/rules/aws-sg-description-ascii.md) — F6 mitigation source
