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
#   4. ntfy.sh push to mobile (when NTFY_TOPIC env var set)
#
# Mobile setup for channel 4:
#   1. Install ntfy app on Android (F-Droid: https://f-droid.org/packages/io.heckel.ntfy/)
#      hoặc iOS (App Store: "ntfy")
#   2. Pick unique topic name (semi-secret), vd: "kite-claude-vkiet-x7k2p9"
#   3. Subscribe to topic trong app
#   4. Set env: thêm vào ~/.claude/settings.json:
#        "env": { "NTFY_TOPIC": "kite-claude-vkiet-x7k2p9" }
#      hoặc export trong ~/.bashrc (cần source bằng login shell)
#   5. Optional: NTFY_PRIORITY (1=min, 3=default, 5=urgent), NTFY_SERVER (default ntfy.sh)
#
# Reference: documents/05-guides/remote-access/remote-control-setup.md §Stop Notification

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

# 4. ntfy.sh push to mobile (Android/iOS) — opt-in via NTFY_TOPIC env var
if [ -n "${NTFY_TOPIC:-}" ] && command -v curl >/dev/null 2>&1; then
  NTFY_SERVER="${NTFY_SERVER:-https://ntfy.sh}"
  NTFY_PRIORITY="${NTFY_PRIORITY:-3}"
  curl -fsS -X POST \
    -H "Title: ${TITLE}" \
    -H "Priority: ${NTFY_PRIORITY}" \
    -H "Tags: robot" \
    -d "${BODY}" \
    "${NTFY_SERVER}/${NTFY_TOPIC}" \
    --max-time 5 \
    >/dev/null 2>&1 || true
fi

exit 0
