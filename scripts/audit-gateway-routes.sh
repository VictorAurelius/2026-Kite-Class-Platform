#!/usr/bin/env bash
# audit-gateway-routes.sh — gateway route ↔ backend controller coverage audit
#
# Verifies for kitehub-gateway application.yml:
#   1. Every gateway route URI hostname is a known kitehub-*/kiteclass-* service
#   2. Every backend controller @*Mapping path is covered by ≥1 gateway route predicate
#   3. Routes don't point services to the wrong backend (wrong-service routing)
#   4. PUBLIC-by-design controller paths (webhook / redeem / public / by-token / landing /
#      accept) are NOT first-matched by a TenantResolver-bearing route — otherwise the
#      provider/recipient (no tenant context) gets a 400 even though the service is correct.
#      This is the "TenantResolver-400 collision" class (GAP-1049 C4/C5 / BS#2).
#
# BS#1 (GAP-1049): scan scope INCLUDES kiteclass-core so kiteclass controllers shadowed by
# a kitehub catch-all (e.g. /api/v1/admin/parent/consent shadowed by kitehub-admin-v1) are
# visible — this was the root cause of recurrence (detector only saw 4 kitehub modules).
#
# Per `.claude/rules/production-env-config-registry.md` v1.1.0 §11.
#
# Exit codes:
#   0 — all backend controller paths covered + correct service + no TenantResolver collision
#   1 — orphan controllers OR orphan routes OR wrong-service OR TenantResolver collision
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

# Known service hostnames mapped to module dir
declare -A KNOWN_HOSTS=(
  ["kitehub-subscription"]="kitehub/kitehub-subscription"
  ["kitehub-branding"]="kitehub/kitehub-branding"
  ["kitehub-admin"]="kitehub/kitehub-admin"
  ["kitehub-email"]="kitehub/kitehub-email"
  ["kiteclass-core"]="kiteclass/kiteclass-core"
)

# 1) Extract routes per block: each route has form
#   - id: foo
#     uri: http://kitehub-subscription:8080
#     predicates:
#       - Path=/api/auth/**,/api/auth/v2/**   # comma-separated multi-path supported
#     filters:
#       - TenantResolver                       # tracked per route (BS#2)
#       - name: CircuitBreaker
#
# Route order = declaration order = Spring Cloud Gateway match order: a controller path
# is "owned" by the FIRST route whose predicate matches it.

ROUTE_PATHS=()  # parallel arrays
ROUTE_HOSTS=()
ROUTE_TR=()     # "1" if route applies TenantResolver, else "0"

current_host=""
current_tr="0"
current_paths=()

flush_route() {
  local p
  for p in "${current_paths[@]:-}"; do
    [[ -z "$p" ]] && continue
    ROUTE_PATHS+=("$p")
    ROUTE_HOSTS+=("$current_host")
    ROUTE_TR+=("$current_tr")
  done
  current_paths=()
  current_tr="0"
}

