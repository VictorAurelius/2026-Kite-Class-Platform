#!/usr/bin/env bash
# extract-citations.sh — Parse IEEE [N] citations from thesis chapter markdown.
#
# Output format: filename:line:cite_key (one cite per line, deduplicated per file)
# Handles:
#   [N]              -> single key N
#   [N, M]           -> two keys N, M (also [N,M] / [N , M])
#   [N]–[M]          -> range N..M expanded (em-dash U+2013)
#   [N]-[M]          -> range N..M expanded (hyphen fallback)
#   Combined [1, 3]–[5] processed as: 1, 3, then range 3..5
#
# Skips:
#   Code blocks (between ``` fences)
#   Markdown link syntax [text](url) where bracket content is non-numeric
#
# Usage:
#   bash extract-citations.sh <chapter-file...>
#   bash extract-citations.sh documents/<thesis-dir>/chapter-1-*.md
#
# Exit codes:
#   0 — success (output on stdout, may be empty if no cites found)
#   2 — usage error (no input files)

set -u

if [ "$#" -lt 1 ]; then
  printf 'Usage: %s <chapter-file...>\n' "$0" >&2
  exit 2
fi

# Process each file
for f in "$@"; do
  if [ ! -f "$f" ]; then
    printf 'WARN: file not found: %s\n' "$f" >&2
    continue
  fi

  # awk: walk file, skip code fences, extract [N] / [N,M] / [N]-[M] patterns
  # Emit one line per cite key: filename:line:N
  awk -v filename="$f" '
    BEGIN { in_code = 0 }

    # Toggle code fence state
    /^```/ {
      in_code = !in_code
      next
    }

    # Skip lines inside code fences
    in_code { next }

    {
      line = $0
      lineno = NR

      # Find all bracket patterns [...]
      # We use match() loop because we want positional control
      pos = 1
      while (match(substr(line, pos), /\[[^]]+\]/)) {
        start = pos + RSTART - 1
        len = RLENGTH
        bracket = substr(line, start, len)
        inner = substr(bracket, 2, len - 2)

        # Check what comes immediately after this bracket: if `(`, it is a markdown link, skip
        next_char = substr(line, start + len, 1)
        if (next_char == "(") {
          pos = start + len
          continue
        }

        # Process inner content
        # Trim leading/trailing whitespace
        gsub(/^[[:space:]]+|[[:space:]]+$/, "", inner)

        # Inner must be all digits, optionally comma-separated
        # Patterns accepted: "5"  "5, 7"  "5,7"  "1, 2, 3"
        if (match(inner, /^[0-9]+([[:space:]]*,[[:space:]]*[0-9]+)*$/)) {
          # Split by comma
          n = split(inner, parts, /[[:space:]]*,[[:space:]]*/)
          for (i = 1; i <= n; i++) {
            key = parts[i] + 0  # numeric coerce
            print filename ":" lineno ":" key
          }

          # After emitting, check for range continuation: ]–[M] or ]-[M]
          # Look at chars after this bracket
          tail = substr(line, start + len)
          # Match en-dash or hyphen followed by [M]
          if (match(tail, /^[[:space:]]*[–-][[:space:]]*\[([0-9]+)\]/)) {
            range_end_str = substr(tail, RSTART, RLENGTH)
            if (match(range_end_str, /\[([0-9]+)\]/)) {
              end_key = substr(range_end_str, RSTART + 1, RLENGTH - 2) + 0
              # Range: from last emitted key of this bracket to end_key
              start_key = parts[n] + 0
              if (end_key > start_key) {
                for (k = start_key + 1; k <= end_key; k++) {
                  print filename ":" lineno ":" k
                }
              }
              # Skip the range-end bracket to avoid double-emit
              pos = start + len + RLENGTH
              continue
            }
          }
        }

        pos = start + len
      }
    }
  ' "$f"
done

exit 0
