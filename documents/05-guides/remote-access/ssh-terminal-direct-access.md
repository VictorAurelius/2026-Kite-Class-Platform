# SSH Terminal Direct Access — WSL2 Dev Machine

**Audience:** solo-dev đang dùng WSL2 trên Windows, cần điều khiển dev machine từ máy khác (laptop khác, phone, máy ở văn phòng) qua SSH thay vì Claude Code remote-control.

**Khi nào dùng:** ops-heavy work cần verification loops dài (Docker build > 30s, CI poll, log tail, mvn build) — terminal trực tiếp giảm chat overhead. Xem `remote-control-setup.md` cho mobile-first flow qua Claude Code (built-in feature).

**Yêu cầu:** WSL2 với `systemd=true` (đã có theo `wsl2-fresh-setup.md`); Windows admin để mở firewall + portproxy.

---

## 1. Quick Reference

```bash
# From outside (laptop/phone) once setup is done:
ssh -p 2222 nguyenvankiet@<windows-host-ip>
tmux attach -t kite || tmux new -s kite     # persistent session
```

Inside tmux session, normal flow:
```bash
cd ~/projects/2026-Kite-Class-Platform
gh pr checks 737                            # CI poll
docker buildx build -f kiteclass/...        # build watch
mvn -pl kitehub/kitehub-admin test          # mvn loop
```

Disconnect (Ctrl+B, D) — session keeps running. Reconnect: same `tmux attach` command.

---

## 2. One-time Setup (do these once per machine)

### 2.1 Inside WSL2 — enable + harden sshd

```bash
sudo apt-get update && sudo apt-get install -y openssh-server
sudo systemctl enable --now ssh.service
sudo systemctl status ssh                   # verify active
```

Write the SSH override (use `install` to avoid heredoc-into-sudo password collision — see §11 lesson):

```bash
cat > /tmp/99-kite-local.conf <<'EOF'
# Project-local SSH overrides for WSL2 dev access
Port 2222                       # avoid host port 22 conflicts
PasswordAuthentication no       # key-only
PubkeyAuthentication yes
PermitRootLogin no
ClientAliveInterval 60
ClientAliveCountMax 3
AllowUsers <your-username>      # replace with your actual username
EOF

sudo install -m 644 -o root -g root /tmp/99-kite-local.conf /etc/ssh/sshd_config.d/99-kite-local.conf
rm /tmp/99-kite-local.conf
sudo systemctl restart ssh
```

**🔥 CRITICAL — Ubuntu 24.04+ socket activation footgun:** modern Ubuntu/Debian uses `ssh.socket` to control which port sshd listens on. The `Port` directive in `sshd_config` is **ignored** — `sshd -T` will report Port 2222 but `ss -tlnp` will still show `:22`. Fix with a socket drop-in:

```bash
sudo mkdir -p /etc/systemd/system/ssh.socket.d

cat > /tmp/listen.conf <<'EOF'
[Socket]
ListenStream=
ListenStream=0.0.0.0:2222
ListenStream=[::]:2222
EOF

sudo install -m 644 -o root -g root /tmp/listen.conf /etc/systemd/system/ssh.socket.d/listen.conf
rm /tmp/listen.conf
sudo systemctl daemon-reload
sudo systemctl restart ssh.socket
sudo systemctl restart ssh.service
```

The empty `ListenStream=` resets the original `:22` from the base unit; the next two lines add the new ports.

Verify both layers:

```bash
ss -tlnp 2>/dev/null | grep -E ':2222\s'    # should show 0.0.0.0:2222 + [::]:2222
sudo sshd -T | grep -E '^(port|passwordauthentication|pubkeyauthentication|permitrootlogin|allowusers)'
systemctl status ssh.socket --no-pager | grep Listen
```

Expected: `Listen: 0.0.0.0:2222 (Stream)`. If still `:22`, the socket drop-in didn't apply — re-run `daemon-reload` + `restart ssh.socket`.

### 2.2 SSH key from your "outside" machine

On the laptop/phone you'll connect FROM:

```bash
ssh-keygen -t ed25519 -C "outside-laptop@kite-dev" -f ~/.ssh/kite_dev
# Copy public key
cat ~/.ssh/kite_dev.pub
```

On WSL2 dev machine (paste the public key):

```bash
mkdir -p ~/.ssh && chmod 700 ~/.ssh
echo "ssh-ed25519 AAAA... outside-laptop@kite-dev" >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
```

Add SSH client config on the outside machine (`~/.ssh/config`):

```
Host kite
  HostName <windows-host-ip-or-hostname>
  Port 2222
  User nguyenvankiet
  IdentityFile ~/.ssh/kite_dev
  ServerAliveInterval 30
  ServerAliveCountMax 3
```

Now `ssh kite` works (after step 2.3).

### 2.3 Windows host — forward port 2222 to WSL2

WSL2 has its own IP (changes on every WSL restart). Windows must portproxy from host port → current WSL IP. Open **PowerShell as Administrator** on the Windows host:

