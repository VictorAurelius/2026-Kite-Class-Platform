# GAP-1052: Wire audit-gateway-routes.sh as HARD CI gate (WARN → FAIL)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (META — force-multiplier per `meta-gap-priority.md` §3)
**Domain:** Backend (gateway) / DevOps — meta
**Found:** 2026-06-07 (GAP-1042 closure — spun off the optional CI-guard AC)
**Affects:** `scripts/audit-gateway-routes.sh` + `.github/workflows/quality-*.yml`

## Problem

GAP-1042 (gateway route-predicate audit META) closed với detector `audit-gateway-routes.sh` đã extended trong #2228 (BS#1 kiteclass-core scan + BS#2 TenantResolver-400 model). Detector hiện chạy WARN-mode (hoặc chưa wire CI). Để eliminate routing-collision class prospectively (catch shadow/expose ở CI thay vì per-flow walk), cần flip detector thành HARD gate (exit 1 on collision).

Currently deferred per `production-env-config-registry.md` §11.

## Proposed Fix

1. Verify `audit-gateway-routes.sh` exit-code semantics (exit 1 on real collision, exit 0 clean với carve-outs present).
2. Wire CI job trong `quality-*.yml` (likely `quality-infra.yml` hoặc `quality-code.yml`) — initially WARN, then flip HARD sau khi confirm 0 false-positives trên current main.
3. Confirm INTERNAL_ONLY_PATTERNS exemption (email) + URI-template `{var}` matcher robustness (BS#1 false-positive guard).

## Acceptance Criteria

- [ ] `audit-gateway-routes.sh` exit 1 khi collision present (synthetic test: remove a carve-out → detector fails)
- [ ] CI job wired + green on current main (0 false-positives)
- [ ] Gate flipped WARN → FAIL (HARD)
- [ ] `production-env-config-registry.md` §11 updated to reflect HARD gate active

## Related

- Parent: GAP-1042 (gateway route-predicate audit META — closed 2026-06-07, this is the spun-off optional CI-guard AC)
- Sister: GAP-1049 (5 carve-out collisions — closed)
- `meta-gap-priority.md` §3 — META P2 force-multiplier (CI gate → eliminate routing-collision recurrence prospectively)
- `production-env-config-registry.md` §11 — current detector ownership + deferral note
