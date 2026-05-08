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
log "Step 1/6: apt update + install mosh + ufw"
DEBIAN_FRONTEND=noninteractive apt-get update -q
DEBIAN_FRONTEND=noninteractive apt-get install -y -q \
  mosh openssh-server ufw curl

# --- 2. SSH server enable ----------------------------------------------------
log "Step 2/6: enable + start SSH"
systemctl enable ssh
systemctl start ssh
systemctl is-active ssh

# --- 3. Hostname → kite-dev --------------------------------------------------
CURRENT_HOSTNAME=$(hostname)
if [[ "$CURRENT_HOSTNAME" != "kite-dev" ]]; then
  log "Step 3/6: rename hostname $CURRENT_HOSTNAME → kite-dev"
  hostnamectl set-hostname kite-dev
  # Update /etc/hosts so sudo doesn't complain
  if ! grep -q "127.0.1.1.*kite-dev" /etc/hosts; then
    sed -i '/127\.0\.1\.1/d' /etc/hosts
    echo "127.0.1.1	kite-dev" >> /etc/hosts
  fi
else
  log "Step 3/6: hostname already kite-dev — skip"
fi

# --- 4. Firewall (UFW) -------------------------------------------------------
log "Step 4/6: configure UFW firewall"
ufw --force enable
ufw allow 22/tcp comment "SSH (mosh bootstrap)"
ufw allow 60000:61000/udp comment "mosh data plane"

# --- 5. Tailscale install ----------------------------------------------------
if ! command -v tailscale &>/dev/null; then
  log "Step 5/6: install Tailscale"
  curl -fsSL https://tailscale.com/install.sh | sh
else
  log "Step 5/6: Tailscale already installed — skip"
fi

# --- 6. Report ---------------------------------------------------------------
log "Step 6/6: setup complete. Summary:"
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
echo "       sudo tailscale up"
echo "     → Open URL in browser, sign in, approve device 'kite-dev'."
echo ""
echo "  2. Get Tailnet IP for mobile config:"
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
