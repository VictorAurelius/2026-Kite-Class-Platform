#!/usr/bin/env bash
# audit-figures.sh — Walk thesis chapter MD files, audit figure curation.
#
# Detects 3 figure types:
#   1. Markdown image:   ![alt](path/to/image.png)
#   2. Mermaid block:    ```mermaid ... ```
#   3. Hình N.M caption: **Hình N.M: <desc>** OR Hình N.M citation in body
#
# Plus detects:
#   - Bảng N.M (tables)
#   - Listing N.M (code listings)
#   - Caption presence per figure number
#   - Numbering sequence integrity (1.1, 1.2, 1.3 — no gaps)
#   - Citation in body within ±3 paragraphs (heuristic)
#
# Usage:
#   ./audit-figures.sh <file-glob-or-paths>
#   ./audit-figures.sh --json <file-glob-or-paths>
#   ./audit-figures.sh --help
#
# Examples:
#   ./audit-figures.sh documents/08-thesis/chapter-1-*.md
#   ./audit-figures.sh --json documents/08-thesis/chapter-2-*.md > data/last-run-chapter-2.json
#
# Exit codes:
#   0 — audit ran (warnings reported, not blocking)
#   2 — usage error

set -u

JSON_MODE=0
FILES=()

usage() {
  sed -n '2,30p' "$0"
  exit 0
}

for arg in "$@"; do
  case "$arg" in
    -h|--help) usage ;;
    --json) JSON_MODE=1 ;;
    -*)
      printf 'ERR: unknown flag: %s\n' "$arg" >&2
      exit 2
      ;;
    *)
      # Skip backup files
      case "$arg" in
        *-backup-*.md) continue ;;
      esac
      FILES+=("$arg")
      ;;
  esac
done

if [ "${#FILES[@]}" -eq 0 ]; then
  printf 'ERR: no input files provided\n' >&2
  printf 'Usage: %s [--json] <file...>\n' "$0" >&2
  exit 2
fi

# Per-chapter accumulators (associative arrays would be nicer; use parallel arrays for bash 3 compat)
# We process each file as one chapter slice.

