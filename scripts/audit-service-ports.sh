#!/usr/bin/env bash
# audit-service-ports.sh — Spring server.port ↔ docker-compose SERVER_PORT ↔ gateway route port consistency
#
# Verifies for each kitehub-* service:
#   1. application.yml `server.port` default extracted (literal or ${SERVER_PORT:N})
#   2. docker-compose.production.yml SERVER_PORT env override (if any)
#   3. Effective production port = SERVER_PORT env (if set) else application.yml default
#   4. Every gateway route URI targeting this service uses the effective port
#
# Per `.claude/rules/production-env-config-registry.md` v1.1.0 §11.
#
# Exit codes:
#   0 — port chain consistent for every service
#   1 — mismatch between yml default / compose env / gateway route URI
#
# Usage:
#   bash scripts/audit-service-ports.sh
#   bash scripts/audit-service-ports.sh --json

set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
COMPOSE="$ROOT/docker-compose.production.yml"
GATEWAY_YAML="$ROOT/kitehub/kitehub-gateway/src/main/resources/application.yml"

declare -A YAML_DEFAULT       # service -> application.yml port default
declare -A COMPOSE_OVERRIDE   # service -> SERVER_PORT env value in compose (or "")
declare -A EFFECTIVE_PORT     # service -> effective production port

# 1) Extract server.port default from each application.yml
for app_yml in "$ROOT"/kitehub/kitehub-*/src/main/resources/application.yml; do
  [[ -f "$app_yml" ]] || continue
  service=$(basename "$(dirname "$(dirname "$(dirname "$(dirname "$app_yml")")")")")
  # Find `server:` block then `port:` line (literal or ${VAR:default})
  port_line=$(awk '
    /^server:/ {in_server=1; next}
    in_server && /^[a-z]/ {in_server=0}
    in_server && /^[[:space:]]+port:/ {print; exit}
  ' "$app_yml")
  if [[ -z "$port_line" ]]; then
    continue
  fi
  # Possibilities: "  port: 8083" or "  port: ${SERVER_PORT:9000}"
  if [[ "$port_line" =~ \$\{[A-Z_]+:([0-9]+)\} ]]; then
    YAML_DEFAULT[$service]="${BASH_REMATCH[1]}"
  elif [[ "$port_line" =~ port:[[:space:]]+([0-9]+) ]]; then
    YAML_DEFAULT[$service]="${BASH_REMATCH[1]}"
  fi
done

# 2) Extract SERVER_PORT env override from docker-compose.production.yml per service block
if [[ -f "$COMPOSE" ]]; then
  current_service=""
  in_env=0
  while IFS= read -r line; do
    if [[ "$line" =~ ^[[:space:]]{2}(kitehub-[a-z-]+|kite-[a-z-]+):[[:space:]]*$ ]]; then
      current_service="${BASH_REMATCH[1]}"
      in_env=0
      continue
    fi
    if [[ -n "$current_service" && "$line" =~ ^[[:space:]]+environment:[[:space:]]*$ ]]; then
      in_env=1
      continue
    fi
    # Leave environment block when dedent or sibling key
    if [[ $in_env -eq 1 && "$line" =~ ^[[:space:]]{4}[a-z_]+:[[:space:]]*$ ]]; then
      in_env=0
    fi
    if [[ $in_env -eq 1 && "$line" =~ ^[[:space:]]+SERVER_PORT:[[:space:]]+([0-9]+) ]]; then
      COMPOSE_OVERRIDE[$current_service]="${BASH_REMATCH[1]}"
    fi
  done < "$COMPOSE"
fi

# 3) Compute effective port
for service in "${!YAML_DEFAULT[@]}"; do
  if [[ -n "${COMPOSE_OVERRIDE[$service]:-}" ]]; then
    EFFECTIVE_PORT[$service]="${COMPOSE_OVERRIDE[$service]}"
  else
    EFFECTIVE_PORT[$service]="${YAML_DEFAULT[$service]}"
  fi
done

# 4) Cross-check gateway route URIs
declare -a ROUTE_HOST_PORT  # entries "host:port"
if [[ -f "$GATEWAY_YAML" ]]; then
  while IFS= read -r line; do
    if [[ "$line" =~ uri:[[:space:]]+(\$\{[A-Z_]+:)?http://([a-z-]+):([0-9]+) ]]; then
      ROUTE_HOST_PORT+=("${BASH_REMATCH[2]}:${BASH_REMATCH[3]}")
    fi
  done < "$GATEWAY_YAML"
fi

declare -a FINDINGS=()  # explicit empty init — bash strict mode `set -u` trips on reference before any append

for service in "${!EFFECTIVE_PORT[@]}"; do
  eff="${EFFECTIVE_PORT[$service]}"
  yml="${YAML_DEFAULT[$service]}"
  ovr="${COMPOSE_OVERRIDE[$service]:-<none>}"

  # gateway uses this service?
  for entry in "${ROUTE_HOST_PORT[@]}"; do
    route_host="${entry%:*}"
    route_port="${entry#*:}"
    if [[ "$route_host" == "$service" ]]; then
      if [[ "$route_port" != "$eff" ]]; then
        FINDINGS+=("$service: gateway route uri uses port $route_port but effective production port = $eff (yml default=$yml, SERVER_PORT env=$ovr)")
      fi
    fi
  done
done

# Output
if [[ "${1:-}" == "--json" ]]; then
  printf '{"services_scanned":%d,"findings":%d}\n' "${#EFFECTIVE_PORT[@]}" "${#FINDINGS[@]}"
  exit $([ ${#FINDINGS[@]} -eq 0 ] && echo 0 || echo 1)
fi

echo "=== Service port chain audit ==="
echo
echo "Per-service effective production ports:"
for service in $(echo "${!EFFECTIVE_PORT[@]}" | tr ' ' '\n' | sort); do
  yml="${YAML_DEFAULT[$service]}"
  ovr="${COMPOSE_OVERRIDE[$service]:-<none>}"
  eff="${EFFECTIVE_PORT[$service]}"
  printf '   %-25s yml-default=%-5s  SERVER_PORT-env=%-5s  effective=%s\n' "$service" "$yml" "$ovr" "$eff"
done
echo

if [[ ${#FINDINGS[@]} -gt 0 ]]; then
  echo "❌ MISMATCH gateway-route ↔ effective-port (${#FINDINGS[@]}):"
  printf '   %s\n' "${FINDINGS[@]}"
  echo
  echo "FAIL: ${#FINDINGS[@]} port-chain finding(s)."
  echo "Fix: align gateway route URI port with SERVER_PORT env override OR application.yml default."
  exit 1
fi

echo "PASS: every gateway route URI port matches its target service's effective production port."
exit 0
