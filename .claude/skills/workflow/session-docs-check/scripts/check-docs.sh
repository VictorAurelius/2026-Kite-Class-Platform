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
    # Only flag TRULY new dirs (no files in BASE_REF tree).
    # If BASE_REF already had any files in $dir → pre-existing, skip.
    EXISTING_IN_BASE="$(git ls-tree -r --name-only "$BASE_REF" -- "$dir" 2>/dev/null | head -1 || true)"
    if [ -n "$EXISTING_IN_BASE" ]; then
      continue
    fi
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
# Rule 13 — Gap status flip to DONE → completeness check
# (per .claude/rules/gap-done-discipline.md + matrix Rule 13)
# ============================================================
# Detect gap files whose diff ADDS a `**Status:** ... 🟢 DONE` line.
DONE_FLIPS="$(git diff "$BASE_REF" --name-only 2>/dev/null \
  | grep -E '^documents/04-quality/gaps/GAP-[0-9]+-.+\.md$' \
  | while IFS= read -r gf; do
      [ -z "$gf" ] && continue
      if git diff "$BASE_REF" -- "$gf" 2>/dev/null \
          | grep -qE '^\+\*\*Status:\*\*.*🟢 DONE'; then
        echo "$gf"
      fi
    done || true)"

# Override-trailer collection — commit log between BASE_REF..HEAD.
# Format: `GAP_DONE_OVERRIDE: GAP-NNN — <reason>`
OVERRIDE_TRAILERS="$(git log "$BASE_REF..HEAD" --pretty=%B 2>/dev/null \
  | grep -E '^GAP_DONE_OVERRIDE: GAP-[0-9]+ — .+' || true)"

