#!/usr/bin/env bash
#
# generate-changelog.sh — Generate Keep-a-Changelog entry from conventional commits.
#
# Usage:
#   scripts/generate-changelog.sh <version> [previous-tag]
#       Generate changelog for <version> from commits since [previous-tag]
#       (defaults to most recent prior tag; falls back to repo root).
#
#   scripts/generate-changelog.sh --self-test
#       Run synthetic-fixture self-test (no git access required).
#
# Output format (Keep a Changelog 1.1):
#   ## [vX.Y.Z] - YYYY-MM-DD
#   ### Added       — feat
#   ### Changed     — refactor, perf, style, build, ci
#   ### Fixed       — fix
#   ### Security    — sec, security, CVE refs
#   ### Removed     — revert, breaking change markers
#   ### Docs        — docs
#   ### Other       — chore, test, anything else
#
# GAP-374 — Wave 38 Bucket A. See `documents/03-planning/roadmap/versioning-policy.md` §6 + §9.
#
set -euo pipefail

# -------- helpers --------------------------------------------------------------

today_iso() {
    date -u +%Y-%m-%d
}

# Classify a commit subject line into a Keep-a-Changelog section.
# Echoes the section name (Added / Changed / Fixed / Security / Removed / Docs / Other).
# Per `versioning-policy.md` §9 conventional-commit types.
classify_commit() {
    local subject="$1"
    local lower
    lower=$(echo "$subject" | tr '[:upper:]' '[:lower:]')

    # Breaking change marker (! after type or BREAKING CHANGE in footer)
    if echo "$subject" | grep -qE '^[a-z]+(\([^)]*\))?!:' || echo "$lower" | grep -q 'breaking change'; then
        echo "Removed"
        return
    fi

    # Type prefix (allow optional scope)
    case "$lower" in
        feat\(*\):*|feat:*)         echo "Added" ;;
        fix\(*\):*|fix:*)           echo "Fixed" ;;
        sec\(*\):*|sec:*|security\(*\):*|security:*) echo "Security" ;;
        docs\(*\):*|docs:*)         echo "Docs" ;;
        refactor\(*\):*|refactor:*) echo "Changed" ;;
        perf\(*\):*|perf:*)         echo "Changed" ;;
        style\(*\):*|style:*)       echo "Changed" ;;
        build\(*\):*|build:*)       echo "Changed" ;;
        ci\(*\):*|ci:*)             echo "Changed" ;;
        revert\(*\):*|revert:*)     echo "Removed" ;;
        *)
            # CVE / security keyword fallback
            if echo "$lower" | grep -qE 'cve-[0-9]{4}-[0-9]+|vulnerability'; then
                echo "Security"
            else
                echo "Other"
            fi
            ;;
    esac
}

# Generate a changelog from a commit-log stream on stdin.
# stdin: lines of "<subject>" (one per commit; first line of message)
# args: $1 = version (e.g. v1.0.0), $2 = date (YYYY-MM-DD)
render_changelog() {
    local version="$1"
    local date_str="$2"

    # Buckets — one entry per line per section.
    local added=""
    local changed=""
    local fixed=""
    local security=""
    local removed=""
    local docs=""
    local other=""

    while IFS= read -r subject; do
        # Skip empty lines and merge commits.
        [[ -z "$subject" ]] && continue
        case "$subject" in
            "Merge "*) continue ;;
        esac

        local section
        section=$(classify_commit "$subject")

        local line="- ${subject}"

        case "$section" in
            Added)    added="${added}${line}"$'\n' ;;
            Changed)  changed="${changed}${line}"$'\n' ;;
            Fixed)    fixed="${fixed}${line}"$'\n' ;;
            Security) security="${security}${line}"$'\n' ;;
            Removed)  removed="${removed}${line}"$'\n' ;;
            Docs)     docs="${docs}${line}"$'\n' ;;
            *)        other="${other}${line}"$'\n' ;;
        esac
    done

    # Emit Keep-a-Changelog section. Always emit Added/Changed/Fixed (even if empty,
    # for downstream-tool friendliness); other sections only if non-empty.
    printf '## [%s] - %s\n\n' "$version" "$date_str"

    printf '### Added\n'
    if [[ -n "$added" ]]; then printf '%s\n' "$added"; else printf '_No new features in this release._\n\n'; fi

    printf '### Changed\n'
    if [[ -n "$changed" ]]; then printf '%s\n' "$changed"; else printf '_No changes in this release._\n\n'; fi

    printf '### Fixed\n'
    if [[ -n "$fixed" ]]; then printf '%s\n' "$fixed"; else printf '_No fixes in this release._\n\n'; fi

    if [[ -n "$security" ]]; then
        printf '### Security\n%s\n' "$security"
    fi
    if [[ -n "$removed" ]]; then
        printf '### Removed / Breaking\n%s\n' "$removed"
    fi
    if [[ -n "$docs" ]]; then
        printf '### Docs\n%s\n' "$docs"
    fi
    if [[ -n "$other" ]]; then
        printf '### Other\n%s\n' "$other"
    fi
}

