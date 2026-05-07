# GAP-407: Docker Compose Profiles cho Dev (27GB RAM friendly)

**Status:** 🔵 OPEN
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

- [ ] `docker-compose.kitehub.yml` updated với 6 profiles
- [ ] Each service tagged đúng profile(s)
- [ ] README documents profile usage matrix
- [ ] `./kitehub/scripts/up.sh <profile>` wrapper added

## Related

- GAP-408 (JVM heap cap)
- GAP-409 (Ollama policy)
- GAP-410 (WSL2 .wslconfig)