```powershell
# Find current WSL2 IP
$wslIp = (wsl hostname -I).Trim().Split(' ')[0]
Write-Host "WSL2 IP: $wslIp"

# Forward host:2222 → WSL2:2222
netsh interface portproxy delete v4tov4 listenport=2222 listenaddress=0.0.0.0 2>$null
netsh interface portproxy add v4tov4 listenport=2222 listenaddress=0.0.0.0 connectport=2222 connectaddress=$wslIp

# Open Windows Firewall
New-NetFirewallRule -DisplayName "WSL2 SSH 2222" -Direction Inbound -Protocol TCP -LocalPort 2222 -Action Allow

# Verify
netsh interface portproxy show all
```

**Persistence:** WSL2 IP changes after every WSL reboot. Save the snippet to `C:\scripts\wsl-ssh-portproxy.ps1`, then register a Task Scheduler job. Run this in **PowerShell as Administrator** (idempotent — re-running re-creates the task):

```powershell
# Create script directory + write the startup script
New-Item -ItemType Directory -Path C:\scripts -Force | Out-Null

@'
# WSL2 SSH portproxy startup script (auto-generated)
$wslIp = (wsl hostname -I).Trim().Split(' ')[0]
if (-not $wslIp) { Write-Host 'No WSL IP — abort'; exit 1 }
Write-Host "WSL2 IP: $wslIp"
netsh interface portproxy delete v4tov4 listenport=2222 listenaddress=0.0.0.0 2>$null
netsh interface portproxy add v4tov4 listenport=2222 listenaddress=0.0.0.0 connectport=2222 connectaddress=$wslIp
'@ | Set-Content -Path C:\scripts\wsl-ssh-portproxy.ps1 -Encoding UTF8

# Register the scheduled task (delete first if exists)
if (Get-ScheduledTask -TaskName 'WSL2-SSH-Portproxy' -ErrorAction SilentlyContinue) {
  Unregister-ScheduledTask -TaskName 'WSL2-SSH-Portproxy' -Confirm:$false
}
$action = New-ScheduledTaskAction -Execute 'powershell.exe' `
  -Argument '-ExecutionPolicy Bypass -NoProfile -WindowStyle Hidden -File C:\scripts\wsl-ssh-portproxy.ps1'
$trigger = New-ScheduledTaskTrigger -AtStartup
$principal = New-ScheduledTaskPrincipal -UserId 'SYSTEM' -RunLevel Highest
$settings = New-ScheduledTaskSettingsSet -StartWhenAvailable `
  -ExecutionTimeLimit (New-TimeSpan -Minutes 5) -DontStopOnIdleEnd
Register-ScheduledTask -TaskName 'WSL2-SSH-Portproxy' `
  -Action $action -Trigger $trigger -Principal $principal -Settings $settings | Out-Null
Get-ScheduledTask -TaskName 'WSL2-SSH-Portproxy' | Format-List TaskName, State
```

After Windows boot, verify portproxy is fresh:
```powershell
netsh interface portproxy show all   # 0.0.0.0 2222 → <current WSL IP> 2222
```

### 2.4 External access (optional but recommended for mobile)

LAN-only access uses the Windows LAN IP (e.g. `172.16.90.30`). For cellular / coffee shop / anywhere access, install Tailscale — zero-config WireGuard mesh VPN, free for solo-dev.

#### Install on Windows

`winget install --id=tailscale.tailscale --silent` should work, but in practice (2026-05-04 setup) the silent flag suppresses the UAC prompt and the install just fails with no diagnostic. Use direct download instead:

```powershell
# In any PowerShell (no admin needed for download)
Invoke-WebRequest -Uri 'https://pkgs.tailscale.com/stable/tailscale-setup-latest.exe' `
  -OutFile 'C:\temp\tailscale-setup.exe' -UseBasicParsing

# Launch with elevation (UAC prompt → Yes → click Install)
Start-Process 'C:\temp\tailscale-setup.exe' -Verb RunAs
```

After GUI install, the Tailscale tray app auto-opens. **Sign in** → choose identity provider (Google / Microsoft / GitHub / Apple). Verify from any shell:

```powershell
& 'C:\Program Files\Tailscale\tailscale.exe' status   # list of your devices
& 'C:\Program Files\Tailscale\tailscale.exe' ip -4    # this machine's Tailscale IP (e.g. 100.90.149.107)
```

That `100.x.y.z` IP is now reachable from any other machine on the same Tailscale network. **Use this IP, not the LAN IP, when connecting from a phone or remote laptop.**

#### Other options

| Option | When to pick |
|---|---|
| **Tailscale** | Solo-dev, mobile access — pick this 95% of the time |
| **Cloudflare Tunnel** | Need SSH over HTTPS only (corporate firewall blocks WireGuard UDP) |
| **Router port forward** | Self-hosted infra with static IP + you've audited fail2ban + key rotation |

---

## 3. tmux Session Patterns

### 3.1 First-time setup

