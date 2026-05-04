#!/usr/bin/env bash
# SSH Mobile Migration — Phase 2: Setup new stack on WSL2
#
# What this installs/configures:
#   - openssh-server (start sshd on port 2222 per existing 99-kite-local.conf)
#   - mosh (UDP server for mobile-resilient connection)
#   - tailscale (peer-to-peer VPN, gives WSL2 its own 100.x.x.x IP)
#   - tmux auto-attach in ~/.bashrc (idempotent — checks duplicate first)
#
# Pre-req: /etc/wsl.conf has systemd=true (already verified).
#
# RUN: bash documents/05-guides/scripts/ssh-mobile-migration/02-setup-wsl2.sh
#
# Idempotent: re-run safe — installed packages skipped, existing services kept.
#
# Interactive: 1 sudo password prompt + 1 browser-link click for Tailscale auth.

set -e

C_CYAN='\033[0;36m'
C_GREEN='\033[0;32m'
C_YELLOW='\033[1;33m'
C_RED='\033[0;31m'
C_GRAY='\033[0;90m'
C_RESET='\033[0m'

step() { echo -e "${C_CYAN}[$1]${C_RESET} $2"; }
ok() { echo -e "    ${C_GREEN}✓${C_RESET} $1"; }
skip() { echo -e "    ${C_GRAY}- $1 (skipped — already done)${C_RESET}"; }
warn() { echo -e "    ${C_YELLOW}!${C_RESET} $1"; }

echo -e "${C_CYAN}=== SSH Mobile Migration — WSL2 setup ===${C_RESET}"
echo ""

# Pre-flight
if ! grep -q '^systemd=true' /etc/wsl.conf 2>/dev/null; then
    echo -e "${C_RED}FAIL: systemd not enabled in /etc/wsl.conf${C_RESET}"
    echo "Add [boot] systemd=true then run 'wsl --shutdown' from PowerShell."
    exit 1
fi

# Ensure sudo cached early (one prompt at start)
sudo -v

# ----------------------------------------------------------------------
step "1/6" "Install openssh-server, mosh, tmux..."
PKGS_TO_INSTALL=()
for pkg in openssh-server mosh tmux; do
    if dpkg -s "$pkg" &>/dev/null; then
        skip "$pkg already installed"
    else
        PKGS_TO_INSTALL+=("$pkg")
    fi
