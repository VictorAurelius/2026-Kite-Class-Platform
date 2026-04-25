#!/usr/bin/env bash
# RTK pilot uninstall — restore unhooked Claude Code state.
# See: documents/05-guides/rtk-pilot/README.md
set -euo pipefail

echo "▶ RTK pilot uninstall"
echo

if ! command -v rtk >/dev/null 2>&1; then
  echo "ℹ rtk binary not in PATH — assuming already uninstalled. Nothing to do."
  exit 0
fi

# 1. Remove the PreToolUse hook
echo "▶ Removing global RTK hook…"
rtk uninit -g || rtk init -g --uninstall || {
  echo "✗ Could not auto-remove hook. Manually edit ~/.claude/settings.json"
  echo "  and delete any 'rtk'-related PreToolUse entry."
}

# 2. Disable telemetry + delete local cache
echo "▶ Disabling telemetry + clearing local data…"
rtk telemetry disable 2>/dev/null || true
rtk telemetry forget 2>/dev/null || true

# 3. Optional: remove the binary
read -p "Remove the rtk binary from your system? (y/N) " -n 1 -r remove
echo
if [[ $remove =~ ^[Yy]$ ]]; then
  if command -v cargo >/dev/null 2>&1; then
    cargo uninstall rtk 2>/dev/null || true
  fi
  if command -v brew >/dev/null 2>&1; then
    brew uninstall rtk 2>/dev/null || true
  fi
  echo "ℹ If installed via curl script, manually delete the binary path."
fi

# 4. Cleanup tee logs
if [ -d "$HOME/.local/share/rtk/tee" ]; then
  read -p "Delete RTK tee logs at ~/.local/share/rtk/tee? (y/N) " -n 1 -r tee
  echo
  if [[ $tee =~ ^[Yy]$ ]]; then
    rm -rf "$HOME/.local/share/rtk/tee"
    echo "✓ Tee logs cleared."
  fi
fi

echo
echo "✓ RTK pilot uninstall complete."
echo "  RESTART Claude Code so the unhooked state takes effect."
echo
echo "  Don't forget: capture the pilot outcome in"
echo "    documents/05-guides/rtk-pilot/measurement-protocol.md"
echo "  and add a memory entry per the pilot doc."