# -------- self-test ------------------------------------------------------------

self_test() {
    echo "Running self-test..." >&2

    local fixture
    fixture=$(cat <<'FIXTURE'
feat(wave-38): GAP-374 tag-based release CI
fix(billing): correct trial period off-by-one
docs(readme): update install instructions
refactor(core): extract billing service
chore(deps): bump spring-boot 3.5.14
sec(api): patch CVE-2026-12345 in jackson-databind
feat!: drop legacy /api/v0 endpoints
ci(workflows): add release-tag job
test(branding): add unit tests
Merge pull request #123 from foo/bar
FIXTURE
)

    local actual
    actual=$(echo "$fixture" | render_changelog "v1.0.0" "2026-05-09")

    # Expected fragments (each must appear in actual output)
    local expected_fragments=(
        "## [v1.0.0] - 2026-05-09"
        "### Added"
        "- feat(wave-38): GAP-374 tag-based release CI"
        "### Changed"
        "- refactor(core): extract billing service"
        "- ci(workflows): add release-tag job"
        "### Fixed"
        "- fix(billing): correct trial period off-by-one"
        "### Security"
        "- sec(api): patch CVE-2026-12345 in jackson-databind"
        "### Removed / Breaking"
        "- feat!: drop legacy /api/v0 endpoints"
        "### Docs"
        "- docs(readme): update install instructions"
        "### Other"
        "- chore(deps): bump spring-boot 3.5.14"
        "- test(branding): add unit tests"
    )

    # Merge commit must NOT appear.
    if echo "$actual" | grep -qF -- "Merge pull request"; then
        echo "FAIL: merge commit leaked into changelog" >&2
        echo "--- actual output ---" >&2
        echo "$actual" >&2
        return 1
    fi

    local missing=0
    for fragment in "${expected_fragments[@]}"; do
        if ! echo "$actual" | grep -qF -- "$fragment"; then
            echo "FAIL: missing fragment: $fragment" >&2
            missing=$((missing + 1))
        fi
    done

    if [[ $missing -gt 0 ]]; then
        echo "--- actual output ---" >&2
        echo "$actual" >&2
        echo "Self-test FAILED ($missing missing fragments)" >&2
        return 1
    fi

    echo "Self-test PASSED — $(echo "$fixture" | wc -l) input commits classified into 7 sections" >&2
    echo "" >&2
    echo "--- self-test output preview ---" >&2
    echo "$actual"
    return 0
}

# -------- main -----------------------------------------------------------------

if [[ "${1:-}" == "--self-test" ]]; then
    self_test
    exit $?
fi

if [[ $# -lt 1 ]]; then
    echo "Usage: $0 <version> [previous-tag]" >&2
    echo "       $0 --self-test" >&2
    exit 2
fi

VERSION="$1"
PREV_TAG="${2:-}"

# Auto-detect previous tag if not supplied. Walk descending semver tags excluding the current one.
if [[ -z "$PREV_TAG" ]]; then
    PREV_TAG=$(git tag --sort=-v:refname --list 'v[0-9]*.[0-9]*.[0-9]*' \
        | grep -vxF "$VERSION" \
        | head -n 1 || true)
fi

# Determine commit range.
if [[ -n "$PREV_TAG" ]] && git rev-parse --verify "$PREV_TAG" >/dev/null 2>&1; then
    RANGE="${PREV_TAG}..HEAD"
    echo "Generating changelog for $VERSION since $PREV_TAG" >&2
else
    RANGE="HEAD"
    echo "No previous tag found; generating changelog from repo root to HEAD" >&2
fi

# Pull commit subjects only. --no-merges excludes GitHub merge commits.
git log "$RANGE" --no-merges --pretty=format:'%s' \
    | render_changelog "$VERSION" "$(today_iso)"
