#!/usr/bin/env bash
# check-docs.sh — Living Docs Completeness Gate
# Usage: ./check-docs.sh [--strict] [--base=main] [--branch=<feature/X>]
#
# Detects changes in current branch vs base, applies 12 doc-rule matrix from
# reference/doc-rules-matrix.md, reports missing co-changes.
#
# Exit codes:
#   0 — no failed checks (or --strict not set)
#   1 — failed checks present AND --strict set
#   2 — script error (e.g. detached HEAD, base unknown)

set -u

STRICT=0
BASE="main"
BRANCH=""
TS="$(date '+%Y-%m-%d %H:%M')"

# Parse args
for arg in "$@"; do
  case "$arg" in
    --strict) STRICT=1 ;;
    --base=*) BASE="${arg#--base=}" ;;
    --branch=*) BRANCH="${arg#--branch=}" ;;
    -h|--help)
      sed -n '2,12p' "$0"
      exit 0
      ;;
  esac
done

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "$REPO_ROOT" || { echo "ERR: not a git repo" >&2; exit 2; }

if [ -z "$BRANCH" ]; then
  BRANCH="$(git branch --show-current 2>/dev/null || echo '')"
fi

if [ -z "$BRANCH" ]; then
  echo "ERR: detached HEAD or no branch — pass --branch=<name>" >&2
  exit 2
fi

if [ "$BRANCH" = "$BASE" ]; then
  echo "## Session Docs Check ($TS) — branch $BRANCH"
  echo
  echo "⚠️  Đang ở branch $BASE, không có gì để check vs chính nó. Skipping."
  exit 0
fi

# Verify base ref exists
if ! git rev-parse --verify "origin/$BASE" >/dev/null 2>&1 \
  && ! git rev-parse --verify "$BASE" >/dev/null 2>&1; then
  echo "ERR: base ref '$BASE' (origin/$BASE) not found — fetch first" >&2
  exit 2
fi

# Use origin/BASE if available else local BASE
if git rev-parse --verify "origin/$BASE" >/dev/null 2>&1; then
  BASE_REF="origin/$BASE"
else
  BASE_REF="$BASE"
fi

CHANGED_FILES="$(git diff "$BASE_REF" --name-only 2>/dev/null || true)"
COMMIT_MSGS="$(git log "$BASE_REF..HEAD" --pretty=format:'%s' 2>/dev/null || true)"

if [ -z "$CHANGED_FILES" ]; then
  echo "## Session Docs Check ($TS) — branch $BRANCH"
  echo
  echo "✅ Không có file thay đổi vs $BASE_REF. Nothing to check."
  exit 0
fi

# Tally helpers
PASSED=0
WARNED=0
FAILED=0
PASSED_LINES=""
WARNED_LINES=""
FAILED_LINES=""

emit_pass()  { PASSED=$((PASSED+1)); PASSED_LINES="${PASSED_LINES}[OK]    $1"$'\n'; }
emit_warn()  { WARNED=$((WARNED+1)); WARNED_LINES="${WARNED_LINES}[WARN]  $1"$'\n'; }
emit_fail()  { FAILED=$((FAILED+1)); FAILED_LINES="${FAILED_LINES}[FAIL]  $1"$'\n'; }

## ============================================================
# Rule 3 — New gap file → ROADMAP entry
# ============================================================
# Helper: file in changed-set?
file_changed() {
  echo "$CHANGED_FILES" | grep -qxF "$1"
}

NEW_GAPS="$(git diff "$BASE_REF" --name-status 2>/dev/null \
  | awk '$1 ~ /^A/ && $2 ~ /^documents\/04-quality\/gaps\/GAP-[0-9]+-.+\.md$/ {print $2}' \
  || true)"

if [ -n "$NEW_GAPS" ]; then
  ROADMAP_CHANGED=0
  if file_changed "documents/04-quality/gaps/ROADMAP.md"; then
    ROADMAP_CHANGED=1
  fi
  while IFS= read -r gap_file; do
    [ -z "$gap_file" ] && continue
    GAP_NUM="$(basename "$gap_file" | grep -oE 'GAP-[0-9]+' | head -1)"
    if [ "$ROADMAP_CHANGED" = "1" ] && git diff "$BASE_REF" -- documents/04-quality/gaps/ROADMAP.md \
        2>/dev/null | grep -qE "^\+.*\b$GAP_NUM\b"; then
      emit_pass "Rule 3 — gap mới $GAP_NUM → ROADMAP.md có entry tương ứng"
    else
      emit_fail "Rule 3 — gap mới $GAP_NUM nhưng ROADMAP.md CHƯA có log entry. Fix: thêm dòng vào §🎯 Current Status Snapshot"
    fi
  done <<< "$NEW_GAPS"
fi

