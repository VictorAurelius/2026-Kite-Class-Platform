# GAP-407: Docker Compose Profiles cho Dev (27GB RAM friendly)

**Status:** 🟢 DONE 2026-05-07 (Wave 37 Bucket D PR pending)
**Priority:** 🟠 P1
**Domain:** DevOps / Local dev
**Found:** 2026-05-07 (Wave 37 — Layer 4 Local dev resource)
**Affects:** Dev iteration speed — 27GB local RAM constraint

## Problem

`docker-compose.kitehub.yml` start ALL 8 services + frontends + Ollama = ~34-45 GB RAM (vượt 27GB local). Dev phải chạy `docker compose up <service>` từng cái thủ công.

## Proposed Fix

Compose profiles per dev work scope:

```yaml
services:
  postgres:
    profiles: ["infra-only", "branding-only", "beta-funnel", "kc-only", "full"]
  kitehub-branding:
    profiles: ["branding-only", "full"]
  kitehub-subscription:
    profiles: ["beta-funnel", "full"]
  ollama:
    profiles: ["branding-only-with-ai"]
  # ...
```

Usage: `docker compose --profile branding-only up`.

| Profile | Services | RAM est |
|---|---|---|
| `infra-only` | PG + Redis + RMQ + MinIO | ~1.5 GB |
| `branding-only` | infra + KH-branding + Ollama + KH-frontend | ~12 GB |
| `branding-only-no-ai` | infra + KH-branding (template-only) + KH-frontend | ~5 GB |
| `beta-funnel` | infra + KH-subscription + KH-email + KH-admin + KH-frontend | ~9 GB |
| `kc-only` | infra + KC-core + KC-gateway + KC-frontend | ~6 GB |
| `full` | tất cả TRỪ Ollama | ~18 GB |

## Acceptance Criteria

- [x] `docker-compose.kitehub.yml` updated với 6 profiles (`infra-only` / `branding-only` / `branding-only-no-ai` / `beta-funnel` / `kc-only` / `full`) — co-existing với 4 profile cũ (`ai-local`, `monitoring`, `backup`, `build-only`)
- [x] Each service tagged đúng profile(s) — verified via `docker compose --profile <name> config`
- [x] Profile usage matrix documented trong `kitehub/scripts/up.sh` header + `documents/05-guides/dev/wsl2-config.md` §"Combine với Compose profiles + JVM cap"
- [x] `./kitehub/scripts/up.sh --profile <name>` wrapper extended (default = `full` để preserve prior behavior)

## Log

- **2026-05-07 (Wave 37 Bucket D):** Shipped. 6 profiles tagged on 13 services (4 infra + 5 KH BE + 1 KC BE + 2 FE + Ollama overlay). `up.sh` defaults to `full` profile khi không pass `--profile`, set `KITE_COMPOSE_PROFILE` env để override. Verified: `docker compose --profile infra-only config` returns 5 services + 1 setup; `--profile full` returns 13 services. Coordinator-applied sau 2 lần Sonnet agent autocompact-thrash.

## Related

- GAP-408 (JVM heap cap)
- GAP-409 (Ollama policy)
- GAP-410 (WSL2 .wslconfig)