```bash
# In WSL2 home dir
cat > ~/.tmux.conf <<'EOF'
set -g default-terminal "screen-256color"
set -g history-limit 50000
set -g mouse on
setw -g mode-keys vi
set -g status-right "#(date '+%H:%M %Z')"
EOF
```

### 3.2 Daily workflow

```bash
# Start session — name "kite"
tmux new -s kite

# Inside tmux: split panes
# Ctrl+B "         horizontal split
# Ctrl+B %         vertical split
# Ctrl+B arrow     navigate
# Ctrl+B z         zoom toggle
# Ctrl+B d         detach (session keeps running)

# Reattach later
tmux attach -t kite

# List sessions
tmux ls
```

### 3.3 Multi-pane Kite ops layout

Recommend 3-pane setup for Kite ops work:

```
┌─────────────────────────┬─────────────────────┐
│ pane 0: editor / git    │ pane 1: docker/mvn  │
│                         │ build watch         │
│                         ├─────────────────────┤
│                         │ pane 2: gh pr poll  │
│                         │ + log tail          │
└─────────────────────────┴─────────────────────┘
```

Script to set up (`~/.local/bin/kite-tmux`):

```bash
#!/bin/bash
SESSION=kite
cd ~/projects/2026-Kite-Class-Platform
tmux has-session -t $SESSION 2>/dev/null && tmux attach -t $SESSION && exit
tmux new -s $SESSION -d
tmux split-window -h -t $SESSION
tmux split-window -v -t $SESSION:0.1
tmux send-keys -t $SESSION:0.0 'clear; git status' Enter
tmux send-keys -t $SESSION:0.1 'echo "build pane — docker buildx / mvn here"' Enter
tmux send-keys -t $SESSION:0.2 'echo "ops pane — gh pr checks / log tail here"' Enter
tmux select-pane -t $SESSION:0.0
tmux attach -t $SESSION
```

```bash
chmod +x ~/.local/bin/kite-tmux
# Add to PATH if not already:  export PATH="$HOME/.local/bin:$PATH"
```

Daily: `ssh kite` → `kite-tmux` → done.

### 3.4 Mosh layer — survives mobile network drops + screen-off (CRITICAL for mobile)

**The problem this section solves:** SSH from Android → WSL2 over LTE/Wi-Fi → user switches app, screen turns off, or roams network → SSH session disconnects → SIGHUP fires → if Claude/agents are running OUTSIDE tmux, they die immediately. Even WITH tmux protecting the process, the SSH client must be re-launched and re-attached every time, breaking flow.

**Mosh fixes this layer:** UDP-based, server-side state holder. SSH client can vanish for hours; mosh-server on WSL2 keeps holding the session. When mobile reconnects, mosh-client picks up exactly where it left off — same scrollback, same prompt, same tmux pane.

**Why mosh + tmux + Tailscale is the production stack for mobile dev (2026 trending):**
- **Tailscale** = peer-to-peer network (no port-forward, survives WAN IP changes)
- **mosh** = connection layer (survives mobile sleep, network roam)
- **tmux** = process layer (survives SSH/mosh process death)

Each layer fixes a different failure mode; combined = mobile disconnect cannot kill long-running work.

#### Setup on WSL2 (one-time)

```bash
# 1. Install mosh
sudo apt install -y mosh

# 2. Verify mosh-server installed
which mosh-server   # → /usr/bin/mosh-server

# 3. Mosh uses UDP 60000-61000 by default. Tailscale tunnels UDP natively
#    over WireGuard, so NO firewall changes needed when reaching WSL2 via
#    its Tailscale 100.x.x.x IP. Skip the netsh portproxy + Windows firewall
#    steps from §2.3 if you ONLY use Tailscale.
```

#### Setup on Android (Termux path)

```bash
# In Termux (already installed per §4.2 / §4.3)
pkg update && pkg install -y mosh

# Connect (same Tailscale IP from §4.5)
mosh user@<wsl2-tailscale-ip>
# → auto-attaches tmux "claude" session per ~/.bashrc snippet from §2.1

# If §2.1 ~/.bashrc snippet not yet added (auto-attach tmux on SSH login):
cat >> ~/.bashrc <<'EOF'

# Auto-attach tmux on SSH/mosh login
if [ -n "$SSH_CONNECTION" ] && [ -z "$TMUX" ] && command -v tmux >/dev/null; then
    tmux attach -t claude 2>/dev/null || tmux new -s claude
fi
EOF
```

#### Termius alternative

Termius supports mosh natively in Pro tier. Toggle host config: **Edit host → Use Mosh = ON**. Free tier: stick with Termux for mosh.

#### Survives matrix

