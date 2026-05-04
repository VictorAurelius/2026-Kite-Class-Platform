# SSH Mobile Migration — Phase 1: Cleanup redundant Windows-side setup
#
# What this removes (from old SSH-via-Windows-host setup):
#   - netsh portproxy rule listenport=2222 -> WSL2
#   - Windows Firewall rule "WSL2 SSH 2222"
#   - Task Scheduler job "WSL2-SSH-Portproxy"
#
# What this DOES NOT touch:
#   - Tailscale Windows app (you may use it for other things; remove manually if not)
#   - WSL2 sshd config (handled in 02-setup-wsl2.sh)
#   - SSH keys on either side
#
# Why removable: the new architecture has Tailscale running INSIDE WSL2,
# giving WSL2 its own 100.x.x.x IP. Mobile connects directly to that IP,
# bypassing Windows network entirely. No port-forward needed.
#
# RUN: from PowerShell as Administrator (UAC prompt).
#
# Idempotent: safe to re-run (each removal is wrapped in try/catch).

$ErrorActionPreference = 'Continue'

Write-Host "=== SSH Mobile Migration — Windows cleanup ===" -ForegroundColor Cyan
Write-Host ""

# 1. Remove netsh portproxy rule
Write-Host "[1/3] Removing netsh portproxy rule (port 2222)..." -ForegroundColor Yellow
try {
    $existing = (netsh interface portproxy show all 2>&1) -join "`n"
    if ($existing -match '0\.0\.0\.0\s+2222') {
        netsh interface portproxy delete v4tov4 listenport=2222 listenaddress=0.0.0.0 | Out-Null
        Write-Host "    -> removed" -ForegroundColor Green
    } else {
        Write-Host "    -> not present, skipping" -ForegroundColor Gray
    }
} catch {
    Write-Host "    -> error: $($_.Exception.Message)" -ForegroundColor Red
}

# 2. Remove Windows Firewall rule
Write-Host "[2/3] Removing Windows Firewall rule 'WSL2 SSH 2222'..." -ForegroundColor Yellow
$rule = Get-NetFirewallRule -DisplayName 'WSL2 SSH 2222' -ErrorAction SilentlyContinue
if ($rule) {
    Remove-NetFirewallRule -DisplayName 'WSL2 SSH 2222'
    Write-Host "    -> removed" -ForegroundColor Green
} else {
    Write-Host "    -> not present, skipping" -ForegroundColor Gray
}

# 3. Remove Task Scheduler job
Write-Host "[3/3] Removing scheduled task 'WSL2-SSH-Portproxy'..." -ForegroundColor Yellow
$task = Get-ScheduledTask -TaskName 'WSL2-SSH-Portproxy' -ErrorAction SilentlyContinue
if ($task) {
    Unregister-ScheduledTask -TaskName 'WSL2-SSH-Portproxy' -Confirm:$false
    Write-Host "    -> removed" -ForegroundColor Green
} else {
    Write-Host "    -> not present, skipping" -ForegroundColor Gray
}

Write-Host ""
Write-Host "=== Verification ===" -ForegroundColor Cyan
Write-Host "Port forwards remaining:"
netsh interface portproxy show all
Write-Host ""
Write-Host "Firewall rules with 'WSL' in name:"
Get-NetFirewallRule -DisplayName '*WSL*' -ErrorAction SilentlyContinue | Format-Table DisplayName, Enabled, Direction, Action -AutoSize
Write-Host ""
Write-Host "Scheduled WSL tasks remaining:"
Get-ScheduledTask -TaskName 'WSL*' -ErrorAction SilentlyContinue | Format-Table TaskName, State -AutoSize

Write-Host ""
Write-Host "=== Done. Next: run 02-setup-wsl2.sh inside WSL2 ===" -ForegroundColor Green
Write-Host "Press any key to close..."
$null = $Host.UI.RawUI.ReadKey('NoEcho,IncludeKeyDown')
