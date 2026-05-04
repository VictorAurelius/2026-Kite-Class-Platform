# SSH Mobile Migration — Phase 1: Dọn setup Windows-side cũ
#
# Xóa (idempotent):
#   - netsh portproxy rule listenport=2222 -> WSL2
#   - Windows Firewall rule "WSL2 SSH 2222"
#   - Task Scheduler job "WSL2-SSH-Portproxy"
#
# KHÔNG đụng:
#   - Tailscale Windows app (có thể bạn dùng cho việc khác; gỡ tay nếu muốn)
#   - WSL2 sshd config (xử lý ở 02-setup-wsl2.sh)
#   - SSH keys cả 2 bên
#
# Tại sao xóa được: kiến trúc mới chạy Tailscale TRỰC TIẾP trong WSL2,
# WSL2 nhận IP riêng 100.x.x.x. Mobile connect thẳng IP đó, không qua
# Windows network. Không cần port-forward.
#
# Cách chạy: PowerShell As Administrator (UAC prompt).
#
# Idempotent: chạy lại an toàn (mỗi bước xóa wrap try/catch).

$ErrorActionPreference = 'Continue'

Write-Host "=== SSH Mobile Migration — Dọn Windows ===" -ForegroundColor Cyan
Write-Host ""

# 1. Xóa netsh portproxy rule
Write-Host "[1/3] Xóa netsh portproxy rule (port 2222)..." -ForegroundColor Yellow
try {
    $existing = (netsh interface portproxy show all 2>&1) -join "`n"
    if ($existing -match '0\.0\.0\.0\s+2222') {
        netsh interface portproxy delete v4tov4 listenport=2222 listenaddress=0.0.0.0 | Out-Null
        Write-Host "    -> da xoa" -ForegroundColor Green
    } else {
        Write-Host "    -> khong co, bo qua" -ForegroundColor Gray
    }
} catch {
    Write-Host "    -> loi: $($_.Exception.Message)" -ForegroundColor Red
}

# 2. Xóa Windows Firewall rule
Write-Host "[2/3] Xóa Windows Firewall rule 'WSL2 SSH 2222'..." -ForegroundColor Yellow
$rule = Get-NetFirewallRule -DisplayName 'WSL2 SSH 2222' -ErrorAction SilentlyContinue
if ($rule) {
    Remove-NetFirewallRule -DisplayName 'WSL2 SSH 2222'
    Write-Host "    -> da xoa" -ForegroundColor Green
} else {
    Write-Host "    -> khong co, bo qua" -ForegroundColor Gray
}

# 3. Xóa Task Scheduler job
Write-Host "[3/3] Xóa scheduled task 'WSL2-SSH-Portproxy'..." -ForegroundColor Yellow
$task = Get-ScheduledTask -TaskName 'WSL2-SSH-Portproxy' -ErrorAction SilentlyContinue
if ($task) {
    Unregister-ScheduledTask -TaskName 'WSL2-SSH-Portproxy' -Confirm:$false
    Write-Host "    -> da xoa" -ForegroundColor Green
} else {
    Write-Host "    -> khong co, bo qua" -ForegroundColor Gray
}

Write-Host ""
Write-Host "=== Kiem tra ===" -ForegroundColor Cyan
Write-Host "Port forward con lai:"
netsh interface portproxy show all
Write-Host ""
Write-Host "Firewall rule co 'WSL' trong ten:"
Get-NetFirewallRule -DisplayName '*WSL*' -ErrorAction SilentlyContinue | Format-Table DisplayName, Enabled, Direction, Action -AutoSize
Write-Host ""
Write-Host "Scheduled task WSL con lai:"
Get-ScheduledTask -TaskName 'WSL*' -ErrorAction SilentlyContinue | Format-Table TaskName, State -AutoSize

Write-Host ""
Write-Host "=== Xong. Tiep theo: chay 02-setup-wsl2.sh trong WSL2 ===" -ForegroundColor Green
Write-Host "Bam phim bat ky de dong..."
$null = $Host.UI.RawUI.ReadKey('NoEcho,IncludeKeyDown')
