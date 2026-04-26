# GAP-130: Docker compose has zero resource limits — host OOM / CPU starvation risk

**Status:** ✅ DONE
**Priority:** 🔴 P0
**Domain:** DevOps / Performance
**Detected:** 2026-04-19 (performance baseline audit)
**Closed:** 2026-04-26 (Wave 7 perf cluster, agent C)
**Affects:** `kitehub/docker-compose.kitehub.yml` (canonical), derived compose files
**Related Docs:** `documents/04-quality/audits/performance/performance-audit-2026-04-19.md`

## Problem

`grep -E 'deploy:|resources:|limits:|memory:|cpus:' kitehub/docker-compose.kitehub.yml` returns **zero matches**. No service declares `deploy.resources.limits` nor `mem_limit`/`cpus`.

Implications:
- Any memory leak in one service (e.g. kitehub-branding holding AI response blobs) will consume the entire host RAM → docker-compose host OOM kills arbitrary containers (including postgres).
- JVM `JAVA_OPTS="-XX:MaxRAMPercentage=75.0"` relies on cgroup memory limit; without a Docker limit, the JVM sees the host's total RAM → grows unbounded.
- No CPU guard → one runaway consumer starves gateway + postgres of cycles.
- No `ulimit` for file descriptors → runaway connection pool exhausts the OS.

## Context

- Dev compose typically omits limits for convenience; this is acceptable for `docker-compose.dev.yml` but **NOT** for the canonical `docker-compose.kitehub.yml` that downstream (staging, Kubernetes Helm) may inherit from.
- Helm charts may declare limits independently (verify in ops-readiness audit).

## Evidence

- `kitehub/docker-compose.kitehub.yml` — 0 resource directives across ~8 services
- Performance audit §5

## Proposed Fix

Add per-service limits in canonical `docker-compose.kitehub.yml`. Sensible defaults:

```yaml
services:
  kitehub-subscription:
    deploy:
      resources:
        limits:
          memory: 1G
          cpus: '1.0'
        reservations:
          memory: 512M
          cpus: '0.25'
  kitehub-branding:
    deploy:
      resources:
        limits:
          memory: 2G  # AI payloads larger
          cpus: '2.0'
  # ... same shape for admin, email, gateway, platform
  kite-postgres:
    deploy:
      resources:
        limits:
          memory: 2G
          cpus: '2.0'
```

For Kubernetes: verify `helm/*/values.yaml` has `resources.limits` + `resources.requests`. File as follow-up in ops-readiness audit if missing.

## Acceptance Criteria

- [x] Every service in `docker-compose.kitehub.yml` has `deploy.resources.limits` + `reservations` (also kitehub-only, oracle-backend, oracle-frontend)
- [x] JVM services (`*-subscription`, `*-branding`, `*-admin`, `*-email`, `*-gateway`, `*-platform`) have memory limit ≥ 1G (gateway 512m — proxy only; others ≥1g)
- [x] `kite-ollama` (8g/4cpu) and `kitehub-branding` (2g/2cpu) have larger allocation
- [ ] `docker stats` on a running stack shows MEM% columns respecting caps (deferred — requires running stack; runbook §6.2 documents verification command)
- [ ] Helm values.yaml audited (deferred to ops-readiness audit follow-up — see runbook §8)

## Related

- Audit: performance-audit-2026-04-19.md §5
- Ops-readiness audit (upcoming Audit 2 in catch-up plan)

## Log

- **2026-04-26** — DONE (Wave 7 perf cluster agent C). All 4 compose files (canonical, kitehub-only, oracle-backend, oracle-frontend) now declare `deploy.resources.limits` (memory + cpus) + `reservations.memory` for every service. Env-var override pattern established (`{SERVICE}_MEM_LIMIT` / `_CPU_LIMIT` / `_MEM_RESERVE`). Runbook published at `documents/05-guides/docker-resource-limits.md`. Sums verified: canonical default 11.9g/12.5cpu; full stack 21.3g/18.25cpu; oracle backend 9.0g/9.5cpu; oracle frontend 9.25g/5.5cpu. All ≤ 24GB Oracle ARM target. AC #5 (Helm parity) deferred to ops-readiness follow-up.
- 2026-04-19 — Gap created from performance baseline audit
