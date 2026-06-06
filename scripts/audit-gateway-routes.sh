#!/usr/bin/env bash
# audit-gateway-routes.sh — gateway route ↔ backend controller coverage audit
#
# Verifies for kitehub-gateway application.yml:
#   1. Every gateway route URI hostname is a known kitehub-* service
#   2. Every backend controller @*Mapping path is covered by at least one gateway route predicate
#   3. Routes don't point services to the wrong backend
#
# Per `.claude/rules/production-env-config-registry.md` v1.1.0 §11.
#
# Exit codes:
#   0 — all backend controller paths covered by gateway routes + URIs map to known services
#   1 — orphan controllers OR orphan routes OR wrong-service routing
#
# Usage:
#   bash scripts/audit-gateway-routes.sh            # check
#   bash scripts/audit-gateway-routes.sh --json     # machine-readable summary

set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
GATEWAY_YAML="$ROOT/kitehub/kitehub-gateway/src/main/resources/application.yml"

if [[ ! -f "$GATEWAY_YAML" ]]; then
  echo "ERROR: gateway application.yml not found at $GATEWAY_YAML" >&2
  exit 1
fi

# Known kitehub-* hostnames mapped to module dir
declare -A KNOWN_HOSTS=(
  ["kitehub-subscription"]="kitehub/kitehub-subscription"
  ["kitehub-branding"]="kitehub/kitehub-branding"
  ["kitehub-admin"]="kitehub/kitehub-admin"
  ["kitehub-email"]="kitehub/kitehub-email"
  ["kiteclass-core"]="kiteclass/kiteclass-core"
)

# 1) Extract routes: (path-prefix, hostname) pairs from gateway yaml
# Each route has form:
#   - id: foo
#     uri: http://kitehub-subscription:8080
#     predicates:
#       - Path=/api/auth/**
#
# We scan sequentially: keep last-seen uri-hostname, capture each Path= predicate.

ROUTE_PATHS=()  # parallel arrays
ROUTE_HOSTS=()

