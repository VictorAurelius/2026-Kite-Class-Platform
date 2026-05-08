#!/usr/bin/env bash
# Category #7 — GitHub Actions Variables/Secrets reference vs repo state.
#
# Standard:
#   - GitHub Actions Hardening (https://docs.github.com/en/actions/security-guides/security-hardening-for-github-actions)
#   - OpenSSF Scorecard "Token-Permissions" / "Variables" hygiene
#
# What it does:
#   Enumerate every `${{ vars.X }}` and `${{ secrets.X }}` reference under
#   `.github/workflows/*.yml` and verify each name exists on the repo via the
#   GitHub REST API. Missing references print FAIL with file:line + the exact
#   `gh api -X POST` command needed to create the variable/secret.
#
# Tier 1 read-only only — uses `gh api` GET endpoints exclusively.
#
# Exit codes:
#   0 PASS / WARN
#   1 FAIL (≥1 referenced var/secret missing)
#
# Fallback: if `gh auth status` is not green, emit WARN and exit 0 (skip).

set -uo pipefail

CHECK_NAME="github-vars-secrets"
CHECK_CATEGORY=7
REPO_SLUG="${REPO_SLUG:-VictorAurelius/2026-Kite-Class-Platform}"

# --- helpers --------------------------------------------------------------

emit() {
  # emit <level> <message>
  printf '[%s][cat-%d][%s] %s\n' "$1" "$CHECK_CATEGORY" "$CHECK_NAME" "$2"
}

# Allowlist of GitHub-provided / context-builtin secrets that don't require
# repo-level configuration (always available to workflows).
# Includes:
#   - GITHUB_TOKEN (auto-injected per job)
#   - secrets.GITHUB_TOKEN is always present
BUILTIN_SECRETS=(
  "GITHUB_TOKEN"
)

is_builtin_secret() {
  local name="$1"
  for b in "${BUILTIN_SECRETS[@]}"; do
    [[ "$name" == "$b" ]] && return 0
  done
  return 1
}

# --- preconditions --------------------------------------------------------

if ! command -v gh >/dev/null 2>&1; then
  emit WARN "gh CLI not installed — skipping (install: https://cli.github.com/)"
  exit 0
fi

if ! gh auth status >/dev/null 2>&1; then
  emit WARN "gh auth status required for Category #7 — run 'gh auth login' to enable"
  exit 0
fi

WF_DIR=".github/workflows"
if [[ ! -d "$WF_DIR" ]]; then
  emit WARN "no $WF_DIR directory — skipping"
  exit 0
fi

# --- collect references ---------------------------------------------------

# Match `${{ vars.NAME }}` or `${{ secrets.NAME }}` (NAME = ALPHA_DIGIT_UNDERSCORE).
# grep -nE prints file:line:match. We post-process to extract scope+name.
TMP_REFS=$(mktemp)
trap 'rm -f "$TMP_REFS"' EXIT

# Use Grep-style here intentionally — bash, no project-tool subst available
# in subscripts. Enforced read-only; no diff produced.
grep -rnE '\$\{\{\s*(vars|secrets)\.[A-Z_][A-Z0-9_]*\s*[}|]' "$WF_DIR" \
  --include='*.yml' --include='*.yaml' 2>/dev/null \
  | sed -E 's/.*\$\{\{\s*(vars|secrets)\.([A-Z_][A-Z0-9_]*).*/__REF__ \1 \2/' \
  > "$TMP_REFS.raw" || true

# Above sed leaves source line prefix. Re-grep to keep file:line + extracted ref.
awk -F: '
  {
    file = $1
    lineno = $2
    # The rest of the original line is everything after first 2 colons
    rest = $0
    sub(/^[^:]*:[^:]*:/, "", rest)
    # Skip YAML comments (line starts with optional whitespace then "#")
    trimmed = rest
    sub(/^[[:space:]]+/, "", trimmed)
    if (trimmed ~ /^#/) next
    line = rest
    # find each occurrence in the matched line
    while (match(line, /\$\{\{[[:space:]]*(vars|secrets)\.[A-Z_][A-Z0-9_]*[^}]*\}\}/)) {
      m = substr(line, RSTART, RLENGTH)
      ref = m
      sub(/^\$\{\{[[:space:]]*/, "", ref)
      sub(/[[:space:]]*\}\}$/, "", ref)
      # Detect optional pattern: contains "|| <fallback>"
      optional = (m ~ /\|\|/) ? "optional" : "required"
      # Extract first word "scope.name"
      head = ref
      sub(/[[:space:]].*$/, "", head)
      split(head, parts, ".")
      print file ":" lineno "\t" parts[1] "\t" parts[2] "\t" optional
      line = substr(line, RSTART + RLENGTH)
    }
  }
