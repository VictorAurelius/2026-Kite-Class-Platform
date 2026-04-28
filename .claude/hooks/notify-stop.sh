#!/usr/bin/env bash
# Stop hook — fires when Claude Code stops to wait for user input.
#
# Wired via ~/.claude/settings.json:
#   "hooks": {
#     "Stop": [
#       { "matcher": "*", "hooks": [
#         { "type": "command",
#           "command": "${CLAUDE_PROJECT_DIR}/.claude/hooks/notify-stop.sh" }
#       ]}
#     ]
#   }
#
# Output channels (best-effort, all non-fatal):
#   1. WSLg desktop notification via notify-send (libnotify)
#   2. Windows toast (when running under WSL2 with powershell.exe accessible)
#   3. Terminal bell (\a) — always tries
#
# Mobile push (when remote-control session active) is handled by Claude Code
# automatically, independent of this hook.
#
# Reference: documents/05-guides/remote-control-setup.md §Stop Notification

set -u

PROJECT="$(basename "${CLAUDE_PROJECT_DIR:-$PWD}")"
TITLE="Claude Code — chờ input"
BODY="Project: ${PROJECT}"

# 1. Desktop notification (WSLg + libnotify-bin)
if command -v notify-send >/dev/null 2>&1; then
  notify-send -u normal -t 8000 -i terminal "$TITLE" "$BODY" 2>/dev/null || true
fi

# 2. Terminal bell — visible in foreground terminal
printf '\a' >&2

# 3. Windows toast via WSL2 interop
if command -v powershell.exe >/dev/null 2>&1; then
  # Toast template 02 = title + body. AppendChild handles XML escaping at the
  # PowerShell side; bash variables are quoted into the script body.
  powershell.exe -NoProfile -Command "
    [Windows.UI.Notifications.ToastNotificationManager, Windows.UI.Notifications, ContentType=WindowsRuntime] | Out-Null
    \$xml = [Windows.UI.Notifications.ToastNotificationManager]::GetTemplateContent(2)
    \$xml.GetElementsByTagName('text')[0].AppendChild(\$xml.CreateTextNode('${TITLE//\'/\'\'}')) | Out-Null
    \$xml.GetElementsByTagName('text')[1].AppendChild(\$xml.CreateTextNode('${BODY//\'/\'\'}')) | Out-Null
    \$toast = [Windows.UI.Notifications.ToastNotification]::new(\$xml)
    [Windows.UI.Notifications.ToastNotificationManager]::CreateToastNotifier('Claude Code').Show(\$toast)
  " 2>/dev/null || true
fi

exit 0
