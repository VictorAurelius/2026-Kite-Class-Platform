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

# Verify listening
ss -tlnp | grep :22
```

Edit `/etc/ssh/sshd_config.d/99-kite-local.conf` (new file — don't touch base config):

```bash
sudo tee /etc/ssh/sshd_config.d/99-kite-local.conf > /dev/null <<'EOF'
# Project-local SSH overrides for WSL2 dev access
Port 2222                       # avoid host port 22 conflicts
PasswordAuthentication no       # key-only
PubkeyAuthentication yes
PermitRootLogin no
ClientAliveInterval 60
ClientAliveCountMax 3
AllowUsers nguyenvankiet
EOF

sudo systemctl restart ssh
```

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

**Persistence:** WSL2 IP changes after every WSL reboot. Make this a startup script — save the PowerShell above as `C:\scripts\wsl-ssh-portproxy.ps1`, then run it on Windows boot via Task Scheduler:

- Trigger: At system startup
- Action: `powershell.exe -ExecutionPolicy Bypass -File C:\scripts\wsl-ssh-portproxy.ps1`
- Run with highest privileges, run whether user is logged on or not

### 2.4 External access (optional)

If connecting from outside the local network (coffee shop, phone on cellular), pick ONE:

| Option | Setup | Tradeoff |
|---|---|---|
| **Tailscale** | Install on Windows + outside machine; `tailscale ip -4` gives stable IP | Easiest; routes via Tailscale relay; free for personal use |
| **Cloudflare Tunnel** | `cloudflared tunnel` on Windows; SSH over HTTPS | No firewall changes; needs Cloudflare account |
| **Router port forward** | Forward TCP 2222 from public IP → Windows host | Direct; exposes 2222 to internet — needs strong key auth + fail2ban |

Recommend Tailscale for solo-dev — zero firewall pain.

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

---

## 4. Common Ops Workflows (the use cases that motivated SSH-direct)

### 4.1 Watch a CI workflow until terminal

```bash
# Pane 2 — block until terminal, no chat overhead
gh pr checks 737 --watch
# OR poll loop with broader matching
until gh pr checks 737 --json conclusion --jq '.[].conclusion' | grep -qE 'success|failure|cancelled'; do
  sleep 30
  date
done && echo "DONE"
```

### 4.2 Local Docker build with live progress

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

### 4.3 Docker stack (use project scripts per CLAUDE.md)

```bash
# NEVER run docker-compose directly — use scripts
cd kitehub
./scripts/up.sh
./scripts/logs.sh kitehub-subscription | grep --line-buffered ERROR
./scripts/down.sh
```

### 4.4 Maven test loop on a single service

```bash
cd kitehub/kitehub-admin
mvn test -Dtest=AdminControllerTest#testGetRevenue
# Or full suite with strict warnings
mvn -pl kitehub/kitehub-admin -P strict-warnings test
```

### 4.5 Tail a long-running log

```bash
# Pane 2 — keeps streaming, Ctrl+C to stop
docker logs -f --tail 50 kite-postgres 2>&1 | grep --line-buffered -E "ERROR|FATAL|panic"
```

---

## 5. Hybrid with Claude Code

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

## 6. Security Checklist

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

## 7. Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| `ssh: connect to host ... port 2222: Connection refused` | sshd not running OR portproxy down | `sudo systemctl status ssh` in WSL2; re-run portproxy script on Windows |
| Can connect locally but not from outside laptop | Windows Firewall blocking 2222 | Re-run `New-NetFirewallRule` (§2.3) |
| Connection drops after WSL2 sleep | systemd ssh.service stopped | `sudo systemctl enable ssh` (already enabled if you followed §2.1) — check `journalctl -u ssh -n 50` |
| `Permission denied (publickey)` | Key not in authorized_keys OR perms wrong | `chmod 700 ~/.ssh; chmod 600 ~/.ssh/authorized_keys` on WSL side |
| WSL2 IP changed → portproxy stale | WSL restarted | Run portproxy script (§2.3); add to Task Scheduler if not already |
| tmux session dies on WSL shutdown | Windows shutdown / WSL --shutdown killed everything | Expected — tmux only persists across SSH disconnects, not WSL reboots. Use `wsl --shutdown` carefully |

---

## 8. Related

- `remote-control-setup.md` — Claude Code mobile remote (alternative path, no SSH needed)
- `wsl2-fresh-setup.md` — clean-room WSL2 setup (this guide assumes that's done)
- `wsl-migration-playbook.md` — migrating WSL2 across machines
- `local-dev-setup-non-wsl.md` — Mac/Linux native setup (SSH section partially applicable)

---

## 9. Log

- **2026-05-04** — Created during GAP-284 closure. Motivated by hotfix session where ops-heavy verification (Docker builds, CI polls, smoke tests) burned ~25-30 min of Claude-session friction that SSH-direct + tmux would have collapsed to ~5 min. Solo-dev mode; no formal review needed.
