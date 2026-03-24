#!/bin/bash
# upgrade-project.sh — Import starter-kit vào dự án đã có skills
#
# Khác với init-project.sh:
#   - KHÔNG overwrite files đã tồn tại
#   - So sánh diff và hỏi trước khi merge
#   - Hỗ trợ selective import (chỉ scripts, chỉ skills, etc.)
#
# Usage:
#   ./upgrade-project.sh /path/to/existing-project              # Interactive
#   ./upgrade-project.sh /path/to/existing-project --scripts    # Chỉ scripts
#   ./upgrade-project.sh /path/to/existing-project --skills     # Chỉ skills
#   ./upgrade-project.sh /path/to/existing-project --memory     # Chỉ memories
#   ./upgrade-project.sh /path/to/existing-project --dry-run    # Preview only
#   ./upgrade-project.sh /path/to/existing-project --force      # Overwrite all

set -euo pipefail

TARGET="${1:-.}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

ONLY_SCRIPTS=false
ONLY_SKILLS=false
ONLY_MEMORY=false
DRY_RUN=false
FORCE=false

for arg in "${@:2}"; do
    case "$arg" in
        --scripts) ONLY_SCRIPTS=true ;;
        --skills) ONLY_SKILLS=true ;;
        --memory) ONLY_MEMORY=true ;;
        --dry-run) DRY_RUN=true ;;
        --force) FORCE=true ;;
    esac
done

# If no specific flag → do all
ALL=true
$ONLY_SCRIPTS || $ONLY_SKILLS || $ONLY_MEMORY && ALL=false

KIT_VERSION=$(cat "$SCRIPT_DIR/VERSION" 2>/dev/null | tr -d '[:space:]' || echo "unknown")

# Check installed version
INSTALLED_VERSION="none"
if [ -f "$TARGET/.claude/.starter-kit-version" ]; then
    INSTALLED_VERSION=$(cat "$TARGET/.claude/.starter-kit-version" | tr -d '[:space:]')
fi

echo "═══════════════════════════════════════════════"
echo "  Upgrade Existing Project from Starter Kit"
echo "═══════════════════════════════════════════════"
echo "  Target:    $TARGET"
echo "  Kit:       v$KIT_VERSION"
echo "  Installed: v$INSTALLED_VERSION"
$DRY_RUN && echo "  Mode:      DRY RUN"
$FORCE && echo "  Mode:      FORCE (overwrite all)"
echo ""

if [ "$INSTALLED_VERSION" = "$KIT_VERSION" ] && ! $FORCE; then
    echo "  ✅ Already on latest version (v$KIT_VERSION)"
    echo "     Use --force to re-apply"
    exit 0
fi

ADDED=0
UPDATED=0
SKIPPED=0
CONFLICTS=0

# Smart copy: compare before overwrite
smart_copy() {
    local src="$1"
    local dst="$2"
    local label="$3"

    if [ ! -f "$dst" ]; then
        # New file — always copy
        echo "  ➕ NEW: $label"
        if ! $DRY_RUN; then
            mkdir -p "$(dirname "$dst")"
            cp "$src" "$dst"
            [ "${dst##*.}" = "sh" ] && chmod +x "$dst"
        fi
        ((ADDED++))
        return
    fi

    # File exists — compare
    if diff -q "$src" "$dst" > /dev/null 2>&1; then
        # Identical
        ((SKIPPED++))
        return
    fi

    if $FORCE; then
        echo "  🔄 OVERWRITE: $label"
        if ! $DRY_RUN; then
            cp "$src" "$dst"
        fi
        ((UPDATED++))
        return
    fi

    # Different — show diff and ask
    echo ""
    echo "  ⚠️  CONFLICT: $label"
    echo "     Kit version differs from existing file."
    echo ""
    diff --color=auto -u "$dst" "$src" 2>/dev/null | head -25
    echo "     ..."
    echo ""
    echo "     Options:"
    echo "       [k] Keep existing (skip)"
    echo "       [u] Use kit version (overwrite)"
    echo "       [m] Merge manually later (copy as .kit-new)"
    read -p "     Choice [k/u/m]: " -n 1 -r
    echo ""

    case "$REPLY" in
        u|U)
            if ! $DRY_RUN; then cp "$src" "$dst"; fi
            ((UPDATED++))
            ;;
        m|M)
            if ! $DRY_RUN; then cp "$src" "${dst}.kit-new"; fi
            echo "     → Saved as ${dst}.kit-new — merge manually"
            ((CONFLICTS++))
            ;;
        *)
            ((SKIPPED++))
            ;;
    esac
}