| Event | Survives? | Why |
|-------|:---:|-----|
| Tắt màn hình Android | ✅ | mosh tolerates idle UDP, tmux independent of connection |
| Switch app (Termux background) | ✅ | mosh-server holds session state |
| Kill Termux app | ✅ | mosh-server still alive on WSL2; mosh-client reconnects to same `MOSH_KEY` on next launch |
| WiFi → 4G roam | ✅ | mosh detects IP change, sends new SSP packet, server resumes |
| Mất mạng 1+ giờ | ✅ | mosh-server waits indefinitely (default — no timeout) |
| Reboot Android | ✅ | Open Termux → `mosh user@ip` → re-attach tmux session |
| Windows sleep/hibernate | ❌ | WSL2 stops → tmux dies. Workaround: power settings = no sleep on AC |
| Windows reboot | ❌ | Expected. Restart claude after WSL2 boot. |
| `wsl --shutdown` | ❌ | Expected. Don't run during active sessions. |

#### Multi-session reconnect from mobile

```bash
# List all sessions still alive on WSL2
mosh user@<ip> -- tmux ls
# claude: 3 windows (created Mon May 4 10:00:00 2026)
# wave17: 1 windows (created Mon May 4 11:30:00 2026)

# Attach to specific session (overrides ~/.bashrc default)
mosh user@<ip> -- tmux attach -t wave17

# Or: connect, then inside tmux: Ctrl+B → S to switch sessions, Ctrl+B → ( ) to cycle
# Or: detach current (Ctrl+B → D), then  tmux new -s sandbox  for fresh session
```

#### Force-takeover from new device (kick stale clients)

```bash
mosh user@<ip> -- tmux attach -d -t claude
# -d = detach all other clients first (e.g. when previous mobile session is stuck)
```

#### Gotchas

1. **Mosh requires interactive shell.** If `~/.bashrc` runs heavy commands (e.g. nvm init, conda activate), mosh handshake may timeout. Keep first-line snippets fast or guard with `[ -n "$PS1" ]`.
2. **Tailscale + mosh UDP:** mosh handshake first runs over SSH/TCP to negotiate UDP key, then switches. If TCP works but UDP doesn't (corp firewall blocking high UDP ports on the Android side), mosh fails silently. Test from a known-good network first.
3. **Battery optimizers (Android):** Termux + Tailscale must BOTH be whitelisted from battery saver, else Android suspends mosh-client + drops UDP between handshakes. Same advice as §4.7 #5 but doubly important for mosh.
4. **Don't run mosh outside tmux.** Mosh protects the connection but if mosh-server itself crashes (rare, but happens after WSL2 OOM or `wsl --shutdown`), processes started directly under mosh die. Always: `mosh → tmux attach → claude`.

---

## 4. Android Phone Setup

Use case: SSH into the WSL2 dev machine from your phone, anywhere with internet. Tested 2026-05-04.

### 4.1 Install Tailscale on Android

1. Play Store → search **"Tailscale"** → Install
2. Open app → **Sign in** → choose the SAME identity provider used on Windows (Google / Microsoft / GitHub / Apple)
3. After login, the machine list should show your Windows device (e.g. `nguyenvankiet-1`) as **Connected**
4. **Enable Always-on VPN** so Tailscale auto-starts after phone boot:
   - Android **Settings** → **Network & internet** → **VPN** → **Tailscale** → toggle **Always-on VPN: ON**
5. **Whitelist Tailscale from battery optimization** (especially Xiaomi / Oppo / Samsung / Huawei ROMs which kill background VPNs):
   - Settings → **Apps** → **Tailscale** → **Battery** → **No restrictions** / **Don't optimize**
   - Without this, Tailscale gets killed in deep sleep and SSH fails with timeouts after the phone wakes

### 4.2 Pick an SSH client

| App | When to pick | Source |
|---|---|---|
| **Termius** | GUI flow, multi-host, easy on phone | Play Store |
| **Termux** | CLI parity with desktop, scriptable, runs `ssh kite` exactly like the WSL guide | **F-Droid** (Play Store version is deprecated by Termux team — do not use) |
| **JuiceSSH** | Simple GUI, free | Play Store |
| **ConnectBot** | Open-source, basic GUI | Play Store / F-Droid |

Recommendation: **Termius for daily use, Termux for power features (mosh, port forwarding, scripting).**

### 4.3 Generate SSH key on phone (Termux path — most reliable)

Termius UI varies by version (key generation may be hidden under "More" / "Keychain" / inside the host edit form depending on app version). Termux is consistent:

```bash
# In Termux app — first time only
pkg update && pkg install openssh -y
ssh-keygen -t ed25519 -C "phone-android@kite-dev" -f ~/.ssh/kite_dev
cat ~/.ssh/kite_dev.pub      # long-press output to copy
```

### 4.4 Install the public key on the WSL2 dev machine

From a desktop session on the dev machine (or via Claude Code), append the phone's `.pub` to `~/.ssh/authorized_keys`:

```bash
echo 'ssh-ed25519 AAAAC3...phone-android@kite-dev' >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
```

Verify:
```bash
cat ~/.ssh/authorized_keys     # one line per device — laptop, phone, etc.
```

### 4.5 Configure host on the phone

