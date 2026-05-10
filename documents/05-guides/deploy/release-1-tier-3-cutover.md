# Release Lần 1 — Tier 3 Cutover Runbook

**Đối tượng:** Solo dev cutover Phase 1 BETA `kitehub.me` từ FE-only state (Vercel apex live + AWS BE stopped) sang full HTTPS stack ready cho beta tenant invite.
**Trạng thái:** ⏳ Chưa thực thi — chờ AWS Activate approval (D+14 reminder 2026-05-23) HOẶC user accept paying ~$85/mo trước approval.
**Last reviewed:** 2026-05-10 (session sequence shipped Tier 1 + 2)
**Tham chiếu:**
- Tier 1: `vercel-production-setup.md` (FE bind + env var) — DONE 2026-05-09
- Tier 2: `cloudflare-setup.md` §6.2 (Origin Cert generate) — DONE 2026-05-10
- Tier 3 standards: `cloudflare-setup.md` §6.1 + §6.3 + §6.4 (Full strict / Always HTTPS / HSTS)
- Tier 3 CLI agent-runnable §5+§6+§7: `scripts/cloudflare-dns.sh` (token extended 2026-05-10 — Zone:DNS:Edit + Zone:SSL:Edit + Zone:Zone Settings:Edit + Zone:Read)
- Cost: `aws-cost-scheduling.md` §4 (resume manual override)
- Decision context: GAP-458 + `release-1-deploy-plan.md` §2.2

---

## 0. Pre-flight checklist — TẤT CẢ phải ✅ trước khi bắt đầu Tier 3

### Tier 1 + 2 verification

- [x] Domain `kitehub.me` claimed (Namecheap qua Student Pack 2026-05-09)
- [x] Cloudflare nameservers active (`melody.ns.cloudflare.com` + `randy.ns.cloudflare.com`)
- [x] DNS records: 9 entries (apex CNAME Vercel + wildcard + api → ALB + 3 MX + 2 TXT + www)
- [x] Vercel apex `kitehub.me` bound + Let's Encrypt cert active (R13, valid 2026-05-10 → 2026-08-08, auto-renew)
- [x] Vercel env var `NEXT_PUBLIC_API_URL=https://api.kitehub.me` Production set
- [x] Cloudflare Email Routing active (`admin@/support@kitehub.me` → Gmail forward)
- [x] **Cloudflare Origin Cert generated** (Path C Dashboard manual; saved `~/.gcal-mcp/cloudflare-origin-cert/`)
- [x] Vercel CLI + Wrangler CLI + Cloudflare API token stored

### AWS state pre-check

- [x] AWS account ID: `906286017800`
- [x] Region: `ap-southeast-1` (Singapore)
- [x] EC2 instances exist: `kitehub-kh-backend` + `kitehub-kc-app` (t3.medium, STOPPED)
- [x] RDS exists: `kitehub-postgres` (postgres engine, STOPPED)
- [x] ALB exists: `kitehub-alb-224105328.ap-southeast-1.elb.amazonaws.com` (active, no HTTPS listener)
- [ ] **AWS Activate approval received** (calendar reminder 2026-05-23) — recommend wait nếu sắp hết deadline
- [ ] No pending billing alerts

### Cost expectation

| Resource | Monthly cost (running) |
|---|---|
| EC2 t3.medium × 2 | ~$60 |
| RDS db.t3.micro | ~$15 (Free Tier Yr1) |
| ALB | ~$16 |
| ElastiCache (nếu provisioned) | ~$10-15 |
| Data transfer | ~$5 |
| **Total** | **~$85-100/month** |

→ Activate $1k credit cover ~10 tháng. Without credit = pay-out-of-pocket. Decide kèm:

**Option A:** wait Activate approval (D+14 reminder) → resume EC2 sau khi approved
**Option B:** resume now, pay first 1-2 weeks if Activate denied → start Phase 1 BETA testing earlier

---

## 0.5 Two execution paths — CLI manual vs workflow_dispatch

§1-§3 (AWS write actions) chấp nhận 2 execution paths:

### Path X — Manual CLI (user paste commands locally)