' < <(grep -rnE '\$\{\{\s*(vars|secrets)\.[A-Z_][A-Z0-9_]*' "$WF_DIR" \
       --include='*.yml' --include='*.yaml' 2>/dev/null) \
  | sort -u > "$TMP_REFS"

REF_COUNT=$(wc -l < "$TMP_REFS" | tr -d ' ')
if [[ "$REF_COUNT" -eq 0 ]]; then
  emit PASS "no vars/secrets references found in $WF_DIR"
  exit 0
fi

emit INFO "found $REF_COUNT distinct ref site(s) across $WF_DIR/*.yml"

# --- fetch repo state -----------------------------------------------------

EXISTING_VARS=$(gh api "repos/$REPO_SLUG/actions/variables" \
  --paginate --jq '.variables[].name' 2>/dev/null | sort -u || true)
EXISTING_SECRETS=$(gh api "repos/$REPO_SLUG/actions/secrets" \
  --paginate --jq '.secrets[].name' 2>/dev/null | sort -u || true)

if [[ -z "$EXISTING_VARS$EXISTING_SECRETS" ]]; then
  emit WARN "could not fetch repo variables/secrets (insufficient gh scopes? need repo + actions:read)"
  exit 0
fi

# --- compare --------------------------------------------------------------

FAIL_COUNT=0
WARN_COUNT=0

# Track per-name-scope to avoid duplicate FAILs (same missing name in N files).
declare -A REPORTED

while IFS=$'\t' read -r site scope name optional; do
  [[ -z "$name" ]] && continue
  key="${scope}:${name}"

  if [[ "$scope" == "secrets" ]] && is_builtin_secret "$name"; then
    continue
  fi

  found=0
  if [[ "$scope" == "vars" ]]; then
    grep -qxF "$name" <<< "$EXISTING_VARS" && found=1
  else
    grep -qxF "$name" <<< "$EXISTING_SECRETS" && found=1
  fi

  if [[ "$found" -eq 0 ]]; then
    if [[ -z "${REPORTED[$key]:-}" ]]; then
      REPORTED[$key]=1
      if [[ "$scope" == "vars" ]]; then
        cmd="gh variable set $name --body '<value>' --repo $REPO_SLUG"
      else
        cmd="gh secret set $name --body '<value>' --repo $REPO_SLUG"
      fi
      if [[ "$optional" == "optional" ]]; then
        WARN_COUNT=$((WARN_COUNT + 1))
        emit WARN "$site references ${scope}.${name} (has '|| fallback' — optional) — set if non-default desired"
        emit WARN "  fix: $cmd"
      else
        FAIL_COUNT=$((FAIL_COUNT + 1))
        emit FAIL "$site references ${scope}.${name} but it is not configured on $REPO_SLUG"
        emit FAIL "  fix: $cmd"
      fi
    fi
  fi
done < "$TMP_REFS"

# --- summary --------------------------------------------------------------

if [[ "$FAIL_COUNT" -gt 0 ]]; then
  emit FAIL "$FAIL_COUNT distinct REQUIRED missing ref(s) — workflow runs will fail at expansion time"
  if [[ "$WARN_COUNT" -gt 0 ]]; then
    emit WARN "$WARN_COUNT additional optional ref(s) unset (have fallbacks)"
  fi
  exit 1
fi

if [[ "$WARN_COUNT" -gt 0 ]]; then
  emit WARN "$WARN_COUNT optional ref(s) unset — workflows fall back to defaults"
fi

emit PASS "all ${REF_COUNT} ref site(s) resolve to configured vars/secrets (or have fallbacks)"
exit 0
