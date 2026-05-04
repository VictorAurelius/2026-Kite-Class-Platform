# SSH Mobile Migration Scripts

One-shot migration from old SSH-via-Windows-host setup to mobile-resilient Tailscale + mosh + tmux stack on WSL2.

**Why migrate:** Old setup (Windows portproxy + LAN IP + plain SSH) drops mobile sessions on app switch / screen off / network roam, killing long-running agents. New stack survives all of these.

**Time:** ~5 minutes machine work + ~10 minutes Android setup.

---

## Run order

| Step | Script | Where to run | Auto/Manual |
|------|--------|--------------|:----------:|
| 1 | `01-cleanup-windows.ps1` | PowerShell **as Administrator** | Manual (one click "Yes" on UAC) |
| 2 | `02-setup-wsl2.sh` | WSL2 bash | Auto (1 sudo prompt + 1 browser auth click) |
| 3 | `03-android-setup-checklist.md` | Android phone | Manual (per checklist) |

## Quick run

```bash
# Phase 1 — Windows cleanup (from WSL2, triggers UAC popup on Windows)
powershell.exe -Command "Start-Process powershell -Verb RunAs -ArgumentList '-ExecutionPolicy', 'Bypass', '-File', '\\\\wsl$\\Ubuntu\\home\\nguyenvankiet\\projects\\2026-Kite-Class-Platform\\documents\\05-guides\\scripts\\ssh-mobile-migration\\01-cleanup-windows.ps1'"

# Phase 2 — WSL2 setup
bash documents/05-guides/scripts/ssh-mobile-migration/02-setup-wsl2.sh

# Phase 3 — Android (read checklist, do on phone)
cat documents/05-guides/scripts/ssh-mobile-migration/03-android-setup-checklist.md
```

---

## What each script does

### 01-cleanup-windows.ps1
**Removes (idempotent):**
- `netsh portproxy` rule listenport=2222 → WSL2
- Windows Firewall rule "WSL2 SSH 2222"
- Task Scheduler job "WSL2-SSH-Portproxy"

**Does NOT touch:** Tailscale Windows app, SSH keys, sshd config.

### 02-setup-wsl2.sh
**Installs (idempotent):**
- openssh-server (sshd on port 2222)
- mosh (UDP server)
- tailscale (peer-to-peer VPN, gives WSL2 its own 100.x.x.x IP)

**Configures:**
- ssh.socket drop-in for port 2222 (Ubuntu 24.04+ socket-activation footgun)
- Enables tailscaled + ssh systemd services
- Adds tmux auto-attach snippet to ~/.bashrc

**Interactive:** 1 sudo password + 1 browser-link click for Tailscale auth.

**Output:** Prints WSL2 Tailscale IP for use in Android setup.

### 03-android-setup-checklist.md
Manual steps on phone:
- Tailscale Android app (sign in same identity)
- Termux from F-Droid + `pkg install mosh openssh tmux`
- SSH key generation + install on WSL2
- Termux SSH config (`~/.ssh/config`)
- Test matrix (3 tests: ssh, mosh, mosh+screen-off+reconnect)
- Daily workflow + troubleshooting + rollback

---

## After migration

The old `ssh kite` flow still works — same hostname alias, but resolves to Tailscale IP instead of LAN IP. The new `mosh kite` is the daily-driver command for mobile.

See main guide `../ssh-terminal-direct-access.md` §3.4 for the architectural rationale.

---

## Created

2026-05-04 — alongside main guide §3.4 mosh layer addition. Triggered by Wave 17 incident where 3/4 background agents died from mobile disconnect SIGHUP cascade. PR #746.