**Termius:**
1. Tab **Hosts** → tap **+** → New Host
2. Fill in:
   - **Alias:** `kite-dev`
   - **Address:** `100.90.149.107` (your **Windows Tailscale IP** — NOT the LAN IP)
   - **Port:** `2222`
   - **Username:** `nguyenvankiet`
   - **Authentication:** switch from "Password" to **"Public Key"** / **"Use Key"** — choose the key generated in §4.3 (or generated in-app)
3. Save → tap host → connects

**Termux:**

```bash
# In Termux
mkdir -p ~/.ssh && chmod 700 ~/.ssh
cat > ~/.ssh/config <<'EOF'
Host kite
  HostName 100.90.149.107
  Port 2222
  User nguyenvankiet
  IdentityFile ~/.ssh/kite_dev
  ServerAliveInterval 30
  ServerAliveCountMax 3
EOF
chmod 600 ~/.ssh/config

ssh kite     # test
```

### 4.6 Daily workflow on phone

Open **Termius** (or Termux) → tap `kite-dev` → connected. Tailscale runs silently in the background — no need to open the Tailscale app every time, thanks to Always-on VPN (§4.1).

**Quick-connect tricks:**
- **Termius:** long-press host → **Add to home screen** → 1-tap connect from launcher
- **Termux:** install **Termux:Widget** from F-Droid → make a launcher widget that runs `ssh kite`

### 4.7 Phone-specific gotchas

| Symptom | Cause | Fix |
|---|---|---|
| Connection times out after phone sleep | ROM killed Tailscale in background | §4.1 step 5 — battery optimization off for Tailscale |
| `ssh: connect to host 100.x.y.z port 2222: Network is unreachable` | Always-on VPN not active OR Tailscale logged out | Open Tailscale app → toggle ON, verify Windows device shows Connected |
| Works on Wi-Fi, fails on cellular | Some carriers block WireGuard UDP | Tailscale auto-falls-back to TCP relay; if still fails, switch to Cloudflare Tunnel option |
| `Permission denied (publickey)` | Public key not added on dev side OR wrong key selected on phone | §4.4 — verify `cat ~/.ssh/authorized_keys` shows the phone's pub key |

---

## 5. Windows 11 Client Setup (reuse mobile key)

**Use case:** SSH from a Windows 11 laptop (Command Prompt, PowerShell, or Windows Terminal) to the WSL2 dev machine, **reusing the `kite_dev` ed25519 key already provisioned for the Termux/Android client (§4)**. No second keygen, no extra public key on WSL2 — same key works from both clients, so revocation is one entry to delete.

**Pre-requisites:** §4 fully done (Termux key generated + `~/.ssh/kite_dev.pub` installed on WSL2 + Tailscale signed in to the same identity on phone). Keep the phone handy to copy the private key from Termux to Windows over Tailscale.

### 5.1 Install Tailscale on Windows 11

Same identity provider as host + Termux (Google / Microsoft / GitHub / Apple). Required because client + host are on different networks (per §2.4 and §4 logic).

```powershell
# PowerShell — direct download (winget --silent has been unreliable per §11 lesson 3)
$tmp = "$env:TEMP\tailscale-setup.exe"
Invoke-WebRequest -Uri 'https://pkgs.tailscale.com/stable/tailscale-setup-latest.exe' `
  -OutFile $tmp -UseBasicParsing
