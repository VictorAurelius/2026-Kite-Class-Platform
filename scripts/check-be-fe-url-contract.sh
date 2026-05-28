#!/usr/bin/env bash
# check-be-fe-url-contract.sh — static BE→FE URL contract check
#
# Per GAP-802 cơ chế #2.
#
# GAP-801 class-of-bug: a backend service builds a *user-facing FE path*
# (embedded into an invite/verification/reset email or an HTTP redirect),
# but that path does NOT correspond to any real Next.js App Router route.
# The invitee clicks the link → 404 → cannot proceed. API tests + unit tests
# + lint all PASS because nothing exercises the link end-to-end.
#
# This script catches the bug class STATICALLY at CI (no running stack):
#   1. grep backend Java for string literals that look like FE paths built
#      next to a *Url / *Link variable OR a `${...base-url}` @Value field.
#   2. For each FE path, assert a matching Next.js App Router route exists
#      under kitehub/kitehub-frontend/src/app/** (and kiteclass-frontend if
#      present), accounting for route-group `(...)` segments + `[dynamic]`
#      segments + stripping query strings.
#
# Heuristics (documented to keep false-positives low):
#   - Only consider literals beginning with "/" that look like an FE route.
#   - EXCLUDE backend API paths "/api/..." and "/actuator/..." — those are
#     server endpoints, not FE routes.
#   - EXCLUDE external absolute URLs "http://" / "https://".
#   - A literal qualifies as an "FE path build" if, on the SAME source line,
#     it is concatenated with (or formatted next to) a base-url-like token:
#       * a variable matching  *[Uu]rl  /  *[Ll]ink  /  *[Bb]ase*
#       * a `%s/...` String.format whose %s is fed a base-url arg
#     This narrows the grep to genuine "BE embeds FE link" sites and skips
#     incidental "/" strings (separators, log prefixes, etc.).
#
# Output (per FE path found):
#   BE path </x> (file:line) → FE route: FOUND <app-path> | MISSING
# Exit 1 if any MISSING. `--json` mode prints {"checked":N,"missing":M}.
#
# Env override:
#   CONTRACT_ROOT — point at a synthetic fixture repo layout for unit tests
#                   (mirrors AUDIT_ROOT in scripts/audit-env-coverage.sh).
#                   Default = real repo root.
#
# Exit codes:
#   0 — every BE FE-path resolves to an existing FE route
#   1 — at least one BE FE-path has NO matching FE route (drift / 404 risk)
#
# Usage:
#   bash scripts/check-be-fe-url-contract.sh           # human-readable
#   bash scripts/check-be-fe-url-contract.sh --json     # machine-readable

set -euo pipefail

ROOT="${CONTRACT_ROOT:-$(cd "$(dirname "$0")/.." && pwd)}"

JSON_MODE=0
[[ "${1:-}" == "--json" ]] && JSON_MODE=1

# Backend Java source roots to scan (FE-path builders live in BE services).
BE_GLOB_ROOTS=()
for d in "$ROOT"/kitehub/*/src/main/java "$ROOT"/kiteclass/*/src/main/java; do
  [[ -d "$d" ]] && BE_GLOB_ROOTS+=("$d")
done

# Frontend App Router roots. kiteclass-frontend may not have src/app yet.
FE_APP_ROOTS=()
for d in "$ROOT"/kitehub/kitehub-frontend/src/app "$ROOT"/kitehub/kiteclass-frontend/src/app; do
  [[ -d "$d" ]] && FE_APP_ROOTS+=("$d")
done

