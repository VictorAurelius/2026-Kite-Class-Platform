#!/usr/bin/env bash
# link-checker.sh — verify <a href> references resolve in kit HTML files.
#
# Scope: every HTML file under documents/02-architecture/design-system/ui_kits/{kit}/
# excluding _shared/ and _v1-baseline/. Resolves relative paths + anchor checks
# of fragments in the same file. SKIPS external http(s):// (slow + flaky) and
# pure anchors (#section-id) without file part.
#
# Usage:
#   bash .claude/skills/quality/ui-review-prototype/scripts/link-checker.sh [--help]
#
# Exit codes:
#   0 — all relative hrefs resolve
#   1 — one or more broken relative hrefs
#   2 — usage / env / file-not-found error
#
# Spec source: `.claude/rules/output-review-mandate.md` v1.3.0 §3 row "HTML/JSX
# prototypes". Tier 2 of `wave-2026-04-29-review-process-improvement.md`.

set -euo pipefail

usage() {
  cat <<'EOF'
link-checker.sh — verify relative <a href> targets exist in kit HTML files.

Usage: bash link-checker.sh [--help]

Exit codes:
  0   all relative hrefs resolve
  1   broken relative hrefs detected (printed to stderr)
  2   environment / setup error

Excludes: _shared/, _v1-baseline/. Skips http(s)://, mailto:, tel:, pure-anchor #foo.
EOF
}

if [[ "${1-}" == "--help" || "${1-}" == "-h" ]]; then
  usage
  exit 0
fi

# ---- Locate paths -----------------------------------------------------------

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SKILL_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../../.." && pwd)"
KITS_DIR="$REPO_ROOT/documents/02-architecture/design-system/ui_kits"
LOG_FILE="$SKILL_DIR/data/runs.log"

if [[ ! -d "$KITS_DIR" ]]; then
  echo "ERROR: ui_kits/ not found at $KITS_DIR" >&2
  exit 2
fi

mkdir -p "$(dirname "$LOG_FILE")"

ts() { date -u '+%Y-%m-%dT%H:%M:%SZ'; }
log_run() {
  local exit_code="$1" summary="$2"
  echo "$(ts) | link-checker.sh | exit=$exit_code | $summary" >> "$LOG_FILE"
}

# ---- Enumerate HTML files ---------------------------------------------------

mapfile -t HTML_FILES < <(
  find "$KITS_DIR" -type f -name '*.html' \
    -not -path "*/_shared/*" \
    -not -path "*/_v1-baseline/*" \
    | sort
)

if [[ ${#HTML_FILES[@]} -eq 0 ]]; then
  echo "ERROR: no HTML files found under $KITS_DIR (excluding _shared, _v1-baseline)" >&2
  log_run 2 "no html files"
  exit 2
fi

# ---- Walk each file, collect broken hrefs -----------------------------------

BROKEN=()
TOTAL_HREFS=0

for f in "${HTML_FILES[@]}"; do
  file_dir="$(dirname "$f")"

  # Extract href values from <a ... href="..."> tags. Tolerate single/double quotes.
  # Use grep -o; fall through gracefully if no matches.
  while IFS= read -r href; do
    [[ -z "$href" ]] && continue
    TOTAL_HREFS=$((TOTAL_HREFS + 1))

    # Skip external + non-http schemes + pure anchors + empty + javascript:
    case "$href" in
      http://*|https://*|//*) continue ;;
      mailto:*|tel:*|javascript:*) continue ;;
      \#*) continue ;;
      "") continue ;;
    esac

    # Strip ?query and #fragment for filesystem check.
    target="${href%%#*}"
    target="${target%%\?*}"

    # Empty target after strip (was pure ?query or #frag) — skip.
    [[ -z "$target" ]] && continue

    # Resolve relative to file's directory.
    if [[ "$target" == /* ]]; then
      # Absolute path inside repo (rare in kits) — resolve from KITS_DIR root.
      candidate="$KITS_DIR$target"
    else
      candidate="$file_dir/$target"
    fi

    # Strip trailing slash (folder reference) — accept folder OR folder/index.html.
    if [[ "$candidate" == */ ]]; then
      base="${candidate%/}"
      if [[ -d "$base" || -f "${base}/index.html" || -f "$base" ]]; then
        continue
      fi
    elif [[ -f "$candidate" || -d "$candidate" ]]; then
      continue
    fi

    rel_file="${f#"$REPO_ROOT/"}"
    BROKEN+=("$rel_file → $href")
  done < <(grep -hoE '<a[[:space:]][^>]*href="[^"]*"' "$f" 2>/dev/null \
            | sed -E 's/.*href="([^"]*)".*/\1/' || true)
done

# ---- Report -----------------------------------------------------------------

echo "Link checker"
echo "  Kits dir: $KITS_DIR"
echo "  HTML files scanned: ${#HTML_FILES[@]}"
echo "  Relative hrefs checked: $((TOTAL_HREFS - 0))"
echo

if [[ ${#BROKEN[@]} -eq 0 ]]; then
  echo "PASS — all relative hrefs resolve."
  log_run 0 "files=${#HTML_FILES[@]} hrefs_seen=$TOTAL_HREFS broken=0"
  exit 0
fi

echo "FAIL — ${#BROKEN[@]} broken relative href(s):" >&2
for b in "${BROKEN[@]}"; do
  echo "  - $b" >&2
done
log_run 1 "files=${#HTML_FILES[@]} hrefs_seen=$TOTAL_HREFS broken=${#BROKEN[@]}"
exit 1
