#!/bin/bash
# upgrade-project.sh — Import starter-kit vào dự án đã có skills
#
# Plan-based flow (không dùng interactive prompt):
#   1. --plan  → scan + tạo upgrade-plan.md (user review)
#   2. --apply → apply theo plan đã review
#   3. --force → apply tất cả không cần plan
#
# Usage:
#   ./upgrade-project.sh /path/to/project --plan              # Tạo plan
#   ./upgrade-project.sh /path/to/project --plan --scripts    # Plan chỉ scripts
#   ./upgrade-project.sh /path/to/project --apply             # Apply plan
#   ./upgrade-project.sh /path/to/project --force             # Apply all, no plan
#   ./upgrade-project.sh /path/to/project --dry-run           # Preview (= --plan nhưng không ghi file)

set -uo pipefail

TARGET="${1:-.}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

MODE="plan"  # default
ONLY_SCRIPTS=false
ONLY_SKILLS=false
ONLY_MEMORY=false

for arg in "${@:2}"; do
    case "$arg" in
        --plan) MODE="plan" ;;
        --apply) MODE="apply" ;;
        --force) MODE="force" ;;
        --dry-run) MODE="dry-run" ;;
        --scripts) ONLY_SCRIPTS=true ;;
        --skills) ONLY_SKILLS=true ;;
        --memory) ONLY_MEMORY=true ;;
    esac
done

ALL=true
$ONLY_SCRIPTS || $ONLY_SKILLS || $ONLY_MEMORY && ALL=false

KIT_VERSION=$(cat "$SCRIPT_DIR/VERSION" 2>/dev/null | tr -d '[:space:]' || echo "unknown")
INSTALLED_VERSION="none"
[ -f "$TARGET/.claude/.starter-kit-version" ] && INSTALLED_VERSION=$(cat "$TARGET/.claude/.starter-kit-version" | tr -d '[:space:]')

PLAN_FILE="$TARGET/.claude/upgrade-plan.md"
ADDED=0
UPDATED=0
SKIPPED=0
CONFLICTS=0

echo "═══════════════════════════════════════════════"
echo "  Starter Kit Upgrade (v$INSTALLED_VERSION → v$KIT_VERSION)"
echo "═══════════════════════════════════════════════"
echo "  Target: $TARGET"
echo "  Mode:   $MODE"
echo ""

if [ "$INSTALLED_VERSION" = "$KIT_VERSION" ] && [ "$MODE" != "force" ]; then
    echo "  ✅ Already on latest version (v$KIT_VERSION)"
    echo "     Use --force to re-apply"
    exit 0
fi

# ─── Collect all file pairs ───
declare -a PAIRS=()

add_pair() {
    local src="$1" dst="$2" label="$3"
    PAIRS+=("$src|$dst|$label")
}

if $ALL || $ONLY_SCRIPTS; then
    add_pair "$SCRIPT_DIR/scripts/check-ci.sh" "$TARGET/scripts/check-ci.sh" "scripts/check-ci.sh"
    add_pair "$SCRIPT_DIR/scripts/test-local.sh" "$TARGET/scripts/test-local.sh" "scripts/test-local.sh"
    add_pair "$SCRIPT_DIR/scripts/pre-commit-check.sh" "$TARGET/.claude/scripts/pre-commit-check.sh" ".claude/scripts/pre-commit-check.sh"
fi

if $ALL || $ONLY_SKILLS; then
    for f in $(find "$SCRIPT_DIR/skills" -name "*.md" | sort); do
        rel="${f#$SCRIPT_DIR/skills/}"
        add_pair "$f" "$TARGET/.claude/skills/$rel" ".claude/skills/$rel"
    done
fi

if $ALL; then
    add_pair "$SCRIPT_DIR/templates/CLAUDE.md.template" "$TARGET/CLAUDE.md" "CLAUDE.md"
    add_pair "$SCRIPT_DIR/templates/README.md.template" "$TARGET/README.md" "README.md"
fi

if $ALL || $ONLY_MEMORY; then
    ABS_TARGET="$(cd "$TARGET" && pwd)"
    MEMORY_DIR="$HOME/.claude/projects/$(echo "$ABS_TARGET" | tr '/' '-')/memory"
    for mem in "$SCRIPT_DIR/memory/"*.md; do
        name=$(basename "$mem")
        add_pair "$mem" "$MEMORY_DIR/$name" "memory/$name"
    done
fi

# ─── Classify each file ───
declare -a NEW_FILES=()
declare -a CHANGED_FILES=()
declare -a IDENTICAL_FILES=()

for pair in "${PAIRS[@]}"; do
    IFS='|' read -r src dst label <<< "$pair"
    if [ ! -f "$dst" ]; then
        NEW_FILES+=("$pair")
    elif diff -q "$src" "$dst" > /dev/null 2>&1; then
        IDENTICAL_FILES+=("$pair")
    else
        CHANGED_FILES+=("$pair")
    fi
done

