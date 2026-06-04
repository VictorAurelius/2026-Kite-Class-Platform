#!/usr/bin/env bash
# verify-citations.sh — Cross-check body citations vs bibliography entries.
#
# Compares:
#   B = set of cite keys extracted from chapter files (via extract-citations.sh)
#   R = set of entry keys parsed from bibliography.md (lines starting with [N])
#
# Reports 3 buckets:
#   matched     = B ∩ R
#   orphan-body = B \ R   (cite without entry — BROKEN REFERENCE)
#   orphan-bib  = R \ B   (entry without cite — DEAD WEIGHT)
#
# Usage:
#   bash verify-citations.sh <chapter-files...> <bibliography-path>
#
# Last argument must be the bibliography file.
#
# Output:
#   - Human-readable summary to stdout
#   - JSON cache to data/last-run.json (relative to script location)
#
# Exit codes:
#   0 — no orphans (clean)
#   1 — orphan-body found (BROKEN REFERENCE — blocking)
#   2 — usage error OR orphan-bib only (warn, not blocking)
#   3 — script error

set -u

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SKILL_DIR="$(dirname "$SCRIPT_DIR")"
DATA_FILE="$SKILL_DIR/data/last-run.json"
EXTRACT_SCRIPT="$SCRIPT_DIR/extract-citations.sh"

if [ "$#" -lt 2 ]; then
  printf 'Usage: %s <chapter-files...> <bibliography-path>\n' "$0" >&2
  exit 2
fi

# Last arg = bibliography file
BIB_FILE="${!#}"
if [ ! -f "$BIB_FILE" ]; then
  printf 'ERR: bibliography file not found: %s\n' "$BIB_FILE" >&2
  exit 3
fi

# All other args = chapter files
CHAPTER_FILES=()
for ((i=1; i<=$#-1; i++)); do
  CHAPTER_FILES+=("${!i}")
done

if [ "${#CHAPTER_FILES[@]}" -eq 0 ]; then
  printf 'ERR: no chapter files provided\n' >&2
  exit 2
fi

if [ ! -x "$EXTRACT_SCRIPT" ]; then
  printf 'ERR: extract script not executable: %s\n' "$EXTRACT_SCRIPT" >&2
  exit 3
fi

# Extract body cites (unique keys only)
BODY_CITES=$(bash "$EXTRACT_SCRIPT" "${CHAPTER_FILES[@]}" 2>/dev/null \
  | awk -F':' '{print $NF}' \
  | sort -un)

# Extract bibliography entries (lines starting with [N] exactly at column 1)
BIB_ENTRIES=$(awk '
  /^\[[0-9]+\][[:space:]]/ {
    match($0, /^\[([0-9]+)\]/)
    key = substr($0, RSTART + 1, RLENGTH - 2)
    print key + 0
  }
' "$BIB_FILE" | sort -un)

# Compute set diff
MATCHED=$(comm -12 <(echo "$BODY_CITES") <(echo "$BIB_ENTRIES"))
ORPHAN_BODY=$(comm -23 <(echo "$BODY_CITES") <(echo "$BIB_ENTRIES"))
ORPHAN_BIB=$(comm -13 <(echo "$BODY_CITES") <(echo "$BIB_ENTRIES"))

# Counts (avoid empty-string counting as 1)
count_lines() {
  if [ -z "$1" ]; then
    echo 0
  else
    echo "$1" | grep -c '^[0-9]'
  fi
}

N_MATCHED=$(count_lines "$MATCHED")
N_ORPHAN_BODY=$(count_lines "$ORPHAN_BODY")
N_ORPHAN_BIB=$(count_lines "$ORPHAN_BIB")
N_BODY_TOTAL=$(count_lines "$BODY_CITES")
N_BIB_TOTAL=$(count_lines "$BIB_ENTRIES")

# ---------- Output ----------
TS=$(date '+%Y-%m-%d %H:%M:%S')
echo
echo "## Thesis Citation Verify Report ($TS)"
echo
printf 'Chapter files: %d\n' "${#CHAPTER_FILES[@]}"
for cf in "${CHAPTER_FILES[@]}"; do printf '  - %s\n' "$cf"; done
printf 'Bibliography: %s\n' "$BIB_FILE"
echo
printf 'Body unique cite keys:  %d\n' "$N_BODY_TOTAL"
printf 'Bibliography entries:   %d\n' "$N_BIB_TOTAL"
echo
printf 'matched      : %d  (cite + entry both present)\n' "$N_MATCHED"
printf 'orphan-body  : %d  (cite WITHOUT entry — BROKEN REFERENCE)\n' "$N_ORPHAN_BODY"
printf 'orphan-bib   : %d  (entry WITHOUT cite — dead weight)\n' "$N_ORPHAN_BIB"
echo

if [ "$N_ORPHAN_BODY" -gt 0 ]; then
  echo "### orphan-body keys (BROKEN — fix before defense)"
  echo "$ORPHAN_BODY" | sed 's/^/  [/' | sed 's/$/]/'
  echo
fi

if [ "$N_ORPHAN_BIB" -gt 0 ]; then
  echo "### orphan-bib keys (dead weight — clean up or justify)"
  echo "$ORPHAN_BIB" | sed 's/^/  [/' | sed 's/$/]/'
  echo
fi

# ---------- JSON cache ----------
mkdir -p "$(dirname "$DATA_FILE")"

# Build JSON arrays (manual — avoid jq dep)
json_array() {
  local input="$1"
  if [ -z "$input" ]; then
    printf '[]'
    return
  fi
  printf '['
  local first=1
  while IFS= read -r line; do
    [ -z "$line" ] && continue
    if [ "$first" -eq 1 ]; then
      first=0
    else
      printf ', '
    fi
    printf '%s' "$line"
  done <<< "$input"
  printf ']'
}

json_files_array() {
  printf '['
  local first=1
  for cf in "${CHAPTER_FILES[@]}"; do
    if [ "$first" -eq 1 ]; then
      first=0
    else
      printf ', '
    fi
    printf '"%s"' "$cf"
  done
  printf ']'
}

{
  printf '{\n'
  printf '  "timestamp": "%s",\n' "$TS"
  printf '  "bibliography": "%s",\n' "$BIB_FILE"
  printf '  "chapter_files": %s,\n' "$(json_files_array)"
  printf '  "counts": {\n'
  printf '    "body_total": %d,\n' "$N_BODY_TOTAL"
  printf '    "bib_total": %d,\n' "$N_BIB_TOTAL"
  printf '    "matched": %d,\n' "$N_MATCHED"
  printf '    "orphan_body": %d,\n' "$N_ORPHAN_BODY"
  printf '    "orphan_bib": %d\n' "$N_ORPHAN_BIB"
  printf '  },\n'
  printf '  "matched": %s,\n' "$(json_array "$MATCHED")"
  printf '  "orphan_body": %s,\n' "$(json_array "$ORPHAN_BODY")"
  printf '  "orphan_bib": %s\n' "$(json_array "$ORPHAN_BIB")"
  printf '}\n'
} > "$DATA_FILE"

printf 'JSON cache written: %s\n' "$DATA_FILE"
echo

# ---------- Exit ----------
if [ "$N_ORPHAN_BODY" -gt 0 ]; then
  echo "VERDICT: FAIL (orphan-body present — fix broken refs)"
  exit 1
elif [ "$N_ORPHAN_BIB" -gt 0 ]; then
  echo "VERDICT: WARN (orphan-bib present — dead weight; justify or clean)"
  exit 2
else
  echo "VERDICT: PASS (0 orphan — production-ready)"
  exit 0
fi
