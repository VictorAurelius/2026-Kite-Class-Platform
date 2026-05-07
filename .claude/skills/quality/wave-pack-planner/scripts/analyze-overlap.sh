#!/usr/bin/env bash
# analyze-overlap.sh — File overlap analyzer for wave-pack candidate gaps
#
# Parses N gap files (documents/04-quality/gaps/GAP-*.md) and produces a
# markdown matrix of (file × gap) with conflict-risk classification. Used by
# the `wave-pack-planner` skill (Step 2) to decide whether a candidate gap
# cluster can run as parallel agents or must be re-bucketed / serialized.
#
# Algorithm (high-level):
#   1. Resolve each input arg to a gap file path:
#        - "GAP-121"               → glob documents/04-quality/gaps/GAP-121-*.md
#        - explicit *.md path      → use as-is
#   2. Extract candidate file references from sections that typically list
#      paths: ## Affects, ## Proposed Fix, ## Files, ## Current State, plus
#      bullets across the body that look path-shaped.
#   3. Filter noise: URLs (http(s)://), git refs (HEAD, main, refs/...),
#      sentence fragments (no slash), and tokens not matching a sane path
#      shape (must contain "/" and either end in known extension or be a
#      directory marker).
#   4. Group references by canonical key. Per-file conflict risk:
#        - 1 gap touches it           → None
#        - ≥2 gaps + .md              → SOFT (sections diverge)
#        - ≥2 gaps + values.yaml/.yml → SOFT (Helm/compose section keys)
#        - ≥2 gaps + .sql migration   → HARD (version slot collision)
#        - ≥2 gaps + pom.xml/package.json/pnpm-lock.yaml → SOFT (dep groups)
#        - ≥2 gaps + anything else    → HARD (default to safer)
#      NEW files (don't exist on disk yet) — flagged "(NEW)" but treated as
#      None (no real conflict possible until something exists).
#   5. Output: markdown matrix table + summary + (if HARD) recommendation.
#
# Usage:
#   ./analyze-overlap.sh GAP-121 GAP-143 GAP-144
#   ./analyze-overlap.sh path/to/GAP-foo.md path/to/GAP-bar.md
#   ./analyze-overlap.sh --root=/abs/path GAP-121 GAP-143
#
# Exit codes:
#   0 — no HARD conflicts (wave-pack OK)
#   1 — ≥1 HARD conflict (re-bucket or serialize)
#   2 — script error (missing gap, bad arg, no input)
#
# Tested clean by shellcheck (POSIX bash strict mode).

set -euo pipefail

ROOT=""
GAP_ARGS=()

for arg in "$@"; do
  case "$arg" in
    --root=*) ROOT="${arg#--root=}" ;;
    -h|--help)
      sed -n '2,40p' "$0"
      exit 0
      ;;
    --*)
      printf 'ERR: unknown flag: %s\n' "$arg" >&2
      exit 2
      ;;
    *)
      GAP_ARGS+=("$arg")
      ;;
  esac
done

if [ "${#GAP_ARGS[@]}" -eq 0 ]; then
  printf 'ERR: no gap IDs/paths supplied. Usage: %s GAP-121 GAP-143 ...\n' "$0" >&2
  exit 2
fi

if [ -z "$ROOT" ]; then
  ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
fi

if [ ! -d "$ROOT" ]; then
  printf 'ERR: root %s not a directory\n' "$ROOT" >&2
  exit 2
fi

GAPS_DIR="$ROOT/documents/04-quality/gaps"

# -----------------------------------------------------------------------------
# Resolve each arg → gap file path + short label (GAP-XXX)
# -----------------------------------------------------------------------------

GAP_PATHS=()
GAP_LABELS=()

for raw in "${GAP_ARGS[@]}"; do
  resolved=""
  label=""
  if [ -f "$raw" ]; then
    resolved="$raw"
    base="$(basename "$raw")"
    label="${base%%-*}-${base#*-}"
    label="${label%%-*}"
    # Re-derive nicer label: e.g. GAP-121-per-alert-runbooks.md → GAP-121
    label="$(printf '%s' "$base" | sed -E 's/^(GAP-[0-9]+).*/\1/')"
  elif printf '%s' "$raw" | grep -Eq '^GAP-[0-9]+$'; then
    # Glob (POSIX-bash compatible: use for-loop expansion)
    matches=("$GAPS_DIR/$raw"-*.md)
    if [ ! -e "${matches[0]}" ]; then
      printf 'ERR: no gap file found for %s in %s\n' "$raw" "$GAPS_DIR" >&2
      exit 2
    fi
    resolved="${matches[0]}"
    label="$raw"
  else
    printf 'ERR: arg %s is neither a file nor GAP-XXX\n' "$raw" >&2
    exit 2
  fi
  GAP_PATHS+=("$resolved")
  GAP_LABELS+=("$label")
done

