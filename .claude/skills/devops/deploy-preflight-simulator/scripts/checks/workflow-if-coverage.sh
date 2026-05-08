#!/usr/bin/env bash
# Check #6 — Workflow if: condition coverage
# Standard: GitHub Actions Security Hardening Guide §"Conditional execution"
#
# For every `if:` condition in .github/workflows/*.yml that references vars.<X> or secrets.<X>:
#   - Extract the var/secret name + workflow file + line + full condition
#   - Warn when the condition uses an unconventional pattern (e.g. `vars.X != ''`
#     instead of `vars.X == 'true'`) — these silently skip jobs when var unset.
#   - Warn when the var/secret name does not follow ALL_CAPS_SNAKE_CASE naming.
#
# NOTE: actual var/secret existence on the GitHub repo is Category #7's job.
# This check enumerates conditions and flags style/scoping anti-patterns.
set -uo pipefail

WORKFLOWS_DIR=".github/workflows"
[[ -d "$WORKFLOWS_DIR" ]] || { echo "  └─ $WORKFLOWS_DIR not found (skip)"; exit 0; }

shopt -s nullglob
WORKFLOWS=( "${WORKFLOWS_DIR}"/*.yml )
shopt -u nullglob
if [[ ${#WORKFLOWS[@]} -eq 0 ]]; then
  echo "  └─ no workflows in $WORKFLOWS_DIR (skip)"
  exit 0
fi

WARN_ISSUES=0
TOTAL_REFS=0

# Approved condition patterns:
#   vars.X == 'true'              (canonical boolean opt-in)
#   vars.X == 'false'             (canonical boolean opt-out)
#   secrets.X != ''               (presence test for secrets — accepted)
#   github.actor == ...           (no var/secret involved — out of scope)
#
# Flagged anti-patterns:
#   vars.X != ''                  (presence test on var — should be == 'true' boolean)
#   vars.X                        (truthy on raw string — implementation-defined)
#
# Naming convention: vars/secrets keys SHOULD be ALL_CAPS_SNAKE_CASE.

for wf in "${WORKFLOWS[@]}"; do
  [[ -f "$wf" ]] || continue
  while IFS= read -r match; do
    [[ -z "$match" ]] && continue
    LINE=$(echo "$match" | cut -d: -f1)
    CONTENT=$(echo "$match" | cut -d: -f2-)
    # Strip leading whitespace
    COND=$(echo "$CONTENT" | sed -E 's/^[[:space:]]*//')
    # Pull out every vars.X / secrets.X reference in the line
    while IFS= read -r ref; do
      [[ -z "$ref" ]] && continue
      TOTAL_REFS=$((TOTAL_REFS + 1))
      KIND=$(echo "$ref" | cut -d. -f1)
      NAME=$(echo "$ref" | cut -d. -f2)

      # 1. Naming convention warning — ALL_CAPS_SNAKE_CASE only
      if ! [[ "$NAME" =~ ^[A-Z][A-Z0-9_]*$ ]]; then
        echo "  ⚠️  $wf:$LINE: \`${KIND}.${NAME}\` does not follow ALL_CAPS_SNAKE_CASE naming convention"
        echo "       condition: $(echo "$COND" | cut -c1-120)"
        echo "       fix: rename to ALL_CAPS_SNAKE_CASE on the repo settings AND update workflow reference"
        echo "       standard: GitHub Actions Hardening — Conditional execution (predictable naming)"
        WARN_ISSUES=$((WARN_ISSUES + 1))
      fi

      # 2. Anti-pattern: `vars.X != ''` — should be `vars.X == 'true'` for boolean intent
      if [[ "$KIND" == "vars" ]]; then
        # Build a regex-safe needle for the var ref
        SAFE_REF=$(printf '%s' "vars.${NAME}" | sed -E 's/[][\\.*^$(){}|+?]/\\&/g')
        if echo "$COND" | grep -qE "${SAFE_REF}\s*!=\s*''"; then
          echo "  ⚠️  $wf:$LINE: \`vars.${NAME} != ''\` — presence test on a Variable (unconventional)"
          echo "       condition: $(echo "$COND" | cut -c1-120)"
          echo "       reason: Variables default to empty string when unset — \`!= ''\` accepts any non-empty value, including stale/wrong values"
          echo "       fix: prefer \`vars.${NAME} == 'true'\` (canonical opt-in) OR move to a Secret presence check"
          echo "       standard: GitHub Actions Hardening — Conditional execution (explicit boolean opt-in)"
          WARN_ISSUES=$((WARN_ISSUES + 1))
        fi
      fi
    done < <(echo "$CONTENT" | grep -oE '(vars|secrets)\.[A-Za-z][A-Za-z0-9_]*' | sort -u)
  done < <(grep -nE '^\s*if:.*((vars|secrets)\.[A-Za-z])' "$wf" 2>/dev/null || true)
done

echo "  └─ scanned ${#WORKFLOWS[@]} workflow(s); $TOTAL_REFS vars/secrets reference(s) in if: conditions"
if [[ $WARN_ISSUES -gt 0 ]]; then
  echo "  └─ $WARN_ISSUES style/scoping warning(s) — see above"
  exit 1
fi
echo "  └─ all if: conditions follow conventional patterns"
exit 0