emit_json_start() {
  printf '{\n'
  printf '  "audit_timestamp": "%s",\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  printf '  "files": [\n'
}

emit_json_end() {
  printf '\n  ]\n'
  printf '}\n'
}

# Counter for json comma separation
FILE_INDEX=0

if [ "$JSON_MODE" -eq 1 ]; then
  emit_json_start
fi

for f in "${FILES[@]}"; do
  if [ ! -f "$f" ]; then
    if [ "$JSON_MODE" -eq 1 ]; then
      [ "$FILE_INDEX" -gt 0 ] && printf ',\n'
      printf '    {"file": "%s", "error": "file not found"}' "$f"
      FILE_INDEX=$((FILE_INDEX + 1))
    else
      printf '⚠️  %s — file not found\n' "$f" >&2
    fi
    continue
  fi

  # Extract numbers: figures (Hình N.M), tables (Bảng N.M), listings (Listing N.M)
  # Caption pattern: **Hình N.M: ...** OR **Hình N.M:** (bold wrapper)
  # Citation pattern: any mention of "Hình N.M" not inside bold wrapper

  # Count Markdown images
  IMG_COUNT=$(grep -cE '!\[[^]]*\]\([^)]+\)' "$f" 2>/dev/null)
  IMG_COUNT=${IMG_COUNT:-0}

  # Count Mermaid blocks (opening fence)
  MERMAID_COUNT=$(grep -cE '^[[:space:]]*```mermaid' "$f" 2>/dev/null)
  MERMAID_COUNT=${MERMAID_COUNT:-0}

  # Count PlantUML blocks
  PLANTUML_COUNT=$(grep -cE '^[[:space:]]*```plantuml' "$f" 2>/dev/null)
  PLANTUML_COUNT=${PLANTUML_COUNT:-0}

  # Extract all Hình N.M captions (bold wrapper, with description)
  # Match: **Hình N.M: ...** or **Hình N.M:** (allow optional period before colon)
  FIGURE_CAPTIONS=$(grep -oE '\*\*Hình [0-9]+\.[0-9]+:[^*]+\*\*' "$f" 2>/dev/null || true)
  if [ -z "$FIGURE_CAPTIONS" ]; then
    FIGURE_CAPTION_COUNT=0
  else
    FIGURE_CAPTION_COUNT=$(printf '%s\n' "$FIGURE_CAPTIONS" | sort -u | wc -l | tr -d ' ')
    FIGURE_CAPTIONS=$(printf '%s\n' "$FIGURE_CAPTIONS" | sort -u)
  fi

  # Extract all unique figure numbers from captions
  if [ -z "$FIGURE_CAPTIONS" ]; then
    FIGURE_NUMBERS=""
  else
    FIGURE_NUMBERS=$(printf '%s\n' "$FIGURE_CAPTIONS" | grep -oE 'Hình [0-9]+\.[0-9]+' | awk '{print $2}' | sort -u -V)
  fi

  # Extract all Bảng N.M captions
  TABLE_CAPTIONS=$(grep -oE '\*\*Bảng [0-9]+\.[0-9]+:[^*]+\*\*' "$f" 2>/dev/null || true)
  if [ -z "$TABLE_CAPTIONS" ]; then
    TABLE_CAPTION_COUNT=0
    TABLE_NUMBERS=""
  else
    TABLE_CAPTION_COUNT=$(printf '%s\n' "$TABLE_CAPTIONS" | sort -u | wc -l | tr -d ' ')
    TABLE_NUMBERS=$(printf '%s\n' "$TABLE_CAPTIONS" | grep -oE 'Bảng [0-9]+\.[0-9]+' | awk '{print $2}' | sort -u -V)
  fi

  # Extract all Listing N.M captions
  LISTING_CAPTIONS=$(grep -oE '\*\*Listing [0-9]+\.[0-9]+:[^*]+\*\*' "$f" 2>/dev/null || true)
  if [ -z "$LISTING_CAPTIONS" ]; then
    LISTING_CAPTION_COUNT=0
  else
    LISTING_CAPTION_COUNT=$(printf '%s\n' "$LISTING_CAPTIONS" | sort -u | wc -l | tr -d ' ')
  fi

  # Citations: count distinct figure number mentions in body (any Hình N.M NOT part of caption)
  # Heuristic: grep for "Hình N.M" outside bold wrapper
  CITATION_NUMBERS=$(grep -oE 'Hình [0-9]+\.[0-9]+' "$f" 2>/dev/null | awk '{print $2}' | sort -u -V)
  CITATION_NUMBERS="${CITATION_NUMBERS:-}"

  # Numbering integrity check: detect gaps
  NUMBERING_GAPS=""
  if [ -n "$FIGURE_NUMBERS" ]; then
    # Group by chapter, check each
    for chapter in $(printf '%s\n' "$FIGURE_NUMBERS" | awk -F. '{print $1}' | sort -u -n); do
      seqs=$(printf '%s\n' "$FIGURE_NUMBERS" | awk -F. -v c="$chapter" '$1==c {print $2}' | sort -u -n)
      prev=0
      for n in $seqs; do
        if [ "$n" -ne $((prev + 1)) ]; then
          if [ "$prev" -eq 0 ] && [ "$n" -ne 1 ]; then
            NUMBERING_GAPS="${NUMBERING_GAPS}chapter ${chapter} starts at ${n} (expected 1); "
          elif [ "$n" -gt $((prev + 1)) ]; then
            NUMBERING_GAPS="${NUMBERING_GAPS}chapter ${chapter} jumps ${prev}→${n}; "
          fi
        fi
        prev=$n
      done
    done
  fi

  # Citations not present in captions = figures cited but not captioned (or extra citations)
  # Captions not in citations = figures captioned but not referenced in body
  UNCITED_FIGURES=""
  if [ -n "$FIGURE_NUMBERS" ]; then
    for fig in $FIGURE_NUMBERS; do
      # Count occurrences of "Hình N.M" — should be ≥2 (1 caption + ≥1 citation)
      total=$(grep -cE "Hình ${fig}([^0-9]|$)" "$f" 2>/dev/null)
      total=${total:-0}
      if [ "$total" -lt 2 ]; then
        UNCITED_FIGURES="${UNCITED_FIGURES}${fig} "
      fi
    done
  fi

  # Compute caption coverage %
  TOTAL_VISUAL=$((IMG_COUNT + MERMAID_COUNT + PLANTUML_COUNT))
  if [ "$TOTAL_VISUAL" -gt 0 ]; then
    CAPTION_COVERAGE=$(( FIGURE_CAPTION_COUNT * 100 / TOTAL_VISUAL ))
  else
    CAPTION_COVERAGE=100
  fi

  if [ "$JSON_MODE" -eq 1 ]; then
    [ "$FILE_INDEX" -gt 0 ] && printf ',\n'
    printf '    {\n'
    printf '      "file": "%s",\n' "$f"
    printf '      "visual_blocks": {\n'
    printf '        "markdown_images": %s,\n' "$IMG_COUNT"
    printf '        "mermaid_blocks": %s,\n' "$MERMAID_COUNT"
    printf '        "plantuml_blocks": %s,\n' "$PLANTUML_COUNT"
    printf '        "total": %s\n' "$TOTAL_VISUAL"
    printf '      },\n'
    printf '      "figures": {\n'
    printf '        "captioned_count": %s,\n' "$FIGURE_CAPTION_COUNT"
    printf '        "numbers": [%s],\n' "$(printf '%s\n' "$FIGURE_NUMBERS" | awk 'NF{printf "\"%s\",",$0}' | sed 's/,$//')"
    printf '        "caption_coverage_pct": %s,\n' "$CAPTION_COVERAGE"
    printf '        "uncited_figures": "%s"\n' "$(printf '%s' "$UNCITED_FIGURES" | sed 's/[[:space:]]*$//')"
    printf '      },\n'
    printf '      "tables": {\n'
    printf '        "captioned_count": %s,\n' "$TABLE_CAPTION_COUNT"
    printf '        "numbers": [%s]\n' "$(printf '%s\n' "$TABLE_NUMBERS" | awk 'NF{printf "\"%s\",",$0}' | sed 's/,$//')"
    printf '      },\n'
    printf '      "listings": {\n'
    printf '        "captioned_count": %s\n' "$LISTING_CAPTION_COUNT"
    printf '      },\n'
    printf '      "numbering_gaps": "%s"\n' "$(printf '%s' "$NUMBERING_GAPS" | sed 's/[[:space:]]*$//')"
    printf '    }'
    FILE_INDEX=$((FILE_INDEX + 1))
  else
    # Human-readable output
    printf '\n📄 %s\n' "$f"
    printf '   Visual blocks: %s (image: %s, mermaid: %s, plantuml: %s)\n' \
      "$TOTAL_VISUAL" "$IMG_COUNT" "$MERMAID_COUNT" "$PLANTUML_COUNT"
    printf '   Figures captioned: %s/%s (%s%% coverage)\n' \
      "$FIGURE_CAPTION_COUNT" "$TOTAL_VISUAL" "$CAPTION_COVERAGE"
    if [ -n "$FIGURE_NUMBERS" ]; then
      printf '   Figure numbers: %s\n' "$(printf '%s\n' "$FIGURE_NUMBERS" | tr '\n' ' ')"
    fi
    if [ "$TABLE_CAPTION_COUNT" -gt 0 ]; then
      printf '   Tables: %s captioned (%s)\n' \
        "$TABLE_CAPTION_COUNT" "$(printf '%s\n' "$TABLE_NUMBERS" | tr '\n' ' ')"
    fi
    if [ "$LISTING_CAPTION_COUNT" -gt 0 ]; then
      printf '   Listings: %s captioned\n' "$LISTING_CAPTION_COUNT"
    fi
    if [ -n "$NUMBERING_GAPS" ]; then
      printf '   ⚠️  Numbering gaps: %s\n' "$NUMBERING_GAPS"
    fi
    if [ -n "$UNCITED_FIGURES" ]; then
      printf '   ⚠️  Figures with no body citation: %s\n' "$UNCITED_FIGURES"
    fi
    if [ "$TOTAL_VISUAL" -gt "$FIGURE_CAPTION_COUNT" ]; then
      missing=$((TOTAL_VISUAL - FIGURE_CAPTION_COUNT))
      printf '   ⚠️  %s visual block(s) missing caption\n' "$missing"
    fi
  fi
done

if [ "$JSON_MODE" -eq 1 ]; then
  emit_json_end
fi

exit 0
