#!/usr/bin/env bash
# check-rules-index-sync.sh — drift detection between rule files and rules-index.csv
#
# Per `.claude/rules/meta-csv-index-pattern.md`: CSV is canonical for rule enumeration.
# This script is a WARN-mode drift detector (paired with stricter validator
# `check-rules-index-csv.sh`) — focuses on bi-directional sync visibility:
#   (1) orphan files: `.claude/rules/*.md` lacking a matching CSV row
#   (2) orphan rows:  CSV rows pointing to a missing rule file
#
# Wave 11 Bucket E spec: WARN mode initially (exit 0 even on drift). Wave 12+
# may flip to HARD STOP (exit 1) once drift baseline is clean and detector is
# wired into PR review process.
#
# Self-test scenarios (synthetic):
#   PASS — every .md file (excluding _README/internals) has matching CSV row;
#          every CSV row's `file` column points to existing rule file.
#   FAIL — drop a row from rules-index.csv → script reports orphan file.
#   FAIL — rename a rule file without updating CSV → script reports orphan row.
#
# Exit codes:
#   0 — always (WARN mode); drift reported on stderr; orphan counts in stdout summary
#
# Re-run with `DRIFT_MODE=hard` to force exit 1 on any drift (preview Wave 12+ semantics).

set -euo pipefail

CSV=".claude/rules/rules-index.csv"
RULES_DIR=".claude/rules"
MODE="${DRIFT_MODE:-warn}"

if [[ ! -f "$CSV" ]]; then
  echo "FAIL: $CSV not found" >&2
  exit 1
fi

# Extract `file` column (6th) from CSV using python csv module for robust
# quoted-cell parsing (rules-index.csv uses quoted multi-glob path_trigger).
csv_files=$(python3 - "$CSV" <<'PYEOF'
import csv, sys
with open(sys.argv[1], newline="") as f:
    for row in csv.reader(f):
        if not row or row[0].startswith("#") or row[0] == "name":
            continue
        if len(row) >= 6 and row[0] and row[0][0].islower():
            print(row[5])
PYEOF
)

# Enumerate rule files (excluding README + underscore-prefix internals)
rule_files=$(find "$RULES_DIR" -maxdepth 1 -name '*.md' -type f \
  ! -name 'README.md' ! -name '_*.md' -printf '%f\n' | sort)

csv_files_sorted=$(echo "$csv_files" | sort)

# Orphan files: .md exists but no CSV row references it
orphan_files=$(comm -23 <(echo "$rule_files") <(echo "$csv_files_sorted") || true)

# Orphan rows: CSV row's `file` points to a non-existent .md
orphan_rows=""
while IFS= read -r f; do
  [[ -z "$f" ]] && continue
  if [[ ! -f "$RULES_DIR/$f" ]]; then
    orphan_rows+="$f"$'\n'
  fi
done <<< "$csv_files_sorted"

count_lines() {
  # Count non-empty lines from stdin. Returns 0 if input empty (no trailing
  # newline confusion). Avoids `grep -c .` pitfall with `set -o pipefail`.
  local n
  n=$(printf '%s' "${1:-}" | awk 'NF' | wc -l)
  echo "$n"
}

n_files=$(count_lines "$rule_files")
n_rows=$(count_lines "$csv_files_sorted")
n_orphan_files=$(count_lines "$orphan_files")
n_orphan_rows=$(count_lines "$orphan_rows")
total_drift=$((n_orphan_files + n_orphan_rows))

echo "rules-index sync check: $n_files rule files / $n_rows CSV rows"

if [[ $n_orphan_files -gt 0 ]]; then
  echo "WARN: $n_orphan_files orphan rule file(s) (no CSV row):" >&2
  while IFS= read -r f; do
    [[ -n "$f" ]] && echo "  - $f" >&2
  done <<< "$orphan_files"
fi

if [[ $n_orphan_rows -gt 0 ]]; then
  echo "WARN: $n_orphan_rows orphan CSV row(s) (file missing):" >&2
  while IFS= read -r f; do
    [[ -n "$f" ]] && echo "  - $f" >&2
  done <<< "$orphan_rows"
fi

if [[ $total_drift -eq 0 ]]; then
  echo "PASS: rules-index.csv ↔ rule files in sync"
  exit 0
fi

echo "DRIFT total: $total_drift (orphan files=$n_orphan_files, orphan rows=$n_orphan_rows)" >&2

if [[ "$MODE" == "hard" ]]; then
  echo "FAIL: DRIFT_MODE=hard — exiting non-zero" >&2
  exit 1
fi

# WARN mode default per Wave 11 Bucket E spec
echo "WARN mode: drift reported but exit 0 (flip to hard mode Wave 12+)" >&2
exit 0