**When:** Bạn có AWS CLI authed locally (đã `aws sts get-caller-identity` returns 906286017800), prefer hands-on control, comfortable đọc output từng step.

**How:** Follow §1-§3 dưới — paste từng `aws ec2 ...` / `aws rds ...` / `aws acm ...` / `aws elbv2 ...` command vào terminal.

**Pros:** Zero setup ngoài AWS CLI; full visibility từng step; easy abort mid-sequence.
**Cons:** Bạn phải paste 15+ commands; nhỡ typo → re-run.

### Path Y — workflow_dispatch GitHub Actions (1-click)

**When:** Bạn muốn 1-click cutover; audit trail trong GitHub Actions; OIDC ephemeral creds (KHÔNG cần admin AWS key trên laptop).

**Workflow:** `.github/workflows/tier-3-cutover.yml` (per `release-deploy-standard.md` §9 carve-out: human-triggered + confirm-input "APPLY" + narrow OIDC role).

**Inputs:**
- `confirm`: phải gõ `APPLY` verbatim
- `step`: `verify-only` / `resume-compute` (§1) / `import-cert` (§2) / `alb-https-listener` (§3) / `all` (§1+2+3)
- `cert_arn_override` (optional): nếu đã có ACM cert ARN, paste vào để skip §2 import