if [ -n "$DONE_FLIPS" ]; then
  while IFS= read -r gap_file; do
    [ -z "$gap_file" ] && continue
    GAP_NUM="$(basename "$gap_file" | grep -oE 'GAP-[0-9]+' | head -1)"

    # Check override trailer first — if present, every Rule-13 issue downgrades.
    HAS_OVERRIDE=0
    if [ -n "$OVERRIDE_TRAILERS" ] \
        && echo "$OVERRIDE_TRAILERS" | grep -qE "GAP_DONE_OVERRIDE: $GAP_NUM "; then
      HAS_OVERRIDE=1
    fi

    issue_emit() {
      # $1 = severity-when-no-override (fail|warn), $2 = message
      if [ "$HAS_OVERRIDE" = "1" ]; then
        emit_warn "Rule 13 — $GAP_NUM (override trailer present): $2"
      elif [ "$1" = "fail" ]; then
        emit_fail "Rule 13 — $GAP_NUM: $2"
      else
        emit_warn "Rule 13 — $GAP_NUM: $2"
      fi
    }

    # ───── 13.1 — Acceptance Criteria fully checked ─────────────────────
    # Extract the "## Acceptance Criteria" section (until next heading).
    AC_SECTION="$(awk '
      /^##[[:space:]]+Acceptance[[:space:]]+Criteria/ {flag=1; next}
      flag && /^##[[:space:]]/ {flag=0}
      flag {print}
    ' "$gap_file" 2>/dev/null || true)"

    if [ -z "$AC_SECTION" ]; then
      emit_warn "Rule 13 — $GAP_NUM DONE without ## Acceptance Criteria section. Legacy gaps OK; new gaps should add one"
    else
      UNCHECKED="$(echo "$AC_SECTION" | grep -cE '^- \[ \]' || true)"
      if [ "$UNCHECKED" -gt 0 ] 2>/dev/null; then
        issue_emit fail "DONE flip but $UNCHECKED unchecked AC item(s) remain. Fix: flip to PARTIAL or check items"
      else
        emit_pass "Rule 13.1 — $GAP_NUM AC fully checked"
      fi
    fi

    # ───── 13.2 — Banned phrases in NEW Log entry, excluding §Out-of-scope ─
    # Get only lines added in the diff that fall under a `## Log` line context.
    # Strategy: take diff hunks containing "## Log" or after; concatenate added
    # lines until any heading boundary; scan for banned phrases.
    LOG_DIFF_LINES="$(git diff "$BASE_REF" -- "$gap_file" 2>/dev/null \
      | awk '
          /^@@/ {hunk=1}
          hunk {print}
        ' \
      | awk '
          /^\+## Log/ || /^ ## Log/ {logsec=1; next}
          /^\+##[[:space:]]/ || /^ ##[[:space:]]/ {logsec=0}
          /^\+## Out-of-scope/ || /^ ## Out-of-scope/ {oos=1; next}
          /^\+##[[:space:]]/ || /^ ##[[:space:]]/ {oos=0}
          logsec && !oos && /^\+/ {print}
        ' || true)"

    BANNED_RE='(deferred|defer to|out of scope|manual run|manual capture|infra block|local can.t|WSL2 too slow|chưa boot|partially|to be captured|to be done)'
    BANNED_HITS="$(echo "$LOG_DIFF_LINES" | grep -iE "$BANNED_RE" || true)"

    if [ -n "$BANNED_HITS" ]; then
      # Check follow-up: same diff mentions a different GAP-NNN
      OWN_NUM="${GAP_NUM#GAP-}"
      FOLLOWUP_REF="$(git diff "$BASE_REF" -- "$gap_file" 2>/dev/null \
        | grep -oE 'GAP-[0-9]+' \
        | grep -vE "^GAP-${OWN_NUM}\$" | head -1 || true)"
      FOLLOWUP_PHRASE="$(echo "$LOG_DIFF_LINES" \
        | grep -iE 'follow-up|tracked in|pending GAP-|tracked separately' || true)"

      if [ -n "$FOLLOWUP_REF" ] && [ -n "$FOLLOWUP_PHRASE" ]; then
        emit_warn "Rule 13.2 — $GAP_NUM Log uses banned phrase but references follow-up $FOLLOWUP_REF — accepted (warn for visibility)"
      else
        issue_emit fail "DONE Log entry contains deferred-language without a follow-up gap reference. Fix: flip to PARTIAL or file follow-up gap"
      fi
    else
      emit_pass "Rule 13.2 — $GAP_NUM Log clean (no deferred-language banned phrases)"
    fi

    # ───── 13.3 — Wave-eligible gap closure references all sub-PRs ─────
    if grep -qiE 'wave-eligible|≥3 sub-pr|disjoint sub-tasks' "$gap_file" 2>/dev/null; then
      PR_REFS="$(echo "$LOG_DIFF_LINES" | grep -oE '#[0-9]+' | sort -u | wc -l || true)"
      if [ "$PR_REFS" -lt 3 ] 2>/dev/null; then
        emit_warn "Rule 13.3 — $GAP_NUM is wave-eligible but DONE Log references only $PR_REFS PR(s) (expected ≥3 sub-PRs)"
      else
        emit_pass "Rule 13.3 — $GAP_NUM wave-eligible closure references $PR_REFS sub-PRs"
      fi
    fi

    # ───── 13.4 — Override trailer logging (audit trail) ──────────────
    if [ "$HAS_OVERRIDE" = "1" ]; then
      OVERRIDE_REASON="$(echo "$OVERRIDE_TRAILERS" \
        | grep -E "GAP_DONE_OVERRIDE: $GAP_NUM " | head -1 || true)"
      emit_warn "Rule 13.4 — $GAP_NUM closed with override: $OVERRIDE_REASON. Logged for quarterly audit"
    fi

  done <<< "$DONE_FLIPS"
fi

# ============================================================
# Rule 15 — Wave plan flipped to status:complete → wave-history.jsonl append
# (per .claude/skills/quality/wave-pack-planner/SKILL.md §Rules + this PR's
#  retro 2026-05-04 — Wave 18a/18b1/18b2 missed appends)
# ============================================================
# Trigger: a wave plan file under documents/03-planning/waves/wave-*.md whose
# diff flips frontmatter `status:` from draft|in-progress|planned to complete.
# Required co-change: a NEW line appended to
# .claude/skills/quality/wave-pack-planner/data/wave-history.jsonl that:
#   - parses as JSON (one event per line)
#   - has a `wave` field whose value loosely matches the plan filename slug
WAVE_HISTORY_PATH=".claude/skills/quality/wave-pack-planner/data/wave-history.jsonl"

# Wave plans whose diff flips status to complete
WAVE_FLIPS="$(git diff "$BASE_REF" --name-only 2>/dev/null \
  | grep -E '^documents/03-planning/waves/wave-.+\.md$' \
  | while IFS= read -r wf; do
      [ -z "$wf" ] && continue
      WD="$(git diff "$BASE_REF" -- "$wf" 2>/dev/null || true)"
      # Must add `+status: complete` AND remove `-status: draft|in-progress|planned`
      if echo "$WD" | grep -qE '^\+status:[[:space:]]+complete\b' \
        && echo "$WD" | grep -qE '^-status:[[:space:]]+(draft|in-progress|planned)\b'; then
        echo "$wf"
      fi
    done || true)"

