# GAP-505: docker-compose healthcheck port mismatch for 4 BE services

**Status:** 🟢 DONE
**Priority:** 🟠 P1 (Phase 1 BETA — docker status cosmetic issue masks real health signal)
**Domain:** DevOps
**Found:** 2026-05-13 (Wave 70 GAP-502 live ops force-recreate verification)
**Affects:** docker container health status for `kitehub-{subscription,branding,email,admin}` — gateway unaffected

## Problem

After Wave 70 Bucket C force-recreate, 4/5 kitehub-* services persisted "unhealthy" docker status despite Spring Boot init clean + API requests succeeding via gateway. Investigation showed image-level Dockerfile `HEALTHCHECK` baked in checking port **8080**, but services actually listen on per-service ports:

| Service | Image healthcheck port | Actual listen port (per application.yml) |
|---|---|---|
| kitehub-subscription | 8080 (Dockerfile) | **8081** (`SERVER_PORT:8081`) |
| kitehub-branding | 8080 | **8083** |
| kitehub-email | 8080 | **8084** |
| kitehub-admin | 8080 | **8083** |
| kitehub-gateway | 8080 | **8080** (correct — `SERVER_PORT: 8080` env override in compose) |

Wave 70 Bucket C initial scope added compose-level healthcheck `curl localhost:8080` for all 5 → would have caused unhealthy → restart loop on 4 BE. Mid-session fix reverted those healthchecks → 4 BE services fell back to image baked-in healthcheck → still port 8080 wrong → "unhealthy" status persists. ALB/gateway routing works (ALB checks gateway only), but docker status misleads operators + breaks `docker compose --wait` patterns.

## Root Cause

Dockerfile-level `HEALTHCHECK CMD-SHELL curl -f http://localhost:8080/actuator/health || exit 1` baked into 4 BE images assumes all services listen on 8080. Reality post-Vercel-pivot: each service has unique port from application.yml. Compose-level healthcheck override needed to use correct port.

## Proposed Fix

**`docker-compose.production.yml`** — override Dockerfile healthcheck per BE service with correct port:

| Service | Override healthcheck command |
|---|---|
| kitehub-subscription | `curl -f http://localhost:8081/actuator/health \|\| exit 1` |
| kitehub-branding | `curl -f http://localhost:8083/actuator/health \|\| exit 1` |
| kitehub-email | `curl -f http://localhost:8084/actuator/health \|\| exit 1` |
| kitehub-admin | `curl -f http://localhost:8083/actuator/health \|\| exit 1` |
| kitehub-gateway | (unchanged — already correct on 8080) |

All BE healthchecks: `interval: 30s, timeout: 10s, retries: 3, start_period: 150s` (matches Spring init time ~70-130s with buffer).

## Acceptance Criteria

- [x] 4 BE services have explicit healthcheck override in `docker-compose.production.yml` với per-service correct port
- [x] start_period 150s ≥ slowest observed Spring init (132s branding)
- [x] All 5 services transition to `(healthy)` ≥10 min post-deploy
- [x] No restart loop triggered by healthcheck false-negative

## Long-term scope (Future)

- Fix at Dockerfile level: parameterize `HEALTHCHECK` to use `${SERVER_PORT}` env var. Requires per-service Dockerfile change + image rebuild. Deferred to next image refresh wave.
- Or: standardize all BE services to listen on 8080 internally (port conflict-free within docker network). Requires application.yml refactor per service. Deferred.

## Related

- Parent: GAP-502 RC1+RC2 (Wave 70)
- Sibling: GAP-504 (rabbit user self-heal)
- Image healthcheck source: per-service Dockerfile (kitehub/*/Dockerfile lines TBD)
- Rule applied: `release-deploy-standard.md` §3.1 reliability pillar (health check endpoint)

## Log

- **2026-05-13:** Filed during Wave 70 GAP-502 live ops post-force-recreate. Inspected via `docker inspect <ctr> --format .Config.Healthcheck` — confirmed 4 BE images carry port-8080 HEALTHCHECK from Dockerfile. Compose-level override per-service shipped in same fix PR.
