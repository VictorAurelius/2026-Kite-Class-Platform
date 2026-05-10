# Cloudflare CLI Setup — Wrangler + REST API Token cho kitehub.me

**Đối tượng:** Solo dev setup Cloudflare CLI tooling (Wrangler + REST API) cho zone `kitehub.me` operations: DNS, Email Routing, Origin Cert, future Workers.
**Closes:** GAP-458 §Related dev tooling extension.
**Tham chiếu:** `.claude/rules/third-party-platform-automation-discovery.md` §6.3 (worked self-test); `02b-github-student-pack-free-domain.md` §2 (domain origin)
**Last reviewed:** 2026-05-10

---

## 0. Decision rationale (per §6.3 worked self-test)

Tier 1 official tooling: **Wrangler v4.90.0** (Cloudflare official CLI).

| Wrangler can | Wrangler cannot |
|---|---|
| ✅ Email Routing rules list/add/remove | ❌ DNS records management |
| ✅ Workers deploy/dev/tail | ❌ Origin Cert generation |
| ✅ Pages deploy | ❌ Zone settings (firewall, SSL mode toggle) |
| ✅ AI / D1 / KV / R2 / Queues | ❌ Page Rules |
| ✅ Email Sending API | ❌ DNS proxy mode toggle |

→ **Need:** Wrangler + REST API token (split tooling).

---

## 1. Wrangler CLI install + auth

### 1.1 Install + login

```bash
# Global install (~14s, 316 packages)
npm install -g wrangler

# Verify
wrangler --version
# Expected: 4.90.0+

# Login OAuth flow (browser)
# WSL2 ~/.config/ permission issue — override XDG_CONFIG_HOME
env XDG_CONFIG_HOME=$HOME/.wrangler-config wrangler login
```

OAuth flow:
1. Browser opens https://dash.cloudflare.com/oauth2/auth?...
2. Login Cloudflare account
3. Permissions request (account/user/workers/d1/pages/zone:read/email_routing/etc.) — click **Allow**
4. Redirect localhost:8976/oauth/callback
5. Terminal: `Successfully logged in.`

### 1.2 Verify

```bash
env XDG_CONFIG_HOME=$HOME/.wrangler-config wrangler whoami
# Expected:
#   👋 You are logged in with an OAuth Token, associated with the email <your-email>
#   Account ID: <your-account-id>
```

### 1.3 Convenience alias (optional)

```bash
# Trong ~/.bashrc hoặc ~/.zshrc:
alias wrangler='env XDG_CONFIG_HOME=$HOME/.wrangler-config wrangler'
```

→ Sau alias, dùng `wrangler` trực tiếp không cần env prefix.

### 1.4 Test Wrangler Email Routing

```bash
wrangler email routing rules list kitehub.me
# Expected output:
#   Rule: <id>
#     Name:     Rule created at ...
#     Enabled:  true
#     Matchers: to:admin@kitehub.me
#     Actions:  forward:<gmail>
```

---

## 2. Cloudflare REST API token (cho DNS + Origin Cert)

### 2.1 Create scoped token via Dashboard

Mở https://dash.cloudflare.com/profile/api-tokens

1. Click **Create Token**
2. Tìm template **"Edit zone DNS"** → click **Use template**
3. Cấu hình:

| Field | Value |
|---|---|
| Token name | `kite-cli-dns-edit` |
| Permissions | (auto từ template) Zone:DNS:Edit + Zone:Zone:Read |
| Zone Resources | **Include — Specific zone — kitehub.me** (least-privilege) |
| Client IP filtering | (skip) |
| TTL | (mặc định không expire OK; có thể set 1 năm rotate) |

4. **Continue to summary** → **Create Token**
5. ⚠️ **COPY TOKEN NGAY** — chỉ hiển thị 1 lần
6. Format: `cfut_<32-char>` (newer Cloudflare token format with prefix)

### 2.2 Save token

**Local shell (one-time):**

```bash
# Append to ~/.bashrc
cat >> ~/.bashrc << 'EOF'

# Cloudflare API token for kite-cli-dns-edit (kitehub.me only, Zone:DNS:Edit + Zone:Read)
export CLOUDFLARE_API_TOKEN="cfut_<your-token>"
export CLOUDFLARE_ZONE_ID_KITEHUB_ME="<zone-id>"
EOF

source ~/.bashrc

# Verify
echo "Token loaded: ${CLOUDFLARE_API_TOKEN:0:10}..."
echo "Zone ID: $CLOUDFLARE_ZONE_ID_KITEHUB_ME"
```

**GitHub Secret (cho CI):**

```bash
gh secret set CLOUDFLARE_API_TOKEN
# (paste value khi prompt)

# Verify
gh secret list | grep CLOUDFLARE
```

→ Workflows future có thể dùng `${{ secrets.CLOUDFLARE_API_TOKEN }}` cho automated DNS updates.

### 2.3 Get Zone ID (one-time)

```bash
curl -s -H "Authorization: Bearer $CLOUDFLARE_API_TOKEN" \
  "https://api.cloudflare.com/client/v4/zones?name=kitehub.me" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['result'][0]['id'])"
# Output: bb54ef8f69b0ef03085ce8903d90a5a4 (per kitehub.me 2026-05-09)
```

### 2.4 Test token

```bash
# List zone metadata
curl -s -H "Authorization: Bearer $CLOUDFLARE_API_TOKEN" \
  "https://api.cloudflare.com/client/v4/user/tokens/verify" | python3 -m json.tool
# Expected: status=active

# List DNS records
bash scripts/cloudflare-dns.sh list
# Expected: 9 records (3 user CNAMEs + www + 3 MX + 2 TXT)
```