# -----------------------------------------------------------------------------
# Extract candidate path tokens from one gap file
#
# Strategy:
#   - Pull anything inside backticks `...`
#   - Pull anything that looks path-shaped on bullet lines (-, *, |, numbered)
#   - Strip surrounding punctuation, quotes
#   - Filter: must contain "/", must NOT start with http/https/refs, must NOT
#     be obviously a sentence ending in punctuation
# -----------------------------------------------------------------------------

extract_paths() {
  local file="$1"

  # 1. Backtick-quoted tokens (most paths in gap files)
  # 2. Bare path-looking tokens on bullets / table cells
  # 3. Bare canonical filenames (values.yaml, pom.xml, etc.) — these often
  #    appear without their full path; we emit a sentinel "@bare/<name>"
  #    that the post-processor maps onto the qualified path used by some
  #    other gap (if any). This rescues cases where one gap says
  #    `infrastructure/helm/kitehub/values.yaml` and another says
  #    `values.yaml`.

  {
    # shellcheck disable=SC2016  # backticks are literal markdown syntax, not expansion
    grep -oE '`[^`]+`' "$file" 2>/dev/null | sed -E 's/^`//; s/`$//' || true
    # Bare tokens with at least one "/"; restrict to typical char set
    grep -oE '[A-Za-z0-9_./{}*-]+/[A-Za-z0-9_./{}*-]+' "$file" 2>/dev/null || true
    # Bare canonical filenames — emit sentinel form that post-process maps
    grep -oE '\b(values\.yaml|values\.yml|pom\.xml|package\.json|pnpm-lock\.yaml|Chart\.yaml|Dockerfile|docker-compose\.yml|docker-compose\.yaml|Cargo\.toml|go\.mod|requirements\.txt)\b' "$file" 2>/dev/null \
      | sed 's|^|@bare/|' || true
  } | awk '
    {
      # Strip trailing punctuation that often clings to inline mentions
      gsub(/[],.;:)\(\}>"\047]+$/, "", $0)
      gsub(/^[\(\[<"\047]+/, "", $0)
      print
    }
  ' | grep -vE '^(https?://|http://|ftp://)' \
    | grep -vE '^(HEAD|main|master|refs/|origin/)' \
    | grep -vE '^(npm|yarn|pnpm|mvn|gh|git|bash|sh)/' \
    | grep -vE '^[A-Z][a-zA-Z]+ [a-z]' \
    | grep -E '/' \
    | grep -E '\.[a-zA-Z0-9]{1,8}$|/$|\*$' \
    | sort -u
}

# -----------------------------------------------------------------------------
# Risk classifier
#   args: file_token, count (number of gaps touching it)
#   echoes: "None" | "SOFT" | "HARD"
# -----------------------------------------------------------------------------

classify_risk() {
  local f="$1"
  local n="$2"

  if [ "$n" -lt 2 ]; then
    printf 'None'
    return
  fi

  case "$f" in
    *.md)
      printf 'SOFT'
      ;;
    *values.yaml|*values.yml|*docker-compose*.yml|*docker-compose*.yaml)
      printf 'SOFT'
      ;;
    */migration/*.sql|*/migrations/*.sql|*V[0-9]*__*.sql)
      printf 'HARD'
      ;;
    *pom.xml|*package.json|*pnpm-lock.yaml|*Cargo.toml|*go.mod|*requirements.txt)
      printf 'SOFT'
      ;;
    *)
      printf 'HARD'
      ;;
  esac
}

# Returns "(NEW)" suffix if the file does not exist on disk; empty otherwise.
# Strip glob/template chars before checking.
new_marker() {
  local f="$1"
  # Skip tokens with glob/template fragments — assume NEW
  if printf '%s' "$f" | grep -qE '[*{}]'; then
    printf ' (template/glob)'
    return
  fi
  if [ ! -e "$ROOT/$f" ] && [ ! -e "$f" ]; then
    printf ' (NEW)'
  fi
}

# -----------------------------------------------------------------------------
# Build per-gap path lists, then aggregate into file → gap-list map
# -----------------------------------------------------------------------------

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

i=0
while [ "$i" -lt "${#GAP_PATHS[@]}" ]; do
  # Tolerate empty extraction (set -e + grep no-match would otherwise abort)
  extract_paths "${GAP_PATHS[$i]}" > "$TMP_DIR/$i.paths" || true
  i=$((i + 1))
done

# Aggregate: file<TAB>gap-label-list (comma-joined)
awk_input="$TMP_DIR/all"
: > "$awk_input"
i=0
while [ "$i" -lt "${#GAP_PATHS[@]}" ]; do
  label="${GAP_LABELS[$i]}"
  while IFS= read -r line; do
    [ -z "$line" ] && continue
    printf '%s\t%s\n' "$line" "$label" >> "$awk_input"
  done < "$TMP_DIR/$i.paths"
  i=$((i + 1))
done

# Post-process: map @bare/<name> sentinels onto qualified paths if any gap
# uses a fully-qualified path ending in /<name>. Otherwise drop the sentinel
# (bare mention without enough context to disambiguate).
NORMALIZED="$TMP_DIR/normalized"
awk -F'\t' '
  # First pass: collect qualified paths
  NR == FNR {
    if ($1 !~ /^@bare\//) {
      paths[$1] = 1
    }
    lines[NR] = $0
    next
  }
' "$awk_input" "$awk_input" > /dev/null  # warm collect (single-file fallback below)

awk -F'\t' '
  {
    if ($1 ~ /^@bare\//) {
      # Defer; collect for second pass after we know all qualified paths
      bare_lines[++bcount] = $0
      bare_name[bcount] = substr($1, 7)  # strip "@bare/"
    } else {
      print
      seen_qual[$1] = 1
      # Index by basename for sentinel resolution
      n = split($1, parts, "/")
      qual_by_base[parts[n]] = qual_by_base[parts[n]] ? qual_by_base[parts[n]] "\t" $1 : $1
    }
  }
  END {
    for (i = 1; i <= bcount; i++) {
      bn = bare_name[i]
      gap = ""
      split(bare_lines[i], fld, "\t")
      gap = fld[2]
      if (qual_by_base[bn]) {
        m = split(qual_by_base[bn], qs, "\t")
        # Emit gap-attribution against EACH known qualified path with that
        # basename (usually 1, occasionally more if multiple modules share a
        # filename like values.yaml).
        for (j = 1; j <= m; j++) {
          printf "%s\t%s\n", qs[j], gap
        }
      }
      # If no qualified path found, sentinel is dropped silently.
    }
  }
' "$awk_input" > "$NORMALIZED"

awk_input="$NORMALIZED"

# Group: emit "file<TAB>gap1,gap2<TAB>count"
GROUPED="$TMP_DIR/grouped"
awk -F'\t' '
  { count[$1]++; if (gaps[$1]) gaps[$1]=gaps[$1] "," $2; else gaps[$1]=$2 }
  END {
    for (f in count) {
      # Dedupe gap list
      n = split(gaps[f], parts, ",")
      delete seen
      out = ""
      uniq = 0
      for (k = 1; k <= n; k++) {
        if (!(parts[k] in seen)) {
          seen[parts[k]] = 1
          out = (out == "" ? parts[k] : out "," parts[k])
          uniq++
        }
      }
      printf "%s\t%s\t%d\n", f, out, uniq
    }
  }
' "$awk_input" | sort > "$GROUPED"

# -----------------------------------------------------------------------------
# Render markdown matrix
# -----------------------------------------------------------------------------

printf '# File overlap analysis\n\n'
printf 'Gaps analyzed: %s\n\n' "$(IFS=,; printf '%s' "${GAP_LABELS[*]}")"
printf '| File | Touched by | Conflict risk |\n'
printf '|------|-----------|:-------------:|\n'

HARD_COUNT=0
SOFT_COUNT=0
NONE_COUNT=0

while IFS=$'\t' read -r file gap_list count; do
  [ -z "$file" ] && continue
  risk="$(classify_risk "$file" "$count")"
  marker="$(new_marker "$file")"
  case "$risk" in
    HARD)
      HARD_COUNT=$((HARD_COUNT + 1))
      risk_disp='**HARD**'
      ;;
    SOFT)
      SOFT_COUNT=$((SOFT_COUNT + 1))
      risk_disp='*SOFT*'
      ;;
    *)
      NONE_COUNT=$((NONE_COUNT + 1))
      risk_disp='None'
      ;;
  esac
  # shellcheck disable=SC2016  # literal backticks render markdown inline-code
  printf '| `%s`%s | %s | %s |\n' "$file" "$marker" "$gap_list" "$risk_disp"
done < "$GROUPED"

printf '\n## Summary\n\n'
printf -- '- Files only touched by 1 gap (None): %d\n' "$NONE_COUNT"
printf -- '- Files with SOFT conflict risk: %d\n' "$SOFT_COUNT"
printf -- '- Files with HARD conflict risk: %d\n' "$HARD_COUNT"

if [ "$HARD_COUNT" -gt 0 ]; then
  printf '\n## Recommendation\n\n'
  printf 'HARD conflicts detected — wave-pack as currently bucketed will collide.\n'
  printf 'Options:\n'
  printf '1. Re-bucket: move conflicting gap to a different agent or next wave.\n'
  printf '2. Foundation PR: ship the contested file in a precursor PR before agents spawn.\n'
  printf '3. Serialize: drop one gap from this wave, run after the others merge.\n'
  exit 1
fi

if [ "$SOFT_COUNT" -gt 0 ]; then
  printf '\n## Note\n\n'
  printf 'SOFT conflicts present — git usually auto-merges different sections.\n'
  printf 'Coordinator must instruct each agent: edit only your section, do not reformat the whole file.\n'
fi

exit 0
