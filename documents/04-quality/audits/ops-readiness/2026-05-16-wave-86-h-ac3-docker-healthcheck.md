---
title: Docker Compose healthcheck + restart spec audit — Wave 86 Bucket H H-AC3
status: complete
created: 2026-05-16
wave: 86
bucket: H
gaps: []
---

# Docker Compose Healthcheck + Restart Spec Audit — Wave 86 Bucket H H-AC3

## Scope

Verify `kitehub/docker-compose.kitehub.yml` (canonical compose file per CLAUDE.md) — every service has:
- `healthcheck:` block (with `test`, `interval`, `timeout`, `retries`)
- `restart: unless-stopped` (or `no` for explicit one-off setup containers)

H-AC3 (Wave 86 plan §3) per `release-deploy-standard.md` §3.1 Reliability pillar.

## Methodology

Per-service inspection via parser script (Python AST-like traversal over compose YAML). Setup/one-off containers (`*-setup`, `*-backup`, `kite-base` build-only) classified as "exempt" — they correctly DON'T need healthcheck but DO benefit from `restart: on-failure`.

## Results table

| # | Service | Type | Healthcheck | Restart | Verdict |
|---|---|---|---|---|---|
| 1 | `kite-postgres` | Infra (data) | ✅ pg_isready | ❌ NO | ⚠️ PARTIAL — add `restart: unless-stopped` |
| 2 | `kite-redis` | Infra (cache) | ✅ redis-cli ping | ❌ NO | ⚠️ PARTIAL — add restart |
| 3 | `kite-rabbitmq` | Infra (queue) | ✅ rabbitmq-diagnostics | ❌ NO | ⚠️ PARTIAL — add restart |
| 4 | `kite-minio` | Infra (object store) | ✅ mc ready | ❌ NO | ⚠️ PARTIAL — add restart |
| 5 | `kite-mailhog` | Dev-only test mail | ❌ NO | ❌ NO | ⚠️ Dev-only acceptable v1 |
| 6 | `kite-minio-setup` | One-off bootstrap | ❌ NO (exempt) | ✅ on-failure-ish | ✅ PASS (exempt) |
| 7 | `kite-minio-backup` | Cron-like backup | ❌ NO (exempt) | ✅ restart | ✅ PASS (exempt) |
| 8 | `kite-prometheus` | Infra (observability) | ❌ NO | ✅ restart | ⚠️ PARTIAL — add healthcheck `curl http://localhost:9090/-/healthy` |
| 9 | `kite-grafana` | Infra (observability) | ❌ NO | ✅ restart | ⚠️ PARTIAL — add healthcheck `curl http://localhost:3000/api/health` |
| 10 | `kite-ollama` | Infra (AI) | ✅ ollama ps | ❌ NO | ⚠️ PARTIAL — add restart (AI optional Phase 1 BETA) |
| 11 | `kite-ollama-setup` | One-off model pull | ❌ NO (exempt) | ✅ restart | ✅ PASS (exempt) |
| 12 | `kite-base` | Build-only base | ❌ NO (exempt) | ❌ NO (exempt) | ✅ PASS (exempt — not runtime) |
| 13 | `kitehub-subscription` | App (Java) | ✅ /actuator/health | ✅ restart | ✅ PASS |
| 14 | `kitehub-branding` | App (Java) | ✅ /actuator/health | ✅ restart | ✅ PASS |
| 15 | `kitehub-email` | App (Java) | ✅ /actuator/health | ✅ restart | ✅ PASS |
| 16 | `kitehub-admin` | App (Java) | ✅ /actuator/health | ✅ restart | ✅ PASS |
| 17 | `kite-gateway` | App (Java) | ✅ /actuator/health | ✅ restart | ✅ PASS |
| 18 | `kiteclass-core` | App (Java) | ✅ /actuator/health | ✅ restart | ✅ PASS |
| 19 | `kitehub-frontend` | App (Next.js) | ❌ NO | ✅ restart | ⚠️ PARTIAL — add `curl -f http://localhost:3000/ \|\| exit 1` healthcheck |
| 20 | `kiteclass-frontend` | App (Next.js) | ❌ NO | ✅ restart | ⚠️ PARTIAL — add healthcheck similar |

## Summary

- Total services: 20 (excluding volumes/networks)
- PASS: 11 (6 backend apps + 4 exempt one-off + 1 build-base)
- PARTIAL: 8 (4 infra missing restart, 2 observability missing healthcheck, 2 frontend missing healthcheck)
- Dev-only acceptable v1: 1 (kite-mailhog)
- FAIL: 0

## Overall verdict: PARTIAL (acceptable v1; small fixes recommended)

Phase 1 BETA infra primarily runs on AWS ECS/EC2 (not Docker Compose) — compose là local dev + integration test. So missing healthcheck on infra services impacts dev experience > production. Fix recommended cho:
1. Restart policy on data services (postgres/redis/rabbitmq/minio) — protects from container drift during dev
2. Healthcheck on observability (prometheus/grafana) + frontends — better stack-up signal

## Recommendations

1. **Apply small fixes** (paired same PR if scope allows, OR file GAP-NEW-20 P2):
   - Add `restart: unless-stopped` to kite-postgres / kite-redis / kite-rabbitmq / kite-minio / kite-ollama
   - Add `healthcheck:` to kite-prometheus / kite-grafana (HTTP probe endpoints exist)
   - Add `healthcheck:` to kitehub-frontend + kiteclass-frontend (Next.js)
2. **Verify production parity** — when ECS task definitions are reviewed (Wave 87+), apply same standard
3. **Cross-link to `release-deploy-standard.md` §3.1** Reliability pillar Phase 1 BETA acceptable v1 with follow-up gap

## References

- `kitehub/docker-compose.kitehub.yml` (canonical compose)
- `.claude/rules/release-deploy-standard.md` §3.1 Reliability pillar
- Wave 86 plan §3 Bucket H H-AC3
- `documents/04-quality/audits/ops-readiness/2026-05-15-wave-84-post-apply.md` (78/100 baseline)