Start-Process $tmp -Verb RunAs        # UAC prompt → Yes → click Install
```

After GUI install + sign-in, verify from any shell:

```powershell
& 'C:\Program Files\Tailscale\tailscale.exe' status      # should list kite-wsl2 = Connected
& 'C:\Program Files\Tailscale\tailscale.exe' ping kite-wsl2
```

**Settings → Run unattended → ON** (Tailscale auto-resumes after Windows wake; equivalent to Always-on VPN on Android per §4.1 step 4).

### 5.2 Copy the mobile key from Termux to Windows

The Termux private key `~/.ssh/kite_dev` was created in §4.3 / migration script 02 and lives only on the phone. To reuse it on Windows, transfer over the Tailscale-only path — file never touches public internet.

**Option A — recommended: Termux sshd → Windows scp pull (Tailscale-only path)**

On the **phone (Termux)**, start sshd one-time:

```bash
# Termux — set a password first (sshd refuses passwordless accounts)
passwd                                    # set a temp password (you'll disable sshd after copy)
sshd                                      # starts on default port 8022
hostname -I                               # phone's Tailscale IP — note this (100.x.y.z)
whoami                                    # Termux username, usually "u0_aXXX" — note this
```

On **Windows 11 (Command Prompt or PowerShell)** — built-in OpenSSH client:

```cmd
mkdir %USERPROFILE%\.ssh 2>nul
scp -P 8022 <termux-user>@<phone-tailscale-ip>:~/.ssh/kite_dev      %USERPROFILE%\.ssh\kite_dev
scp -P 8022 <termux-user>@<phone-tailscale-ip>:~/.ssh/kite_dev.pub  %USERPROFILE%\.ssh\kite_dev.pub
```

Enter the Termux password when prompted (each scp). After copy, **stop Termux sshd + clear password**:

```bash
# Termux
pkill sshd
passwd -d $(whoami)                       # remove the temp password (optional but tidy)
```

**Option B — fallback: cat + paste via secure note app**

If Tailscale-only scp doesn't work (Termux sshd misbehaves on some ROMs):

```bash
# Termux
cat ~/.ssh/kite_dev      # paste into Windows %USERPROFILE%\.ssh\kite_dev    (LF endings!)
cat ~/.ssh/kite_dev.pub  # paste into Windows %USERPROFILE%\.ssh\kite_dev.pub
```

⚠️ **Critical: preserve LF line endings.** Windows clipboard / Notepad converts LF → CRLF, which breaks the OpenSSH key parser silently. Use VS Code or Notepad++ → **Edit → EOL Conversion → Unix (LF)** before save. Verify:

```cmd
findstr /R "BEGIN OPENSSH" %USERPROFILE%\.ssh\kite_dev
:: must print: -----BEGIN OPENSSH PRIVATE KEY-----
```

If the line ends with `\r\n` (CRLF), `ssh -i kite_dev` will fail with `Load key: error in libcrypto`.

### 5.3 Lock private-key ACL (Windows OpenSSH refuses keys readable by others)

```powershell
icacls "$env:USERPROFILE\.ssh\kite_dev" /inheritance:r /grant:r "$($env:USERNAME):(R)"
icacls "$env:USERPROFILE\.ssh\kite_dev"
# Expected output: only your user has (R), no Authenticated Users / Everyone
```

Without this lock, `ssh kite` errors with:

```
Permissions for 'C:\Users\<you>\.ssh\kite_dev' are too open.
This private key will be ignored.
```

### 5.4 SSH config on Windows

Edit `%USERPROFILE%\.ssh\config` (create if missing):

```
Host kite
  HostName 100.69.110.122
  Port 2222
  User nguyenvankiet
  IdentityFile ~/.ssh/kite_dev
  ServerAliveInterval 60
  ServerAliveCountMax 3
```

Same `Host kite` alias as phone (per §4.5). The `~` expands correctly on Windows 11 OpenSSH (resolves to `%USERPROFILE%`).

### 5.5 Test from Windows

```cmd
ssh kite "echo ok && hostname && whoami"
```

Expected: prints `ok`, the WSL2 hostname, and your WSL username. Common failures:

| Symptom | Cause | Fix |
|---|---|---|
| `Permission denied (publickey)` | CRLF in private key, OR ACL too open, OR pubkey not yet on WSL2 | Re-do §5.2 with LF-preserving editor; re-run §5.3 icacls; verify `cat ~/.ssh/authorized_keys` on WSL2 contains the same `kite_dev.pub` |
| `Connection timed out` | Tailscale not active OR not signed-in | Verify §5.1 — `tailscale status` must show `kite-wsl2` Connected |
| `Could not resolve hostname` | Using LAN IP (`172.x.x.x`) from off-network | Use Tailscale IP (`100.x.y.z`) per §4.5 / §11 lesson 4 |
| `Load key ... error in libcrypto` | CRLF line endings | §5.2 LF-preservation step |

### 5.6 mosh on Windows (optional)

Stock Windows OpenSSH does not ship `mosh`. Three paths:

| Path | Setup | When to pick |
|---|---|---|
| **mosh via WSL on the client** | `wsl --install`, then in WSL: `apt install -y mosh-client`; run `mosh kite` from WSL shell | Client already has WSL or you'll install it |
| **mosh-for-windows port** | Download from https://mosh.org/#getting (community Windows binary) | Native cmd usage without WSL |
| **Plain ssh** | Already works after §5.5 | Laptop on stable Wi-Fi/Ethernet — mosh's main value (UDP survival across mobile network roams + screen-off) doesn't apply to laptops |

For a non-mobile Windows client on stable Wi-Fi/Ethernet, plain `ssh kite` is sufficient. The 3-layer Tailscale + mosh + tmux stack from §3.4 collapses to **Tailscale + tmux** on a laptop — tmux still survives Windows reboot's WSL shutdown? No (§3.4 survives matrix). It survives `ssh` disconnect, which is what laptops actually hit (lid close → Wi-Fi pause → SSH timeout).

### 5.7 Daily flow

```cmd
ssh kite
```

Inside WSL2, the `~/.bashrc` snippet from §2.1 / migration script 02 auto-attaches tmux session `claude`. Detach with `Ctrl+B D`; reconnect with the same `ssh kite` — same session, same scrollback.

**Force-takeover** (kick a stuck phone session):

```cmd
ssh kite -t tmux attach -d -t claude
```

The `-t` allocates a TTY so tmux renders correctly; `-d` detaches all other clients first.

### 5.8 Windows-specific gotchas

1. **CRLF line endings in private key** — see §5.2; this is the #1 silent failure mode on Windows. Always use a LF-preserving editor for the paste fallback.
2. **`%USERPROFILE%` vs `~` in `IdentityFile`** — both work in Windows 11 OpenSSH; `~` is portable and matches the phone's `~/.ssh/config` entry from §4.5.
3. **Windows Defender Firewall on the client** — outbound SSH (port 2222) is allowed by default; inbound is irrelevant since the client initiates the connection. No firewall rule needed on the client.
4. **Windows Terminal vs Command Prompt** — both work; Windows Terminal renders tmux colors better (256-color out of the box). PowerShell users: same `ssh kite` command, same config file.
5. **Reusing the key revokes from both at once** — if the phone is lost, removing `kite_dev.pub` from `~/.ssh/authorized_keys` on WSL2 disables BOTH the phone and the laptop. Decide if that's the intent before reusing; if you want independent revocation, generate a separate `kite_dev_laptop` key on Windows (§2.2 procedure) and add its `.pub` as a second entry on WSL2.

---

## 6. Common Ops Workflows (the use cases that motivated SSH-direct)

### 6.1 Watch a CI workflow until terminal

```bash
# Pane 2 — block until terminal, no chat overhead
gh pr checks 737 --watch
# OR poll loop with broader matching
until gh pr checks 737 --json conclusion --jq '.[].conclusion' | grep -qE 'success|failure|cancelled'; do
  sleep 30
  date