current_host=""
while IFS= read -r line; do
  # match uri lines
  if [[ "$line" =~ uri:[[:space:]]+(\$\{[A-Z_]+:)?http://([a-z-]+):[0-9]+ ]]; then
    current_host="${BASH_REMATCH[2]}"
    continue
  fi
  # match Path= predicates
  if [[ "$line" =~ Path=([^[:space:],]+) ]]; then
    path_pred="${BASH_REMATCH[1]}"
    ROUTE_PATHS+=("$path_pred")
    ROUTE_HOSTS+=("$current_host")
  fi
done < "$GATEWAY_YAML"

# 2) Extract all backend controller paths from kitehub modules
# Combine @RequestMapping class-level + @{Verb}Mapping method-level.

CTRL_PATHS=()    # canonicalized full path
CTRL_MODULES=()  # module dir of controller

for module in kitehub-subscription kitehub-branding kitehub-admin kitehub-email; do
  module_dir="$ROOT/kitehub/$module"
  [[ -d "$module_dir" ]] || continue
  while IFS= read -r java_file; do
    # Extract class-level @RequestMapping (may not exist — set -e tolerant)
    class_path=$(grep -oE '@RequestMapping\s*\(\s*"([^"]+)"' "$java_file" 2>/dev/null | head -1 | sed -E 's/.*"([^"]+)".*/\1/' || true)
    class_path="${class_path:-}"
    # Extract all method-level mappings @PostMapping/@GetMapping/etc.
    while IFS= read -r method_line; do
      [[ -z "$method_line" ]] && continue
      method_path=$(echo "$method_line" | sed -E 's/.*"([^"]*)".*/\1/')
      # Combine class + method path
      if [[ -n "$class_path" && -n "$method_path" ]]; then
        full_path="${class_path}${method_path}"
      elif [[ -n "$class_path" ]]; then
        full_path="$class_path"
      else
        full_path="$method_path"
      fi
      # Normalize double slashes
      full_path=$(echo "$full_path" | sed 's|//|/|g')
      [[ -z "$full_path" || "$full_path" == "/" ]] && continue
      CTRL_PATHS+=("$full_path")
      CTRL_MODULES+=("$module")
    done < <(grep -E '@(Get|Post|Put|Delete|Patch)Mapping\s*\(\s*"' "$java_file" 2>/dev/null)
  done < <(find "$module_dir/src/main/java" -name '*Controller.java' 2>/dev/null)
done

# 3) Cross-check: does each controller path match a gateway route?
#    Match = gateway path predicate prefix matches controller path
#    e.g. Path=/api/auth/** covers /api/auth/login

ORPHAN_CONTROLLERS=()  # controller path:module — not covered by ANY route
WRONG_SERVICE=()       # controller path covered by route, but route uri != module's expected service

# Expected service per module dir (host name without "kitehub-" prefix when matching)
declare -A MODULE_EXPECTED_HOST=(
  ["kitehub-subscription"]="kitehub-subscription"
  ["kitehub-branding"]="kitehub-branding"
  ["kitehub-admin"]="kitehub-admin"
  ["kitehub-email"]="kitehub-email"
)

# Helper: does gateway path predicate cover controller path?
# Gateway predicates use Ant pattern (** = any). Controller paths are literal.
matches_predicate() {
  local pred="$1" ctrl="$2"
  # Strip trailing /** and check prefix
  local pred_prefix="${pred%/\*\*}"
  if [[ "$pred" == "$pred_prefix" ]]; then
    # Exact match required (e.g. Path=/api/auth/register)
    [[ "$pred" == "$ctrl" ]] && return 0
    return 1
  fi
  # Prefix match — controller path must start with pred_prefix/
  [[ "$ctrl" == "$pred_prefix" || "$ctrl" == "$pred_prefix"/* ]]
}

# Allowed fallback routes — these are gateway internal endpoints, not backend
# We don't audit /fallback/** or /docs/**
SKIP_PATH_PATTERNS='^(/fallback|/docs/)'

# GAP-1031 (Wave security-1) — intentionally internal-only controllers that MUST
# NOT have a gateway route (service-to-service only on the docker network). Exposing
# them via the gateway is a security hole. kitehub-email/EmailController is reached
# directly (http://kitehub-email:8080) by EmailConsumer/EmailServiceClient/EmailSenderService.
INTERNAL_ONLY_PATTERNS='^/api/platform/emails'

for i in "${!CTRL_PATHS[@]}"; do
  ctrl_path="${CTRL_PATHS[$i]}"
  ctrl_module="${CTRL_MODULES[$i]}"

  # Skip fallback/docs gateway-internal
  if [[ "$ctrl_path" =~ $SKIP_PATH_PATTERNS ]]; then
    continue
  fi

  # Skip intentionally-internal controllers (no gateway route by design — GAP-1031)
  if [[ "$ctrl_path" =~ $INTERNAL_ONLY_PATTERNS ]]; then
    continue
  fi

  expected_host="${MODULE_EXPECTED_HOST[$ctrl_module]}"
  matched_route=""
  matched_host=""

  for j in "${!ROUTE_PATHS[@]}"; do
    route_path="${ROUTE_PATHS[$j]}"
    route_host="${ROUTE_HOSTS[$j]}"
    if matches_predicate "$route_path" "$ctrl_path"; then
      matched_route="$route_path"
      matched_host="$route_host"
      break
    fi
  done

  if [[ -z "$matched_route" ]]; then
    ORPHAN_CONTROLLERS+=("$ctrl_module: $ctrl_path")
  elif [[ "$matched_host" != "$expected_host" ]]; then
    WRONG_SERVICE+=("$ctrl_module: $ctrl_path routes to $matched_host (expected $expected_host) via $matched_route")
  fi
done

# 4) Check gateway route URIs map to known hosts
UNKNOWN_HOSTS=()
seen_hosts=" "
for host in "${ROUTE_HOSTS[@]}"; do
  [[ -z "$host" ]] && continue
  [[ "$seen_hosts" == *" $host "* ]] && continue
  seen_hosts="$seen_hosts$host "
  if [[ -z "${KNOWN_HOSTS[$host]:-}" ]]; then
    UNKNOWN_HOSTS+=("$host")
  fi
done

# Output
if [[ "${1:-}" == "--json" ]]; then
  printf '{"routes":%d,"controllers":%d,"orphan_controllers":%d,"wrong_service":%d,"unknown_hosts":%d}\n' \
    "${#ROUTE_PATHS[@]}" "${#CTRL_PATHS[@]}" "${#ORPHAN_CONTROLLERS[@]}" \
    "${#WRONG_SERVICE[@]}" "${#UNKNOWN_HOSTS[@]}"
  if [[ ${#ORPHAN_CONTROLLERS[@]} -eq 0 && ${#WRONG_SERVICE[@]} -eq 0 && ${#UNKNOWN_HOSTS[@]} -eq 0 ]]; then
    exit 0
  fi
  exit 1
fi

echo "=== Gateway route ↔ backend controller audit ==="
echo
echo "Scanned: ${#ROUTE_PATHS[@]} gateway routes, ${#CTRL_PATHS[@]} backend controller endpoints"
echo

if [[ ${#UNKNOWN_HOSTS[@]} -gt 0 ]]; then
  echo "❌ UNKNOWN route hosts (${#UNKNOWN_HOSTS[@]}):"
  printf '   %s\n' "${UNKNOWN_HOSTS[@]}"
  echo
fi

if [[ ${#WRONG_SERVICE[@]} -gt 0 ]]; then
  echo "❌ WRONG-SERVICE routing (${#WRONG_SERVICE[@]}):"
  printf '   %s\n' "${WRONG_SERVICE[@]}"
  echo
fi

if [[ ${#ORPHAN_CONTROLLERS[@]} -gt 0 ]]; then
  echo "❌ ORPHAN backend controllers — exposed by backend but NOT covered by any gateway route (${#ORPHAN_CONTROLLERS[@]}):"
  printf '   %s\n' "${ORPHAN_CONTROLLERS[@]}"
  echo
fi

total_findings=$((${#UNKNOWN_HOSTS[@]} + ${#WRONG_SERVICE[@]} + ${#ORPHAN_CONTROLLERS[@]}))
if [[ $total_findings -eq 0 ]]; then
  echo "PASS: every backend controller path has a matching gateway route + every route URI maps to a known service."
  exit 0
fi

echo "FAIL: $total_findings finding(s)."
echo "Fix: either add gateway route(s) in kitehub-gateway/src/main/resources/application.yml,"
echo "     or remove orphan controllers, or correct route URI hostname."
exit 1
