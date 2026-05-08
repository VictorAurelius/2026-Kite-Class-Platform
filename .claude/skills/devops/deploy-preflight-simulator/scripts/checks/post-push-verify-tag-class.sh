#!/usr/bin/env bash
#
# Check #9 — Post-push verification step requirements vs tag class
#
# Standard: GitHub Actions Hardening Guide §"Conditional execution"
#           + Sigstore Cosign Best Practices (release attachment requires
#             GitHub Release object)
#
# Catches: workflow steps that require GitHub Release / Sigstore /
# permission-elevated APIs but lack tag-class conditional. These steps
# fail with 403 / "Resource not accessible" when triggered on pre-release
# tags (staging.*, rc.*) before the corresponding Release object exists.
#
# Phase 3 staging.7 (run #25529897610) hit this: anchore/sbom-action
# defaults to attach SBOM to GitHub Release; staging tag has no Release →
# 403 → workflow fail. Cosign sign + attest similar pattern.
#
# Per release-fix-retry-budget.md §4 pivot matrix: workflow YAML
# over-spec'd for pre-release tags. This check ensures every
# Release-dependent step has tag-class `if:` guard.
set -uo pipefail

WORKFLOWS_DIR=".github/workflows"
[[ -d $WORKFLOWS_DIR ]] || { echo "  └─ no $WORKFLOWS_DIR (skip)"; exit 0; }

# Step actions / patterns that REQUIRE GitHub Release object (or elevated
# perms) at runtime. If used in workflow triggered on tags, MUST be
# conditional on production tag class.
RELEASE_DEP_ACTIONS=(
  "anchore/sbom-action"
  "softprops/action-gh-release"
  "actions/create-release"
  "ncipollo/release-action"
  "release-drafter/release-drafter"
  "sigstore/cosign-installer"
)

# Patterns that signal release-asset / release-attachment intent
RELEASE_ATTACH_PATTERNS=(
  "upload-release-assets:"
  "cosign attest"
  "cosign sign"
  "release.assets"
  "gh release upload"
  "gh release create"
)

ISSUES=0
WARNINGS=0

for wf in "$WORKFLOWS_DIR"/*.yml "$WORKFLOWS_DIR"/*.yaml; do
  [[ -f $wf ]] || continue

  # Workflow triggered on tags?
  if ! awk '/^on:/,/^[a-z]:/' "$wf" 2>/dev/null | grep -qE "tags:"; then
    continue
  fi

  # Find each release-dependent action used
  for action in "${RELEASE_DEP_ACTIONS[@]}"; do
    matches=$(grep -nE "uses:.*${action}" "$wf" 2>/dev/null || true)
    if [[ -z "$matches" ]]; then
      continue
    fi

    while IFS= read -r line; do
      lineno=$(awk -F: '{print $1}' <<< "$line")
      # Read 3 lines BEFORE the uses: line — should have tag-class `if:` guard
      ctx_start=$((lineno > 5 ? lineno - 5 : 1))
      ctx=$(sed -n "${ctx_start},${lineno}p" "$wf")

      # Look for tag-class `if:` clause (e.g. !contains(github.ref, '-staging.')
      # OR contains(github.ref, '-rc') OR refs/tags/v[0-9])
      if echo "$ctx" | grep -qE 'if:.*(!?contains.*ref.*-staging|!?contains.*ref.*-rc|refs/tags/v\[0-9\])'; then
        # Has tag-class guard — OK
        continue
      fi

      # No tag-class guard — flag
      svc_name=$(grep -B1 "$line" "$wf" 2>/dev/null | grep "name:" | head -1 | sed 's/.*name: *//')
      echo "  ❌ FAIL $wf:$lineno: action '$action' has NO tag-class \`if:\` guard"
      echo "       step: ${svc_name:-<unnamed>}"
      echo "       risk: 403 Forbidden on pre-release tags (staging.*, rc.*) — Release object doesn't exist yet"
      echo "       fix:  add \`if: \${{ !contains(github.ref, '-staging.') }}\` (or equivalent tag-class guard)"
      echo "       standard: GitHub Actions Hardening + release-fix-retry-budget.md §4"
      ISSUES=$((ISSUES + 1))
    done <<< "$matches"
  done

  # Find inline cosign / gh release commands in run: blocks
  for pattern in "${RELEASE_ATTACH_PATTERNS[@]}"; do
    matches=$(grep -nE "$pattern" "$wf" 2>/dev/null || true)
    if [[ -z "$matches" ]]; then
      continue
    fi

    while IFS= read -r line; do
      lineno=$(awk -F: '{print $1}' <<< "$line")
      # Skip if-line itself (not in a run: block)
      if grep -qE "^\s*if:" <<< "$line"; then
        continue
      fi

      # Look back for the enclosing step's `if:` clause
      ctx=$(sed -n "1,${lineno}p" "$wf" | tac | awk '/^\s*-\s*name:|^\s*if:/ {print; if (/^\s*-\s*name:/) exit}' | tac)

      if echo "$ctx" | grep -qE 'if:.*(!?contains.*ref.*-staging|!?contains.*ref.*-rc|refs/tags/v\[0-9\])'; then
        continue
      fi

      svc_name=$(echo "$ctx" | grep "name:" | head -1 | sed 's/.*name: *//' | tr -d "'\"" )
      echo "  ⚠️  $wf:$lineno: \`$pattern\` in step without tag-class \`if:\` guard"
      echo "       step: ${svc_name:-<inline run>}"
      echo "       fix:  wrap step or run: block with \`if: \${{ !contains(github.ref, '-staging.') }}\`"
      WARNINGS=$((WARNINGS + 1))
    done <<< "$matches"
  done
done

if [[ $ISSUES -gt 0 ]]; then
  echo "  └─ $ISSUES Release-dependent action(s) without tag-class guard"
  exit 2
fi
if [[ $WARNINGS -gt 0 ]]; then
  echo "  └─ $WARNINGS attach-pattern warning(s)"
  exit 1
fi
echo "  └─ all Release-dependent steps have tag-class guards"
exit 0
