#!/usr/bin/env bash
# check-gateway-shared-breaker.sh — pre-walk static check: Gateway circuit
# breaker shared between read + write paths under same route family
#
# Origin: Wave flow-kh1 G2 walk session 2026-06-04 — GAP-928 surfaced that a
# single CircuitBreaker `authCircuitBreaker` gated BOTH read (login/refresh)
# and write (register) endpoints. A slow 2FA verify trip the breaker for
# unrelated register attempts. Phase 1 fix attempted didn't carve write
# path; Phase 2 introduced authWriteCircuitBreaker to isolate writes.
#
# Heuristic:
#   Parse kitehub/kitehub-gateway/src/main/resources/application.yml routes.
#   For each route: capture (predicates.Method, predicates.Path,
#   filters.CircuitBreaker.args.name).
#   Group route entries by CircuitBreaker.name. Within each group, examine
#   the set of HTTP methods and path families:
#     - If methods include BOTH a read-class (GET/HEAD) AND a write-class
#       (POST/PUT/PATCH/DELETE) for the SAME breaker-name AND paths share
#       the same family prefix (/api/auth/**, /api/v1/auth/**, /api/v1/instances/**)
#       → WARN: shared breaker between read + write under auth/instances family.
#
# Default: exit 1 if shared (BLOCKING).
# --warn: exit 0 + WARN (advisory).
#
# Per pre-walk-static-audit-bundle.md §3 (this rule landing same PR).

set -euo pipefail

WARN_ONLY=0
for arg in "$@"; do
  case "$arg" in
    --warn) WARN_ONLY=1 ;;
    -h|--help)
      echo "Usage: $0 [--warn]"
      echo "  --warn   exit 0 even if matches (default: exit 1)"
      exit 0
      ;;
  esac
done

YAML="kitehub/kitehub-gateway/src/main/resources/application.yml"
if [ ! -f "$YAML" ]; then
  echo "INFO: $YAML not found — skipping gateway-shared-breaker check"
  exit 0
fi

# Delegate YAML walk to Python (PyYAML universally available)
python3 - "$YAML" "$WARN_ONLY" <<'PYEOF'
import sys
import yaml

yaml_path = sys.argv[1]
warn_only = sys.argv[2] == "1"

SENSITIVE_FAMILIES = (
    "/api/auth/",
    "/api/v1/auth/",
    "/api/v1/instances/",
)

READ_METHODS = {"GET", "HEAD"}
WRITE_METHODS = {"POST", "PUT", "PATCH", "DELETE"}


def extract_predicates(preds):
    """Return (methods_set, paths_list) from a route's `predicates` list."""
    methods = set()
    paths = []
    if not isinstance(preds, list):
        return methods, paths
    for p in preds:
        if not isinstance(p, str):
            continue
        if p.startswith("Method="):
            for m in p[len("Method="):].split(","):
                methods.add(m.strip().upper())
        elif p.startswith("Path="):
            for path in p[len("Path="):].split(","):
                paths.append(path.strip())
    return methods, paths


def extract_breaker_name(filters):
    if not isinstance(filters, list):
        return None
    for f in filters:
        if not isinstance(f, dict):
            continue
        if f.get("name") == "CircuitBreaker":
            args = f.get("args") or {}
            return args.get("name")
    return None


try:
    with open(yaml_path) as fh:
        data = yaml.safe_load(fh)
except Exception as exc:
    print(f"WARN: unable to parse {yaml_path}: {exc}")
    sys.exit(0)

routes = (
    (((data or {}).get("spring") or {}).get("cloud") or {}).get("gateway") or {}
).get("routes") or []

# group: breaker_name -> list of (route_id, methods, paths)
groups = {}
for r in routes:
    if not isinstance(r, dict):
        continue
    rid = r.get("id", "<unknown>")
    breaker = extract_breaker_name(r.get("filters"))
    if not breaker:
        continue
    methods, paths = extract_predicates(r.get("predicates"))
    groups.setdefault(breaker, []).append((rid, methods, paths))

violations = []
for breaker, entries in groups.items():
    # Collect aggregate methods + path families touched
    all_methods = set()
    all_paths = []
    for _rid, methods, paths in entries:
        all_methods |= methods
        all_paths += paths

    has_read = bool(all_methods & READ_METHODS)
    has_write = bool(all_methods & WRITE_METHODS)
    if not (has_read and has_write):
        continue

    # Check at least one sensitive family is matched
    families_hit = set()
    for path in all_paths:
        for fam in SENSITIVE_FAMILIES:
            if path.startswith(fam):
                families_hit.add(fam)
                break

    if not families_hit:
        continue

    violations.append((breaker, sorted(all_methods), sorted(families_hit), entries))

if not violations:
    print("OK: no shared read+write circuit breakers under auth/instances families")
    sys.exit(0)

print()
for breaker, methods, families, entries in violations:
    print(f"❌ CircuitBreaker `{breaker}` shared across read+write paths")
    print(f"    methods : {', '.join(methods)}")
    print(f"    families: {', '.join(families)}")
    print(f"    routes  :")
    for rid, ms, ps in entries:
        ms_str = ",".join(sorted(ms)) if ms else "?"
        ps_str = ",".join(ps) if ps else "?"
        print(f"      - {rid:30s} [{ms_str}] {ps_str}")
    print()

print(f"FAIL: {len(violations)} shared read+write breaker(s) under auth/instances families.")
print("      Per GAP-928 lesson — carve write breaker (e.g. authWriteCircuitBreaker)")
print("      so a slow read endpoint cannot trip the write path.")
if warn_only:
    print("      (--warn mode → exit 0)")
    sys.exit(0)
sys.exit(1)
PYEOF
