#!/bin/bash
# starter-kit-diff.sh — Triaged diff of local `.claude/` vs remote starter-kit
#
# Usage:
#   ./scripts/starter-kit-diff.sh                  # full triaged diff to stdout
#   ./scripts/starter-kit-diff.sh --output FILE    # write to FILE
#   ./scripts/starter-kit-diff.sh --category rules # only rules or skills
#   ./scripts/starter-kit-diff.sh --clean          # remove /tmp/kit before clone
#
# Exit codes:
#   0 = diff produced (even if empty)
#   1 = prerequisite missing (git, gh)
#   2 = remote clone failed
#
# GAP-195 Phase 1 tooling. Does NOT push to remote — operator decides.

set -euo pipefail

REMOTE_REPO="https://github.com/VictorAurelius/claude-starter-kit.git"
REMOTE_DIR="/tmp/kit"
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LOCAL_CLAUDE_DIR="$PROJECT_ROOT/.claude"

OUTPUT=""
CATEGORY="all"   # all | rules | skills
CLEAN=false

for arg in "$@"; do
  case "$arg" in
    --output=*) OUTPUT="${arg#*=}" ;;
    --output)   shift; OUTPUT="${1:-}" ;;
    --category=*) CATEGORY="${arg#*=}" ;;
    --category) shift; CATEGORY="${1:-all}" ;;
    --clean) CLEAN=true ;;
    -h|--help)
      sed -n '1,20p' "$0"
      exit 0
      ;;
  esac
done

# ---- prerequisites ----
command -v git >/dev/null || { echo "ERROR: git required" >&2; exit 1; }
command -v diff >/dev/null || { echo "ERROR: diff required" >&2; exit 1; }

# ---- clone remote (or reuse) ----
if $CLEAN && [ -d "$REMOTE_DIR" ]; then
  rm -rf "$REMOTE_DIR"
fi

if [ ! -d "$REMOTE_DIR/.git" ]; then
  echo "Cloning remote starter-kit to $REMOTE_DIR..." >&2
  git clone --depth 1 "$REMOTE_REPO" "$REMOTE_DIR" >/dev/null 2>&1 || {
    echo "ERROR: failed to clone $REMOTE_REPO" >&2
    exit 2
  }
else
  echo "Reusing existing $REMOTE_DIR (pass --clean to refresh)" >&2
  (cd "$REMOTE_DIR" && git pull --quiet --ff-only) || true
fi

REMOTE_VERSION=$(cat "$REMOTE_DIR/VERSION" 2>/dev/null || echo "unknown")
LOCAL_VERSION=$(cat "$LOCAL_CLAUDE_DIR/starter-kit/VERSION" 2>/dev/null || echo "none")

# ---- build output ----
exec 3>&1
if [ -n "$OUTPUT" ]; then
  exec 1>"$OUTPUT"
fi

print_header() {
  echo "# Starter-Kit Diff Report"
  echo ""
  echo "- **Generated:** $(date -Iseconds)"
  echo "- **Remote:** $REMOTE_REPO"
  echo "- **Remote VERSION:** $REMOTE_VERSION"
  echo "- **Local kit copy VERSION:** $LOCAL_VERSION"
  echo "- **Project root:** $PROJECT_ROOT"
  echo "- **Category filter:** $CATEGORY"
  echo ""
  echo "Triage legend:"
  echo "- 🆕 **NEW (local)** — exists locally, missing remote → candidate for retro-sync"
  echo "- 🆕 **NEW (remote)** — exists remote, missing locally → import candidate"
  echo "- ✏️  **MODIFIED** — exists both sides, content differs → merge decision needed"
  echo "- 🔒 **PROJECT-SPECIFIC** — local only, but tagged project-specific → omit from sync"
  echo ""
}

is_project_specific() {
  # Heuristic: file name or first-20-lines content mentions project-specific markers.
  # Adjust as the kit's generic vs specific taxonomy matures.
  local f="$1"
  local basename
  basename="$(basename "$f")"
  case "$basename" in
    *kitehub*|*kiteclass*|*ai-branding*) return 0 ;;
  esac
  head -n 30 "$f" 2>/dev/null | grep -qiE "kitehub|kiteclass|ai-branding|smart-quiz" && return 0
  return 1
}

diff_dir() {
  local rel="$1"
  local local_dir="$LOCAL_CLAUDE_DIR/$rel"
  local remote_dir="$REMOTE_DIR/.claude/$rel"

  echo ""
  echo "## Section: \`.claude/$rel/\`"
  echo ""

  if [ ! -d "$local_dir" ] && [ ! -d "$remote_dir" ]; then
    echo "_Neither side has this directory; skipping._"
    return
  fi

  # NEW (local only) + PROJECT-SPECIFIC
  if [ -d "$local_dir" ]; then
    echo "### 🆕 NEW (local only) + 🔒 PROJECT-SPECIFIC"
    echo ""
    while IFS= read -r f; do
      local rel_path="${f#$local_dir/}"
      if [ ! -e "$remote_dir/$rel_path" ]; then
        if is_project_specific "$f"; then
          echo "- 🔒 \`$rel/$rel_path\` — project-specific, omit from sync"
        else
          echo "- 🆕 \`$rel/$rel_path\` — candidate for remote PR"
        fi
      fi
    done < <(find "$local_dir" -type f \( -name "*.md" -o -name "*.sh" -o -name "*.py" \) 2>/dev/null | sort)
  fi

  # NEW (remote only)
  if [ -d "$remote_dir" ]; then
    echo ""
    echo "### 🆕 NEW (remote only) — import candidates"
    echo ""
    while IFS= read -r f; do
      local rel_path="${f#$remote_dir/}"
      if [ ! -e "$local_dir/$rel_path" ]; then
        echo "- 🆕 \`$rel/$rel_path\` — newer than local; pull?"
      fi
    done < <(find "$remote_dir" -type f \( -name "*.md" -o -name "*.sh" -o -name "*.py" \) 2>/dev/null | sort)
  fi

  # MODIFIED
  if [ -d "$local_dir" ] && [ -d "$remote_dir" ]; then
    echo ""
    echo "### ✏️  MODIFIED (both sides, content differs)"
    echo ""
    while IFS= read -r f; do
      local rel_path="${f#$local_dir/}"
      if [ -e "$remote_dir/$rel_path" ]; then
        if ! diff -q "$f" "$remote_dir/$rel_path" >/dev/null 2>&1; then
          echo "- ✏️  \`$rel/$rel_path\` — differs; review diff manually"
        fi
      fi
    done < <(find "$local_dir" -type f \( -name "*.md" -o -name "*.sh" -o -name "*.py" \) 2>/dev/null | sort)
  fi
}

print_header

case "$CATEGORY" in
  rules)  diff_dir "rules" ;;
  skills) diff_dir "skills" ;;
  all)
    diff_dir "rules"
    diff_dir "skills"
    ;;
  *) echo "ERROR: --category must be one of: all | rules | skills" >&2; exit 1 ;;
esac

echo ""
echo "## Summary"
echo ""
echo "Review the triaged diff above and:"
echo "1. For 🆕 NEW (local) non-project-specific: candidate for PR to \`$REMOTE_REPO\`"
echo "2. For 🆕 NEW (remote): decide if project should adopt"
echo "3. For ✏️  MODIFIED: run \`diff -u\` locally to inspect"
echo "4. For 🔒 PROJECT-SPECIFIC: document in this project only; do not sync"
echo ""
echo "See \`documents/05-guides/starter-kit-retro-sync.md\` for the triage + PR runbook."
