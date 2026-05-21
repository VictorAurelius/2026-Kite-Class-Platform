#!/bin/bash
#
# Docker daemon preflight check.
# Per GAP-694 Phase 0B + Phase 0C: verify Docker reachable before up.sh/setup.sh.
#
# Behavior:
#   - Docker reachable             → exit 0 silently
#   - Docker unreachable, auto     → launch Docker Desktop via powershell.exe + exit 1
#                                    with "wait 30-60s then re-run" hint
#   - WSL2 + Windows host          → use powershell.exe Start-Process
#   - Non-WSL host                 → print diagnostic + exit 1
#
# Usage: bash kitehub/scripts/check-docker.sh
#
# References:
#   - .claude/rules/agent-action-bias.md §1 Part B (command-over-UI)
#   - documents/04-quality/audits/local-stack/2026-05-21-local-self-test-investigation.md #1

set -u

# Quick reachable check — exit 0 silently nếu daemon OK.
if docker version >/dev/null 2>&1; then
    exit 0
fi

# Daemon unreachable — collect diagnostics.
echo "[check-docker] ❌ Docker daemon UNREACHABLE." >&2
echo "" >&2

# Phát hiện WSL2.
IS_WSL=0
if grep -qi microsoft /proc/version 2>/dev/null; then
    IS_WSL=1
fi

# Diagnostic: WSL distros state.
if [ "$IS_WSL" = "1" ]; then
    WSL_EXE="/mnt/c/Windows/System32/wsl.exe"
    if [ -x "$WSL_EXE" ]; then
        echo "[check-docker] WSL distro state:" >&2
        "$WSL_EXE" -l -v 2>&1 | tr -d '\0' | grep -iE 'NAME|docker-desktop|kite-dev' >&2 || true
        echo "" >&2
    fi
fi

# Diagnostic: docker CLI binary.
if command -v docker >/dev/null 2>&1; then
    echo "[check-docker] docker CLI: $(command -v docker)" >&2
else
    echo "[check-docker] docker CLI: NOT FOUND on PATH" >&2
fi

# Auto-launch attempt (WSL2 + Windows host).
if [ "$IS_WSL" = "1" ]; then
    POWERSHELL="/mnt/c/Windows/System32/WindowsPowerShell/v1.0/powershell.exe"
    DOCKER_EXE='C:\Program Files\Docker\Docker\Docker Desktop.exe'
    if [ -x "$POWERSHELL" ]; then
        echo "" >&2
        echo "[check-docker] 🚀 Auto-launching Docker Desktop via powershell.exe..." >&2
        "$POWERSHELL" -NoProfile -Command "Start-Process '$DOCKER_EXE'" >/dev/null 2>&1 || true
        echo "[check-docker] Wait 30-60s for Docker Desktop to boot, then re-run this command." >&2
        echo "[check-docker] Verify boot: docker version" >&2
        exit 1
    fi
fi

# Non-WSL fallback — manual instruction.
echo "" >&2
echo "[check-docker] Manual action required:" >&2
echo "[check-docker]   - macOS:   open -a Docker" >&2
echo "[check-docker]   - Linux:   sudo systemctl start docker" >&2
echo "[check-docker]   - Windows: Start 'Docker Desktop' từ Start Menu" >&2
exit 1
