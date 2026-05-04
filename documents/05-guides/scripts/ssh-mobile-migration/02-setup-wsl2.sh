#!/usr/bin/env bash
# SSH Mobile Migration — Phase 2: Setup stack mới trên WSL2
#
# Cài + cấu hình:
#   - openssh-server (sshd port 2222 theo /etc/ssh/sshd_config.d/99-kite-local.conf)
#   - mosh (UDP server cho mobile-resilient connection)
#   - tailscale (peer-to-peer VPN, cấp WSL2 IP riêng 100.x.x.x)
#   - tmux auto-attach trong ~/.bashrc (idempotent)
#
# Pre-req: /etc/wsl.conf có systemd=true.
#
# Cách chạy: bash documents/05-guides/scripts/ssh-mobile-migration/02-setup-wsl2.sh
#
# Idempotent: chạy lại an toàn (package đã có sẽ skip, service đã enable giữ nguyên).
#
# Tương tác: 1 prompt sudo password + 1 click browser link để Tailscale auth.

set -e

C_CYAN='\033[0;36m'
C_GREEN='\033[0;32m'
C_YELLOW='\033[1;33m'
C_RED='\033[0;31m'
C_GRAY='\033[0;90m'
C_RESET='\033[0m'

step() { echo -e "${C_CYAN}[$1]${C_RESET} $2"; }
ok() { echo -e "    ${C_GREEN}✓${C_RESET} $1"; }
skip() { echo -e "    ${C_GRAY}- $1 (bỏ qua — đã có sẵn)${C_RESET}"; }
warn() { echo -e "    ${C_YELLOW}!${C_RESET} $1"; }

echo -e "${C_CYAN}=== SSH Mobile Migration — Cài đặt WSL2 ===${C_RESET}"
echo ""

# Pre-flight
if ! grep -q '^systemd=true' /etc/wsl.conf 2>/dev/null; then
    echo -e "${C_RED}LỖI: systemd chưa enable trong /etc/wsl.conf${C_RESET}"
    echo "Thêm [boot] systemd=true rồi chạy 'wsl --shutdown' từ PowerShell."
    exit 1
fi

# Cache sudo sớm để hỏi password 1 lần duy nhất
sudo -v

# ----------------------------------------------------------------------
step "1/6" "Cài openssh-server, mosh, tmux..."
PKGS_TO_INSTALL=()
for pkg in openssh-server mosh tmux; do
    if dpkg -s "$pkg" &>/dev/null; then
        skip "$pkg đã cài"
    else
        PKGS_TO_INSTALL+=("$pkg")
    fi
