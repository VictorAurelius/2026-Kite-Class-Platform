#!/usr/bin/env bash
# RTK pilot install — single-developer opt-in only.
# See: documents/05-guides/rtk-pilot/README.md
set -euo pipefail

cd "$(dirname "$0")/../.."

echo "▶ RTK pilot install — single-developer opt-in"
echo

# 0. Pre-flight: pilot doc must exist, user must have read it
if [ ! -f documents/05-guides/rtk-pilot/README.md ]; then
  echo "✗ Pilot doc missing — abort."
  exit 1
fi

read -p "Have you read documents/05-guides/rtk-pilot/README.md? (y/N) " -n 1 -r ack
echo
if [[ ! $ack =~ ^[Yy]$ ]]; then
  echo "✗ Read the pilot doc first. Abort."
  exit 1
fi

# 1. Install rtk binary if missing
if ! command -v rtk >/dev/null 2>&1; then
  echo "ℹ rtk binary not found in PATH."
  echo "  Install per upstream: https://github.com/rtk-ai/rtk"
  echo "  (cargo install rtk OR brew install rtk OR curl install script)"
  echo "  Re-run this script after install."
  exit 1
fi

echo "✓ rtk version: $(rtk --version 2>/dev/null || echo unknown)"

# 2. Force telemetry-disabled BEFORE init runs (defense in depth — env var wins)
export RTK_TELEMETRY_DISABLED=1
echo "✓ RTK_TELEMETRY_DISABLED=1 set for this shell"
echo
echo "ℹ Pilot rule: telemetry MUST stay disabled."
echo "  Add to your shell rc (~/.bashrc or ~/.zshrc):"
echo "    export RTK_TELEMETRY_DISABLED=1"
echo

# 3. Run rtk init -g (installs PreToolUse hook globally)
#    User must answer "no" to telemetry opt-in if prompted.
echo "▶ Running 'rtk init -g' — DECLINE telemetry when prompted (env var also blocks it)."
read -p "Continue? (y/N) " -n 1 -r go
echo
if [[ ! $go =~ ^[Yy]$ ]]; then
  echo "✗ Aborted by user."
  exit 1
fi

rtk init -g || {
  echo "✗ rtk init failed. Check upstream docs."
  exit 1
}

# 4. Verify telemetry is OFF
echo
echo "▶ Verifying telemetry state…"
rtk telemetry status 2>&1 | tee /tmp/rtk-pilot-telemetry-status
if grep -qiE 'enabled|opt[- ]?in.*true|consent.*granted' /tmp/rtk-pilot-telemetry-status; then
  echo "✗ Telemetry appears ENABLED. Disabling explicitly:"
  rtk telemetry disable
fi

# 5. Reminder for follow-up
echo
echo "✓ RTK pilot install complete."
echo
echo "Next steps:"
echo "  1. RESTART Claude Code so the new hook takes effect."
echo "  2. Run a normal session (wave / audit / debug)."
echo "  3. After session, fill in:"
echo "       documents/05-guides/rtk-pilot/measurement-protocol.md"
echo "  4. To roll back:"
echo "       ./scripts/rtk-pilot/uninstall.sh"