while IFS= read -r line; do
  # New route block — flush the previous route, reset accumulators
  if [[ "$line" =~ ^[[:space:]]*-[[:space:]]+id: ]]; then
    flush_route
    current_host=""
    continue
  fi
  # uri line → capture hostname (optionally wrapped in ${VAR:...})
  if [[ "$line" =~ uri:[[:space:]]+(\$\{[A-Z_]+:)?http://([a-z-]+):[0-9]+ ]]; then
    current_host="${BASH_REMATCH[2]}"
    continue
  fi
  # Path= predicate (may be comma-separated: Path=/a/**,/b/**)
  if [[ "$line" =~ Path=([^[:space:]]+) ]]; then
    local_paths="${BASH_REMATCH[1]}"
    IFS=',' read -ra split_paths <<< "$local_paths"
    for sp in "${split_paths[@]}"; do
      [[ -n "$sp" ]] && current_paths+=("$sp")
    done
    continue
  fi
  # TenantResolver filter line (bare list item) → mark route
  if [[ "$line" =~ ^[[:space:]]*-[[:space:]]+TenantResolver[[:space:]]*$ ]]; then
    current_tr="1"
  fi
done < "$GATEWAY_YAML"
flush_route  # flush the last route block

# 2) Extract all backend controller paths.
# BS#1: scan kiteclass-core in addition to the 4 kitehub modules so kiteclass
# controllers shadowed by a kitehub catch-all are visible.

CTRL_PATHS=()    # canonicalized full path
CTRL_MODULES=()  # module key of controller

# module-key → source root + expected gateway host
declare -A MODULE_SRC=(
  ["kitehub-subscription"]="$ROOT/kitehub/kitehub-subscription/src/main/java"
  ["kitehub-branding"]="$ROOT/kitehub/kitehub-branding/src/main/java"
  ["kitehub-admin"]="$ROOT/kitehub/kitehub-admin/src/main/java"
  ["kitehub-email"]="$ROOT/kitehub/kitehub-email/src/main/java"
  ["kiteclass-core"]="$ROOT/kiteclass/kiteclass-core/src/main/java"
)
declare -A MODULE_EXPECTED_HOST=(
  ["kitehub-subscription"]="kitehub-subscription"
  ["kitehub-branding"]="kitehub-branding"
  ["kitehub-admin"]="kitehub-admin"
  ["kitehub-email"]="kitehub-email"
  ["kiteclass-core"]="kiteclass-core"
)

for module in kitehub-subscription kitehub-branding kitehub-admin kitehub-email kiteclass-core; do
  src_root="${MODULE_SRC[$module]}"
  [[ -d "$src_root" ]] || continue
  while IFS= read -r java_file; do
    # Extract class-level @RequestMapping (may not exist)
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
      full_path="${full_path//\/\//\/}"
      [[ -z "$full_path" || "$full_path" == "/" ]] && continue
      CTRL_PATHS+=("$full_path")
      CTRL_MODULES+=("$module")
    done < <(grep -E '@(Get|Post|Put|Delete|Patch)Mapping\s*\(\s*"' "$java_file" 2>/dev/null)
  done < <(find "$src_root" -name '*Controller.java' 2>/dev/null)
done

# 3) Cross-check each controller path against gateway routes (first-match wins).

ORPHAN_CONTROLLERS=()   # controller path:module — not covered by ANY route
WRONG_SERVICE=()        # covered, but route uri != module's expected service
TENANT_RESOLVER_COLL=() # BS#2 — public-by-design path first-matched by a TenantResolver route

# Convert a Spring Ant route predicate to an anchored regex.
#   trailing /** → optional subpath (Ant: /foo/** also matches /foo)
#   internal **  → .*       (multi-segment)
#   single *     → [^/]*    (within one path segment)
predicate_to_regex() {
  local pred="$1" trailing=""
  if [[ "$pred" == */\*\* ]]; then
    pred="${pred%/\*\*}"
    trailing="(/.*)?"
  fi
  # Normalize wildcards + URI-template vars to tokens (alnum, not regex-special, never in paths).
  # Spring Path predicate {id} is a single-segment URI-template variable — same as *.
  pred="${pred//\*\*/MULTIWILDCARD}"
  pred="${pred//\*/SINGLEWILDCARD}"
  pred=$(printf '%s' "$pred" | sed -E 's/\{[^}]+\}/SINGLEWILDCARD/g')
  # Escape regex metacharacters in the literal remainder
  pred=$(printf '%s' "$pred" | sed -E 's/[.[\]()+?^$|]/\\&/g')
  pred="${pred//MULTIWILDCARD/.*}"
  pred="${pred//SINGLEWILDCARD/[^/]*}"
  printf '^%s%s$' "$pred" "$trailing"
}

# Helper: does gateway path predicate cover controller path?
# Controller path-variables {id} are normalized to a single non-slash token so a
# route segment wildcard (*) matches them (e.g. /api/v1/branding/*/versions/** covers
# /api/v1/branding/{instanceId}/versions). Without this BS#1 (kiteclass-core scan)
# would false-positive on every {var} + mid-path-* route.
matches_predicate() {
  local pred="$1" ctrl="$2" ctrl_norm re
  ctrl_norm=$(printf '%s' "$ctrl" | sed -E 's/\{[^}]+\}/X/g')
  re=$(predicate_to_regex "$pred")
  [[ "$ctrl_norm" =~ $re ]]
}

# Gateway-internal paths we don't audit
SKIP_PATH_PATTERNS='^(/fallback|/docs/)'

# GAP-1031 — intentionally internal-only controllers (no gateway route by design,
# reached directly on the docker network). kitehub-email /api/platform/emails (GAP-1031)
# + kiteclass-core /internal/** service-to-service endpoints (provisioning notify,
# cross-service reference lookups) — never exposed via the gateway.
INTERNAL_ONLY_PATTERNS='^(/api/platform/emails|/internal/)'

# BS#2 — PUBLIC-by-design controller path markers. A path matching these is reached by a
# caller WITHOUT tenant context (payment provider, email-link recipient, anonymous SSR), so
# a TenantResolver-bearing route that first-matches it returns 400 — a routing collision even
# though the URI points to the correct service.
PUBLIC_PATH_PATTERNS='(/webhook|/redeem/|/redeem$|/public$|/public/|/by-token/|/landing$|/accept$)'

for i in "${!CTRL_PATHS[@]}"; do
  ctrl_path="${CTRL_PATHS[$i]}"
  ctrl_module="${CTRL_MODULES[$i]}"

  # Skip fallback/docs gateway-internal
  [[ "$ctrl_path" =~ $SKIP_PATH_PATTERNS ]] && continue
  # Skip intentionally-internal controllers (GAP-1031)
  [[ "$ctrl_path" =~ $INTERNAL_ONLY_PATTERNS ]] && continue

  expected_host="${MODULE_EXPECTED_HOST[$ctrl_module]}"
  matched_route=""
  matched_host=""
  matched_tr="0"

  for j in "${!ROUTE_PATHS[@]}"; do
    if matches_predicate "${ROUTE_PATHS[$j]}" "$ctrl_path"; then
      matched_route="${ROUTE_PATHS[$j]}"
      matched_host="${ROUTE_HOSTS[$j]}"
      matched_tr="${ROUTE_TR[$j]}"
      break
    fi
  done

  if [[ -z "$matched_route" ]]; then
    ORPHAN_CONTROLLERS+=("$ctrl_module: $ctrl_path")
  elif [[ "$matched_host" != "$expected_host" ]]; then
    WRONG_SERVICE+=("$ctrl_module: $ctrl_path routes to $matched_host (expected $expected_host) via $matched_route")
  elif [[ "$matched_tr" == "1" && "$ctrl_path" =~ $PUBLIC_PATH_PATTERNS ]]; then
    # Right service, but a public-by-design path first-matched by a TenantResolver route → 400
    TENANT_RESOLVER_COLL+=("$ctrl_module: $ctrl_path (PUBLIC) first-matched by TenantResolver route $matched_route → 400 (carve a no-TenantResolver route before it)")
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
  printf '{"routes":%d,"controllers":%d,"orphan_controllers":%d,"wrong_service":%d,"tenant_resolver_collision":%d,"unknown_hosts":%d}\n' \
    "${#ROUTE_PATHS[@]}" "${#CTRL_PATHS[@]}" "${#ORPHAN_CONTROLLERS[@]}" \
    "${#WRONG_SERVICE[@]}" "${#TENANT_RESOLVER_COLL[@]}" "${#UNKNOWN_HOSTS[@]}"
  if [[ ${#ORPHAN_CONTROLLERS[@]} -eq 0 && ${#WRONG_SERVICE[@]} -eq 0 \
        && ${#TENANT_RESOLVER_COLL[@]} -eq 0 && ${#UNKNOWN_HOSTS[@]} -eq 0 ]]; then
    exit 0
  fi
  exit 1
fi

echo "=== Gateway route ↔ backend controller audit ==="
echo
echo "Scanned: ${#ROUTE_PATHS[@]} gateway routes, ${#CTRL_PATHS[@]} backend controller endpoints (incl. kiteclass-core)"
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

if [[ ${#TENANT_RESOLVER_COLL[@]} -gt 0 ]]; then
  echo "❌ TENANT-RESOLVER collision — PUBLIC path shadowed by a TenantResolver route → 400 (${#TENANT_RESOLVER_COLL[@]}):"
  printf '   %s\n' "${TENANT_RESOLVER_COLL[@]}"
  echo
fi

if [[ ${#ORPHAN_CONTROLLERS[@]} -gt 0 ]]; then
  echo "❌ ORPHAN backend controllers — exposed by backend but NOT covered by any gateway route (${#ORPHAN_CONTROLLERS[@]}):"
  printf '   %s\n' "${ORPHAN_CONTROLLERS[@]}"
  echo
fi

total_findings=$((${#UNKNOWN_HOSTS[@]} + ${#WRONG_SERVICE[@]} + ${#TENANT_RESOLVER_COLL[@]} + ${#ORPHAN_CONTROLLERS[@]}))
if [[ $total_findings -eq 0 ]]; then
  echo "PASS: every backend controller path has a matching gateway route + correct service + no TenantResolver collision."
  exit 0
fi

echo "FAIL: $total_findings finding(s)."
echo "Fix: add/correct gateway route(s) in kitehub-gateway/src/main/resources/application.yml"
echo "     (specific carve-outs MUST precede catch-alls; PUBLIC paths SKIP TenantResolver),"
echo "     or remove orphan controllers, or correct route URI hostname."
exit 1