# Wave-history override-trailer collection
WAVE_OVERRIDE_TRAILERS="$(git log "$BASE_REF..HEAD" --pretty=%B 2>/dev/null \
  | grep -E '^WAVE_HISTORY_OVERRIDE: .+' || true)"

if [ -n "$WAVE_FLIPS" ]; then
  # Lines added (starting with `+` but not `+++`) to wave-history.jsonl in this diff
  HISTORY_ADDED="$(git diff "$BASE_REF" -- "$WAVE_HISTORY_PATH" 2>/dev/null \
    | awk '/^\+[^+]/ {sub(/^\+/, ""); print}' || true)"

  while IFS= read -r wave_file; do
    [ -z "$wave_file" ] && continue
    PLAN_SLUG="$(basename "$wave_file" .md)"   # e.g. wave-2026-05-04-foo
    SHORT_SLUG="${PLAN_SLUG#wave-}"             # e.g. 2026-05-04-foo

    HAS_OVERRIDE_W=0
    if [ -n "$WAVE_OVERRIDE_TRAILERS" ]; then
      HAS_OVERRIDE_W=1
    fi

    wave_emit() {
      # $1 = severity (fail|warn), $2 = message
      if [ "$HAS_OVERRIDE_W" = "1" ]; then
        emit_warn "Rule 15 — $PLAN_SLUG (override trailer present): $2"
      elif [ "$1" = "fail" ]; then
        emit_fail "Rule 15 — $PLAN_SLUG: $2"
      else
        emit_warn "Rule 15 — $PLAN_SLUG: $2"
      fi
    }

    if [ -z "$HISTORY_ADDED" ]; then
      wave_emit fail "Wave plan flipped to status:complete but $WAVE_HISTORY_PATH was not appended. Per wave-pack-planner SKILL.md §Rules, every wave closure must record wall-clock + lessons for future wave planning. See incident-to-rule-pipeline.md retro 2026-05-04 Wave 18a/18b1/18b2."
      continue
    fi

    # Try to find a JSON line whose `wave` field references this plan
    MATCH_LINE=""
    BAD_JSON=0
    while IFS= read -r jline; do
      [ -z "$jline" ] && continue
      # Validate JSON parse — prefer python3, fall back to grep heuristic
      if command -v python3 >/dev/null 2>&1; then
        if ! printf '%s\n' "$jline" \
          | python3 -c 'import sys,json; json.loads(sys.stdin.read())' \
          >/dev/null 2>&1; then
          BAD_JSON=1
          continue
        fi
      else
        # Best-effort: must start with { and end with }
        case "$jline" in
          \{*\}) ;;
          *) BAD_JSON=1; continue ;;
        esac
      fi
      # Extract wave field
      WAVE_VAL="$(printf '%s\n' "$jline" \
        | grep -oE '"wave"[[:space:]]*:[[:space:]]*"[^"]*"' \
        | sed -E 's/.*"([^"]*)"$/\1/' || true)"
      if [ -n "$WAVE_VAL" ]; then
        # Loose match: WAVE_VAL contains SHORT_SLUG OR vice versa OR shares date+keyword
        if [ "$WAVE_VAL" = "wave-$SHORT_SLUG" ] \
          || echo "$WAVE_VAL" | grep -qF "$SHORT_SLUG" \
          || echo "$SHORT_SLUG" | grep -qF "$WAVE_VAL"; then
          MATCH_LINE="$jline"
          break
        fi
      fi
    done <<< "$HISTORY_ADDED"

    if [ -n "$MATCH_LINE" ]; then
      emit_pass "Rule 15 — $PLAN_SLUG status:complete + wave-history.jsonl appended (wave=$WAVE_VAL, valid JSON)"
    elif [ "$BAD_JSON" = "1" ]; then
      wave_emit fail "$WAVE_HISTORY_PATH appended but added line(s) failed JSON parse. Fix: ensure single-line valid JSON per existing entries."
    else
      # Lines added but no `wave` field matches plan slug
      wave_emit warn "$WAVE_HISTORY_PATH appended but no entry's \`wave\` field matches plan slug '$SHORT_SLUG'. Verify the appended record references this wave."
    fi
  done <<< "$WAVE_FLIPS"
fi