# ─── Scripts ───
if $ALL || $ONLY_SCRIPTS; then
    echo "📜 Scripts:"
    mkdir -p "$TARGET/scripts" "$TARGET/.claude/scripts" 2>/dev/null || true
    smart_copy "$SCRIPT_DIR/scripts/check-ci.sh" "$TARGET/scripts/check-ci.sh" "scripts/check-ci.sh"
    smart_copy "$SCRIPT_DIR/scripts/test-local.sh" "$TARGET/scripts/test-local.sh" "scripts/test-local.sh"
    smart_copy "$SCRIPT_DIR/scripts/pre-commit-check.sh" "$TARGET/.claude/scripts/pre-commit-check.sh" ".claude/scripts/pre-commit-check.sh"
    echo ""
fi

# ─── Skills ───
if $ALL || $ONLY_SKILLS; then
    echo "📋 Skills:"
    for skill_file in $(find "$SCRIPT_DIR/skills" -name "*.md" | sort); do
        rel_path="${skill_file#$SCRIPT_DIR/skills/}"
        smart_copy "$skill_file" "$TARGET/.claude/skills/$rel_path" ".claude/skills/$rel_path"
    done
    echo ""
fi

# ─── Templates ───
if $ALL; then
    echo "📝 Templates:"
    smart_copy "$SCRIPT_DIR/templates/CLAUDE.md.template" "$TARGET/CLAUDE.md" "CLAUDE.md"
    smart_copy "$SCRIPT_DIR/templates/README.md.template" "$TARGET/README.md" "README.md"
    echo ""
fi

# ─── Memory ───
if $ALL || $ONLY_MEMORY; then
    echo "🧠 Seed Memories:"
    # Detect project memory path
    ABS_TARGET="$(cd "$TARGET" && pwd)"
    MEMORY_DIR="$HOME/.claude/projects/$(echo "$ABS_TARGET" | tr '/' '-')/memory"
    mkdir -p "$MEMORY_DIR" 2>/dev/null || true

    for mem in "$SCRIPT_DIR/memory/"*.md; do
        name=$(basename "$mem")
        smart_copy "$mem" "$MEMORY_DIR/$name" "memory/$name"
    done

    # Create MEMORY.md index if missing
    if [ ! -f "$MEMORY_DIR/MEMORY.md" ]; then
        echo "  ➕ Creating MEMORY.md index"
        if ! $DRY_RUN; then
            cat > "$MEMORY_DIR/MEMORY.md" << 'MEMEOF'
# Project Memory Index

## Feedback (lessons learned from starter-kit)
- [feedback_scripts_not_adhoc.md](feedback_scripts_not_adhoc.md) — Scripts, not ad-hoc commands
- [feedback_ci_before_scoring.md](feedback_ci_before_scoring.md) — CI must complete before scoring
- [feedback_self_test_before_push.md](feedback_self_test_before_push.md) — Test local before push
- [feedback_business_design_first.md](feedback_business_design_first.md) — Business docs before code
MEMEOF
        fi
        ((ADDED++))
    fi
    echo ""
fi

# ─── Git hooks ───
if $ALL || $ONLY_SCRIPTS; then
    if [ -d "$TARGET/.git" ]; then
        echo "🔗 Git Hooks:"
        HOOK="$TARGET/.git/hooks/pre-commit"
        if [ ! -f "$HOOK" ]; then
            echo "  ➕ Linking pre-commit hook"
            if ! $DRY_RUN; then
                ln -sf "../../.claude/scripts/pre-commit-check.sh" "$HOOK"
            fi
            ((ADDED++))
        else
            echo "  ⏭️  pre-commit hook already exists"
            ((SKIPPED++))
        fi
        echo ""
    fi
fi

# ─── Summary ───
echo "═══════════════════════════════════════════════"
echo "  ➕ Added:     $ADDED"
echo "  🔄 Updated:   $UPDATED"
echo "  ⏭️  Skipped:   $SKIPPED"
echo "  ⚠️  Conflicts: $CONFLICTS"
echo "═══════════════════════════════════════════════"
$DRY_RUN && echo "  (dry run — no files changed)"

if [ $CONFLICTS -gt 0 ]; then
    echo ""
    echo "  ⚠️  $CONFLICTS file(s) saved as .kit-new"
    echo "  Review and merge manually:"
    find "$TARGET" -name "*.kit-new" 2>/dev/null | sed 's/^/    /'
fi

# Track installed version
if ! $DRY_RUN && [ $((ADDED + UPDATED)) -gt 0 ]; then
    mkdir -p "$TARGET/.claude"
    echo "$KIT_VERSION" > "$TARGET/.claude/.starter-kit-version"
    echo ""
    echo "  📦 Installed version: v$KIT_VERSION"
fi

echo ""
echo "Next steps:"
echo "  1. Review imported files"
echo "  2. Edit CLAUDE.md — replace {placeholders}"
echo "  3. Edit scripts/test-local.sh — configure PROJECT_DIRS"
