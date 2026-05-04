# SSH Mobile Migration — Phase 3: Checklist Android

**Pre-req:** Phase 1 + 2 đã chạy thành công (script Windows cleanup + WSL2 setup). Bạn đã có WSL2 Tailscale IP từ output script 2 (dạng `100.69.110.122`).

**Thời gian:** ~10 phút.

---

## A) Tailscale Android app

Nếu đã cài + sign in (theo guide chính §4.1) → **bỏ qua, sang B**. Nếu chưa:

1. Play Store → search **"Tailscale"** → Install (Tailscale Inc. official)
2. Mở app → **Sign in** → dùng CÙNG identity provider đã dùng cho Tailscale WSL2 (Google / Microsoft / GitHub / Apple)
3. Sau login, danh sách máy phải thấy **kite-wsl2** = Connected
4. **Settings → Always-on VPN: ON** (Tailscale tự resume sau khi phone wake)
5. **Settings → Battery optimization: Tailscale = Don't optimize** (Android 12+; tùy ROM)

---

## B) Termux từ F-Droid

⚠ **Dùng F-Droid version, KHÔNG phải Play Store version.** F-Droid là canonical maintained release; Play Store là unofficial fork.

1. Browser → https://f-droid.org/packages/com.termux/ → Tải APK
2. Cài (cho phép "Install unknown apps" cho browser nếu hỏi)
3. Mở Termux, chạy:
   ```bash
   pkg update && pkg upgrade -y
   pkg install -y mosh openssh tmux
   ```

---

## C) SSH key Termux → WSL2

### C.1) Check key đã có chưa

```bash
# Trong Termux
ls -la ~/.ssh/kite_dev ~/.ssh/kite_dev.pub 2>/dev/null && echo "KEY EXISTS" || echo "NO KEY — sang C.2"
```

Nếu in `KEY EXISTS` + 2 file (private + .pub) → đã có key. **Verify key đã install trên WSL2:**

```bash
# Trong Termux — quick test (success nếu key chạy OK, fail nếu chưa install)
ssh -i ~/.ssh/kite_dev -p 2222 -o StrictHostKeyChecking=no -o BatchMode=yes nguyenvankiet@100.69.110.122 "echo OK" 2>&1 | tail -1
# In "OK"                                          → key đã installed, sang D
# In "Permission denied" hoặc "Connection refused" → key chưa install, làm C.3
```

### C.2) Generate key mới (chỉ khi C.1 báo NO KEY)

```bash
# Trong Termux
ssh-keygen -t ed25519 -C "android-termux@kite-dev" -f ~/.ssh/kite_dev -N ""
# -N "" = không passphrase (mobile-friendly; bảo vệ bằng Tailscale auth + device unlock)
```

**Auto-copy public key vào clipboard** (tốt nhất — paste thẳng vào Claude chat):

```bash
# Trong Termux — cài clipboard tool nếu chưa (1 lần, ~3MB)
pkg install -y termux-api

# Copy pubkey vào clipboard Android (KHÔNG cần cat + select tay)
cat ~/.ssh/kite_dev.pub | termux-clipboard-set
echo "Public key đã copy vào clipboard. Paste vào Claude chat."

# Optional: verify clipboard có gì
termux-clipboard-get
```