done && echo "DONE"
```

### 6.2 Local Docker build with live progress

```bash
# Pane 1 — direct foreground; you see every layer
docker buildx build --progress=plain \
  -f kiteclass/kiteclass-frontend/Dockerfile \
  -t kc-fe:local .

# Smoke test
docker run --rm -d --name kc-fe-smoke -p 13000:3000 kc-fe:local
sleep 5
curl -sI http://localhost:13000/
docker logs kc-fe-smoke
docker stop kc-fe-smoke
```

### 6.3 Docker stack (use project scripts per CLAUDE.md)

```bash
# NEVER run docker-compose directly — use scripts
cd kitehub
./scripts/up.sh
./scripts/logs.sh kitehub-subscription | grep --line-buffered ERROR
./scripts/down.sh
```

### 6.4 Maven test loop on a single service

```bash
cd kitehub/kitehub-admin
mvn test -Dtest=AdminControllerTest#testGetRevenue
# Or full suite with strict warnings
mvn -pl kitehub/kitehub-admin -P strict-warnings test
```

### 6.5 Tail a long-running log

```bash
# Pane 2 — keeps streaming, Ctrl+C to stop
docker logs -f --tail 50 kite-postgres 2>&1 | grep --line-buffered -E "ERROR|FATAL|panic"
```

---

## 7. Hybrid with Claude Code

SSH-direct và Claude Code không đối nghịch — bổ trợ nhau:

| Task | Tool |
|---|---|
| Edit Dockerfile / workflow YAML / config files | Claude Code (multi-file context, regex-precise edits) |
| File a GAP, draft PR body, write commit msg | Claude Code (structured artifacts) |
| Update memory (`feedback_*.md`) | Claude Code (persistent across sessions) |
| Open / merge / close PRs | Either (Claude Code via `gh`, or SSH `gh pr merge`) |
| Watch CI to terminal | **SSH + tmux** (no chat-turn overhead) |
| Docker build verify | **SSH** (`docker buildx build` foreground) |
| Smoke test container | **SSH** (`docker run + curl + docker logs`) |
| Mvn test loop on a flake | **SSH** (`mvn test -Dtest=... -Dmaven.surefire.debug`) |
| Tail logs during incident | **SSH + tmux** (multi-pane log streams) |

Decision rule: **anything that's a `for/while/until` loop or "watch this thing for N minutes" → SSH**. **Anything that's a state-aware decision or artifact creation → Claude Code.**

---

## 8. Security Checklist

- [x] `PasswordAuthentication no` — key-only (set in §2.1)
- [x] `PermitRootLogin no` — never root
- [x] `AllowUsers nguyenvankiet` — explicit allowlist
- [x] Non-default port `2222` — reduces drive-by scans
- [ ] Install `fail2ban`: `sudo apt install fail2ban && sudo systemctl enable --now fail2ban`
- [ ] If exposing externally: use Tailscale instead of port-forward (zero public surface)
- [ ] Rotate SSH key annually: `ssh-keygen` new, append to authorized_keys, remove old
- [ ] Audit access: `last -n 20` and `journalctl -u ssh -n 100`

**Never expose port 2222 directly to the internet** without fail2ban + Tailscale OR strong PAM + key rotation. Solo-dev with Tailscale = best risk/reward.

---

## 9. Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| `ssh: connect to host ... port 2222: Connection refused` | sshd not running OR portproxy down | `sudo systemctl status ssh` in WSL2; re-run portproxy script on Windows |
| Can connect locally but not from outside laptop | Windows Firewall blocking 2222 | Re-run `New-NetFirewallRule` (§2.3) |
| Connection drops after WSL2 sleep | systemd ssh.service stopped | `sudo systemctl enable ssh` (already enabled if you followed §2.1) — check `journalctl -u ssh -n 50` |
| `Permission denied (publickey)` | Key not in authorized_keys OR perms wrong | `chmod 700 ~/.ssh; chmod 600 ~/.ssh/authorized_keys` on WSL side |
| WSL2 IP changed → portproxy stale | WSL restarted | Run portproxy script (§2.3); add to Task Scheduler if not already |
| tmux session dies on WSL shutdown | Windows shutdown / WSL --shutdown killed everything | Expected — tmux only persists across SSH disconnects, not WSL reboots. Use `wsl --shutdown` carefully |

---

## 10. Related

- `remote-control-setup.md` — Claude Code mobile remote (alternative path, no SSH needed)
- `wsl2-fresh-setup.md` — clean-room WSL2 setup (this guide assumes that's done)
- `wsl-migration-playbook.md` — migrating WSL2 across machines
- `local-dev-setup-non-wsl.md` — Mac/Linux native setup (SSH section partially applicable)

---

## 11. Lessons learned (during 2026-05-04 setup)

Pitfalls discovered while actually following this guide end-to-end. Documented inline in the relevant sections, repeated here for searchability:

1. **`ssh.socket` overrides `Port` in `sshd_config`** on Ubuntu 24.04+ (and any distro with socket-activated sshd). `sshd -T` reports the new port but `ss -tlnp` still shows `:22`. Fix: drop-in at `/etc/systemd/system/ssh.socket.d/listen.conf` — see §2.1.
2. **`sudo tee <<HEREDOC` collides with sudo password prompt** when piped via `echo password | sudo -S`. Sudo reads the heredoc body as password attempts and bails. Workaround: write the file to `/tmp` first, then `sudo install -m 644 -o root -g root /tmp/file /destination`.
3. **`winget install --silent` swallows UAC failures.** If UAC isn't accepted, install fails with no diagnostic and `winget list` reports nothing installed. Direct `Invoke-WebRequest` + `Start-Process -Verb RunAs` with the GUI installer is more reliable.
4. **Use the Tailscale IP, not the LAN IP, when configuring phone clients.** LAN IPs (e.g. `172.16.x.x`) only work on the same Wi-Fi. Tailscale IPs (`100.x.y.z`) work anywhere with internet.
5. **Android battery optimizers kill Tailscale.** Xiaomi / Oppo / Samsung / Huawei ROMs aggressively suspend background apps. Whitelist Tailscale or get random connection timeouts after the phone wakes from deep sleep.
6. **Termius UI varies wildly across versions.** Key generation is hidden under different paths (Keychain / More / inside host edit form / "+" in auth field). Termux is more consistent if you hit Termius UI fog.
7. **Termius defaults to Password auth.** With key-only sshd, must explicitly switch the host's auth method from Password to Public Key — leaving the password field blank doesn't auto-switch.

---

## 12. Log

- **2026-05-07 (Windows 11 client)** — Added §5 "Windows 11 Client Setup (reuse mobile key)" covering Tailscale install on Windows, copying the existing `kite_dev` ed25519 key from Termux to Windows over Tailscale-only scp (Option A) or LF-preserving paste (Option B), Windows ACL lock via `icacls`, `~/.ssh/config` entry mirroring §4.5 phone config, mosh-on-Windows options (WSL/native port/skip), daily flow, and 5 Windows-specific gotchas. Renumbered §5–§11 → §6–§12. Updated cross-reference in §2.1 from "§10 lesson" → "§11 lesson" to track renumbering. Triggered by user request 2026-05-07: "thêm setup instructions for Windows 11 client + reuse mobile key" — avoid second keygen so revocation stays one-entry on WSL2 `authorized_keys`.
- **2026-05-04 (mosh layer)** — Added §3.4 "Mosh layer — survives mobile network drops + screen-off (CRITICAL for mobile)". Triggered by Wave 17 incident: 3/4 background agents killed silently when mobile SSH session disconnected (root cause = SIGHUP cascade, not runtime limit). Documents the 3-layer Tailscale + mosh + tmux stack as 2026 trending pattern for mobile dev. Includes setup, survives matrix, multi-session reconnect, force-takeover, 4 gotchas. References memory `feedback_agent_kill_root_cause.md`.
- **2026-05-04 (extended)** — Updated after end-to-end Android setup completed: §2.1 ssh.socket drop-in (CRITICAL — fix the silent footgun); §2.3 actual Task Scheduler PowerShell that worked (replacing the bullet-point abstract); §2.4 expanded with direct-download Tailscale install path (winget --silent failed silently); new §4 Android Phone Setup (Tailscale Always-on, Termux key gen flow, Termius host config gotchas, battery optimization caveat); new §10 Lessons learned section codifying 7 pitfalls discovered during real setup. Renumbered §5-§10 accordingly.
- **2026-05-04** — Created during GAP-284 closure. Motivated by hotfix session where ops-heavy verification (Docker builds, CI polls, smoke tests) burned ~25-30 min of Claude-session friction that SSH-direct + tmux would have collapsed to ~5 min. Solo-dev mode; no formal review needed.
