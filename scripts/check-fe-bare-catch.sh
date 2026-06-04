#!/usr/bin/env bash
# check-fe-bare-catch.sh — pre-walk static check: FE auth forms with bare `} catch {`
#
# Origin: Wave flow-kh1 G2 walk session 2026-06-04 — GAP-924 + GAP-926 surfaced
# bare-catch blocks in auth forms (`} catch {` with no error parameter) that
# masked real error semantics, causing silent 401s + ambiguous failure UI.
# Per cross-flow-bug-class-sweep.md §3 — each bug class must be grep-swept.
#
# Scope: kitehub-frontend auth components + (auth) route group.
#   kitehub/kitehub-frontend/src/components/auth/**/*.tsx
#   kitehub/kitehub-frontend/src/app/(auth)/**/*.tsx
#
# Pattern: `} catch {` (bare — no `(err)` parameter). `} catch (err) {` is OK.
# Excludes: __tests__/ + *.test.tsx + *.spec.tsx
#
# Default: exit 1 if matches found (BLOCKING for fix sites).
# --warn: exit 0 + WARN (advisory).
#
# Per pre-walk-static-audit-bundle.md §3 (this rule landing same PR).

set -euo pipefail

WARN_ONLY=0
for arg in "$@"; do
  case "$arg" in
    --warn) WARN_ONLY=1 ;;
    -h|--help)
      echo "Usage: $0 [--warn]"
      echo "  --warn   exit 0 even if matches (default: exit 1)"
      exit 0
      ;;
  esac
done

ROOTS=(
  "kitehub/kitehub-frontend/src/components/auth"
  "kitehub/kitehub-frontend/src/app/(auth)"
)

FOUND=0
TOTAL=0

for root in "${ROOTS[@]}"; do
  [ -d "$root" ] || continue

  # find .tsx files, exclude tests
  while IFS= read -r file; do
    [ -z "$file" ] && continue
    case "$file" in
      *__tests__*|*.test.tsx|*.spec.tsx) continue ;;
    esac

    # bare catch: `} catch {` (no parameter) — exclude `} catch (` form
    matches=$(grep -nE '\} *catch *\{' "$file" 2>/dev/null || true)
    [ -z "$matches" ] && continue

    while IFS= read -r m; do
      [ -z "$m" ] && continue
      line=${m%%:*}
      TOTAL=$((TOTAL + 1))
      FOUND=1
      echo ""
      echo "❌ $file:$line — bare catch block (masks error semantics)"
      # 3-line context window (line-1, line, line+1)
      start=$((line - 1))
      [ "$start" -lt 1 ] && start=1
      end=$((line + 1))
      sed -n "${start},${end}p" "$file" 2>/dev/null | sed 's/^/    /'
    done <<<"$matches"
  done < <(find "$root" -type f -name '*.tsx' 2>/dev/null)
done

echo ""
if [ "$FOUND" -eq 1 ]; then
  echo "FAIL: $TOTAL bare-catch site(s) in FE auth components."
  echo "      Per cross-flow-bug-class-sweep.md §3 — replace with"
  echo "      \`} catch (err) {\` + map per-status response to user-facing semantic."
  if [ "$WARN_ONLY" -eq 1 ]; then
    echo "      (--warn mode → exit 0)"
    exit 0
  fi
  exit 1
fi

echo "OK: no bare-catch blocks in FE auth components"
exit 0