# ---------------------------------------------------------------------------
# Step 1 — collect FE-route URL prefixes from App Router file tree.
#
# Next.js App Router: directory  src/app/<segments>/page.tsx  →  URL /<segments>
# Transforms applied to each route dir relative to src/app:
#   - strip route-group segments  (auth) (public) (customer) (admin) ...
#     → they do NOT appear in the URL
#   - dynamic segments [slug] / [id] / [...catchAll] match ANY value at that
#     position → normalized to a wildcard token "*" for matching
#   - a route "exists" when page.tsx OR route.ts is present in that dir
# Output: newline-separated normalized route URLs, each starting with "/".
# Root page.tsx maps to "/".
# ---------------------------------------------------------------------------
collect_fe_routes() {
  local app_root url rel dir
  for app_root in "${FE_APP_ROOTS[@]}"; do
    # find route-defining files (page.tsx / route.ts / route.tsx)
    while IFS= read -r f; do
      dir=$(dirname "$f")
      rel="${dir#"$app_root"}"          # leading path relative to app root, e.g. /(auth)/beta-signup/code
      # Drop route-group segments "(...)": remove any path segment fully
      # wrapped in parentheses.
      # Split on "/" and rebuild, skipping (group) segments.
      url=""
      IFS='/' read -ra parts <<< "$rel"
      for seg in "${parts[@]}"; do
        [[ -z "$seg" ]] && continue
        # route group? e.g. (auth) — skip from URL
        if [[ "$seg" == \(*\) ]]; then
          continue
        fi
        # dynamic segment? [slug] / [id] / [...catch] — normalize to wildcard
        if [[ "$seg" == \[*\] ]]; then
          url="$url/*"
        else
          url="$url/$seg"
        fi
      done
      [[ -z "$url" ]] && url="/"        # src/app/page.tsx → "/"
      echo "$url"
    done < <(find "$app_root" -type f \( -name 'page.tsx' -o -name 'page.jsx' -o -name 'route.ts' -o -name 'route.tsx' -o -name 'route.js' \) 2>/dev/null)
  done | sort -u
}

# ---------------------------------------------------------------------------
# Step 2 — does a BE FE-path match any FE route?
#   $1 = candidate FE path (already query-stripped, leading "/")
# Matching honours dynamic-segment wildcards: an FE route segment "*"
# matches any single BE path segment. Segment counts must be equal.
# Returns matched route URL on stdout + exit 0 if found; exit 1 if not.
# ---------------------------------------------------------------------------
route_matches() {
  local candidate="$1" route
  for route in $FE_ROUTES; do
    if path_matches "$candidate" "$route"; then
      echo "$route"
      return 0
    fi
  done
  return 1
}

# segment-wise match: route "*" segment is a wildcard.
path_matches() {
  local cand="$1" route="$2"
  # exact fast path
  [[ "$cand" == "$route" ]] && return 0
  local -a c r
  IFS='/' read -ra c <<< "$cand"
  IFS='/' read -ra r <<< "$route"
  [[ "${#c[@]}" -ne "${#r[@]}" ]] && return 1
  local i
  for i in "${!c[@]}"; do
    [[ "${r[$i]}" == "*" ]] && continue   # wildcard matches any
    [[ "${c[$i]}" != "${r[$i]}" ]] && return 1
  done
  return 0
}

# ---------------------------------------------------------------------------
# Step 3 — extract candidate FE paths from backend Java.
#
# Two builder shapes, both anchored on a base-url-like token on the SAME line:
#   (a) concatenation:  someBaseUrl + "/reset-password?token=" + token
#   (b) String.format:  String.format("%s/signup/beta?code=%s", base, code)
#
# We grep lines containing both a base-url-like token AND a "/path" literal,
# then sed out the first FE-path literal on the line.
# ---------------------------------------------------------------------------
extract_be_paths() {
  local root
  for root in "${BE_GLOB_ROOTS[@]}"; do
    # Shape (a): "*Url"/"*Link"/"*base*" variable concatenated with "/literal"
    grep -rnE '([A-Za-z_][A-Za-z0-9_]*([Uu]rl|[Ll]ink)|[Bb]ase[A-Za-z0-9_]*) *\+ *"/[A-Za-z]' \
      "$root" --include='*.java' 2>/dev/null || true
    # Shape (b): String.format("%s/path...") where the format string starts %s/
    grep -rnE 'String\.format\( *"%s/[A-Za-z]' \
      "$root" --include='*.java' 2>/dev/null || true
  done | sort -u
}