# ============================================================
# Rule 4 — Modified gap file → ROADMAP entry (status flip / new log)
# ============================================================
MOD_GAPS="$(git diff "$BASE_REF" --name-status 2>/dev/null \
  | awk '$1 ~ /^M/ && $2 ~ /^documents\/04-quality\/gaps\/GAP-[0-9]+-.+\.md$/ {print $2}' \
  || true)"

if [ -n "$MOD_GAPS" ]; then
  while IFS= read -r gap_file; do
    [ -z "$gap_file" ] && continue
    GAP_NUM="$(basename "$gap_file" | grep -oE 'GAP-[0-9]+' | head -1)"
    # Detect status change in diff
    STATUS_CHANGE="$(git diff "$BASE_REF" -- "$gap_file" 2>/dev/null \
      | grep -E '^\+\*\*Status:\*\*' || true)"
    if [ -n "$STATUS_CHANGE" ]; then
      # Status changed → check ROADMAP has entry
      if file_changed "documents/04-quality/gaps/ROADMAP.md" \
        && git diff "$BASE_REF" -- documents/04-quality/gaps/ROADMAP.md 2>/dev/null \
          | grep -qE "^\+.*\b$GAP_NUM\b"; then
        emit_pass "Rule 4 — gap $GAP_NUM status thay đổi → ROADMAP có entry"
      else
        emit_fail "Rule 4 — gap $GAP_NUM status thay đổi nhưng ROADMAP CHƯA có entry. Fix: thêm log entry §🎯 Current Status Snapshot"
      fi
    else
      # Body edit only (e.g. log entry append) — WARN if ROADMAP unchanged
      if file_changed "documents/04-quality/gaps/ROADMAP.md"; then
        emit_pass "Rule 4 — gap $GAP_NUM Log appended → ROADMAP cũng modified"
      else
        emit_warn "Rule 4 — gap $GAP_NUM modified nhưng ROADMAP unchanged. Có thể chỉ là cosmetic edit (skip OK) hoặc thiếu ROADMAP sync"
      fi
    fi
  done <<< "$MOD_GAPS"
fi

# ============================================================
# Rule 5 — New skill → _README-skills-index.md
# ============================================================
NEW_SKILLS="$(git diff "$BASE_REF" --name-status 2>/dev/null \
  | awk '$1 ~ /^A/ && $2 ~ /^\.claude\/skills\/.+\/SKILL\.md$/ {print $2}' \
  || true)"

if [ -n "$NEW_SKILLS" ]; then
  INDEX_CHANGED=0
  if file_changed ".claude/skills/_README-skills-index.md"; then
    INDEX_CHANGED=1
  fi
  while IFS= read -r skill_file; do
    [ -z "$skill_file" ] && continue
    SKILL_NAME="$(basename "$(dirname "$skill_file")")"
    if [ "$INDEX_CHANGED" = "1" ] && git diff "$BASE_REF" -- .claude/skills/_README-skills-index.md \
        2>/dev/null | grep -qE "^\+.*$SKILL_NAME"; then
      emit_pass "Rule 5 — skill mới $SKILL_NAME → _README-skills-index.md có entry"
    else
      emit_fail "Rule 5 — skill mới $SKILL_NAME nhưng _README-skills-index.md CHƯA có entry. Fix: thêm dòng vào index"
    fi
  done <<< "$NEW_SKILLS"
fi

# ============================================================
# Rule 7 — Rule edit → Version bump + Log entry
# ============================================================
MOD_RULES="$(git diff "$BASE_REF" --name-status 2>/dev/null \
  | awk '$1 ~ /^M/ && $2 ~ /^\.claude\/rules\/.+\.md$/ {print $2}' \
  || true)"

if [ -n "$MOD_RULES" ]; then
  while IFS= read -r rule_file; do
    [ -z "$rule_file" ] && continue
    RULE_NAME="$(basename "$rule_file" .md)"
    DIFF="$(git diff "$BASE_REF" -- "$rule_file" 2>/dev/null || true)"
    HAS_VERSION_BUMP="$(echo "$DIFF" | grep -E '^\+.*Version:.*[0-9]+\.[0-9]+\.[0-9]+' || true)"
    HAS_LOG_ENTRY="$(echo "$DIFF" | grep -E '^\+.*\*\*[0-9]{4}-[0-9]{2}-[0-9]{2}\*\*' || true)"

    if [ -n "$HAS_VERSION_BUMP" ] && [ -n "$HAS_LOG_ENTRY" ]; then
      emit_pass "Rule 7 — rule $RULE_NAME edited với Version bump + Log entry"
    elif [ -n "$HAS_VERSION_BUMP" ] || [ -n "$HAS_LOG_ENTRY" ]; then
      emit_warn "Rule 7 — rule $RULE_NAME edited nhưng thiếu Version bump HOẶC Log entry. Per rule-change-process.md §3 cần CẢ HAI"
    else
      emit_fail "Rule 7 — rule $RULE_NAME edited nhưng KHÔNG có Version bump + Log entry. Fix: bump frontmatter Version + append §Log per rule-change-process.md §4"
    fi
  done <<< "$MOD_RULES"
