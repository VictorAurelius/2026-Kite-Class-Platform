#!/usr/bin/env bash
# RTK pilot status check — verify install + telemetry posture.
# See: documents/05-guides/rtk-pilot/README.md
set -euo pipefail

ok=1
fail() { ok=0; echo "  ✗ $1"; }
pass() { echo "  ✓ $1"; }

echo "▶ RTK pilot status check"
echo

# 1. Binary present
if command -v rtk >/dev/null 2>&1; then
  pass "rtk binary present: $(command -v rtk)"
  pass "version: $(rtk --version 2>/dev/null || echo unknown)"
else
  fail "rtk binary not in PATH — run ./scripts/rtk-pilot/install.sh first"
fi

# 2. Telemetry env var set
if [ "${RTK_TELEMETRY_DISABLED:-}" = "1" ]; then
  pass "RTK_TELEMETRY_DISABLED=1 in current shell"
else
  fail "RTK_TELEMETRY_DISABLED not set — add to ~/.bashrc or ~/.zshrc"
fi

# 3. Telemetry config state
if command -v rtk >/dev/null 2>&1; then
  status=$(rtk telemetry status 2>/dev/null || echo "unknown")
  if echo "$status" | grep -qiE 'disabled|opt[- ]?in.*false|no.*consent'; then
    pass "rtk telemetry status: disabled"
  else
    fail "rtk telemetry status unclear or enabled — run: rtk telemetry disable"
    echo "    raw: $status"
  fi
fi

# 4. Hook present in Claude Code settings
settings_file="$HOME/.claude/settings.json"
if [ -f "$settings_file" ]; then
  if grep -q '"rtk"' "$settings_file" 2>/dev/null; then
    pass "RTK hook found in $settings_file"
  else
    fail "RTK hook NOT in $settings_file — install incomplete"
  fi
else
  fail "$settings_file does not exist"
fi

# 5. Tee mode default
config_file="$HOME/.config/rtk/config.toml"
if [ -f "$config_file" ]; then
  if grep -qE '^enabled\s*=\s*true|^mode\s*=\s*"failures"|^mode\s*=\s*"always"' "$config_file"; then
    pass "tee mode appears active in $config_file"
  else
    echo "  ⚠ tee mode config not found in $config_file (may use defaults)"
  fi
else
  echo "  ⚠ $config_file does not exist (RTK using defaults)"
fi

echo
if [ "$ok" = "1" ]; then
  echo "✓ RTK pilot ready."
  exit 0
else
  echo "✗ RTK pilot has issues — see above."
  exit 1
fi