# Pull the FE path out of one grep hit "file:line:content".
# Echoes "file|line|path" (path query-stripped, leading "/") or nothing.
parse_hit() {
  local hit="$1"
  local file line content path
  file="${hit%%:*}"
  local rest="${hit#*:}"
  line="${rest%%:*}"
  content="${rest#*:}"

  # Shape (b): "%s/<path>..." — capture everything after %s up to the closing
  # quote. The literal may embed a query string with extra %s placeholders
  # (e.g. "%s/signup/beta?code=%s"); we grab the whole inner literal then strip
  # the query below. [^"] avoids tripping on `%` / `?` / `=` inside the path.
  if [[ "$content" =~ \"%s(/[^\"]*)\" ]]; then
    path="${BASH_REMATCH[1]}"
  # Shape (a): + "/path..." — capture the "/path" literal that follows a +
  elif [[ "$content" =~ \+\ *\"(/[^\"]*)\" ]]; then
    path="${BASH_REMATCH[1]}"
  else
    return 0
  fi

  # Strip query string / fragment.
  path="${path%%\?*}"
  path="${path%%#*}"
  # Strip trailing slash (except root).
  [[ "$path" != "/" ]] && path="${path%/}"

  # Exclude paths whose segments still embed a printf placeholder (%s %d %02d...).
  # These are dynamic object-storage keys / file paths (e.g. tenant upload keys
  # "%s/uploads/%d/%02d/..."), NOT static FE routes. A genuine FE link path has
  # no placeholder left after the query string is stripped.
  case "$path" in
    *%*) return 0 ;;
  esac

  # Exclude backend API/actuator endpoints — not FE routes.
  case "$path" in
    /api/*|/actuator/*|/api|/actuator) return 0 ;;
  esac

  # Exclude object-storage / upload key prefixes (defensive denylist).
  case "$path" in
    /uploads/*|/uploads) return 0 ;;
  esac

  # Make file path relative to ROOT for readable output.
  printf '%s|%s|%s\n' "${file#"$ROOT"/}" "$line" "$path"
}

# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
FE_ROUTES=$(collect_fe_routes)

declare -a RESULTS=()
CHECKED=0
MISSING=0

while IFS= read -r hit; do
  [[ -z "$hit" ]] && continue
  parsed=$(parse_hit "$hit")
  [[ -z "$parsed" ]] && continue
  IFS='|' read -r rel line path <<< "$parsed"
  CHECKED=$((CHECKED + 1))
  if matched=$(route_matches "$path"); then
    RESULTS+=("BE path $path ($rel:$line) → FE route: FOUND $matched")
  else
    RESULTS+=("BE path $path ($rel:$line) → FE route: MISSING")
    MISSING=$((MISSING + 1))
  fi
done < <(extract_be_paths)

if [[ "$JSON_MODE" -eq 1 ]]; then
  printf '{"checked":%d,"missing":%d}\n' "$CHECKED" "$MISSING"
  [[ "$MISSING" -eq 0 ]] && exit 0 || exit 1
fi

echo "=== BE → FE URL contract check (GAP-802 cơ chế #2) ==="
echo
if [[ "$CHECKED" -eq 0 ]]; then
  echo "No BE FE-path builders detected (nothing to check)."
  echo "PASS."
  exit 0
fi
printf '%s\n' "${RESULTS[@]}"
echo
echo "Checked: $CHECKED · Missing: $MISSING"
echo
if [[ "$MISSING" -gt 0 ]]; then
  echo "FAIL: $MISSING BE-built FE path(s) have NO matching FE route → invitee/user hits 404."
  echo "Fix: align the BE path literal with the real Next.js App Router route,"
  echo "     OR add the missing FE route under src/app/** (route-group parens stripped)."
  exit 1
fi
echo "PASS: every BE-built FE path resolves to an existing FE route."
exit 0