fi

# ============================================================
# Rule 1 + 2 — Code change → docs (heuristic, WARN only)
# ============================================================
CODE_CHANGED="$(echo "$CHANGED_FILES" | grep -E '\.java$' \
  | grep -vE '/(test|Test)/' || true)"
if [ -n "$CODE_CHANGED" ]; then
  # Check if any 01-business doc was modified
  BIZ_DOCS_CHANGED="$(echo "$CHANGED_FILES" \
    | grep -E '^documents/01-business/.+/(rules|use-cases|api-contract)\.md$' || true)"

  if [ -z "$BIZ_DOCS_CHANGED" ]; then
    # Distinguish refactor vs feature by commit msg
    if echo "$COMMIT_MSGS" | grep -qiE '^(feat|fix)\(' ; then
      emit_warn "Rules 1+2 — feat/fix Java code thay đổi nhưng không có docs/01-business/*.md update. Có thể chỉ là internal refactor (skip OK) hoặc thiếu docs sync"
    else
      emit_pass "Rules 1+2 — Java code thay đổi (commit type non-feat/fix) — skip docs check"
    fi
  else
    emit_pass "Rules 1+2 — Java code + 01-business/*.md cả hai modified"
  fi
fi

# ============================================================
# Rule 8 — New folder under documents/ → README chain (heuristic)
# ============================================================
NEW_DOC_FILES="$(git diff "$BASE_REF" --name-status 2>/dev/null \
  | awk '$1 ~ /^A/ && $2 ~ /^documents\/.+/ {print $2}' \
  || true)"

if [ -n "$NEW_DOC_FILES" ]; then
  # Get unique parent dirs
  NEW_DIRS="$(echo "$NEW_DOC_FILES" | xargs -n1 dirname 2>/dev/null | sort -u || true)"
  while IFS= read -r dir; do
    [ -z "$dir" ] && continue
    [ "$dir" = "documents" ] && continue
    # Check if README.md exists in that dir (post-change)
    if [ -f "$dir/README.md" ]; then
      emit_pass "Rule 8 — folder $dir có README.md"
    else
      # Maybe folder is intentional flat — only WARN
      emit_warn "Rule 8 — folder $dir có files mới nhưng KHÔNG có README.md. Per docs-folder-structure.md §3 mỗi top-level folder cần README."
    fi
  done <<< "$NEW_DIRS"
fi

# ============================================================
# Rule 9 — Migration → rules.md (basic check)
# ============================================================
NEW_MIGRATIONS="$(git diff "$BASE_REF" --name-status 2>/dev/null \
  | awk '$1 ~ /^A/ && $2 ~ /db\/migration\/V[0-9]+__.+\.sql$/ {print $2}' \
  || true)"

if [ -n "$NEW_MIGRATIONS" ]; then
  RULES_MD_CHANGED="$(echo "$CHANGED_FILES" | grep -E '^documents/01-business/.+/rules\.md$' || true)"
  while IFS= read -r mig; do
    [ -z "$mig" ] && continue
    if [ -n "$RULES_MD_CHANGED" ]; then
      emit_pass "Rule 9 — migration $(basename "$mig") + rules.md cả hai modified"
    else
      emit_warn "Rule 9 — migration $(basename "$mig") thêm nhưng KHÔNG có rules.md update. Verify Config Key column / table reference đã sync"
    fi
  done <<< "$NEW_MIGRATIONS"
fi

# ============================================================
# Output
# ============================================================
echo "## Session Docs Check ($TS) — branch $BRANCH (vs $BASE_REF)"
echo
echo "✅ PASSED $PASSED checks"
echo "⚠️  WARN $WARNED checks"
echo "❌ FAILED $FAILED checks"
echo
if [ -n "$FAILED_LINES" ]; then
  echo "### ❌ Failed (PHẢI fix trước commit/PR)"
  echo
  echo "$FAILED_LINES" | grep -v '^$' || true
  echo
fi
if [ -n "$WARNED_LINES" ]; then
  echo "### ⚠️  Warnings (cảnh báo, không block)"
  echo
  echo "$WARNED_LINES" | grep -v '^$' || true
  echo
fi
if [ -n "$PASSED_LINES" ]; then
  echo "### ✅ Passed"
  echo
  echo "$PASSED_LINES" | grep -v '^$' || true
  echo
fi
echo
if [ "$STRICT" = "1" ] && [ "$FAILED" -gt 0 ]; then
  echo "STRICT mode: $FAILED failed check(s) → exit 1"
  exit 1
fi
exit 0
