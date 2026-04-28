#!/usr/bin/env bash
# check-skill-conventions.sh — Skill Conventions Compliance Gate
#
# Validates each .claude/skills/**/SKILL.md and loose .claude/skills/**/*.md
# skill files against `.claude/rules/skill-conventions.md`.
#
# Checks per skill file:
#   1. Has frontmatter with `name` (<=64 chars) AND `description` (<=1024 chars).
#      For loose .md skills `name` is optional (filename used).
#   2. Description contains a trigger-condition keyword
#      (Use when, Dùng khi, When the user, Triggered, Apply when, Khi nào,
#      Auto-trigger, etc.).
#   3. Body has a "## Gotchas" OR "## Anti-patterns" OR "## Common Mistakes"
#      section (project-specific failure points).
#   4. Body length <=500 lines (Anthropic 2026 relaxed limit).
#   5. (Optional WARN) Skills whose folder name ends in "-audit" or contains
#      "-review" should have a `data/eval-fixtures/` directory with >=3 files.
#      Emits WARN, NOT FAIL — turn into FAIL with --strict.
#
# Bonus check (GAP-252 hook):
#   6. Drift between SKILL.md count on disk and rows in
#      `.claude/skills/_README-skills-index.md`. WARN by default.
#
# Usage:
#   ./scripts/check-skill-conventions.sh [--strict] [--root=<path>]
#
# Exit codes:
#   0 — no FAIL checks (or --strict not set)
#   1 — FAIL checks present AND --strict set
#   2 — script error (e.g. not a git repo, root not found)
#
# Self-test fixtures live at:
#   .claude/skills/quality/skill-conventions-check/data/fixtures/
#     good.md, bad-no-gotchas.md, bad-description-style.md
#
# Usage in CI: pair with --strict; locally pair without it for warn-only mode.
#
# Closes GAP-251 + drift-check side of GAP-252 (Wave Meta-Gov 1, Move 2).

set -u

STRICT=0
ROOT=""

for arg in "$@"; do
  case "$arg" in
    --strict) STRICT=1 ;;
    --root=*) ROOT="${arg#--root=}" ;;
    -h|--help)
      sed -n '2,40p' "$0"
      exit 0
      ;;
    *)
      printf 'ERR: unknown arg: %s\n' "$arg" >&2
      exit 2
      ;;
  esac
done

if [ -z "$ROOT" ]; then
  ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
fi

if [ ! -d "$ROOT/.claude/skills" ]; then
  printf 'ERR: %s/.claude/skills not found\n' "$ROOT" >&2
  exit 2
fi

cd "$ROOT" || exit 2

# Colors only when stdout is a TTY
if [ -t 1 ]; then
  C_PASS='\033[32m'
  C_WARN='\033[33m'
  C_FAIL='\033[31m'
  C_BOLD='\033[1m'
  C_RST='\033[0m'
else
  C_PASS=''
  C_WARN=''
  C_FAIL=''
  C_BOLD=''
  C_RST=''
fi

PASSED=0
WARNED=0
FAILED=0
WARN_LINES=""
FAIL_LINES=""

emit_pass() {
  PASSED=$((PASSED + 1))
}

emit_warn() {
  WARNED=$((WARNED + 1))
  WARN_LINES="${WARN_LINES}[WARN]  $1"$'\n'
}

emit_fail() {
  FAILED=$((FAILED + 1))
  FAIL_LINES="${FAIL_LINES}[FAIL]  $1"$'\n'
}

# Trigger-condition keywords (lowercased; we lowercase description before match)
# Captures English ("Use when", "When", "Triggered", "Apply when", "Use this",
# "Use to") and Vietnamese ("Dùng khi", "Dùng trước", "Khi nào", "Trước khi",
# "Auto-trigger") natural-language trigger-style descriptions.
TRIGGER_KEYWORDS='use when|use this|use to|use the |dùng khi|dung khi|dùng trước|dung truoc|dùng cho|dung cho|when the user|when ci|when pr|triggered|trigger when|trigger:|apply when|khi nào|khi nao|trước khi|truoc khi|auto-trigger|auto trigger|invoked when|run when|starts? when|nhắc|nhac '