**One-time setup required (PR #TBD):**
1. Apply terraform (qua existing `terraform-apply.yml` workflow_dispatch) để tạo new IAM role `kitehub-github-tier-3-cutover`. Capture output `github_tier_3_cutover_role_arn`.
2. Add **Repository Variable** `AWS_TIER_3_CUTOVER_ROLE_ARN` = `<arn from step 1>` (Settings → Secrets and variables → Actions → Variables tab → New).
3. Add **Repository Secrets** (cho §2 import):
   - `CLOUDFLARE_ORIGIN_CERT_PEM` = paste content `~/.gcal-mcp/cloudflare-origin-cert/kitehub.me.pem`
   - `CLOUDFLARE_ORIGIN_CERT_KEY` = paste content `~/.gcal-mcp/cloudflare-origin-cert/kitehub.me.key`
4. Verify GitHub Environment `production` exists (Settings → Environments). Optional: add protection rules (required reviewer = solo dev, wait timer 0 min).

**How to trigger:**
1. GitHub Actions tab → **Tier 3 Cutover (kitehub.me)**
2. Run workflow → Branch `main` → Inputs:
   - confirm: `APPLY`
   - step: `verify-only` first (sanity), then `all` (or step-by-step for safety)
3. Click **Run workflow**

**Pros:** 1-click; OIDC ephemeral creds (no admin key on laptop); audit trail; idempotent re-run safe (`verify-only` first).
**Cons:** ~1h setup (terraform apply IAM + 3 secrets/vars); GitHub Environment protection adds 1 approval click per run.

### Recommended

Path X cho first run (full visibility); Path Y cho subsequent re-runs (vd cert renewal yearly, listener cert rotation). Path Y setup cost amortizes nếu cutover repeated ≥3 lần.

§5+§6+§7 (Cloudflare ops) luôn agent CLI — `scripts/cloudflare-dns.sh` (token extended scope per §6).

---

## 1. Step 1 — Resume EC2 + RDS từ cost-save (~5-10 phút)

### 1.1 Read `aws-cost-scheduling.md` §4

Manual override commands cho EC2 + RDS start. Schedule auto-stop tonight at 22:00 ICT cũng disable temporarily nếu cần.

### 1.2 Verify AWS CLI authed

```bash
aws sts get-caller-identity --query Account --output text
# Expected: 906286017800
```

### 1.3 Get instance IDs

```bash
aws ec2 describe-instances --region ap-southeast-1 \
  --filters "Name=tag:Name,Values=kitehub-kh-backend,kitehub-kc-app" \
  --query 'Reservations[].Instances[].[Tags[?Key==`Name`]|[0].Value,InstanceId,State.Name]' \
  --output table
```

### 1.4 Start EC2 instances

```bash
# Get IDs
KH_BACKEND_ID=$(aws ec2 describe-instances --region ap-southeast-1 \
  --filters "Name=tag:Name,Values=kitehub-kh-backend" \
  --query 'Reservations[0].Instances[0].InstanceId' --output text)
KC_APP_ID=$(aws ec2 describe-instances --region ap-southeast-1 \
  --filters "Name=tag:Name,Values=kitehub-kc-app" \
  --query 'Reservations[0].Instances[0].InstanceId' --output text)

# Start both
aws ec2 start-instances --region ap-southeast-1 \
  --instance-ids "$KH_BACKEND_ID" "$KC_APP_ID"

# Wait running
aws ec2 wait instance-running --region ap-southeast-1 \
  --instance-ids "$KH_BACKEND_ID" "$KC_APP_ID"

# Verify
aws ec2 describe-instances --region ap-southeast-1 \
  --instance-ids "$KH_BACKEND_ID" "$KC_APP_ID" \
  --query 'Reservations[].Instances[].[Tags[?Key==`Name`]|[0].Value,State.Name,PublicIpAddress]' \
  --output table
```

### 1.5 Start RDS

```bash
aws rds start-db-instance --region ap-southeast-1 \
  --db-instance-identifier kitehub-postgres

# RDS takes 3-5 phút to be available
aws rds wait db-instance-available --region ap-southeast-1 \
  --db-instance-identifier kitehub-postgres

# Verify
aws rds describe-db-instances --region ap-southeast-1 \
  --db-instance-identifier kitehub-postgres \
  --query 'DBInstances[0].[DBInstanceStatus,Endpoint.Address]' \
  --output table
```

### 1.6 Wait for application boot (~2 phút)

EC2 boot ~30s; Spring Boot app start ~60-120s; database connection establish ~30s. Total ~2-3 phút post EC2-running.

```bash
# SSH (nếu có) hoặc SSM Session Manager:
# aws ssm start-session --region ap-southeast-1 --target $KH_BACKEND_ID

# Verify health endpoint locally on EC2:
# curl http://localhost:8080/actuator/health
```

### 1.7 Verify ALB target group healthy

```bash
TG_ARN=$(aws elbv2 describe-target-groups --region ap-southeast-1 \
  --query 'TargetGroups[?contains(TargetGroupName, `kitehub`)].TargetGroupArn' \
  --output text | head -1)

aws elbv2 describe-target-health --region ap-southeast-1 \
  --target-group-arn "$TG_ARN" \
  --query 'TargetHealthDescriptions[].[Target.Id,TargetHealth.State]' \
  --output table
# Expected: all targets State=healthy
```

### 1.8 Cờ đỏ Step 1

| Symptom | Fix |
|---|---|
| `instance-running` wait timeout > 5min | Check Status Check console; verify cloud-init logs |
| ALB target unhealthy after 5min | Spring Boot still booting; wait additional 2-3 min |
| RDS không khởi động | Check Status; có thể cần manual MaintenanceWindow trigger |
| EC2 boot loop / OOM | Per `right-size-stress-test.md` — verify t3.medium memory đủ |

---

## 2. Step 2 — Import Cloudflare Origin Cert vào AWS ACM (~3 phút)

### 2.1 Verify cert files local

```bash
ls -la ~/.gcal-mcp/cloudflare-origin-cert/
# Expected: kitehub.me.pem + kitehub.me.key (both chmod 600)

# Validate cert
openssl x509 -in ~/.gcal-mcp/cloudflare-origin-cert/kitehub.me.pem -noout \
  -subject -dates -ext subjectAltName
# Expected:
#   subject=O = "CloudFlare, Inc.", OU = CloudFlare Origin CA, CN = CloudFlare Origin Certificate
#   notBefore=May 10 05:55:00 2026 GMT
#   notAfter=May  6 05:55:00 2041 GMT
#   X509v3 Subject Alternative Name:
#     DNS:*.kitehub.me, DNS:kitehub.me
```

### 2.2 Import vào ACM ap-southeast-1

```bash
CERT_ARN=$(aws acm import-certificate --region ap-southeast-1 \
  --certificate fileb://$HOME/.gcal-mcp/cloudflare-origin-cert/kitehub.me.pem \
  --private-key fileb://$HOME/.gcal-mcp/cloudflare-origin-cert/kitehub.me.key \
  --tags Key=Name,Value=cloudflare-origin-kitehub.me Key=GAP,Value=GAP-458 \
  --query 'CertificateArn' --output text)

echo "✓ Cert imported: $CERT_ARN"
# Save ARN for next step
```

### 2.3 Verify ACM listing

```bash
aws acm list-certificates --region ap-southeast-1 \
  --query 'CertificateSummaryList[?contains(DomainName, `kitehub.me`)].[CertificateArn,DomainName,Status]' \
  --output table
# Expected: cert ARN + DomainName=kitehub.me + Status=ISSUED
```

### 2.4 Cờ đỏ Step 2

| Symptom | Fix |
|---|---|
| `Could not parse certificate` | Verify .pem format intact (no extra newlines); regenerate if corrupted |
| `Could not parse private key` | Verify .key format (PKCS8 PEM); convert via `openssl rsa -in old.key -out new.key` |
| Cert + Key mismatch | Per session 2026-05-10 verify: `openssl x509 -modulus -noout` vs `openssl rsa -modulus -noout` md5 phải match |

---

## 3. Step 3 — Add HTTPS listener vào ALB (~5 phút)

### 3.1 Get ALB ARN

```bash
ALB_ARN=$(aws elbv2 describe-load-balancers --region ap-southeast-1 \
  --query 'LoadBalancers[?LoadBalancerName==`kitehub-alb`].LoadBalancerArn' \
  --output text)
echo "ALB ARN: $ALB_ARN"
```

### 3.2 Get target group ARN

```bash
TG_ARN=$(aws elbv2 describe-target-groups --region ap-southeast-1 \
  --query 'TargetGroups[?contains(TargetGroupName, `kitehub`)].TargetGroupArn' \
  --output text | head -1)
echo "Target group ARN: $TG_ARN"
```

### 3.3 Check existing listeners

```bash
aws elbv2 describe-listeners --region ap-southeast-1 \
  --load-balancer-arn "$ALB_ARN" \
  --query 'Listeners[].[ListenerArn,Port,Protocol]' \
  --output table
# Expected: HTTP:80 listener exists; HTTPS:443 missing
```

### 3.4 Create HTTPS listener với cert binding

```bash
aws elbv2 create-listener --region ap-southeast-1 \
  --load-balancer-arn "$ALB_ARN" \
  --protocol HTTPS \
  --port 443 \
  --certificates "CertificateArn=$CERT_ARN" \
  --ssl-policy ELBSecurityPolicy-TLS13-1-2-2021-06 \
  --default-actions "Type=forward,TargetGroupArn=$TG_ARN"
```

### 3.5 Update HTTP listener → redirect HTTPS (optional)

```bash
HTTP_LISTENER_ARN=$(aws elbv2 describe-listeners --region ap-southeast-1 \
  --load-balancer-arn "$ALB_ARN" \
  --query 'Listeners[?Port==`80`].ListenerArn' --output text)

aws elbv2 modify-listener --region ap-southeast-1 \
  --listener-arn "$HTTP_LISTENER_ARN" \
  --default-actions 'Type=redirect,RedirectConfig={Protocol=HTTPS,Port=443,Host=#{host},Path=/#{path},Query=#{query},StatusCode=HTTP_301}'
```

→ Mọi HTTP request tới ALB tự redirect HTTPS.

### 3.6 Verify HTTPS listener active

```bash
aws elbv2 describe-listeners --region ap-southeast-1 \
  --load-balancer-arn "$ALB_ARN" \
  --query 'Listeners[].[Port,Protocol,Certificates[0].CertificateArn]' \
  --output table
# Expected: 80 HTTP + 443 HTTPS với cert ARN
```

### 3.7 Cờ đỏ Step 3

| Symptom | Fix |
|---|---|
| `Listener already exists for port 443` | Modify existing listener instead: `aws elbv2 modify-listener --listener-arn ... --certificates CertificateArn=$CERT_ARN` |
| Target group ARN không match | Re-list TGs; có thể nhiều TGs — pick correct cho kitehub backend |
| SSL policy không support TLS 1.3 | Fall back ELBSecurityPolicy-2016-08 (older but compatible) |

---

## 4. Step 4 — Test HTTPS direct (~2 phút)

### 4.1 Test ALB HTTPS direct (bypass Cloudflare)

```bash
# Ignore cert verify (vì Origin Cert chỉ valid Cloudflare ↔ ALB, không browser CA chain)
curl -k -sI -m 10 https://kitehub-alb-224105328.ap-southeast-1.elb.amazonaws.com/actuator/health
# Expected: HTTP/1.1 200 OK + Spring Boot health body
```

### 4.2 Test apex API qua Cloudflare (vẫn DNS only mode)

```bash
# api.kitehub.me hiện DNS only → resolve trực tiếp ALB IP
curl -sI -m 10 https://api.kitehub.me/actuator/health
# Expected: 200 OK với CN=kitehub.me cert (Cloudflare Origin Cert presented bởi ALB)
# ⚠️ Nếu browser CA validation fail → expected behavior (Origin Cert not browser-trusted)
```

→ Lúc này ALB serving HTTPS với Cloudflare Origin Cert OK. Browser sẽ warn vì cert KHÔNG do browser-trusted CA ký. Đây chính là lý do cần Step 5 — switch Cloudflare proxy ON để browser thấy Cloudflare's Universal SSL cert (browser-trusted) thay vì Origin Cert.

---

## 5. Step 5 — Switch Cloudflare CNAME `api` → Proxied (~1 phút)

### 5.1 Toggle qua helper script

```bash
# Pre-condition: env vars loaded từ ~/.bashrc
source ~/.bashrc

bash scripts/cloudflare-dns.sh toggle-proxy api.kitehub.me
# Expected: Toggle api.kitehub.me (CNAME) from proxied=False → true
#           OK
```

### 5.2 Verify Proxied (ngay)

```bash
bash scripts/cloudflare-dns.sh list 2>&1 | grep "api.kitehub.me"
# Expected: 'api.kitehub.me ... Proxied'
```

### 5.3 Verify DNS resolve qua Cloudflare proxy

```bash
getent hosts api.kitehub.me
# Expected: Cloudflare IP (vd 104.21.x.x hoặc 172.67.x.x), KHÔNG phải AWS ALB IP

# Cert presented (qua Cloudflare):
echo | timeout 5 openssl s_client -connect api.kitehub.me:443 -servername api.kitehub.me 2>/dev/null \
  | openssl x509 -noout -subject -issuer
# Expected: subject=CN=kitehub.me, issuer=Cloudflare Inc ECC CA-3 (Universal SSL, browser-trusted)
```

→ Browser giờ thấy Cloudflare's Universal SSL cert (đã trust); Cloudflare ↔ ALB dùng Origin Cert (Cloudflare-internal trust).

---

## 6. Step 6 — Switch Cloudflare SSL mode → Full (strict) (~1 phút)

**Yêu cầu:** Cloudflare API token với scope `Zone:DNS:Edit + Zone:SSL and Certificates:Edit + Zone:Zone Settings:Edit + Zone:Zone:Read`. Setup per `documents/05-guides/dev/cloudflare-cli-setup.md` §3 (token đã extended 2026-05-10).

### 6.1 Verify current state

```bash
bash scripts/cloudflare-dns.sh get-ssl-mode
# Pre-Tier-3 expected: full (Universal SSL active, NOT strict yet)
```

### 6.2 Switch → strict (CLI agent-runnable)

```bash
bash scripts/cloudflare-dns.sh set-ssl-mode strict
# Expected: OK — SSL mode now: strict
```

### 6.3 Alternative — Cloudflare Dashboard (fallback nếu token chưa extended)

🔗 https://dash.cloudflare.com/3adf4fc6532225cb928acbf57ca0206c/kitehub.me/ssl-tls/configuration

1. Sidebar **SSL/TLS** → **Overview** (hoặc **Configuration**)
2. **SSL/TLS encryption mode** dropdown → change to **Full (strict)**
3. Save

### 6.4 Test end-to-end với Full strict

```bash
# Browser-style test (verify cert chain valid)
curl -sI -m 10 https://api.kitehub.me/actuator/health
# Expected: HTTP/2 200, server=cloudflare, cert valid

# Test apex Vercel
curl -sI -m 10 https://kitehub.me
# Expected: 200 từ Vercel
```

### 6.5 Cờ đỏ Step 6

| Symptom | Fix |
|---|---|
| `525 SSL handshake failed` từ Cloudflare | Origin Cert chưa import ACM hoặc ALB chưa serve HTTPS — quay lại Step 2-3 |
| `526 Invalid SSL certificate` | Origin Cert mismatch hoặc expired; verify cert active trong ACM |
| Browser cert warning | Full (strict) chưa apply; verify `bash scripts/cloudflare-dns.sh get-ssl-mode` returns `strict` |

---

## 7. Step 7 — Enable Always Use HTTPS (~1 phút)

### 7.1 Verify current state

```bash
bash scripts/cloudflare-dns.sh get-always-https
# Pre-Tier-3 expected: off
```

### 7.2 Toggle ON (CLI agent-runnable)

```bash
bash scripts/cloudflare-dns.sh set-always-https on
# Expected: OK — Always Use HTTPS now: on
```

### 7.3 Verify HTTP→HTTPS redirect

```bash
curl -sI -m 10 http://api.kitehub.me/actuator/health
# Expected: HTTP/1.1 301 Moved Permanently + location: https://...
```

### 7.4 Verify apex (Vercel)

Vercel apex bind đã có Always Use HTTPS built-in. Verify:

```bash
curl -sI -m 10 http://kitehub.me
# Expected: HTTP/1.1 308 Permanent Redirect → https://kitehub.me/
```

---

## 8. Step 8 — Defer HSTS (1+ tuần stable trước khi enable)

⚠️ **HSTS preload 1-year commitment** — KHÔNG enable ngay. Wait ≥1 tuần stable HTTPS để verify không phá:

| When | Action |
|---|---|
| **Tuần 1 post-Tier 3** | Monitor logs / errors / cert renewal |
| **Tuần 2** | Enable HSTS với max-age conservative (vd 6 tháng = 15768000s); KHÔNG include subdomains đầu tiên |
| **Tháng 2** | Bump max-age 1 năm; include subdomains nếu wildcard cert ổn |
| **Tháng 3+** | Enable HSTS preload (submit hstspreload.org) — IRREVERSIBLE 1-year cho domain |

Tạo Calendar reminder:

```
echo "Schedule HSTS enable check 2026-05-17 (1 week post-Tier 3)" >&2
# (Sẽ create event qua MCP khi Tier 3 thực thi)
```

---

## 9. Step 9 — Smoke test suite (~10 phút)

### 9.1 Per `release-1-deploy-plan.md` §2.4 BETA smoke tests

```bash
bash scripts/smoke-test.sh 2>&1 | tail -20
# Expected: 18 assertions pass per Wave 26 GAP-377
```

### 9.2 Manual browser tests

1. https://kitehub.me — Vercel FE loads
2. https://api.kitehub.me/actuator/health — backend responsive
3. DevTools Network tab — verify FE call api.kitehub.me thành công, no CORS errors
4. Beta access form (`/beta-access` hoặc tương đương) — test submit

### 9.3 Verify Email Routing functional

```bash
# Send test email từ external Gmail tới admin@kitehub.me
# Expect forward về personal Gmail trong 1-3 phút
```

---

## 10. Step 10 — Update Calendar reminder D+1 monitoring (~1 phút)

```bash
# Sẽ tạo qua Google Calendar MCP khi thực thi:
# Title: "Tier 3 cutover D+1 verify - kitehub.me"
# Date: <next day after Tier 3>
# Action: re-run smoke test, check error logs, verify cert + DNS still active
```

---

## 11. Cost-aware decision — auto-stop schedule

Sau Tier 3 verify, có 2 paths:

### Path 1 — Keep running (recommended nếu beta tenants imminent)

- EC2 + RDS chạy 24/7 — beta tenants có thể access bất cứ lúc nào
- Cost: ~$85-100/month
- Activate credit cover ~10 tháng nếu approved

### Path 2 — Re-stop với schedule (nếu beta launch defer)

- Re-enable EventBridge stop/start schedule per `aws-cost-scheduling.md`
- Save ~$30-40/month if instances stopped 50% of time
- Risk: tenant access during stopped hours = 503

→ Pick based on Phase 1 BETA invite timeline. If invites đã sent → Path 1.

---

## 12. Rollback procedure

Nếu Tier 3 fail mid-execution:

| Step failed | Rollback |
|---|---|
| Step 1 (EC2 resume) | Stop instances back: `aws ec2 stop-instances ...` |
| Step 2 (ACM import) | Delete cert: `aws acm delete-certificate --certificate-arn ...` |
| Step 3 (HTTPS listener) | Delete listener: `aws elbv2 delete-listener --listener-arn ...` |
| Step 5 (Cloudflare Proxy) | Toggle back DNS only: `bash scripts/cloudflare-dns.sh toggle-proxy api.kitehub.me` |
| Step 6 (Full strict) | Switch back Flexible (Cloudflare Dashboard) |
| Step 7 (Always HTTPS) | Toggle OFF |

→ Each step independent reversible. Worst case: revert all back to Tier 2 state (Vercel apex + DNS only mode).

---

## 13. Acceptance criteria

Tier 3 considered DONE when:

- [ ] EC2 + RDS healthy + ALB target group healthy
- [ ] ACM cert imported + ALB HTTPS listener active
- [ ] api.kitehub.me Cloudflare Proxied + Full(strict)
- [ ] kitehub.me apex Vercel live (already done Tier 1)
- [ ] HTTP → HTTPS redirect (Cloudflare Always Use HTTPS) ON
- [ ] Smoke test 18/18 pass
- [ ] Manual browser test: kitehub.me + api.kitehub.me both 200 with browser-trusted cert
- [ ] Email Routing test still functional post-cutover
- [ ] No P0 errors in 1 hour post-cutover

→ Flip GAP-458 status PARTIAL → 🟢 DONE; update `release-1-deploy-plan.md` §2 progression to "Phase 1 BETA infra READY".

---

## 14. Scheduling recommendation

**Best time to execute Tier 3:**

| Pre-condition | Why |
|---|---|
| AWS Activate approved (D+14 = 2026-05-23) | Credit cover compute cost during stable period |
| Beta tenant invite list ready | Capitalize on resume — no idle running |
| Solo dev có 2-3h block buffer | Cutover + verify + monitor logs |
| Weekday morning ICT | AWS support response time ngắn nếu hit infra issue |

**KHÔNG nên execute Tier 3:**

- Friday afternoon / weekend without monitoring
- Before AWS Activate decision (cost burn unknown)
- Without Origin Cert ready (Tier 2 done) — KHÔNG có ALB cert
- Without rollback plan understood

---

## 15. Cross-references

- **Tier 1:** `vercel-production-setup.md` — FE bind + env var (DONE 2026-05-09)
- **Tier 2:** `cloudflare-setup.md` §6.2 — Origin Cert (DONE 2026-05-10)
- **Tier 3 standards:** `cloudflare-setup.md` §6.1 / §6.3 / §6.4 — Full strict, Always HTTPS, HSTS
- **Cost ops:** `aws-cost-scheduling.md` §4 — manual override resume
- **Smoke test:** `release-1-deploy-plan.md` §2.4 — 18 assertions
- **Plan:** `release-1-deploy-plan.md` §2.2 deploy steps
- **Decision context:** GAP-458 (`kitehub-me-domain-decision.md`)
- **Helper scripts:** `scripts/cloudflare-dns.sh` (toggle-proxy + others)
- **Rule:** `agent-action-bias.md` (agent-do for AWS read; user-confirm for AWS write)

## 16. Log

- **2026-05-10:** Runbook drafted post Tier 2 completion. Sequence captured 10 steps + rollback + 13-criterion acceptance. Awaiting execution trigger (Activate approval OR user explicit accept cost). Per `release-deploy-standard.md` §3.1 PRE-RELEASE artifact requirements: covers monitoring, rollback procedure, smoke test integration.
