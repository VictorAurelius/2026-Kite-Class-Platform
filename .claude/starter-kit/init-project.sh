#!/bin/bash
# init-project.sh — Setup Claude Code starter kit for a new project
#
# Usage:
#   ./init-project.sh /path/to/new-project
#   ./init-project.sh .   # current directory
#
# What it does:
#   1. Copies generic skills to {project}/.claude/skills/
#   2. Copies generic scripts to {project}/scripts/
#   3. Copies templates to {project}/
#   4. Copies seed memories to Claude project memory
#   5. Creates directory structure

set -euo pipefail

TARGET="${1:-.}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "═══════════════════════════════════════════════"
echo "  Claude Code Starter Kit — Project Init"
echo "═══════════════════════════════════════════════"
echo ""
echo "Target: $TARGET"
echo "Source: $SCRIPT_DIR"
echo ""

# Create directories
echo "📁 Creating directory structure..."
mkdir -p "$TARGET/.claude/skills/core"
mkdir -p "$TARGET/.claude/skills/workflow"
mkdir -p "$TARGET/.claude/skills/quality"
mkdir -p "$TARGET/.claude/skills/reference"
mkdir -p "$TARGET/.claude/scripts"
mkdir -p "$TARGET/scripts"
mkdir -p "$TARGET/documents/01-business"
mkdir -p "$TARGET/documents/02-architecture"
mkdir -p "$TARGET/documents/03-planning"
mkdir -p "$TARGET/documents/04-quality"

# Copy skills
echo "📋 Copying skills..."
cp -r "$SCRIPT_DIR/skills/"* "$TARGET/.claude/skills/"
echo "   Copied: core (5), workflow (1), quality (1), reference (2)"

# Copy scripts
echo "📜 Copying scripts..."
cp "$SCRIPT_DIR/scripts/"*.sh "$TARGET/scripts/"
cp "$SCRIPT_DIR/scripts/pre-commit-check.sh" "$TARGET/.claude/scripts/"
chmod +x "$TARGET/scripts/"*.sh
chmod +x "$TARGET/.claude/scripts/"*.sh
echo "   Copied: check-ci.sh, test-local.sh, pre-commit-check.sh"

# Copy templates (don't overwrite existing)
echo "📝 Copying templates..."
if [ ! -f "$TARGET/CLAUDE.md" ]; then
    cp "$SCRIPT_DIR/templates/CLAUDE.md.template" "$TARGET/CLAUDE.md"
    echo "   Created: CLAUDE.md (from template)"
else
    echo "   Skipped: CLAUDE.md (already exists)"
fi

if [ ! -f "$TARGET/README.md" ]; then
    cp "$SCRIPT_DIR/templates/README.md.template" "$TARGET/README.md"
    echo "   Created: README.md (from template)"
else
    echo "   Skipped: README.md (already exists)"
fi

# Claude Code permissions (local only, not committed)
if [ ! -f "$TARGET/.claude/settings.local.json" ]; then
    cp "$SCRIPT_DIR/templates/settings.local.json.template" "$TARGET/.claude/settings.local.json"
    echo "   Created: .claude/settings.local.json (bypass permissions)"
else
    echo "   Skipped: .claude/settings.local.json (already exists)"
fi

# VS Code settings
mkdir -p "$TARGET/.vscode"
if [ ! -f "$TARGET/.vscode/settings.json" ]; then
    cp "$SCRIPT_DIR/templates/vscode-settings.json.template" "$TARGET/.vscode/settings.json"
    echo "   Created: .vscode/settings.json (uncomment your stack)"
else
    echo "   Skipped: .vscode/settings.json (already exists)"
fi

# Copy seed memories
echo "🧠 Copying seed memories..."
MEMORY_DIR="$HOME/.claude/projects/$(echo "$TARGET" | tr '/' '-')/memory"
mkdir -p "$MEMORY_DIR"
cp "$SCRIPT_DIR/memory/"*.md "$MEMORY_DIR/" 2>/dev/null || true

if [ ! -f "$MEMORY_DIR/MEMORY.md" ]; then
    cat > "$MEMORY_DIR/MEMORY.md" << 'MEMEOF'
# Project Memory Index

## Feedback (lessons learned)
- [feedback_scripts_not_adhoc.md](feedback_scripts_not_adhoc.md) — PHẢI dùng scripts, KHÔNG lệnh ad-hoc
- [feedback_ci_before_scoring.md](feedback_ci_before_scoring.md) — CI phải complete trước khi scoring
- [feedback_self_test_before_push.md](feedback_self_test_before_push.md) — Test local trước push
- [feedback_business_design_first.md](feedback_business_design_first.md) — Business docs trước code
MEMEOF
    echo "   Created: MEMORY.md + 4 seed memories"
else
    echo "   Skipped: MEMORY.md (already exists)"
    echo "   Seed memories copied to: $MEMORY_DIR"
fi

# Setup git hooks
echo "🔗 Setting up git hooks..."
if [ -d "$TARGET/.git" ]; then
    HOOK_DIR="$TARGET/.git/hooks"
    if [ ! -f "$HOOK_DIR/pre-commit" ]; then
        ln -sf "../../.claude/scripts/pre-commit-check.sh" "$HOOK_DIR/pre-commit"
        echo "   Linked: pre-commit hook"
    else
        echo "   Skipped: pre-commit hook (already exists)"
    fi
fi

# Track installed version
KIT_VERSION=$(cat "$SCRIPT_DIR/VERSION" 2>/dev/null | tr -d '[:space:]' || echo "1.0.0")
echo "$KIT_VERSION" > "$TARGET/.claude/.starter-kit-version"
echo "📦 Installed starter-kit v$KIT_VERSION"

echo ""
echo "═══════════════════════════════════════════════"
echo "  ✅ Setup complete! (starter-kit v$KIT_VERSION)"
echo "═══════════════════════════════════════════════"
echo ""
echo "Next steps:"
echo "  1. Edit CLAUDE.md — replace {placeholders} with project info"
echo "  2. Edit scripts/test-local.sh — configure PROJECT_DIRS"
echo "  3. Run: scripts/test-local.sh --quick  (verify setup)"
echo "  4. Start coding with Superpowers methodology!"
echo ""
echo "Available skills:"
echo "  .claude/skills/core/          — Brainstorm, TDD, Review, Debug"
echo "  .claude/skills/workflow/      — Git, PR, CI workflow"
echo "  .claude/skills/quality/       — Quality audit framework"
echo "  .claude/skills/reference/     — Business docs, service docs"
echo ""
echo "Available scripts:"
echo "  scripts/test-local.sh         — Test before push"
echo "  scripts/check-ci.sh           — Monitor CI after push"
echo "  scripts/check-ci.sh --status  — Quick CI status"
