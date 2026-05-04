# SSH Mobile Migration — Bộ script chuyển đổi

Migration một-lần từ setup SSH cũ (qua Windows host) sang stack mobile-resilient Tailscale + mosh + tmux trên WSL2.

**Tại sao migrate:** setup cũ (Windows portproxy + LAN IP + plain SSH) bị rớt session khi mobile switch app / tắt màn hình / đổi mạng → kill long-running agent. Stack mới sống sót cả 3 trường hợp.

**Thời gian:** ~5 phút máy + ~10 phút phone.

---

## Thứ tự chạy

| Bước | Script | Chạy ở đâu | Tự động/Tay |
|------|--------|------------|:-----------:|
| 1 | `01-cleanup-windows.ps1` | PowerShell **as Administrator** | Tay (1 click "Yes" UAC) |
| 2 | `02-setup-wsl2.sh` | WSL2 bash | Auto (1 prompt sudo + 1 click browser auth) |
| 3 | `03-android-setup-checklist.md` | Android phone | Tay (theo checklist) |

## Quick run

```bash
# Phase 1 — Dọn Windows (từ WSL2, popup UAC trên Windows)
powershell.exe -Command "Start-Process powershell -Verb RunAs -ArgumentList '-ExecutionPolicy', 'Bypass', '-File', '\\\\wsl$\\Ubuntu\\home\\nguyenvankiet\\projects\\2026-Kite-Class-Platform\\documents\\05-guides\\scripts\\ssh-mobile-migration\\01-cleanup-windows.ps1'"

# Phase 2 — Cài đặt WSL2
bash documents/05-guides/scripts/ssh-mobile-migration/02-setup-wsl2.sh

# Phase 3 — Android (đọc checklist + làm trên phone)
cat documents/05-guides/scripts/ssh-mobile-migration/03-android-setup-checklist.md
```

---

## Mỗi script làm gì

### 01-cleanup-windows.ps1
**Xóa (idempotent):**
- `netsh portproxy` rule listenport=2222 → WSL2
- Windows Firewall rule "WSL2 SSH 2222"
- Task Scheduler job "WSL2-SSH-Portproxy"

**KHÔNG đụng:** Tailscale Windows app, SSH keys, sshd config.

### 02-setup-wsl2.sh
**Cài (idempotent):**
- openssh-server (sshd port 2222)
- mosh (UDP server)
- tailscale (peer-to-peer VPN, cấp WSL2 IP riêng 100.x.x.x)

**Cấu hình:**
- ssh.socket drop-in cho port 2222 (footgun socket-activation Ubuntu 24.04+)
- Enable tailscaled + ssh systemd services
- Thêm tmux auto-attach snippet vào ~/.bashrc

**Tương tác:** 1 sudo password + 1 click browser link Tailscale auth.

**Output:** in WSL2 Tailscale IP để dùng cho Android setup.

### 03-android-setup-checklist.md
Bước tay trên phone:
- Tailscale Android app (sign in same identity)
- Termux từ F-Droid + `pkg install mosh openssh tmux`
- Generate SSH key + install lên WSL2
- Termux SSH config (`~/.ssh/config`)
- Test matrix (3 tests: ssh, mosh, mosh+tắt-màn-hình+reconnect)
- Daily workflow + troubleshooting + rollback

---

## Sau khi migrate

`ssh kite` cũ vẫn chạy — cùng hostname alias, nhưng resolve sang Tailscale IP thay vì LAN IP. Lệnh mới `mosh kite` là daily-driver cho mobile.

Xem guide chính `../ssh-terminal-direct-access.md` §3.4 cho phần kiến trúc.

---

## Tạo

2026-05-04 — kèm với guide chính §3.4 (mosh layer). Trigger bởi sự cố Wave 17 — 3/4 background agent chết do SIGHUP cascade từ mobile disconnect. PR #746.
