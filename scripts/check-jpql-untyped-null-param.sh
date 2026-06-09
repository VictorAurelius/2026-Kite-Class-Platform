#!/usr/bin/env bash
# check-jpql-untyped-null-param.sh — detect the `(:param IS NULL OR ...)` JPQL
# optional-filter idiom in @Query strings that triggers Postgres 42P18
# ("could not determine data type of parameter") at PREPARE time.
#
# Why it breaks: Hibernate expands a named param used twice into two positional
# placeholders; the `? IS NULL` occurrence carries no type context, so Postgres
# rejects an untyped null bind. H2 (test) silently accepts it as VARCHAR, so the
# bug is invisible in H2 IT + Mockito unit tests and only surfaces on production
# Postgres.
#
# Fix pattern (GAP-1106 / GAP-1028): split into a no-cursor query + a typed-param
# query and branch on null in a repository default method — no untyped null is
# ever bound.
#
# Origin: GAP-1106 (subscription cursor 42P18 sweep) — sister of GAP-1028
# (AdminAuditLogRepository) + GAP-1105 (branding lifecycle-events).
# Per .claude/rules/cross-flow-bug-class-sweep.md §4.1 (statically-detectable
# class → persistent detector) + postgres-specific-type-testcontainers.md.
#
# Scope: kitehub/*/src/main/java + kiteclass/*/src/main/java (*.java).
# Comment lines (javadoc `*`, `//`, `/*`) are excluded — they describe the
# anti-pattern, they are not live JPQL.
#
# Default: WARN-mode v1 (exit 0, lists hits). --strict: exit 1 if matches found.

set -euo pipefail

STRICT=0
for arg in "$@"; do
  case "$arg" in
    --strict) STRICT=1 ;;
    --warn) STRICT=0 ;;
    -h|--help)
      echo "Usage: $0 [--strict|--warn]"
      echo "  --warn    (default) exit 0 even if matches found"
      echo "  --strict  exit 1 if matches found"
      exit 0
      ;;
  esac
done

ROOTS=()
for d in kitehub/*/src/main/java kiteclass/*/src/main/java; do
  [ -d "$d" ] && ROOTS+=("$d")
done

FOUND=0
TOTAL=0

for root in "${ROOTS[@]}"; do
  while IFS= read -r file; do
    [ -z "$file" ] && continue
    # `:word IS NULL OR` (case-insensitive keyword run). Single match per line is
    # enough to flag the line.
    matches=$(grep -nEi ":[a-zA-Z_][a-zA-Z0-9_]* +is +null +or" "$file" 2>/dev/null || true)
    [ -z "$matches" ] && continue
    while IFS= read -r m; do
      [ -z "$m" ] && continue
      line=${m%%:*}
      content=${m#*:}
      # Trim leading whitespace (bash-only, no subprocess).
      trimmed=${content#"${content%%[![:space:]]*}"}
      # Skip comment lines (javadoc `*`, line `//`, block `/*`).
      case "$trimmed" in
        \**|//*|/\**) continue ;;
      esac
      TOTAL=$((TOTAL + 1))
      FOUND=1
      echo ""
      echo "WARN ${file}:${line} — ':param IS NULL OR ...' JPQL (Postgres 42P18 risk)"
      printf '    %s\n' "$trimmed"
    done <<<"$matches"
  done < <(find "$root" -type f -name '*.java' 2>/dev/null)
done

echo ""
if [ "$FOUND" -eq 1 ]; then
  echo "WARN: ${TOTAL} ':param IS NULL OR' site(s) in @Query JPQL."
  echo "      These bind an untyped null in the IS NULL position -> Postgres 42P18"
  echo "      at PREPARE time (H2 hides it). Split into a no-param query + a"
  echo "      typed-param query and branch on null (see GAP-1106 / GAP-1028 fix)."
  echo "      Sister rule: cross-flow-bug-class-sweep.md §4.1 +"
  echo "      postgres-specific-type-testcontainers.md."
  if [ "$STRICT" -eq 1 ]; then
    exit 1
  fi
  exit 0
fi

echo "OK: no ':param IS NULL OR' JPQL sites found"
exit 0
