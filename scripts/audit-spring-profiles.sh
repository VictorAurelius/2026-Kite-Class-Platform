#!/usr/bin/env bash
# audit-spring-profiles.sh — SPRING_PROFILES_ACTIVE ↔ application-{profile}.yml existence
#
# Verifies for each service block in docker-compose.production.yml that sets
# SPRING_PROFILES_ACTIVE=<profile>, the corresponding application-<profile>.yml
# (or application-<profile>.properties) file exists in the service's resources.
#
# Without a matching profile file, Spring silently ignores the profile env —
# production overrides in the profile file never load.
#
# Per `.claude/rules/production-env-config-registry.md` v1.1.0 §11.
#
# Exit codes:
#   0 — every SPRING_PROFILES_ACTIVE has a matching application-<profile>.yml
#   1 — at least one service references a profile with no matching file
#
# Usage:
#   bash scripts/audit-spring-profiles.sh
#   bash scripts/audit-spring-profiles.sh --json

set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
COMPOSE="$ROOT/docker-compose.production.yml"

if [[ ! -f "$COMPOSE" ]]; then
  echo "ERROR: $COMPOSE not found" >&2
  exit 1
fi

declare -a FINDINGS=()  # explicit empty init — bash strict mode `set -u` trips on reference before any append
declare -a CHECKED=()

current_service=""
in_env=0

while IFS= read -r line; do
  if [[ "$line" =~ ^[[:space:]]{2}(kitehub-[a-z-]+|kite-[a-z-]+|kiteclass-[a-z-]+):[[:space:]]*$ ]]; then
    current_service="${BASH_REMATCH[1]}"
    in_env=0
    continue
  fi
  if [[ -n "$current_service" && "$line" =~ ^[[:space:]]+environment:[[:space:]]*$ ]]; then
    in_env=1
    continue
  fi
  if [[ $in_env -eq 1 && "$line" =~ ^[[:space:]]{4}[a-z_]+:[[:space:]]*$ ]]; then
    in_env=0
  fi
  if [[ $in_env -eq 1 && "$line" =~ ^[[:space:]]+SPRING_PROFILES_ACTIVE:[[:space:]]+([\"\']?)([A-Za-z0-9_,-]+)([\"\']?) ]]; then
    profiles_str="${BASH_REMATCH[2]}"
    # SPRING_PROFILES_ACTIVE can be comma-separated
    IFS=',' read -ra profiles <<< "$profiles_str"
    for profile in "${profiles[@]}"; do
      profile=$(echo "$profile" | tr -d '[:space:]')
      [[ -z "$profile" ]] && continue
      CHECKED+=("$current_service:$profile")

      # Search for application-<profile>.yml or .properties in the matching module
      resources_dir=""
      if [[ "$current_service" =~ ^kitehub- ]]; then
        resources_dir="$ROOT/kitehub/$current_service/src/main/resources"
      elif [[ "$current_service" =~ ^kiteclass- ]]; then
        # core / gateway / frontend
        candidate="$ROOT/kiteclass/$current_service/src/main/resources"
        [[ -d "$candidate" ]] && resources_dir="$candidate"
      fi

      if [[ -z "$resources_dir" || ! -d "$resources_dir" ]]; then
        # Service has no resources dir (e.g. shared infra container) — skip
        continue
      fi

      yml_file="$resources_dir/application-${profile}.yml"
      yaml_file="$resources_dir/application-${profile}.yaml"
      props_file="$resources_dir/application-${profile}.properties"

      if [[ ! -f "$yml_file" && ! -f "$yaml_file" && ! -f "$props_file" ]]; then
        FINDINGS+=("$current_service: SPRING_PROFILES_ACTIVE=$profile but no application-${profile}.yml in $(realpath --relative-to="$ROOT" "$resources_dir")/")
      fi
    done
  fi
done < "$COMPOSE"

if [[ "${1:-}" == "--json" ]]; then
  printf '{"checked":%d,"findings":%d}\n' "${#CHECKED[@]}" "${#FINDINGS[@]}"
  exit $([ ${#FINDINGS[@]} -eq 0 ] && echo 0 || echo 1)
fi

echo "=== Spring profile coverage audit ==="
echo
echo "Checked ${#CHECKED[@]} (service, profile) pairs from $COMPOSE"
echo

if [[ ${#FINDINGS[@]} -gt 0 ]]; then
  echo "❌ MISSING application-<profile>.yml files (${#FINDINGS[@]}):"
  printf '   %s\n' "${FINDINGS[@]}"
  echo
  echo "FAIL: ${#FINDINGS[@]} profile(s) referenced in compose but no matching file."
  echo "Fix: either create application-<profile>.yml in service resources OR remove SPRING_PROFILES_ACTIVE env."
  echo "Without matching file, Spring silently ignores the profile and production overrides never apply."
  exit 1
fi

echo "PASS: every SPRING_PROFILES_ACTIVE has a matching application-<profile>.yml file."
exit 0
