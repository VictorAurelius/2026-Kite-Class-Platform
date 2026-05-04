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
#   5. Optional: NTFY_PRIORITY (1=min, 3=default, 5=urgent), NTFY_SERVER (default ntfy.sh),
#      NTFY_CLICK (URL on tap — see note below), NTFY_ACTIONS (broadcast actions)
#
# NOTE on NTFY_CLICK: ntfy Android app uses ACTION_VIEW + Uri.parse, NOT
# URI_INTENT_SCHEME. So `intent:#Intent;...;end` URLs do NOT work for launching
# apps that don't register a URL scheme (Termux registers none). To actually
# open Termux on tap, use NTFY_ACTIONS with broadcast type — requires Termux:API
# add-on installed and `allow-external-apps = true` in ~/.termux/termux.properties.
# See: documents/05-guides/scripts/ssh-mobile-migration/03-android-setup-checklist.md
#
# Reference: documents/05-guides/remote-access/remote-control-setup.md §Stop Notification

set -u

PROJECT="$(basename "${CLAUDE_PROJECT_DIR:-$PWD}")"
TITLE="Claude · ${PROJECT}"
BODY=""

# Parse hook stdin (Claude Code sends JSON with session_id + transcript_path + cwd).
# Extract last assistant text block from transcript JSONL → richer notification body.
if command -v jq >/dev/null 2>&1; then
  HOOK_INPUT="$(cat 2>/dev/null || true)"
  TRANSCRIPT="$(echo "$HOOK_INPUT" | jq -r '.transcript_path // empty' 2>/dev/null)"

  if [ -n "$TRANSCRIPT" ] && [ -f "$TRANSCRIPT" ] && command -v tac >/dev/null 2>&1; then
    # Find last assistant message with text content (skip pure tool_use turns).
    # message.content can be string OR array of {type: text|tool_use, ...}.
    LAST_TEXT="$(tac "$TRANSCRIPT" 2>/dev/null | head -200 | jq -r '
      select(.type == "assistant") |
      .message.content |
      if type == "string" then .
      elif type == "array" then [.[] | select(.type == "text") | .text] | join(" ")
      else empty end
    ' 2>/dev/null | grep -v '^$' | head -1)"

    if [ -n "$LAST_TEXT" ]; then
      # Collapse newlines + multi-spaces → single line; strip lead whitespace
      CLEAN="$(echo "$LAST_TEXT" | tr '\n\t' '  ' | sed 's/  */ /g' | sed 's/^[[:space:]]*//')"
      # Truncate to 220 chars (ntfy mobile readable + leaves space for ellipsis)
      if [ ${#CLEAN} -gt 220 ]; then
        BODY="${CLEAN:0:220}…"
      else
        BODY="$CLEAN"
      fi
    fi
  fi
fi

# Fallback when transcript parse fails or jq unavailable
[ -z "$BODY" ] && BODY="(chờ input — không parse được transcript)"

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
  # NTFY_CLICK: optional URL fired on tap. Default empty — Click does NOT support
  # Android `intent:#Intent;...;end` URIs (ntfy app uses ACTION_VIEW + Uri.parse,
  # not URI_INTENT_SCHEME). Termux registers no URL scheme so it can't be opened
  # via Click. Use NTFY_ACTIONS broadcast (Termux:API + allow-external-apps) to
  # actually launch Termux on tap. See: ssh-mobile-migration/03-android-setup §G.
  CURL_ARGS=(-fsS -X POST
    -H "Title: ${TITLE}"
    -H "Priority: ${NTFY_PRIORITY}"
    -H "Tags: robot")
  [ -n "${NTFY_CLICK:-}" ] && CURL_ARGS+=(-H "Click: ${NTFY_CLICK}")
  [ -n "${NTFY_ACTIONS:-}" ] && CURL_ARGS+=(-H "Actions: ${NTFY_ACTIONS}")
  curl "${CURL_ARGS[@]}" \
    -d "${BODY}" \
    "${NTFY_SERVER}/${NTFY_TOPIC}" \
    --max-time 5 \
    >/dev/null 2>&1 || true
fi

exit 0