> Pre-req cho `termux-clipboard-set`: cài **Termux:API** companion app từ F-Droid (https://f-droid.org/packages/com.termux.api/) — cùng source với Termux. Thiếu nó, `termux-clipboard-set` fail im lặng.

**Fallback (không có Termux:API):** `cat ~/.ssh/kite_dev.pub` → long-press output → Select all → Copy.

### C.3) Install pubkey lên WSL2

**Dễ nhất từ mobile:** paste public key vào Claude Code chat — bảo "install Termux pubkey này lên WSL2", Claude sẽ append vào `~/.ssh/authorized_keys`.

**Hoặc thủ công** (nếu có SSH access khác vào WSL2):

```bash
# Trên WSL2
mkdir -p ~/.ssh && chmod 700 ~/.ssh
echo 'ssh-ed25519 AAAA... android-termux@kite-dev' >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
```

**Verify install:** chạy lại lệnh test ở C.1 — bây giờ phải in `OK`.

---

## D) Termux SSH config

```bash
# Trong Termux — tạo ~/.ssh/config
mkdir -p ~/.ssh && chmod 700 ~/.ssh
cat > ~/.ssh/config <<'EOF'
Host kite
  HostName 100.69.110.122
  Port 2222
  User nguyenvankiet
  IdentityFile ~/.ssh/kite_dev
  ServerAliveInterval 60
EOF
chmod 600 ~/.ssh/config
```

---

## E) Test stack mới

```bash
# Trong Termux

# Test 1: Plain SSH chạy (TCP path, chưa mosh)
ssh kite "echo 'SSH OK from Termux at' \$(date)"

# Test 2: Mosh chạy (UDP path, lớp survival)
mosh kite
# → tự attach tmux session "claude" (theo snippet ~/.bashrc của script 2)
# → nếu session "claude" mới → bash prompt rỗng trong tmux

# Bên trong session "claude":
# - Chạy: echo "Hello from mobile" > /tmp/mobile-test.txt
# - Bấm Ctrl+B rồi D để detach (session vẫn alive trên WSL2)
# - Đóng Termux hẳn (swipe khỏi recent apps)
# - Đợi 1 phút
# - Mở lại Termux, chạy: mosh kite
# - Phải về đúng tmux + prompt vừa rồi
# - Verify: cat /tmp/mobile-test.txt — phải in "Hello from mobile"
```

3 test pass → migration thành công. Mất mạng, switch app, tắt màn hình — không cái nào kill session nữa.

---

## F) Daily workflow trên phone

```bash
# Sáng — mở Termux, 1 lệnh:
mosh kite
# → tự attach tmux session "claude"
# → Claude Code chạy bình thường:
#     cd ~/projects/2026-Kite-Class-Platform
#     claude

# Switch app, tắt màn hình, mất mạng:
# KHÔNG làm gì. Mosh + tmux survive.

# Mở lại Termux sau:
# Đang attached? Cứ tiếp tục gõ.
# Disconnected? Gõ 'mosh kite' lại — về đúng chỗ vừa rời.

# Force-takeover (vd quên detach trên laptop):
mosh kite -- tmux attach -d -t claude
```

---

## G) Polish (optional)

### Termux widget (one-tap launch)

Cài **Termux:Widget** từ F-Droid → home screen widget shortcut chạy `mosh kite`. Tap để instant-connect.

### Wakelock (chống Android kill Termux khi mosh idle lâu)

```bash
# Trong Termux, cài API tools
pkg install -y termux-api
# Acquire wakelock khi mosh active
termux-wake-lock
# Release khi xong
termux-wake-unlock
```

Caveat: tốn pin chút. Chỉ dùng khi chạy long agent cần monitor.

### ntfy push notification (đã wire vào Claude stop hook!)

`.claude/hooks/notify-stop.sh` channel #4 tự động push qua ntfy.sh mỗi lần Claude dừng chờ input. **Không cần curl thủ công.**

**Setup (1 lần):**

1. Cài ntfy app:
   - F-Droid (recommended): https://f-droid.org/packages/io.heckel.ntfy/
   - Hoặc Play Store: search "ntfy"

2. Pick topic name unique (semi-secret):
   ```bash
   # Trong WSL2 — generate random topic
   python3 -c "import secrets, string; print('kite-claude-vkiet-' + ''.join(secrets.choice(string.ascii_lowercase + string.digits) for _ in range(8)))"
   ```

3. Trong app ntfy → tap "+" → Subscribe to topic → paste topic name → Subscribe

4. Set env var để hook pickup. 2 cách (làm cả 2 cho chắc):
   ```bash
   # ~/.bashrc
   echo 'export NTFY_TOPIC="kite-claude-vkiet-..."' >> ~/.bashrc
   ```
   ```json
   // ~/.claude/settings.json (user-global, NOT repo)
   {
     "env": { "NTFY_TOPIC": "kite-claude-vkiet-..." }
   }
   ```

5. Restart Claude (`/exit` + relaunch) để pickup env var

**Test:**
```bash
NTFY_TOPIC="kite-claude-vkiet-..." \
CLAUDE_PROJECT_DIR="$(pwd)" \
bash .claude/hooks/notify-stop.sh
# → phone hiện notification trong vài giây
```

**Customize env vars:**
- `NTFY_PRIORITY` — 1 (min) → 5 (urgent), default 3
- `NTFY_SERVER` — default `https://ntfy.sh`; đổi nếu self-host
- `NTFY_CLICK` — Android intent URL fired khi tap notification. Default mở Termux:
  `intent:#Intent;action=android.intent.action.MAIN;package=com.termux;end`.
  Đổi sang app khác bằng cách thay `package=...`. Chỉ áp dụng trên Android (iOS bỏ qua).

**Tap-to-resume tmux (advanced — Cách 2):** thay vì chỉ mở Termux, có thể chạy luôn `tmux attach` qua Termux:RUN_COMMAND broadcast. Cần thêm Termux:API add-on (F-Droid) + `allow-external-apps = true` trong `~/.termux/termux.properties` + `termux-reload-settings`. Sau đó set:
```bash
export NTFY_CLICK="intent:#Intent;action=com.termux.RUN_COMMAND;package=com.termux;\
S.com.termux.RUN_COMMAND_PATH=/data/data/com.termux/files/usr/bin/bash;\
S.com.termux.RUN_COMMAND_ARGUMENTS=-c%20%27tmux%20attach%20%7C%7C%20tmux%20new%27;end"
```

**Privacy:** topic name là URL public — ai biết = nhận message. Random suffix dài + KHÔNG commit topic vào public repo. Self-host ntfy server với auth nếu lo (overkill cho solo dev).

---

## H) Rollback (nếu migration fail)

```bash
# WSL2 — gỡ snippet tmux khỏi ~/.bashrc
sed -i '/# >>> ssh-mobile-migration tmux auto-attach >>>/,/# <<< ssh-mobile-migration tmux auto-attach <<</d' ~/.bashrc

# WSL2 — stop services (không uninstall, để dùng lại sau nếu cần)
sudo systemctl disable --now tailscaled
sudo systemctl disable --now ssh

# Windows — add lại portproxy nếu trước có (chạy ngược script cleanup thủ công,
# hoặc theo guide chính §2.3)
```

Failure modes hay gặp + fix:
- **Mosh handshake timeout:** corp/uni firewall block UDP 60000-61000. Test từ mạng nhà trước.
- **Tailscale "logged out" sau khi reboot phone:** Always-on VPN chưa enable. Làm lại bước A4.
- **Session "claude" không auto-attach:** snippet ~/.bashrc thiếu. Chạy lại script 2 (idempotent).
- **WSL2 chết qua đêm:** Windows sleep settings. Power Options → "When plugged in, never sleep".

---

## Xong

Bạn đã ở stack mobile-resilient. Mobile disconnect không kill được long-running agent nữa.

Cross-references:
- Kiến trúc tổng quan: `documents/05-guides/ssh-terminal-direct-access.md` §3.4
- Memory: `feedback_agent_kill_root_cause.md` (root cause analysis)
- PR introducing: #746