done
if [ ${#PKGS_TO_INSTALL[@]} -gt 0 ]; then
    sudo apt-get update -qq
    sudo apt-get install -y "${PKGS_TO_INSTALL[@]}"
    ok "installed: ${PKGS_TO_INSTALL[*]}"
fi

# ----------------------------------------------------------------------
step "2/6" "Enable + start sshd (using existing /etc/ssh/sshd_config.d/99-kite-local.conf)..."
sudo systemctl enable --now ssh.service >/dev/null 2>&1
ok "ssh.service enabled + started"

# Apply ssh.socket port override per Ubuntu 24.04+ socket-activation footgun (guide §2.1)
if ! [ -f /etc/systemd/system/ssh.socket.d/listen.conf ]; then
    sudo mkdir -p /etc/systemd/system/ssh.socket.d
    sudo tee /etc/systemd/system/ssh.socket.d/listen.conf >/dev/null <<'EOF'
[Socket]
ListenStream=
ListenStream=0.0.0.0:2222
ListenStream=[::]:2222
EOF
    sudo systemctl daemon-reload
    sudo systemctl restart ssh.socket
    sudo systemctl restart ssh.service
    ok "ssh.socket drop-in created — sshd now listens on :2222"
else
    skip "ssh.socket drop-in already exists"
fi

# ----------------------------------------------------------------------
step "3/6" "Install Tailscale on WSL2..."
if command -v tailscale &>/dev/null; then
    skip "tailscale already installed ($(tailscale version | head -1))"
else
    curl -fsSL https://tailscale.com/install.sh | sh
    ok "tailscale installed"
fi

# ----------------------------------------------------------------------
step "4/6" "Enable + start tailscaled..."
sudo systemctl enable --now tailscaled >/dev/null 2>&1
ok "tailscaled enabled + started"

# ----------------------------------------------------------------------
step "5/6" "Tailscale auth (interactive — click the browser URL on any device)..."
TS_STATUS=$(sudo tailscale status 2>&1 || true)
if echo "$TS_STATUS" | grep -q "Logged out"; then
    echo ""
    echo -e "${C_YELLOW}>>> Browser auth required. Click the URL below from any device${C_RESET}"
    echo -e "${C_YELLOW}    (phone OK), log in with same Google/Microsoft/GitHub identity${C_RESET}"
    echo -e "${C_YELLOW}    used for Windows Tailscale (or sign up fresh):${C_RESET}"
    echo ""
    sudo tailscale up --hostname=kite-wsl2 --operator="$USER"
elif echo "$TS_STATUS" | grep -q "BackendState=Running"; then
    skip "tailscale already authenticated"
else
    # already logged in case
    if tailscale status 2>&1 | head -1 | grep -qE '^[0-9]+\.[0-9]+'; then
        skip "tailscale already authenticated"
    else
        sudo tailscale up --hostname=kite-wsl2 --operator="$USER"
    fi
fi
ok "tailscale up"

TAILSCALE_IP=$(tailscale ip -4 2>/dev/null | head -1)
if [ -z "$TAILSCALE_IP" ]; then
    warn "Could not detect Tailscale IP yet — re-run 'tailscale ip -4' in a moment"
else
    ok "WSL2 Tailscale IP: $TAILSCALE_IP"
fi

# ----------------------------------------------------------------------
step "6/6" "Add tmux auto-attach to ~/.bashrc..."
SNIPPET_MARKER="# >>> ssh-mobile-migration tmux auto-attach >>>"
if grep -qF "$SNIPPET_MARKER" ~/.bashrc 2>/dev/null; then
    skip "tmux auto-attach snippet already in ~/.bashrc"
else
    cat >> ~/.bashrc <<'BASHRC_SNIPPET'

# >>> ssh-mobile-migration tmux auto-attach >>>
# Auto-attach tmux session "claude" on SSH/mosh login.
# Detach: Ctrl+B then D. Reconnect from anywhere: same SSH/mosh command.
if [ -n "$SSH_CONNECTION" ] && [ -z "$TMUX" ] && command -v tmux >/dev/null 2>&1; then
    tmux attach -t claude 2>/dev/null || tmux new -s claude
fi
# <<< ssh-mobile-migration tmux auto-attach <<<
BASHRC_SNIPPET
    ok "tmux auto-attach added to ~/.bashrc"
fi

# ----------------------------------------------------------------------
echo ""
echo -e "${C_CYAN}=== Verification ===${C_RESET}"
echo ""
echo "sshd listening:"
sudo ss -tlnp 2>/dev/null | grep -E ':2222\s' || echo "  ⚠ sshd not on :2222 — check 'sudo systemctl status ssh'"
echo ""
echo "mosh-server installed:"
which mosh-server && echo "  ✓ ready (UDP 60000-61000)" || echo "  ⚠ mosh-server missing"
echo ""
echo "Tailscale status (first 3 peers):"
tailscale status 2>/dev/null | head -3 || echo "  ⚠ tailscale not reachable"
echo ""
echo "tmux auto-attach in ~/.bashrc:"
grep -q "$SNIPPET_MARKER" ~/.bashrc && echo "  ✓ present" || echo "  ⚠ not found"

echo ""
echo -e "${C_GREEN}=== Done. Migration to mobile-resilient stack complete on WSL2. ===${C_RESET}"
echo ""
if [ -n "$TAILSCALE_IP" ]; then
    echo -e "${C_CYAN}Your WSL2 Tailscale IP for mobile use:${C_RESET}"
    echo -e "    ${C_GREEN}$TAILSCALE_IP${C_RESET}"
    echo ""
    echo "Save this. Use in Termux/Termius:"
    echo "    mosh $USER@$TAILSCALE_IP"
fi
echo ""
echo "Next: follow 03-android-setup-checklist.md on phone."