# ============================================================
# Rule 16 — New wave plan → State-Check Evidence section
# (per .claude/rules/audit-to-gap-pipeline.md §2.6 + GAP-356 5th-recurrence escalation)
# ============================================================
# Trigger: a NEW file under documents/03-planning/waves/wave-*.md (added by this PR).
# Required: file contains "## State-Check Evidence" (or "## 4. State-Check Evidence")
# AND every symbol-shaped reference (BR-*, V[0-9]+__*, Class.field, Class.METHOD) inside
# backticks within `## Scope` / `### Bucket` sections appears in that evidence table.
NEW_WAVE_PLANS="$(git diff "$BASE_REF" --name-status 2>/dev/null \
  | awk '$1 == "A" && $2 ~ /^documents\/03-planning\/waves\/wave-.*\.md$/ {print $2}' || true)"

# Wave-plan-state-check override-trailer collection
WAVE_STATE_OVERRIDES="$(git log "$BASE_REF..HEAD" --pretty=%B 2>/dev/null \
  | grep -E '^STATE_CHECK_OVERRIDE: .+' || true)"

if [ -n "$NEW_WAVE_PLANS" ]; then
  while IFS= read -r plan; do
    [ -z "$plan" ] && continue
    [ ! -f "$plan" ] && continue

    PLAN_BASE="$(basename "$plan" .md)"
    HAS_OVERRIDE_S=0
    if [ -n "$WAVE_STATE_OVERRIDES" ]; then
      HAS_OVERRIDE_S=1
    fi

    state_emit() {
      if [ "$HAS_OVERRIDE_S" = "1" ]; then
        emit_warn "Rule 16 — $PLAN_BASE: STATE_CHECK_OVERRIDE trailer detected — accepted; logged for quarterly retro. ($1)"
      else
        emit_fail "Rule 16 — $PLAN_BASE: $1"
      fi
    }

    # Skip if file is missing Scope section entirely (legacy format → warn only)
    if ! grep -qE '^##[[:space:]]+(3\.[[:space:]]+)?Scope([[:space:]]|$)|^###[[:space:]]+Bucket' "$plan" 2>/dev/null; then
      emit_warn "Rule 16 — $PLAN_BASE: no '## Scope' or '### Bucket' section detected (legacy format). Adopt template from documents/03-planning/waves/_TEMPLATE.md."
      continue
    fi

    # Verify State-Check Evidence section present
    if ! grep -qE '^##[[:space:]]+([0-9]+\.[[:space:]]+)?State-Check Evidence' "$plan" 2>/dev/null; then
      state_emit "Wave plan missing '## State-Check Evidence' section per audit-to-gap-pipeline.md §2.6. Use _TEMPLATE.md State-Check Evidence subsection."
      continue
    fi

    # Extract Scope/Bucket region using awk (no \b — not POSIX-portable)
    SCOPE_BLOCK="$(awk '
      /^##[[:space:]]+([0-9]+\.[[:space:]]+)?Scope([[:space:]]|$)/ { in_scope=1 }
      /^###[[:space:]]+Bucket/ { in_scope=1 }
      /^##[[:space:]]+([0-9]+\.[[:space:]]+)?State-Check Evidence/ { in_scope=0 }
      /^##[[:space:]]+([0-9]+\.[[:space:]]+)?Verification Gates/ { in_scope=0 }
      in_scope { print }
    ' "$plan")"

    EVIDENCE_BLOCK="$(awk '
      /^##[[:space:]]+([0-9]+\.[[:space:]]+)?State-Check Evidence/ { in_ev=1; next }
      /^##[[:space:]]+/ && in_ev { in_ev=0 }
      in_ev { print }
    ' "$plan")"

    # Extract symbols inside backticks in scope block
    # Patterns:
    #   BR-XYZ-NNN
    #   V<digits>__name
    #   ClassName.field (uppercase first char of class, lowercase first char of field)
    #   ClassName.CONSTANT
    SCOPE_SYMBOLS="$(printf '%s\n' "$SCOPE_BLOCK" \
      | grep -oE '`[^`]+`' \
      | sed 's/^`//; s/`$//' \
      | grep -E '^(BR-[A-Z][A-Z0-9-]+-[0-9]+|V[0-9]+__[A-Za-z0-9_]+(\.sql)?|[A-Z][A-Za-z0-9]+\.[a-z][A-Za-z0-9_]*|[A-Z][A-Za-z0-9]+\.[A-Z][A-Z_0-9]+)$' \
      | sort -u || true)"

    if [ -z "$SCOPE_SYMBOLS" ]; then
      emit_pass "Rule 16 — $PLAN_BASE: no symbol-shaped references in §Scope to verify (or none in backticks)"
      continue
    fi

    MISSING_SYMBOLS=""
    PRESENT_COUNT=0
    while IFS= read -r sym; do
      [ -z "$sym" ] && continue
      # Symbol counts as present in evidence if it appears AT ALL inside the evidence block
      if printf '%s\n' "$EVIDENCE_BLOCK" | grep -qF -- "$sym"; then
        PRESENT_COUNT=$((PRESENT_COUNT+1))
      else
        MISSING_SYMBOLS="${MISSING_SYMBOLS}${sym}, "
      fi
    done <<< "$SCOPE_SYMBOLS"

    if [ -n "$MISSING_SYMBOLS" ]; then
      MISSING_SYMBOLS="${MISSING_SYMBOLS%, }"
      state_emit "$(echo "$SCOPE_SYMBOLS" | wc -l | tr -d ' ') symbol(s) in §Scope; missing State-Check Evidence row for: $MISSING_SYMBOLS. Per audit-to-gap-pipeline.md §2.6 every symbol must have ✅ exists or 🆕 to-be-created verdict."
    else
      emit_pass "Rule 16 — $PLAN_BASE: $PRESENT_COUNT symbol(s) in §Scope all have State-Check Evidence rows"
    fi

  done <<< "$NEW_WAVE_PLANS"
