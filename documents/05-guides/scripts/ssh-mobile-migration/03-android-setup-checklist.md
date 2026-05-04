# SSH Mobile Migration — Phase 3: Android setup checklist

**Pre-req:** Phases 1+2 done (Windows cleanup + WSL2 setup scripts ran successfully). You should have your WSL2 Tailscale IP from script 2 output (format `100.69.110.122`).

**Time:** ~10 minutes total.

---

## A) Tailscale Android app

If already installed and signed in (per main guide §4.1), **skip to B**. Otherwise:

1. Play Store → search **"Tailscale"** → Install (official Tailscale Inc.)
2. Open → **Sign in** → use SAME identity provider you used for WSL2 Tailscale (Google / Microsoft / GitHub / Apple)
3. After login, machine list should show **kite-wsl2** as Connected
4. **Settings → Always-on VPN: ON** (so Tailscale auto-resumes after phone wakes)
5. **Settings → Battery optimization: Tailscale = Don't optimize** (Android 12+; varies by ROM)

---

## B) Termux from F-Droid

⚠ **Use F-Droid version, NOT Play Store version.** F-Droid is the canonical maintained release; Play Store is an unofficial fork.

1. Browser → https://f-droid.org/packages/com.termux/ → Download APK
2. Install (allow "Install unknown apps" for browser if prompted)
3. Open Termux, run:
   ```bash
   pkg update && pkg upgrade -y
   pkg install -y mosh openssh tmux
   ```

---

## C) SSH key for Termux → WSL2

### C.1) Check existing key first

```bash
# In Termux
ls -la ~/.ssh/kite_dev ~/.ssh/kite_dev.pub 2>/dev/null && echo "KEY EXISTS" || echo "NO KEY — go to C.2"
```

If output shows `KEY EXISTS` + 2 files (private + .pub) → already have key. **Verify it's installed on WSL2:**

```bash
# In Termux — quick test (will succeed if key works, fail if needs install)
ssh -i ~/.ssh/kite_dev -p 2222 -o StrictHostKeyChecking=no -o BatchMode=yes nguyenvankiet@100.69.110.122 "echo OK" 2>&1 | tail -1
# Output "OK"            → key installed, skip to D
# Output "Permission denied" or "Connection refused" → key not installed, do C.3
```

### C.2) Generate new key (only if C.1 said NO KEY)

```bash
# In Termux
ssh-keygen -t ed25519 -C "android-termux@kite-dev" -f ~/.ssh/kite_dev -N ""
# -N "" = no passphrase (mobile-friendly; protected by Tailscale auth + device unlock)
cat ~/.ssh/kite_dev.pub
# → copy the entire "ssh-ed25519 AAAA... android-termux@kite-dev" line
```

### C.3) Install pubkey on WSL2

**Easiest from mobile:** paste the public key line into Claude Code chat — say "install this Termux pubkey on WSL2" and Claude appends it to `~/.ssh/authorized_keys`.

**Or manual** (if you have other SSH access to WSL2):

```bash
# On WSL2
mkdir -p ~/.ssh && chmod 700 ~/.ssh
echo 'ssh-ed25519 AAAA... android-termux@kite-dev' >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
```

**Verify install:** repeat the test command from C.1 — should now print `OK`.

---

## D) Termux SSH config

```bash
# In Termux — create ~/.ssh/config
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

## E) Test the new stack

```bash
# In Termux

# Test 1: Plain SSH works (TCP path, no mosh yet)
ssh kite "echo 'SSH OK from Termux at' \$(date)"

# Test 2: Mosh works (UDP path, the survival layer)
mosh kite
# → should auto-attach tmux session "claude" (per ~/.bashrc snippet from script 2)
# → if a fresh "claude" session, you'll see empty bash prompt inside tmux

# Once inside tmux session "claude":
# - Run: echo "Hello from mobile" > /tmp/mobile-test.txt
# - Press Ctrl+B then D to detach (session stays alive on WSL2)
# - Close Termux completely (swipe away from app switcher)
# - Wait 1 minute
# - Reopen Termux, run: mosh kite
# - You should land back in same tmux with the prompt where you left off
# - Verify: cat /tmp/mobile-test.txt — should print "Hello from mobile"
```

If all 3 pass → migration successful. Network drops, app switches, screen-off — none kill the session anymore.

---

## F) Daily workflow on phone

```bash
# Morning — open Termux, one command:
mosh kite
# → auto-attaches to "claude" tmux session
# → Claude Code commands work normally:
#     cd ~/projects/2026-Kite-Class-Platform
#     claude

# Switch app, screen off, lose network:
# DO NOTHING. Mosh + tmux survive.

# When you reopen Termux later:
# Already attached? Just keep typing.
# Disconnected? Type 'mosh kite' again — back where you left off.

# Force-takeover (e.g. forgot to detach on laptop):
mosh kite -- tmux attach -d -t claude
```

---

## G) Optional polish

### Termux widget (one-tap launch)

Install **Termux:Widget** from F-Droid → home screen widget shortcut to `mosh kite` script. Tap to instant-connect.

### Wakelock (prevent Android killing Termux during long mosh idle)

```bash
# In Termux, install API tools
pkg install -y termux-api
# Acquire wakelock when mosh active
termux-wake-lock
# Release when done
termux-wake-unlock
```

Caveat: drains battery slightly. Use only when running long agents you want to monitor.

### ntfy push notification (optional — long-running task complete)

Add to WSL2 (e.g. as Claude stop hook):

```bash
curl -d "Wave done in $(date +%H:%M)" ntfy.sh/your-secret-topic-name
```

Install ntfy app on Android, subscribe to same topic → get push when long agents finish (don't have to keep checking mosh).

---

## H) Rollback (if migration fails)

```bash
# WSL2 — revert ~/.bashrc tmux snippet
sed -i '/# >>> ssh-mobile-migration tmux auto-attach >>>/,/# <<< ssh-mobile-migration tmux auto-attach <<</d' ~/.bashrc

# WSL2 — stop services (don't uninstall in case you re-enable later)
sudo systemctl disable --now tailscaled
sudo systemctl disable --now ssh

# Windows — re-add portproxy if you used it before (run cleanup script in reverse manually,
# or follow main guide §2.3)
```

Most likely failure modes + fixes:
- **Mosh handshake timeout:** corp/uni firewall blocks UDP 60000-61000. Test from home network.
- **Tailscale "logged out" after phone reboot:** Always-on VPN not enabled. Re-do step A4.
- **Session "claude" not auto-attaching:** ~/.bashrc snippet missing. Re-run script 2 (idempotent).
- **WSL2 dies overnight:** Windows sleep settings. Power Options → "When plugged in, never sleep".

---

## Done

You're now on the mobile-resilient stack. Mobile disconnect cannot kill long-running agents.

Cross-references:
- Architecture overview: `documents/05-guides/ssh-terminal-direct-access.md` §3.4
- Memory: `feedback_agent_kill_root_cause.md` (root cause analysis)
- PR introducing this: #746
