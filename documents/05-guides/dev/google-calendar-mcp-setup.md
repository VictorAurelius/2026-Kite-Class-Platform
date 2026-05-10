# Google Calendar MCP Server — Hướng Dẫn Cài Đặt cho Claude Code

**Đối tượng:** Solo dev cài đặt `@cocal/google-calendar-mcp` MCP server lần đầu để Claude Code tự động hóa Google Calendar operations (tạo event, query upcoming, schedule reminders).
**Mục đích:** Persist setup steps đã verify trong session walkthrough 2026-05-09 — tránh lặp lại mistakes khi re-setup (đổi máy / re-onboard team member).
**Closes:** GAP-458 §Related dev tooling extension.
**Last reviewed:** 2026-05-09

---

## 0. Use case + tradeoff

### Khi nào cần MCP setup

✅ **Phù hợp:**
- Schedule recurring reminders (vd auto-renew domain check, weekly retro events)
- Query upcoming meetings để pre-prep audit suite
- Log Phase 1 BETA milestones như calendar events
- Multi-step automation (vd tạo event + add Meet link + send invites trong 1 prompt)

❌ **Overkill cho:**
- 1-time reminder duy nhất (mở https://calendar.google.com manual, ~1 phút)
- Simple meeting scheduling (Google Calendar UI nhanh hơn)

→ **Nguyên tắc:** Setup ~30-45 phút first-time đáng giá nếu dùng ≥5-10 lần/tháng.

### Tradeoff vs alternative

| Approach | Effort first-time | Ongoing cost | Phù hợp |
|---|---|---|---|
| **MCP server** (this guide) | 30-45 phút | $0 | Heavy automation |
| Google Calendar web manual | 0 phút | ~1 phút/event | Light use |
| n8n / Zapier integration | 1-2h + monthly cost | $20-50/month | Cross-service workflows |
| Google Apps Script | 1-2h | $0 | Custom triggers (cron-ish) |

---

## 1. Pre-flight check

Trước khi bắt đầu:

1. ✅ Gmail account active (personal OK; không cần Google Workspace)
2. ✅ Node.js + npm installed (`node --version` ≥18, `npm --version` ≥9)
3. ✅ Claude Code CLI installed (`claude --version`)
4. ✅ WSL2 hoặc Linux/macOS terminal (Windows Native PowerShell may need path adaptation)

> **Lưu ý:** `@cocal/google-calendar-mcp` v2.6.1 (active maintenance 2026-03) là choice tốt nhất tại thời điểm 2026-05-09. Alternative `google-calendar-mcp` v1.0.9 (2025-04) less maintained — skip.

---

## 2. Phase A — Google Cloud Console setup (~15 phút, browser)

### A.1 Create Google Cloud project

1. Mở https://console.cloud.google.com/
2. Top header → click dropdown **Select a project** → **NEW PROJECT**
3. Project name: vd `kite-mcp-tools` hoặc `oauth2-task` (free-form)
4. Organization: **No organization** (cho personal Gmail)
5. Click **CREATE**
6. Đợi ~30s → project active → switch sang project mới (verify ở top dropdown)

### A.2 Enable Google Calendar API

1. Sidebar trái → **APIs & Services** → **Library**
2. Search box → nhập `Google Calendar API`
3. Click result **Google Calendar API**
4. Click button **Enable** → đợi ~5s → API enabled

### A.3 Configure OAuth consent screen

1. Sidebar trái → **APIs & Services** → **OAuth consent screen**
2. **User Type:** chọn **External** → **CREATE**
3. **App information page:**
   - App name: `Kite MCP Tools` (hoặc bất kỳ)
   - User support email: chọn email Gmail của bạn (dropdown)
   - Skip App logo, App domain
   - Developer contact: email Gmail của bạn
   - Click **SAVE AND CONTINUE**
4. **Scopes page:**
   - Click **ADD OR REMOVE SCOPES**
   - Tìm/check 2 scopes:
     - `https://www.googleapis.com/auth/calendar` (full Calendar access)
     - `https://www.googleapis.com/auth/calendar.events` (event create/edit)
   - Click **UPDATE** → **SAVE AND CONTINUE**
5. **Test users page** ⚠️ **CRITICAL — đừng skip step này:**
   - Click **+ ADD USERS** → nhập email Gmail của bạn (vd `vankiet14491@gmail.com`)
   - Click **ADD** → click **SAVE AND CONTINUE**
6. **Summary page:** click **BACK TO DASHBOARD**

> **Status sẽ là "Testing" — KHÔNG cần Publishing verification cho personal use.** Refresh tokens trong Test mode hết hạn sau 7 ngày — re-run auth khi cần.

### A.4 Create OAuth credentials (Desktop app)

1. Sidebar trái → **APIs & Services** → **Credentials**
2. Top: click **+ CREATE CREDENTIALS** → chọn **OAuth client ID**
3. **Application type:** chọn **Desktop app** (KHÔNG chọn Web application)
4. **Name:** `Claude Code MCP Calendar`
5. Click **CREATE**
6. Modal hiện ra với Client ID + Secret → click **DOWNLOAD JSON**
7. File save dạng `client_secret_<long-id>.apps.googleusercontent.com.json` — đổi tên thành `credentials.json` cho gọn

---

## 3. Phase B — Local install + auth (~10 phút)

### B.1 Save credentials vào WSL2 home (user-writable)

⚠️ **Gotcha thực tế (verified 2026-05-09):** Trên WSL2, `~/.config/` có thể được tạo bởi root khi WSL initialize, → user không có write permission. Default token path của MCP server (`~/.config/google-calendar-mcp/`) sẽ fail với `EACCES: permission denied`.

**Fix:** dùng folder user-owned tách biệt + override token path qua env var (xem B.3).

```bash
# Tạo folder user-owned
mkdir -p ~/.gcal-mcp/
chmod 700 ~/.gcal-mcp/

# Copy credentials.json từ Windows host (giả định download trên Windows)
# Adjust /mnt/c/, /mnt/d/, /mnt/f/ tương ứng drive bạn save file
cp /mnt/f/code/client_secret_*.apps.googleusercontent.com.json \
   ~/.gcal-mcp/credentials.json
chmod 600 ~/.gcal-mcp/credentials.json

# Verify
ls -la ~/.gcal-mcp/credentials.json
# Expect: -rw------- (chmod 600)
```

### B.2 Validate credentials structure

```bash
python3 -c "
import json
d = json.load(open('$HOME/.gcal-mcp/credentials.json'))
key = list(d.keys())[0]
print(f'Top-level key: {key}')
inner = d[key]
print(f'client_id: {inner.get(\"client_id\",\"?\")[:50]}...')
print(f'project_id: {inner.get(\"project_id\",\"?\")}')
print(f'redirect_uris: {inner.get(\"redirect_uris\",[])}')
"
# Expected output:
# Top-level key: installed
# client_id: 930867590762-jml1a6pi4afosnu1qpdett4cfot3r3pk.apps...
# project_id: oauth2-task-458710
# redirect_uris: ['http://localhost']
```

⚠️ Nếu top-level key KHÔNG phải `installed` → bạn pick sai Application type ở A.4 (chắc chọn Web application). Quay lại A.4 chọn Desktop app.

### B.3 Run OAuth flow với token path override

⚠️ **Gotcha verified 2026-05-09:** `@cocal/google-calendar-mcp` v2.6.1 default token path = `~/.config/google-calendar-mcp/tokens.json` (qua `XDG_CONFIG_HOME || ~/.config`). Nếu `~/.config/` không writable, auth fail. Override qua env `GOOGLE_CALENDAR_MCP_TOKEN_PATH`:

```bash
GOOGLE_OAUTH_CREDENTIALS=$HOME/.gcal-mcp/credentials.json \
GOOGLE_CALENDAR_MCP_TOKEN_PATH=$HOME/.gcal-mcp/tokens.json \
npx -y @cocal/google-calendar-mcp auth
```

**Server sẽ:**
1. In ra OAuth URL trong terminal:
   ```
   🔗 Authentication URL: https://accounts.google.com/o/oauth2/v2/auth?...
   Or visit: http://localhost:3500
   Browser opened automatically. If it didn't open, use the URL above.
   Authentication server started. Please complete the authentication in your browser...
   ```
2. (Có thể) tự open browser; nếu không → copy URL paste vào browser

**Browser flow:**
1. Google sign-in → chọn email Gmail (cùng email đã add Test user ở A.3)
2. ⚠️ **Cảnh báo "Google hasn't verified this app"** — đúng, vì Test mode:
   - Click **Advanced**
   - Click **Go to {project name} (unsafe)**
3. Cho phép 2 scopes Calendar
4. Browser redirect tới `localhost:3500/oauth2callback` → "Authentication successful" → đóng tab
5. Terminal in: `Tokens saved successfully... Authentication completed successfully!`

### B.4 Verify auth output

```bash
ls -la ~/.gcal-mcp/
# Expected:
#   credentials.json  (Phase B.1 copy)
#   tokens.json       (Phase B.3 generated)

python3 -c "
import json
d = json.load(open('$HOME/.gcal-mcp/tokens.json'))
print('Top-level keys:', list(d.keys()))
print('Has refresh_token:', 'refresh_token' in str(d))
print('Has access_token:', 'access_token' in str(d))
"
# Expected:
# Top-level keys: ['normal']
# Has refresh_token: True
# Has access_token: True
```

> Token structure dùng key `normal` cho default account. Multi-account support (work / personal): xem §6.

---

## 4. Phase C — Register MCP với Claude Code (~2 phút)

```bash
claude mcp add google-calendar -s user \
  --env "GOOGLE_OAUTH_CREDENTIALS=$HOME/.gcal-mcp/credentials.json" \
  --env "GOOGLE_CALENDAR_MCP_TOKEN_PATH=$HOME/.gcal-mcp/tokens.json" \
  -- npx -y @cocal/google-calendar-mcp
```

> ⚠️ **Path tuyệt đối** — KHÔNG dùng `~` shorthand trong env paths; expand qua `$HOME` để Claude Code config lưu đúng.

**Verify:**

```bash
claude mcp list
```

Output mong đợi:
```
github:           docker run -i --rm -e GITHUB_PERSONAL_ACCESS_TOKEN ghcr.io/github/github-mcp-server - ✓ Connected
google-calendar:  npx -y @cocal/google-calendar-mcp - ✓ Connected
```

> Config saved tới `~/.claude.json` user-level. Per `mcp-first-with-fallback.md` rule — MCP-first preferred over CLI.

---

## 5. Phase D — Test trong Claude Code session

⚠️ **MCP tools CHỈ load khi Claude Code session start.** Nếu bạn đang trong session hiện tại, tools `google-calendar` chưa active. Phải:

- Đóng Claude Code rồi mở lại, HOẶC
- `/clear` command trong session hiện tại (reload context, MCP tools refresh)

**Sau restart, prompt thử:**

```
Tạo event Google Calendar:
- Title: Auto-renew check kitehub.me
- Date: 2027-04-09
- Time: 9:00 AM - 10:00 AM (Asia/Ho_Chi_Minh)
- Calendar: primary
- Description: Domain Namecheap (GAP-458). Renewal $10-20/year. Alternative: .vn paid $60/year. Decision factor: Phase 1.5 PAID launch status + market trust signal.
- Reminders: email + popup 1 day before
```

Claude session mới sẽ dùng MCP tool `create-event` từ `google-calendar` server tạo event thật trong Calendar primary của bạn.

---

## 6. Multi-account setup (optional)

Nếu bạn có 2 accounts (vd personal + work Workspace):

```bash
# Authenticate "work" account
GOOGLE_OAUTH_CREDENTIALS=$HOME/.gcal-mcp/credentials.json \
GOOGLE_CALENDAR_MCP_TOKEN_PATH=$HOME/.gcal-mcp/tokens.json \
npx -y @cocal/google-calendar-mcp auth work

# Token sẽ saved với key 'work' trong cùng file tokens.json
# Sử dụng: prompt Claude "tạo event trong work calendar..."
# Server tự switch account dựa trên context hoặc env GOOGLE_ACCOUNT_MODE=work
```

---

## 7. Token expiry + renewal

⚠️ **Test mode OAuth:** refresh tokens hết hạn **7 ngày** (Google policy cho unverified apps).

**Symptom:** Sau 7 ngày, MCP tool calls fail với `invalid_grant` error.

**Fix:** Re-run B.3 auth flow:
```bash
GOOGLE_OAUTH_CREDENTIALS=$HOME/.gcal-mcp/credentials.json \
GOOGLE_CALENDAR_MCP_TOKEN_PATH=$HOME/.gcal-mcp/tokens.json \
npx -y @cocal/google-calendar-mcp auth
```

**Long-term fix** (nếu thấy pain point recurring): Publish OAuth app trong Cloud Console → submit verification → refresh tokens không expire. Yêu cầu domain verification + privacy policy. Effort ~1-2h + 1-3 weeks Google review. Cho personal use thường không đáng — chỉ re-auth weekly.

---

## 8. Cờ đỏ thường gặp

| Symptom | Nguyên nhân | Fix |
|---|---|---|
| `Access blocked: oauth2-task chưa hoàn tất quy trình xác minh` | Email login chưa add Test user | A.3 step 5 — add email vào Test users |
| `Cannot find module @cocal/google-calendar-mcp` | npm cache stale | `npm cache clean --force` rồi retry |
| `EACCES: permission denied, mkdir '/home/.../.config/google-calendar-mcp'` | `~/.config/` thuộc root (WSL2 specific) | Set `GOOGLE_CALENDAR_MCP_TOKEN_PATH=$HOME/.gcal-mcp/tokens.json` |
| `claude mcp list` báo `✗ Failed` | Credentials path expand sai trong env | `claude mcp remove google-calendar` rồi retry add với path tuyệt đối (không `~`) |
| `Insufficient permissions` khi tạo event | Scope thiếu `calendar.events` | Re-run B.3 auth — chấp nhận đầy đủ scopes (cả 2: `calendar` + `calendar.events`) |
| `invalid_grant` sau 7 ngày | Refresh token expired (Test mode policy) | Re-run B.3 auth (xem §7) |
| `Tools không xuất hiện` trong session | Claude Code session khởi tạo trước khi `mcp add` | `/clear` hoặc restart Claude Code |
| OAuth callback không nhận trong WSL2 | Windows browser → WSL2 localhost forwarding fail (rare) | Verify port 3500 không bị firewall; thử `curl http://localhost:3500` từ WSL trong khi auth chạy |

---

## 9. Cross-references

- **Use case origin:** `documents/05-guides/account-prep/02b-github-student-pack-free-domain.md` §3 (Auto-renew calendar reminder cho `kitehub.me` 11 tháng)
- **MCP rule:** `.claude/rules/mcp-first-with-fallback.md` — MCP-first tool selection
- **Agent rule:** `.claude/rules/agent-action-bias.md` — agent làm thay vì instruct user (calendar event create)
- **GAP origin:** GAP-458 §Related (domain decision discovered MCP need cho automation)

---

## 10. Out of scope

- Google Workspace org-wide deployment (cần admin Workspace cài per-domain)
- 2-way email integration via Google Workspace (xem `email-ses-setup-runbook.md` cho transactional outbound)
- Other Google MCP servers (Drive, Gmail, Sheets) — separate guides per service
- OAuth app verification submission process — defer until pain points recurring

---

## 11. Log

- **2026-05-09** Runbook created — capture full walkthrough verified during GAP-458 session. Real gotchas documented: Test users requirement (A.3 §5 mandatory), `~/.config/` permission issue trên WSL2 (B.1 fix), `GOOGLE_CALENDAR_MCP_TOKEN_PATH` env override (discovered via grep source). Future re-setup time expected ~10-15 phút (vs 30-45 phút first-time với gotchas).
