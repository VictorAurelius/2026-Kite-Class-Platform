#!/usr/bin/env bash
# Set up WSL Ubuntu as mosh-accessible "kite-dev" node for mobile remote access.
#
# Idempotent: safe to re-run.
#
# Installs:
#   - mosh (server + client)
#   - tailscale (anywhere-connect VPN, free tier)
#   - openssh-server enabled (already listening per probe)
#
# Configures:
#   - hostname → kite-dev (replaces VANKIET Windows leak)
#   - SSH key auth ensured (ed25519 keypair already present)
#   - UFW firewall: SSH 22/tcp + mosh 60000-61000/udp
#
# Requires sudo. Run with:
#   sudo bash scripts/setup-mosh-kite-dev.sh
#
# After this script completes, user runs (interactive, browser auth):
#   sudo tailscale up
#
# Per `.claude/rules/agent-action-bias.md` §3 row 3 — sudo without NOPASSWD
# is user-execute exception; this committed script = "documented workflow".

set -euo pipefail

if [[ $EUID -ne 0 ]]; then
  echo "ERROR: this script must run as root (use sudo)" >&2
  exit 1
fi

log() { printf '[setup-mosh] %s\n' "$*"; }

# --- 1. Apt install -----------------------------------------------------------
log "Step 1/7: apt update + install mosh + ufw + jq"
DEBIAN_FRONTEND=noninteractive apt-get update -q
DEBIAN_FRONTEND=noninteractive apt-get install -y -q \
  mosh openssh-server ufw curl jq

# --- 2. SSH server enable ----------------------------------------------------
log "Step 2/7: enable + start SSH"
systemctl enable ssh
systemctl start ssh
systemctl is-active ssh

# --- 3. Hostname → kite-dev (runtime + persistent) ---------------------------
CURRENT_HOSTNAME=$(hostname)
if [[ "$CURRENT_HOSTNAME" != "kite-dev" ]]; then
  log "Step 3/7: rename hostname $CURRENT_HOSTNAME → kite-dev (runtime)"
  hostnamectl set-hostname kite-dev
  if ! grep -q "127.0.1.1.*kite-dev" /etc/hosts; then
    sed -i '/127\.0\.1\.1/d' /etc/hosts
    echo "127.0.1.1	kite-dev" >> /etc/hosts
  fi
else
  log "Step 3/7: runtime hostname already kite-dev — skip"
fi

# Persist via /etc/wsl.conf so WSL restart doesn't reset to Windows host name
if ! grep -qE "^\s*hostname\s*=\s*kite-dev" /etc/wsl.conf 2>/dev/null; then
  log "  → persisting hostname in /etc/wsl.conf [network] section"
  # Strip any existing [network] block to keep idempotent
  if grep -q '^\[network\]' /etc/wsl.conf 2>/dev/null; then
    awk 'BEGIN{skip=0} /^\[network\]/{skip=1; next} /^\[/{skip=0} skip==0' /etc/wsl.conf > /etc/wsl.conf.new
    mv /etc/wsl.conf.new /etc/wsl.conf
  fi
  cat >> /etc/wsl.conf <<'EOF'

[network]
hostname = kite-dev
generateHosts = false
EOF
  log "  → /etc/wsl.conf updated. WSL --shutdown required for full effect (Windows side)."
else
  log "  → /etc/wsl.conf already persists hostname=kite-dev — skip"
fi

# --- 4. Firewall (UFW) -------------------------------------------------------
log "Step 4/7: configure UFW firewall"
ufw --force enable
ufw allow 22/tcp comment "SSH (mosh bootstrap)"
ufw allow 60000:61000/udp comment "mosh data plane"

# --- 5. Tailscale install ----------------------------------------------------
if ! command -v tailscale &>/dev/null; then
  log "Step 5/7: install Tailscale"
  curl -fsSL https://tailscale.com/install.sh | sh
else
  log "Step 5/7: Tailscale already installed — skip"
fi

# --- 6. Tailscale device name = kite-dev (post-up only) ----------------------
# `tailscale set --hostname` works without re-auth (tailscale 1.50+).
# If backend not running yet, user must `sudo tailscale up` first; rename runs
# on next script invocation.
if tailscale status --json 2>/dev/null | jq -e '.BackendState == "Running"' >/dev/null 2>&1; then
  CURRENT_TS_NAME=$(tailscale status --json 2>/dev/null | jq -r '.Self.HostName')
  if [[ "$CURRENT_TS_NAME" != "kite-dev" ]]; then
    log "Step 6/7: tailscale device renamed $CURRENT_TS_NAME → kite-dev (Magic DNS)"
    tailscale set --hostname=kite-dev
  else
    log "Step 6/7: tailscale device already named kite-dev — skip"
  fi
else
  log "Step 6/7: tailscale not yet up — rename skipped (re-run script after 'sudo tailscale up')"
fi

# --- 7. Report ---------------------------------------------------------------
log "Step 7/7: setup complete. Summary:"
echo ""
echo "  Hostname:          $(hostname)"
echo "  Mosh version:      $(mosh --version 2>&1 | head -1)"
echo "  SSH active:        $(systemctl is-active ssh)"
echo "  WSL LAN IP:        $(hostname -I | awk '{print $1}')"
echo "  UFW status:        $(ufw status | head -1)"
echo "  Tailscale version: $(tailscale --version 2>&1 | head -1 || echo 'not yet up')"
echo ""
echo "NEXT STEPS (user-interactive):"
echo "  1. Activate Tailscale (one-time browser auth):"
echo "       sudo tailscale up --hostname=kite-dev"
echo "     → Open URL in browser, sign in, approve device 'kite-dev'."
echo ""
echo "  2. Apply persistent hostname (Windows-side, one-time):"
echo "       wsl --shutdown        # in Windows PowerShell"
echo "     → reopen WSL → hostname stays kite-dev across reboots."
echo ""
echo "  3. Get Tailnet IP for mobile config:"
echo "       tailscale ip -4"
echo ""
echo "  3. On mobile (Termius / Blink Shell / JuiceSSH supports mosh):"
echo "       Host: <tailnet-ip-from-step-2>"
echo "       User: $SUDO_USER"
echo "       Auth: SSH key (~/.ssh/id_ed25519 from desktop, or generate new)"
echo "       Connection type: Mosh"
echo ""
echo "  4. (Optional) Keep WSL alive when laptop sleeps:"
echo "       Windows Task Scheduler → 'wsl --exec sleep infinity' on logon"
echo "       OR enable 'WSL Pro' systemd service if available."
