#!/usr/bin/env bash
# check-admin-scan-coverage.sh — GAP-382 build-time detector
#
# Prevents recurrence of admin scan drift (Wave 25 Consent / Wave 26 DSAR /
# Wave 33 Beta — 3 prior recurrences).
#
# Detects when a new @Entity / @Repository package under
# kitehub-subscription/src/main/java/com/kitehub/subscription/** is NOT covered
# by KiteHubAdminApplication's @EnableJpaRepositories.basePackages or
# @EntityScan.basePackages — fails BEFORE admin module @SpringBootTest, so
# the diagnostic message points at the missing scan entry directly.
#
# Memory: feedback_admin_scan_packages_after_module_add.md (recurrence #4
# prevention). Per `incident-to-rule-pipeline.md` Stage 3, this detector is
# the enforcement parity for that memory rule (GAP-382).

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SUBSCRIPTION_SRC="$REPO_ROOT/kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription"
ADMIN_APP="$REPO_ROOT/kitehub/kitehub-admin/src/main/java/com/kitehub/admin/KiteHubAdminApplication.java"

print_help() {
    cat <<'EOF'
Usage: scripts/check-admin-scan-coverage.sh [--help]

Verifies that every @Entity / @Repository package under
kitehub-subscription is covered by KiteHubAdminApplication's
@EnableJpaRepositories + @EntityScan basePackages lists.

Exit codes:
  0  All entity + repository packages covered (silent on success).
  1  Drift detected — admin scan list missing one or more packages.
  2  Setup error (paths missing, etc.).

Reference: GAP-382, feedback_admin_scan_packages_after_module_add.md.
EOF
}

if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
    print_help
    exit 0
fi

if [[ ! -d "$SUBSCRIPTION_SRC" ]]; then
    echo "ERROR: subscription source not found at $SUBSCRIPTION_SRC" >&2
    exit 2
fi
if [[ ! -f "$ADMIN_APP" ]]; then
    echo "ERROR: admin application not found at $ADMIN_APP" >&2
    exit 2
fi

# Step 1 — collect entity packages (file contains class-level @Entity)
declare -A entity_pkgs=()
while IFS= read -r -d '' file; do
    if grep -qE '^@Entity\b|^[[:space:]]*@Entity[(]' "$file"; then
        pkg=$(grep -m1 -E '^package [a-zA-Z0-9_.]+;' "$file" | sed -E 's/^package (.+);/\1/')
        if [[ -n "$pkg" ]]; then
            entity_pkgs["$pkg"]=1
        fi
    fi
done < <(find "$SUBSCRIPTION_SRC" -type f -name '*.java' -print0)

# Step 2 — collect repository packages (file contains @Repository OR
# extends Jpa/Crud/PagingAndSorting Repository)
declare -A repo_pkgs=()
while IFS= read -r -d '' file; do
    if grep -qE '@Repository\b|extends[[:space:]]+(JpaRepository|CrudRepository|PagingAndSortingRepository)\b' "$file"; then
        pkg=$(grep -m1 -E '^package [a-zA-Z0-9_.]+;' "$file" | sed -E 's/^package (.+);/\1/')
        if [[ -n "$pkg" ]]; then
            repo_pkgs["$pkg"]=1
        fi
    fi
done < <(find "$SUBSCRIPTION_SRC" -type f -name '*.java' -print0)

# Step 3 — extract admin scan lists. We parse blocks
# @EnableJpaRepositories(basePackages = { ... })
# @EntityScan(basePackages = { ... })
# tolerating // line comments inside the block.
extract_block() {
    local annotation="$1"
    awk -v ann="$annotation" '
        BEGIN { in_block = 0 }
        index($0, "@" ann "(basePackages") { in_block = 1 }
        in_block {
            print
            if (index($0, "})") || index($0, "} )")) { in_block = 0 }
        }
    ' "$ADMIN_APP" \
        | sed 's://[^"]*$::' \
        | grep -oE '"[a-zA-Z0-9_.]+"' \
        | tr -d '"' \
        | sort -u
}

mapfile -t admin_repo_list < <(extract_block "EnableJpaRepositories")
mapfile -t admin_entity_list < <(extract_block "EntityScan")

if [[ ${#admin_repo_list[@]} -eq 0 || ${#admin_entity_list[@]} -eq 0 ]]; then
    echo "ERROR: failed to parse @EnableJpaRepositories / @EntityScan in $ADMIN_APP" >&2
    echo "  Repo list size:   ${#admin_repo_list[@]}" >&2
    echo "  Entity list size: ${#admin_entity_list[@]}" >&2
    exit 2
fi

# Step 4 — coverage check (admin entry is a prefix of pkg, or equal)
is_covered() {
    local pkg="$1"
    shift
    local entry
    for entry in "$@"; do
        if [[ "$pkg" == "$entry" || "$pkg" == "$entry."* ]]; then
            return 0
        fi
    done
    return 1
}

missing_entity=()
for pkg in "${!entity_pkgs[@]}"; do
    if ! is_covered "$pkg" "${admin_entity_list[@]}"; then
        missing_entity+=("$pkg")
    fi
done

missing_repo=()
for pkg in "${!repo_pkgs[@]}"; do
    if ! is_covered "$pkg" "${admin_repo_list[@]}"; then
        missing_repo+=("$pkg")
    fi
done

if [[ ${#missing_entity[@]} -eq 0 && ${#missing_repo[@]} -eq 0 ]]; then
    echo "✅ Admin scan coverage OK (entity packages: ${#entity_pkgs[@]}, repository packages: ${#repo_pkgs[@]})"
    exit 0
fi

echo "❌ Admin scan drift detected:"
echo
if [[ ${#missing_entity[@]} -gt 0 ]]; then
    for pkg in $(printf '%s\n' "${missing_entity[@]}" | sort -u); do
        echo "Entity package not covered: $pkg"
    done
fi
if [[ ${#missing_repo[@]} -gt 0 ]]; then
    for pkg in $(printf '%s\n' "${missing_repo[@]}" | sort -u); do
        echo "Repository package not covered: $pkg"
    done
fi

cat <<'EOF'

Fix: thêm vào KiteHubAdminApplication:
@EnableJpaRepositories(basePackages = {..., "<missing-repo-package>"})
@EntityScan(basePackages = {..., "<missing-entity-package>"})

Tham chiếu: feedback_admin_scan_packages_after_module_add.md (recurrence #4 prevented).
File: kitehub/kitehub-admin/src/main/java/com/kitehub/admin/KiteHubAdminApplication.java
EOF

exit 1