# Files to skip (fixtures + canonical docs that are not skills themselves)
should_skip() {
  case "$1" in
    .claude/skills/_README-skills-index.md) return 0 ;;
    .claude/skills/quality/skill-conventions-check/data/fixtures/*) return 0 ;;
    *)
      case "$1" in
        */data/eval-fixtures/*) return 0 ;;
        */data/fixtures/*) return 0 ;;
        */reference/*) return 0 ;;
        */quick-reference/*) return 0 ;;
        */assets/*) return 0 ;;
        */scripts/*) return 0 ;;
        *) return 1 ;;
      esac
      ;;
  esac
}

# Grandfathered exemption list — skills that pre-date `skill-conventions.md`
# and missing required sections. Tracked via GAP-252 follow-ups (one cleanup
# gap per skill). Each entry is exempted from check 3 (Gotchas section)
# AND/OR check 2 (trigger keyword) AND/OR check 1 (frontmatter).
#
# Exemption format: `<path>:<which-checks-skipped>` where which-checks-skipped
# is comma-separated of: "frontmatter", "trigger", "gotchas"
#
# Process to remove an exemption: fix the underlying skill, then delete the
# entry. Goal: empty list by Wave 9.
GRANDFATHERED_EXEMPTIONS='
.claude/skills/workflow/development-workflow.md:frontmatter,trigger,gotchas
.claude/skills/workflow/priority-pr-planning.md:frontmatter,trigger,gotchas
'

# Returns 0 (true) if the file is exempt from a specific check.
# Args: $1 = file path, $2 = check name (frontmatter|trigger|gotchas)
is_exempt() {
  local path="$1"
  local check="$2"
  local entry
  entry="$(printf '%s\n' "$GRANDFATHERED_EXEMPTIONS" | grep -F "^$path:" 2>/dev/null || printf '%s\n' "$GRANDFATHERED_EXEMPTIONS" | awk -v p="$path" -F: '$1==p {print $2}')"
  if [ -z "$entry" ]; then
    return 1
  fi
  case ",$entry," in
    *",$check,"*) return 0 ;;
  esac
  return 1
}

# Extract frontmatter block (between first two `---` lines).
# Stdin: file content. Stdout: frontmatter body (without delimiters).
extract_frontmatter() {
  awk '
    BEGIN { state = 0 }
    state == 0 && /^---[[:space:]]*$/ { state = 1; next }
    state == 1 && /^---[[:space:]]*$/ { exit }
    state == 1 { print }
  '
}

# Extract a single field from frontmatter (handles quoted + unquoted + folded
# scalars `>` / `|` minimally — we collapse following indented lines).
extract_field() {
  local field="$1"
  awk -v f="$field" '
    BEGIN { capturing = 0; out = "" }
    {
      if (capturing) {
        # Continuation: indented line with leading spaces
        if (match($0, /^[[:space:]]+[^[:space:]]/)) {
          # Strip leading whitespace
          line = $0
          sub(/^[[:space:]]+/, "", line)
          out = out " " line
          next
        } else {
          # Field ended
          capturing = 0
          print out
          out = ""
          # fallthrough; this line may start a new field — re-enter logic below
        }
      }
      # Match `field: value` or `field: >` or `field: |`
      if (match($0, "^" f ":[[:space:]]*")) {
        rest = substr($0, RLENGTH + 1)
        # Folded/literal scalar markers
        if (rest == ">" || rest == "|" || rest == ">-" || rest == "|-") {
          capturing = 1
          out = ""
          next
        }
        # Strip surrounding quotes
        gsub(/^["'\''[:space:]]+|["'\''[:space:]]+$/, "", rest)
        print rest
        exit
      }
    }
    END { if (capturing) print out }
  '
}

# Body line count (lines after second `---`). If no frontmatter, count all
# lines.
body_line_count() {
  awk '
    BEGIN { state = 0; count = 0 }
    state == 0 {
      if (/^---[[:space:]]*$/) { state = 1; next }
      # No frontmatter on first line => count all
      state = 2
    }
    state == 1 {
      if (/^---[[:space:]]*$/) { state = 2; next }
      next
    }
    state == 2 { count++ }
    END { print count }
  '
}

# Check a single skill file. Args: $1 = path.
check_file() {
  local f="$1"
  local content fm name desc body_lines

  if ! content="$(cat "$f" 2>/dev/null)"; then
    emit_fail "$f: cannot read file"
    return
  fi

  fm="$(printf '%s\n' "$content" | extract_frontmatter)"

  if [ -z "$fm" ]; then
    if is_exempt "$f" "frontmatter"; then
      emit_warn "$f: no YAML frontmatter (EXEMPT — grandfathered, GAP-252 follow-up)"
    else
      emit_fail "$f: no YAML frontmatter (--- block)"
    fi
    return
  fi

  name="$(printf '%s\n' "$fm" | extract_field 'name')"
  desc="$(printf '%s\n' "$fm" | extract_field 'description')"

  # Check 1: name + description present
  # Loose .md skills under quality/, core/, workflow/ are allowed to skip
  # `name` (filename is the implicit name). SKILL.md files require name.
  case "$f" in
    */SKILL.md)
      if [ -z "$name" ]; then
        if is_exempt "$f" "frontmatter"; then
          emit_warn "$f: SKILL.md missing 'name' field (EXEMPT — grandfathered)"
        else
          emit_fail "$f: SKILL.md missing 'name' field in frontmatter"
        fi
        return
      fi
      ;;
  esac

  if [ -z "$desc" ]; then
    if is_exempt "$f" "frontmatter"; then
      emit_warn "$f: missing 'description' field (EXEMPT — grandfathered)"
    else
      emit_fail "$f: missing 'description' field in frontmatter"
    fi
    return
  fi

  # Check 1a: name length
  if [ -n "$name" ]; then
    if [ ${#name} -gt 64 ]; then
      emit_fail "$f: name too long (${#name} chars > 64)"
      return
    fi
  fi

  # Check 1b: description length
  if [ ${#desc} -gt 1024 ]; then
    emit_fail "$f: description too long (${#desc} chars > 1024)"
    return
  fi

  # Check 2: description contains trigger-condition keyword
  desc_lower="$(printf '%s' "$desc" | tr '[:upper:]' '[:lower:]')"
  if ! printf '%s' "$desc_lower" | grep -qE "$TRIGGER_KEYWORDS"; then
    if is_exempt "$f" "trigger"; then
      emit_warn "$f: description has no trigger-condition keyword (EXEMPT — grandfathered)"
    else
      emit_fail "$f: description has no trigger-condition keyword (Use when / Dùng khi / Khi nào / Triggered / Apply when / Auto-trigger / Use this / Use to)"
      return
    fi
  fi

  # Check 3: body has Gotchas | Anti-patterns | Common Mistakes section
  if ! printf '%s\n' "$content" | grep -qE '^## (Gotchas|Anti-patterns|Anti-Patterns|Common Mistakes)'; then
    if is_exempt "$f" "gotchas"; then
      emit_warn "$f: body missing '## Gotchas' section (EXEMPT — grandfathered)"
    else
      emit_fail "$f: body missing '## Gotchas' OR '## Anti-patterns' OR '## Common Mistakes' section"
      return
    fi
  fi

  # Check 4: body line count <= 500
  body_lines="$(printf '%s\n' "$content" | body_line_count)"
  if [ "$body_lines" -gt 500 ]; then
    emit_fail "$f: body too long ($body_lines lines > 500 limit)"
    return
  fi

  # Check 5 (WARN-only): audit/review skill should have eval-fixtures/
  case "$f" in
    */SKILL.md)
      dir="$(dirname "$f")"
      base="$(basename "$dir")"
      case "$base" in
        *-audit|*-review|*-check)
          if [ ! -d "$dir/data/eval-fixtures" ]; then
            emit_warn "$f: audit/review skill missing data/eval-fixtures/ directory (per GAP-253 best-practice)"
          else
            fixture_count="$(find "$dir/data/eval-fixtures" -maxdepth 1 -type f -name '*.md' 2>/dev/null | wc -l | tr -d ' ')"
            if [ "$fixture_count" -lt 3 ]; then
              emit_warn "$f: data/eval-fixtures/ has $fixture_count fixtures (need >=3 per GAP-253)"
            fi
          fi
          ;;
      esac
      ;;
  esac

  emit_pass
}

# Drift check (GAP-252). Compare SKILL.md count on disk vs rows in index.
drift_check() {
  local index=".claude/skills/_README-skills-index.md"
  if [ ! -f "$index" ]; then
    emit_warn "$index: missing — cannot run drift detector"
    return
  fi

  # Count SKILL.md files
  local on_disk
  on_disk="$(find .claude/skills -name 'SKILL.md' -type f 2>/dev/null | wc -l | tr -d ' ')"

  # Count rows in index that look like skill rows: `path/SKILL.md` OR
  # `path.md` mention. Conservative: count lines containing 'SKILL.md'.
  local in_index
  in_index="$(grep -cE 'SKILL\.md' "$index" 2>/dev/null || echo 0)"

  if [ "$on_disk" -ne "$in_index" ]; then
    emit_warn "skills-index drift: $on_disk SKILL.md on disk vs $in_index references in $index (GAP-252)"
  fi
}

# Discover skill files. SKILL.md files are always skills. Loose .md under
# core/, quality/ (root), workflow/ (root) are also skills if they have
# frontmatter starting with `---`.
discover_skills() {
  # All SKILL.md
  find .claude/skills -name 'SKILL.md' -type f 2>/dev/null | sort

  # Loose .md files in known skill folders (not in subdirectories like
  # reference/, scripts/, etc.)
  find .claude/skills/core -maxdepth 1 -type f -name '*.md' 2>/dev/null | sort
  find .claude/skills/quality -maxdepth 1 -type f -name '*.md' 2>/dev/null | sort
  find .claude/skills/workflow -maxdepth 1 -type f -name '*.md' 2>/dev/null | sort
}

# ---------- Main ----------

TS="$(date '+%Y-%m-%d %H:%M')"
printf '%b\n' "${C_BOLD}## Skill Conventions Check ($TS)${C_RST}"
printf '%b\n' "Root: $ROOT"
echo

SKILL_FILES="$(discover_skills | sort -u)"

while IFS= read -r f; do
  [ -z "$f" ] && continue
  if should_skip "$f"; then
    continue
  fi
  check_file "$f"
done <<EOF
$SKILL_FILES
EOF

drift_check

# Summary
TOTAL=$((PASSED + WARNED + FAILED))
printf '%b\n' "${C_BOLD}--- Summary ---${C_RST}"
printf '%b\n' "${C_PASS}PASS:${C_RST}  $PASSED"
printf '%b\n' "${C_WARN}WARN:${C_RST}  $WARNED"
printf '%b\n' "${C_FAIL}FAIL:${C_RST}  $FAILED"
printf 'Total: %d skill files checked\n' "$TOTAL"
echo

if [ "$WARNED" -gt 0 ]; then
  printf '%b\n' "${C_BOLD}Warnings:${C_RST}"
  printf '%b' "$WARN_LINES"
  echo
fi

if [ "$FAILED" -gt 0 ]; then
  printf '%b\n' "${C_BOLD}Failures:${C_RST}"
  printf '%b' "$FAIL_LINES"
  echo
fi

# Exit logic
if [ "$STRICT" -eq 1 ]; then
  if [ "$FAILED" -gt 0 ] || [ "$WARNED" -gt 0 ]; then
    exit 1
  fi
else
  if [ "$FAILED" -gt 0 ]; then
    exit 1
  fi
fi

exit 0