# ─── MODE: plan / dry-run → generate review file ───
if [ "$MODE" = "plan" ] || [ "$MODE" = "dry-run" ]; then
    echo "Scanning ${#PAIRS[@]} files..."
    echo ""
    echo "  ➕ New:       ${#NEW_FILES[@]}"
    echo "  📝 Changed:   ${#CHANGED_FILES[@]}"
    echo "  = Identical:  ${#IDENTICAL_FILES[@]}"
    echo ""

    if [ ${#NEW_FILES[@]} -eq 0 ] && [ ${#CHANGED_FILES[@]} -eq 0 ]; then
        echo "  ✅ Nothing to upgrade — all files identical"
        exit 0
    fi

    # Generate plan file
    PLAN_CONTENT="# Upgrade Plan: Starter Kit v$INSTALLED_VERSION → v$KIT_VERSION

Generated: $(date '+%Y-%m-%d %H:%M')
Target: $TARGET

## How to use this plan

1. Review each section below
2. For CHANGED files: edit the Action column (accept/skip/merge)
3. Run: \`upgrade-project.sh $TARGET --apply\`

---

## New Files (will be added automatically)

| File | Action |
|------|--------|
"
    for pair in "${NEW_FILES[@]}"; do
        IFS='|' read -r src dst label <<< "$pair"
        PLAN_CONTENT+="| $label | **add** |
"
    done

    PLAN_CONTENT+="
## Changed Files (review required)

"
    for pair in "${CHANGED_FILES[@]}"; do
        IFS='|' read -r src dst label <<< "$pair"
        PLAN_CONTENT+="### $label

**Action:** accept / skip / merge ← EDIT THIS

\`\`\`diff
$(diff -u "$dst" "$src" 2>/dev/null | head -30)
\`\`\`

---

"
    done

    PLAN_CONTENT+="## Identical Files (no action needed)

"
    for pair in "${IDENTICAL_FILES[@]}"; do
        IFS='|' read -r src dst label <<< "$pair"
        PLAN_CONTENT+="- $label
"
    done

    if [ "$MODE" = "plan" ]; then
        mkdir -p "$(dirname "$PLAN_FILE")"
        echo "$PLAN_CONTENT" > "$PLAN_FILE"
        echo "  📝 Plan created: $PLAN_FILE"
        echo ""
        echo "  Next steps:"
        echo "    1. Review: cat $PLAN_FILE"
        echo "    2. Edit Action for each CHANGED file (accept/skip/merge)"
        echo "    3. Apply: ./upgrade-project.sh $TARGET --apply"
    else
        echo "$PLAN_CONTENT"
        echo "  (dry-run — no files written)"
    fi
    exit 0
fi

# ─── MODE: apply → read plan + apply ───
if [ "$MODE" = "apply" ]; then
    if [ ! -f "$PLAN_FILE" ]; then
        echo "  ❌ No plan found at: $PLAN_FILE"
        echo "     Run first: ./upgrade-project.sh $TARGET --plan"
        exit 1
    fi

    echo "Applying plan from: $PLAN_FILE"
    echo ""

    # Apply all new files
    for pair in "${NEW_FILES[@]}"; do
        IFS='|' read -r src dst label <<< "$pair"
        mkdir -p "$(dirname "$dst")"
        cp "$src" "$dst"
        [ "${dst##*.}" = "sh" ] && chmod +x "$dst"
        echo "  ➕ Added: $label"
        ((ADDED++))
    done

    # Apply changed files based on plan
    for pair in "${CHANGED_FILES[@]}"; do
        IFS='|' read -r src dst label <<< "$pair"

        # Read action from plan file
        ACTION=$(grep -A1 "### $label" "$PLAN_FILE" 2>/dev/null | grep "Action:" | grep -oE "accept|skip|merge" | head -1 || echo "skip")

        case "$ACTION" in
            accept)
                cp "$src" "$dst"
                echo "  ✅ Updated: $label"
                ((UPDATED++))
                ;;
            merge)
                cp "$src" "${dst}.kit-new"
                echo "  📝 Merge: $label → saved as .kit-new"
                ((CONFLICTS++))
                ;;
            skip|*)
                echo "  ⏭️  Skipped: $label"
                ((SKIPPED++))
                ;;
        esac
    done

    # Track version
    mkdir -p "$TARGET/.claude"
    echo "$KIT_VERSION" > "$TARGET/.claude/.starter-kit-version"

    # Cleanup plan
    rm -f "$PLAN_FILE"

    echo ""
    echo "═══════════════════════════════════════════════"
    echo "  ➕ Added: $ADDED  ✅ Updated: $UPDATED  ⏭️ Skipped: $SKIPPED  📝 Merge: $CONFLICTS"
    echo "  📦 Version: v$KIT_VERSION"
    echo "═══════════════════════════════════════════════"
    exit 0
fi

# ─── MODE: force → apply everything ───
if [ "$MODE" = "force" ]; then
    echo "Force applying all files..."
    echo ""

    for pair in "${PAIRS[@]}"; do
        IFS='|' read -r src dst label <<< "$pair"
        mkdir -p "$(dirname "$dst")"
        cp "$src" "$dst"
        [ "${dst##*.}" = "sh" ] && chmod +x "$dst"

        if [ -f "$dst" ]; then
            echo "  🔄 $label"
            ((UPDATED++))
        else
            echo "  ➕ $label"
            ((ADDED++))
        fi
    done

    mkdir -p "$TARGET/.claude"
    echo "$KIT_VERSION" > "$TARGET/.claude/.starter-kit-version"

    echo ""
    echo "═══════════════════════════════════════════════"
    echo "  ➕ Added: $ADDED  🔄 Updated: $UPDATED"
    echo "  📦 Version: v$KIT_VERSION"
    echo "═══════════════════════════════════════════════"
fi
