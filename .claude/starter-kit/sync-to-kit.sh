#!/bin/bash
# sync-to-kit.sh — Sync project skills/scripts → starter-kit
#
# Chạy khi dự án gốc cập nhật skills, rules, workflows
# Script sẽ so sánh và hỏi trước khi overwrite
#
# Usage:
#   ./sync-to-kit.sh                    # Interactive mode
#   ./sync-to-kit.sh --dry-run          # Chỉ hiển thị diff, không thay đổi
#   ./sync-to-kit.sh --auto             # Auto-sync không hỏi

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
KIT_DIR="$SCRIPT_DIR"
DRY_RUN=false
AUTO=false

for arg in "$@"; do
    case "$arg" in
        --dry-run) DRY_RUN=true ;;
        --auto) AUTO=true ;;
    esac
done

echo "═══════════════════════════════════════════════"
echo "  Sync Project → Starter Kit"
echo "═══════════════════════════════════════════════"
$DRY_RUN && echo "  Mode: DRY RUN (no changes)"
echo ""

# Mapping: project file → kit file (chỉ sync generic-safe files)
# Format: "source|destination|description"
MAPPINGS=(
    "scripts/check-ci.sh|scripts/check-ci.sh|CI monitoring script"
    "scripts/test-local.sh|scripts/test-local.sh|Local test runner"
    ".claude/scripts/pre-commit-check.sh|scripts/pre-commit-check.sh|Pre-commit hooks"
)

UPDATED=0
SKIPPED=0
UNCHANGED=0

sync_file() {
    local src="$1"
    local dst="$KIT_DIR/$2"
    local desc="$3"

    if [ ! -f "$src" ]; then
        echo "  ⏭️  $desc — source not found: $src"
        ((SKIPPED++))
        return
    fi

    if [ ! -f "$dst" ]; then
        echo "  ➕ $desc — NEW file"
        if ! $DRY_RUN; then
            mkdir -p "$(dirname "$dst")"
            cp "$src" "$dst"
        fi
        ((UPDATED++))
        return
    fi

    if diff -q "$src" "$dst" > /dev/null 2>&1; then
        ((UNCHANGED++))
        return
    fi

    echo "  📝 $desc — CHANGED"
    echo "     Source: $src"
    echo "     Kit:    $dst"

    if ! $DRY_RUN; then
        if $AUTO; then
            cp "$src" "$dst"
            ((UPDATED++))
        else
            echo ""
            diff --color=auto -u "$dst" "$src" | head -30
            echo ""
            read -p "     Apply update? [y/N] " -n 1 -r
            echo ""
            if [[ $REPLY =~ ^[Yy]$ ]]; then
                cp "$src" "$dst"
                ((UPDATED++))
            else
                ((SKIPPED++))
            fi
        fi
    else
        diff --color=auto -u "$dst" "$src" | head -20
        echo "     ..."
        ((UPDATED++))
    fi
}

echo "📜 Checking scripts..."
for mapping in "${MAPPINGS[@]}"; do
    IFS='|' read -r src dst desc <<< "$mapping"
    sync_file "$src" "$dst" "$desc"
done

echo ""
echo "📋 Checking seed memories..."
for mem in .claude/starter-kit/memory/feedback_*.md; do
    if [ -f "$mem" ]; then
        name=$(basename "$mem")
        # Check if project memory has newer version
        proj_mem="$HOME/.claude/projects/*/memory/$name"
        for pm in $proj_mem; do
            if [ -f "$pm" ] && ! diff -q "$pm" "$mem" > /dev/null 2>&1; then
                echo "  📝 $name — project memory differs"
                if ! $DRY_RUN && ! $AUTO; then
                    diff --color=auto -u "$mem" "$pm" | head -15
                    read -p "     Update kit from project memory? [y/N] " -n 1 -r
                    echo ""
                    if [[ $REPLY =~ ^[Yy]$ ]]; then
                        cp "$pm" "$mem"
                        ((UPDATED++))
                    fi
                fi
            fi
        done
    fi
done

echo ""
echo "═══════════════════════════════════════════════"
echo "  Results: ✅ $UPDATED updated, ⏭️  $SKIPPED skipped, = $UNCHANGED unchanged"
echo "═══════════════════════════════════════════════"
$DRY_RUN && echo "  (dry run — no files changed)"

# Update version timestamp
if ! $DRY_RUN && [ $UPDATED -gt 0 ]; then
    echo ""
    date "+%Y-%m-%d %H:%M" > "$KIT_DIR/.last-sync"
    echo "  📅 Last sync: $(cat "$KIT_DIR/.last-sync")"
fi
