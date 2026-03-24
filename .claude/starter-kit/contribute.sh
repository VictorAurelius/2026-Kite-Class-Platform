#!/bin/bash
# contribute.sh — Đề xuất thay đổi từ dự án → starter-kit
#
# KHÔNG tự động apply. Tạo proposal file để review trước khi merge.
#
# Quy trình:
#   1. Dự án chạy contribute.sh → tạo proposal (.proposal/)
#   2. Reviewer đánh giá proposal (diff, lý do, impact)
#   3. Nếu approved → apply-proposal.sh merge vào kit + bump version
#
# Usage:
#   ./contribute.sh /path/to/project "Lý do đề xuất"
#   ./contribute.sh /path/to/project "Cải thiện TDD skill" --file skills/core/tdd-enforcement.md
#   ./contribute.sh --list                    # Xem proposals đang chờ
#   ./contribute.sh --apply <proposal-id>          # Apply proposal (interactive confirm)
#   ./contribute.sh --apply <proposal-id> --yes   # Apply without confirm (CI/Claude safe)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
KIT_DIR="$SCRIPT_DIR"
PROPOSAL_DIR="$KIT_DIR/.proposals"
mkdir -p "$PROPOSAL_DIR"

# ─── List pending proposals ───
if [ "${1:-}" = "--list" ]; then
    echo "═══════════════════════════════════════════════"
    echo "  Pending Proposals"
    echo "═══════════════════════════════════════════════"
    if [ -z "$(ls "$PROPOSAL_DIR"/*.proposal 2>/dev/null)" ]; then
        echo "  (none)"
    else
        for p in "$PROPOSAL_DIR"/*.proposal; do
            id=$(basename "$p" .proposal)
            source_project=$(head -1 "$p" | sed 's/^# //')
            reason=$(sed -n '2p' "$p")
            echo "  [$id] $source_project — $reason"
        done
    fi
    exit 0
fi

# ─── Apply approved proposal ───
if [ "${1:-}" = "--apply" ]; then
    PROPOSAL_ID="${2:-}"
    YES_FLAG=false
    [ "${3:-}" = "--yes" ] && YES_FLAG=true
    PROPOSAL_FILE="$PROPOSAL_DIR/${PROPOSAL_ID}.proposal"

    if [ ! -f "$PROPOSAL_FILE" ]; then
        echo "❌ Proposal not found: $PROPOSAL_ID"
        echo "   Run: ./contribute.sh --list"
        exit 1
    fi

    echo "═══════════════════════════════════════════════"
    echo "  Applying Proposal: $PROPOSAL_ID"
    echo "═══════════════════════════════════════════════"
    cat "$PROPOSAL_FILE"
    echo ""

    if ! $YES_FLAG; then
        read -p "Confirm apply? This will update kit files. [y/N] " -n 1 -r
        echo ""
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            echo "Cancelled."
            exit 0
        fi
    fi

    # Apply diffs from proposal
    PATCH_DIR="$PROPOSAL_DIR/${PROPOSAL_ID}.patches"
    if [ -d "$PATCH_DIR" ]; then
        for patch in "$PATCH_DIR"/*.patch; do
            target_file=$(head -1 "$patch" | sed 's/^# target: //')
            new_file="$PATCH_DIR/$(basename "$patch" .patch).new"
            if [ -f "$new_file" ]; then
                cp "$new_file" "$KIT_DIR/$target_file"
                echo "  ✅ Updated: $target_file"
            fi
        done
    fi

    # Bump version
    CURRENT_VERSION=$(cat "$KIT_DIR/VERSION" | tr -d '[:space:]')
    IFS='.' read -r major minor patch <<< "$CURRENT_VERSION"
    NEW_VERSION="$major.$((minor + 1)).0"
    echo "$NEW_VERSION" > "$KIT_DIR/VERSION"
    echo "  📦 Version: $CURRENT_VERSION → $NEW_VERSION"

    # Archive proposal
    mv "$PROPOSAL_FILE" "$PROPOSAL_DIR/${PROPOSAL_ID}.applied"
    [ -d "$PATCH_DIR" ] && mv "$PATCH_DIR" "$PROPOSAL_DIR/${PROPOSAL_ID}.applied-patches"

    echo ""
    echo "  ✅ Proposal applied. Next:"
    echo "     1. Update CHANGELOG.md"
    echo "     2. Review changes: git diff"
    echo "     3. Commit: git commit -m 'feat(starter-kit): v$NEW_VERSION — description'"
    exit 0
fi

# ─── Create new proposal ───
PROJECT_PATH="${1:-.}"
REASON="${2:-No reason provided}"
SPECIFIC_FILE="${4:-}"  # --file flag value

# Parse --file flag
for i in "${@:2}"; do
    if [ "$prev_was_file" = true ]; then
        SPECIFIC_FILE="$i"
        prev_was_file=false
    fi
    [ "$i" = "--file" ] && prev_was_file=true
done
prev_was_file=false

PROJECT_NAME=$(basename "$(cd "$PROJECT_PATH" && pwd)")
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
PROPOSAL_ID="${PROJECT_NAME}-${TIMESTAMP}"

echo "═══════════════════════════════════════════════"
echo "  Create Proposal: $PROPOSAL_ID"
echo "═══════════════════════════════════════════════"
echo "  From:   $PROJECT_PATH ($PROJECT_NAME)"
echo "  Reason: $REASON"
echo ""

PATCH_DIR="$PROPOSAL_DIR/${PROPOSAL_ID}.patches"
mkdir -p "$PATCH_DIR"

CHANGES=0

scan_and_diff() {
    local kit_subdir="$1"
    local project_subdir="$2"
    local label="$3"

    if [ ! -d "$PROJECT_PATH/$project_subdir" ]; then
        return
    fi

    echo "  Scanning $label..."
    for project_file in $(find "$PROJECT_PATH/$project_subdir" -name "*.md" -o -name "*.sh" 2>/dev/null | sort); do
        rel_path="${project_file#$PROJECT_PATH/$project_subdir/}"

        # Skip if specific file requested and doesn't match
        if [ -n "$SPECIFIC_FILE" ] && [ "$kit_subdir/$rel_path" != "$SPECIFIC_FILE" ]; then
            continue
        fi

        kit_file="$KIT_DIR/$kit_subdir/$rel_path"

        if [ ! -f "$kit_file" ]; then
            echo "    ➕ NEW: $kit_subdir/$rel_path"
            cp "$project_file" "$PATCH_DIR/$(echo "$kit_subdir/$rel_path" | tr '/' '_').new"
            echo "# target: $kit_subdir/$rel_path" > "$PATCH_DIR/$(echo "$kit_subdir/$rel_path" | tr '/' '_').patch"
            ((CHANGES++))
        elif ! diff -q "$project_file" "$kit_file" > /dev/null 2>&1; then
            echo "    📝 CHANGED: $kit_subdir/$rel_path"
            diff -u "$kit_file" "$project_file" > "$PATCH_DIR/$(echo "$kit_subdir/$rel_path" | tr '/' '_').patch" 2>/dev/null || true
            echo "# target: $kit_subdir/$rel_path" | cat - "$PATCH_DIR/$(echo "$kit_subdir/$rel_path" | tr '/' '_').patch" > /tmp/patch_tmp && mv /tmp/patch_tmp "$PATCH_DIR/$(echo "$kit_subdir/$rel_path" | tr '/' '_').patch"
            cp "$project_file" "$PATCH_DIR/$(echo "$kit_subdir/$rel_path" | tr '/' '_').new"
            ((CHANGES++))
        fi
    done
}

# Scan project directories that map to kit
scan_and_diff "skills" ".claude/skills" "Skills"
scan_and_diff "scripts" "scripts" "Scripts"

if [ $CHANGES -eq 0 ]; then
    echo ""
    echo "  ✅ No differences found — kit is up to date"
    rm -rf "$PATCH_DIR"
    exit 0
fi

# Write proposal file
cat > "$PROPOSAL_DIR/${PROPOSAL_ID}.proposal" << PROPEOF
# $PROJECT_NAME
$REASON
Date: $(date +%Y-%m-%d)
Changes: $CHANGES file(s)
Kit Version: $(cat "$KIT_DIR/VERSION" | tr -d '[:space:]')

## Files Changed
$(ls "$PATCH_DIR"/*.patch 2>/dev/null | while read f; do
    echo "- $(head -1 "$f" | sed 's/^# target: //')"
done)

## Review Checklist
- [ ] Changes are generic (no project-specific references)
- [ ] Improves process quality (not just preference)
- [ ] Backward compatible with existing projects using kit
- [ ] CHANGELOG entry prepared
PROPEOF

echo ""
echo "═══════════════════════════════════════════════"
echo "  ✅ Proposal created: $PROPOSAL_ID"
echo "     $CHANGES change(s)"
echo ""
echo "  Next steps:"
echo "    1. Review: cat $PROPOSAL_DIR/${PROPOSAL_ID}.proposal"
echo "    2. Check diffs: ls $PATCH_DIR/"
echo "    3. If approved: ./contribute.sh --apply $PROPOSAL_ID"
echo "═══════════════════════════════════════════════"
