#!/usr/bin/env bash
# check-public-page-duplicate-chrome.sh
#
# Detector for the "(public) page duplicates PublicLayout chrome" bug class.
#
# Pages under kitehub-frontend `src/app/(public)/**` are wrapped by
# `(public)/layout.tsx` → `PublicLayout`, which already renders the site
# <header> (logo + nav), a single <main>, and the shared <Footer />. When a
# page (or the landing SSR shell) ALSO renders its own top-level <header>/
# <main>/<footer>, the result is a duplicated header/footer (visible on
# SSR / no-JS / for bots) plus an invalid nested <main>.
#
# Bug class origin: 2026-06-10 G2 browser-walk found /contact rendering a 2nd
# header; sweep found the same class on /waitlist + the homepage
# (LandingShellSSR) + nested <main> on beta-status + 4 legal pages. Fixed in
# commit 7e6cc613. This detector prevents recurrence per
# `.claude/rules/cross-flow-bug-class-sweep.md` §4.1 (statically-detectable
# class → ship persistent detector, not just one-time manual sweep).
#
# Heuristics (grep-based, no AST):
#   FAIL  — any <main ...> tag in a (public) page/shell file. PublicLayout
#           provides the single <main>; a page rendering its own nests a 2nd.
#           Zero legitimate cases (section content uses <div>/<section>).
#   WARN  — a <header> OR <footer> tag whose block contains a brand link
#           `href="/"` (site-level chrome), which duplicates PublicLayout's
#           header/Footer. Section-level <header> (doc titles, no nav) is fine.
#
# Modes:
#   --warn   (default) print findings, exit 0
#   --strict exit 1 on any FAIL (use once repo is clean)
#
# Scope: kitehub/kitehub-frontend/src/app/(public)/**/*.tsx (excludes __tests__).

set -uo pipefail

MODE="--warn"
[ "${1:-}" = "--strict" ] && MODE="--strict"
[ "${1:-}" = "--warn" ] && MODE="--warn"

REPO_ROOT=$(git rev-parse --show-toplevel 2>/dev/null || pwd)
PUBLIC_DIR="$REPO_ROOT/kitehub/kitehub-frontend/src/app/(public)"

if [ ! -d "$PUBLIC_DIR" ]; then
  echo "check-public-page-duplicate-chrome: (public) dir not found ($PUBLIC_DIR) — skip"
  exit 0
fi

FAIL_COUNT=0
WARN_COUNT=0

# Collect candidate .tsx files (exclude tests). LandingShellSSR.tsx + LandingClient.tsx
# live at the (public) root and are in scope too.
mapfile -t FILES < <(find "$PUBLIC_DIR" -name "*.tsx" -not -path "*/__tests__/*" 2>/dev/null | sort)

for f in "${FILES[@]}"; do
  rel="${f#"$REPO_ROOT"/}"

  # FAIL: any <main tag (PublicLayout already provides the single <main>).
  # Exclude comment lines (// or * leading) to avoid matching doc comments.
  main_hits=$(grep -nE "<main(\s|>|$)" "$f" 2>/dev/null | grep -vE "^\s*[0-9]+:\s*(//|\*|<!--)" || true)
  if [ -n "$main_hits" ]; then
    while IFS= read -r line; do
      echo "FAIL: $rel:${line%%:*} — top-level <main> in (public) page (PublicLayout already provides <main>; use <div>)"
      FAIL_COUNT=$((FAIL_COUNT + 1))
    done <<< "$main_hits"
  fi

  # WARN: site-level <header>/<footer> = file renders <header or <footer AND a
  # brand link href="/" (logo link). Section <header> with only an <h1> is fine.
  if grep -qE "<(header|footer)(\s|>|$)" "$f" 2>/dev/null \
     && grep -qE 'href="/"' "$f" 2>/dev/null; then
    # Only flag if the header/footer is not a comment reference.
    hf_line=$(grep -nE "<(header|footer)(\s|>|$)" "$f" 2>/dev/null \
      | grep -vE "^\s*[0-9]+:\s*(//|\*|<!--)" | head -1)
    if [ -n "$hf_line" ]; then
      echo "WARN: $rel:${hf_line%%:*} — <header>/<footer> with brand link href=\"/\" may duplicate PublicLayout chrome (verify it is section-level, not site-level)"
      WARN_COUNT=$((WARN_COUNT + 1))
    fi
  fi
done

echo "─────────────────────────────────────────────"
echo "check-public-page-duplicate-chrome: scanned ${#FILES[@]} (public) .tsx file(s) — $FAIL_COUNT FAIL, $WARN_COUNT WARN"

if [ "$FAIL_COUNT" -gt 0 ] && [ "$MODE" = "--strict" ]; then
  echo "STRICT mode: failing build (nested <main> in (public) page). Per cross-flow-bug-class-sweep.md §4.1."
  exit 1
fi

exit 0