fi

# ============================================================
# Rule 17 — Gap status flip → gap-status.csv row sync
# (per .claude/rules/post-merge-sync-completeness.md §2 target 1 +
#  .claude/rules/gap-architecture-v2.md §3 CSV canonical authority)
# ============================================================
# Trigger: diff modifies any `documents/04-quality/gaps/GAP-*.md` file AND adds
# a `+**Status:**` line. Required co-change: `documents/04-quality/gaps/gap-status.csv`
# is ALSO modified in the same diff (the row for that GAP-NNN, but a coarse
# "file touched" check is sufficient — auditor can verify row-level intent).
#
# Override trailer: `POST_MERGE_SYNC_OVERRIDE: GAP-NNN — <reason>` in any commit
# body between BASE_REF..HEAD downgrades FAIL → WARN.
STATUS_FLIPS_R17="$(git diff "$BASE_REF" --name-only 2>/dev/null \
  | grep -E '^documents/04-quality/gaps/GAP-[0-9]+-.+\.md$' \
  | while IFS= read -r gf; do
      [ -z "$gf" ] && continue
      if git diff "$BASE_REF" -- "$gf" 2>/dev/null \
          | grep -qE '^\+\*\*Status:\*\*'; then
        echo "$gf"
      fi
    done || true)"

POST_MERGE_OVERRIDES="$(git log "$BASE_REF..HEAD" --pretty=%B 2>/dev/null \
  | grep -E '^POST_MERGE_SYNC_OVERRIDE: .+' || true)"

CSV_TOUCHED_R17=0
if file_changed "documents/04-quality/gaps/gap-status.csv"; then
  CSV_TOUCHED_R17=1
fi

if [ -n "$STATUS_FLIPS_R17" ]; then
  while IFS= read -r gap_file; do
    [ -z "$gap_file" ] && continue
    GAP_NUM="$(basename "$gap_file" | grep -oE 'GAP-[0-9]+' | head -1)"

    HAS_OVERRIDE_R17=0
    if [ -n "$POST_MERGE_OVERRIDES" ] \
        && echo "$POST_MERGE_OVERRIDES" | grep -qE "POST_MERGE_SYNC_OVERRIDE: $GAP_NUM "; then
      HAS_OVERRIDE_R17=1
    fi

    if [ "$CSV_TOUCHED_R17" = "1" ]; then
      # CSV touched — accept (auditor verifies row-level intent)
      emit_pass "Rule 17 — $GAP_NUM Status flipped + gap-status.csv touched in same diff"
    else
      if [ "$HAS_OVERRIDE_R17" = "1" ]; then
        OVERRIDE_REASON_R17="$(echo "$POST_MERGE_OVERRIDES" \
          | grep -E "POST_MERGE_SYNC_OVERRIDE: $GAP_NUM " | head -1 || true)"
        emit_warn "Rule 17 — $GAP_NUM Status flipped but gap-status.csv NOT updated (override trailer present: $OVERRIDE_REASON_R17). Logged for quarterly audit"
      else
        emit_fail "Rule 17 — $GAP_NUM Status flipped but gap-status.csv NOT updated in same PR. Per post-merge-sync-completeness.md §2 target 1, CSV is canonical — must sync in same diff. Fix: edit documents/04-quality/gaps/gap-status.csv row for $GAP_NUM OR add commit trailer 'POST_MERGE_SYNC_OVERRIDE: $GAP_NUM — <reason>'"
      fi
    fi
  done <<< "$STATUS_FLIPS_R17"
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
