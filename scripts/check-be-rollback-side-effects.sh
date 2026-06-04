#!/usr/bin/env bash
# check-be-rollback-side-effects.sh — pre-walk static check: BE rollback methods
# that rotate secrets/tokens without resurfacing them to the caller
#
# Origin: Wave flow-kh1 G2 walk session 2026-06-04 — GAP-927 surfaced
# rollbackSignup() rotating a secret/token internally with no return path to
# the caller. Retry path then used the old value → silent failure.
#
# Heuristic:
#   Find methods named `rollback*` (public or private void/return) in
#   kitehub/*/src/main/java/**/*.java + kiteclass/*/src/main/java/**/*.java.
#   For each method body, flag if body contains:
#     UUID.randomUUID() / RandomString / SecureRandom / Token.generate*
#     AND any `.set...(...)` (writing a value back to entity/DTO).
#
# This indicates rotating a value without returning it (caller can't see the
# new value → retry blind).
#
# Default: exit 1 if matches (BLOCKING).
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

# Find all .java files in BE source dirs
JAVA_FILES=$(find kitehub kiteclass -path '*/src/main/java/*' -name '*.java' 2>/dev/null || true)

if [ -z "$JAVA_FILES" ]; then
  echo "INFO: no Java source files found — skipping rollback side-effect check"
  exit 0
fi

FOUND=0
TOTAL=0

# Find candidate methods (rollback*)
while IFS= read -r file; do
  [ -z "$file" ] && continue
  # locate method-declaration lines containing rollback name
  decl_lines=$(grep -nE '(public|private|protected|void|[A-Za-z<>]+)[[:space:]]+rollback[A-Za-z0-9_]*\s*\(' "$file" 2>/dev/null || true)
  [ -z "$decl_lines" ] && continue

  while IFS= read -r line_info; do
    [ -z "$line_info" ] && continue
    start_line=${line_info%%:*}

    # awk: from start_line, scan forward, track brace depth, extract body
    body=$(awk -v start="$start_line" '
      NR < start { next }
      {
        for (i=1; i<=length($0); i++) {
          ch = substr($0, i, 1)
          if (ch == "{") {
            depth++
            started = 1
          } else if (ch == "}") {
            depth--
            if (started && depth == 0) {
              print collected
              exit
            }
          }
          if (started) collected = collected ch
        }
        if (started) collected = collected "\n"
      }
    ' "$file")

    [ -z "$body" ] && continue

    # Heuristic: must contain rotation-generator AND .set(...)
    has_rotation=$(echo "$body" | grep -E 'UUID\.randomUUID\(\)|RandomString|SecureRandom|new\s+SecureRandom|Token\.generate|generateToken|generateSecret' || true)
    has_setter=$(echo "$body" | grep -E '\.set[A-Z][A-Za-z0-9_]*\s*\(' || true)

    if [ -n "$has_rotation" ] && [ -n "$has_setter" ]; then
      method_sig=$(sed -n "${start_line}p" "$file")
      TOTAL=$((TOTAL + 1))
      FOUND=1
      echo ""
      echo "❌ $file:$start_line — rollback method rotates value without surfacing to caller"
      echo "    signature: $(echo "$method_sig" | sed 's/^[[:space:]]*//')"
      echo "    rotation: $(echo "$has_rotation" | head -1 | sed 's/^[[:space:]]*//')"
      echo "    setter: $(echo "$has_setter" | head -1 | sed 's/^[[:space:]]*//')"
    fi
  done <<<"$decl_lines"
done <<<"$JAVA_FILES"

echo ""
if [ "$FOUND" -eq 1 ]; then
  echo "FAIL: $TOTAL rollback method(s) rotate secret/token without return path."
  echo "      Per cross-flow-bug-class-sweep.md §3 + GAP-927 lesson —"
  echo "      either surface the rotated value via return type OR remove rotation"
  echo "      from rollback path (rollback should restore prior state, not generate)."
  if [ "$WARN_ONLY" -eq 1 ]; then
    echo "      (--warn mode → exit 0)"
    exit 0
  fi
  exit 1
fi

echo "OK: no rollback methods rotating-without-surfacing detected"
exit 0