---

## 3. Helper script `scripts/cloudflare-dns.sh`

Wrapper around Cloudflare REST API cho common ops. Commands:

| Command | Effect |
|---|---|
| `list` | List all DNS records |
| `list-mx` / `list-cname` / `list-txt` / `list-a` | Filter by type |
| `add <TYPE> <NAME> <CONTENT> [--proxied]` | Add new record |
| `delete <record-id>` | Remove record |
| `toggle-proxy <name>` | Switch DNS only ↔ Proxied |
| `origin-cert [hostnames]` | Generate Cloudflare Origin Cert via API |
| `zone` | Show zone metadata |

**Examples:**

```bash
# List all records
bash scripts/cloudflare-dns.sh list

# Add staging CNAME
bash scripts/cloudflare-dns.sh add CNAME staging cname.vercel-dns.com

# Switch api.kitehub.me from DNS only → Proxied (sau khi ALB cert ready Tier 3)
bash scripts/cloudflare-dns.sh toggle-proxy api.kitehub.me

# Generate Origin Cert + save to ~/.gcal-mcp/cloudflare-origin-cert/
bash scripts/cloudflare-dns.sh origin-cert
```

→ Script require `CLOUDFLARE_API_TOKEN` + `CLOUDFLARE_ZONE_ID_KITEHUB_ME` env vars.

---

## 4. When to use which tool

| Operation | Tool | Why |
|---|---|---|
| List/test Email Routing rules | **Wrangler** `email routing` | Native subcommand |
| Add/remove DNS records | **REST API** via `cloudflare-dns.sh` | Wrangler doesn't expose DNS |
| Toggle proxy mode | **REST API** | Wrangler doesn't expose |
| Generate Origin Cert | **REST API** | Replaces Dashboard click flow |
| Deploy Workers | **Wrangler** | Primary use case |
| Deploy Pages | **Wrangler** | Primary use case |
| Workers tail logs | **Wrangler** | Native |
| Custom Page Rules | **REST API** OR Terraform | Wrangler doesn't expose |
| Zone-level firewall config | **REST API** OR Terraform | Wrangler doesn't expose |

---

## 5. Future automation use cases (kitehub.me Phase 1 BETA → 1.5 PAID)

| Use case | Tool | Phase |
|---|---|---|
| Switch CNAME api.kitehub.me proxy DNS only → Proxied | `cloudflare-dns.sh toggle-proxy` | After ALB cert imported (Tier 3) |
| Generate Origin Cert for ALB binding | `cloudflare-dns.sh origin-cert` | Tier 2 (anytime) |
| Add wildcard DKIM records (post SES production) | `cloudflare-dns.sh add` | After GAP-370 SES approved |
| Bulk add tenant subdomains (vd `tenant1.kitehub.me`) | Loop `cloudflare-dns.sh add` | Phase 1 BETA invite onboarding |
| Update Cloudflare Worker for tenant routing | `wrangler deploy` | Phase 2 scaling |
| Email Routing rules per beta admin | `wrangler email routing rules create` | Phase 1 BETA |

---

## 6. Cờ đỏ thường gặp

| Symptom | Nguyên nhân | Fix |
|---|---|---|
| `wrangler login` báo `Failed to write to log file Error: EACCES` | `~/.config/` thuộc root (WSL2) | Set `XDG_CONFIG_HOME=$HOME/.wrangler-config` |
| `wrangler whoami` báo `Not authenticated` | Chưa run `wrangler login` HOẶC env override missing | Re-run với `XDG_CONFIG_HOME` |
| API token verify fail | Token expired hoặc revoked | Tạo lại qua Dashboard, update env |
| `Authentication error (10000)` | Token scope thiếu | Verify token permissions = Zone:DNS:Edit + Zone:Read |
| `Zone not found` | Token scope chỉ specific zone, request khác zone | Tạo token mới với All zones HOẶC dùng đúng zone token |
| Wrangler `email routing rules list` báo `open beta` | Cảnh báo info, không phải error | Ignore — feature works |

---

## 7. Token rotation (annual)

Token hết hạn sau 1 năm (nếu set TTL) hoặc revoked manually:

1. Cloudflare Dashboard → **My Profile → API Tokens**
2. Find `kite-cli-dns-edit` row → **Roll** (rotate) hoặc **Delete + Create new**
3. Update `~/.bashrc` + `gh secret set CLOUDFLARE_API_TOKEN` với token mới
4. `source ~/.bashrc`
5. Verify `bash scripts/cloudflare-dns.sh zone`

---

## 8. Cross-references

- **Rule** `.claude/rules/third-party-platform-automation-discovery.md` — §6.3 worked self-test cho Wrangler decision
- **Sister runbook** `documents/05-guides/dev/google-calendar-mcp-setup.md` — same pattern (CLI install + OAuth + REST API fallback)
- **Origin runbook** `documents/05-guides/account-prep/02b-github-student-pack-free-domain.md` — kitehub.me domain claim + nameservers
- **Deploy runbook** `documents/05-guides/deploy/cloudflare-setup.md` — original UI walkthrough; Tier 3 ops (Full strict, HSTS, Origin Cert) sẽ dùng cloudflare-dns.sh

## 9. Log

- **2026-05-10:** Runbook + helper script `scripts/cloudflare-dns.sh` shipped — captures full Wrangler + REST API setup for kitehub.me. Verified 9 DNS records list correctly. Future Tier 3 ops (proxy toggle, Origin Cert generate) automated. Per `third-party-platform-automation-discovery.md` §6.3 decision (✅ Setup, matrix row 3).