done
if [ ${#PKGS_TO_INSTALL[@]} -gt 0 ]; then
    sudo apt-get update -qq
    sudo apt-get install -y "${PKGS_TO_INSTALL[@]}"
    ok "đã cài: ${PKGS_TO_INSTALL[*]}"
fi

# ----------------------------------------------------------------------
step "2/6" "Enable + start sshd (dùng /etc/ssh/sshd_config.d/99-kite-local.conf có sẵn)..."
sudo systemctl enable --now ssh.service >/dev/null 2>&1
ok "ssh.service đã enable + chạy"

# Áp ssh.socket port override theo footgun socket-activation Ubuntu 24.04+ (xem guide §2.1)
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
    ok "đã tạo ssh.socket drop-in — sshd lắng nghe trên :2222"
else
    skip "ssh.socket drop-in đã có"
fi

# ----------------------------------------------------------------------
step "3/6" "Cài Tailscale trên WSL2..."
if command -v tailscale &>/dev/null; then
    skip "tailscale đã cài ($(tailscale version | head -1))"
else
    curl -fsSL https://tailscale.com/install.sh | sh
    ok "tailscale cài xong"
fi

# ----------------------------------------------------------------------
step "4/6" "Enable + start tailscaled..."
sudo systemctl enable --now tailscaled >/dev/null 2>&1
ok "tailscaled đã enable + chạy"

# ----------------------------------------------------------------------
step "5/6" "Tailscale auth (click URL trong browser từ thiết bị bất kỳ)..."
TS_STATUS=$(sudo tailscale status 2>&1 || true)
if echo "$TS_STATUS" | grep -q "Logged out"; then
    echo ""
    echo -e "${C_YELLOW}>>> Cần auth qua browser. Click URL bên dưới từ thiết bị bất kỳ${C_RESET}"
    echo -e "${C_YELLOW}    (phone OK), login với cùng identity Google/Microsoft/GitHub${C_RESET}"
    echo -e "${C_YELLOW}    đã dùng cho Tailscale trên Windows (hoặc đăng ký mới):${C_RESET}"
    echo ""
    sudo tailscale up --hostname=kite-wsl2 --operator="$USER"
elif echo "$TS_STATUS" | grep -q "BackendState=Running"; then
    skip "tailscale đã auth"
else
    # Trường hợp đã login
    if tailscale status 2>&1 | head -1 | grep -qE '^[0-9]+\.[0-9]+'; then
        skip "tailscale đã auth"
    else
        sudo tailscale up --hostname=kite-wsl2 --operator="$USER"
    fi
fi
ok "tailscale up"

TAILSCALE_IP=$(tailscale ip -4 2>/dev/null | head -1)
if [ -z "$TAILSCALE_IP" ]; then
    warn "Chưa lấy được Tailscale IP — chạy 'tailscale ip -4' lát sau"
else
    ok "Tailscale IP của WSL2: $TAILSCALE_IP"
fi

# ----------------------------------------------------------------------
step "6/6" "Thêm tmux auto-attach vào ~/.bashrc..."
SNIPPET_MARKER="# >>> ssh-mobile-migration tmux auto-attach >>>"
if grep -qF "$SNIPPET_MARKER" ~/.bashrc 2>/dev/null; then
    skip "snippet tmux auto-attach đã có trong ~/.bashrc"
else
    cat >> ~/.bashrc <<'BASHRC_SNIPPET'

# >>> ssh-mobile-migration tmux auto-attach >>>
# Tự attach tmux session "claude" khi SSH/mosh login.
# Detach: Ctrl+B rồi D. Reconnect từ bất kỳ đâu: cùng lệnh SSH/mosh.
if [ -n "$SSH_CONNECTION" ] && [ -z "$TMUX" ] && command -v tmux >/dev/null 2>&1; then
    tmux attach -t claude 2>/dev/null || tmux new -s claude
fi
# <<< ssh-mobile-migration tmux auto-attach <<<
BASHRC_SNIPPET
    ok "đã thêm tmux auto-attach vào ~/.bashrc"
fi

# ----------------------------------------------------------------------
echo ""
echo -e "${C_CYAN}=== Kiểm tra ===${C_RESET}"
echo ""
echo "sshd đang lắng nghe:"
sudo ss -tlnp 2>/dev/null | grep -E ':2222\s' || echo "  ⚠ sshd không trên :2222 — check 'sudo systemctl status ssh'"
echo ""
echo "mosh-server đã cài:"
which mosh-server && echo "  ✓ sẵn sàng (UDP 60000-61000)" || echo "  ⚠ thiếu mosh-server"
echo ""
echo "Tailscale status (3 peers đầu):"
tailscale status 2>/dev/null | head -3 || echo "  ⚠ tailscale không reach được"
echo ""
echo "tmux auto-attach trong ~/.bashrc:"
grep -q "$SNIPPET_MARKER" ~/.bashrc && echo "  ✓ có" || echo "  ⚠ không thấy"

echo ""
echo -e "${C_GREEN}=== Xong. Migration sang stack mobile-resilient hoàn tất trên WSL2. ===${C_RESET}"
echo ""
if [ -n "$TAILSCALE_IP" ]; then
    echo -e "${C_CYAN}Tailscale IP của WSL2 (dùng cho mobile):${C_RESET}"
    echo -e "    ${C_GREEN}$TAILSCALE_IP${C_RESET}"
    echo ""
    echo "Lưu lại. Dùng trong Termux/Termius:"
    echo "    mosh $USER@$TAILSCALE_IP"
fi
echo ""
echo "Tiếp theo: làm theo 03-android-setup-checklist.md trên phone."
