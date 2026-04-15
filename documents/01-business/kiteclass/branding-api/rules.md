# Branding API — Business Rules

**Domain:** branding-api
**Source:** Wave 3 Sub-PR 3.4, ADR-009

## Rules

### Composite package endpoint
| ID | Rule |
|----|------|
| BR-BAPI-001 | `GET /api/v1/branding/{instanceId}/package` returns theme + assets + metadata in one payload |
| BR-BAPI-002 | Response includes strong ETag derived from `brandingVersion` + payload hash |
| BR-BAPI-003 | Repeat requests with matching `If-None-Match` → HTTP 304 Not Modified (no body) |
| BR-BAPI-004 | Server-side cache `branding-package` evicted on `instance.deployed` / `instance.regenerating` |

### Instance lifecycle REST surface
| ID | Rule |
|----|------|
| BR-BAPI-010 | Every write endpoint delegates to `InstanceLifecycleService` — NO direct status mutation in controllers |
| BR-BAPI-011 | Slug format: `^[a-z0-9][a-z0-9-]*[a-z0-9]$`, length 3-80 |
| BR-BAPI-012 | `failed` endpoint requires non-blank reason (≤1000 chars) |
| BR-BAPI-013 | Retry returns 409 when `retryCount >= MAX_RETRIES` (raised by service layer) |

### Internal webhooks
| ID | Rule |
|----|------|
| BR-BAPI-020 | `/internal/**` endpoints gated to internal network by gateway filter |
| BR-BAPI-021 | `POST /internal/notify/instance-deployed?instanceId=X` evicts `branding-package` cache for that id |
| BR-BAPI-022 | Webhooks idempotent — repeated calls safe (cache eviction is naturally idempotent) |

## Endpoints

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/api/v1/instances` | Initiate — NOT_STARTED → INITIALIZING |
| GET | `/api/v1/instances/{id}` | Fetch instance |
| GET | `/api/v1/instances?status=X` | List (optional status filter) |
| POST | `/api/v1/instances/{id}/infrastructure-ready` | INITIALIZING → GENERATING |
| POST | `/api/v1/instances/{id}/branding-completed` | GENERATING\|REGENERATING → DEPLOYED |
| POST | `/api/v1/instances/{id}/rebrand` | DEPLOYED → REGENERATING |
| POST | `/api/v1/instances/{id}/failed` | * → FAILED |
| POST | `/api/v1/instances/{id}/retry` | FAILED → INITIALIZING |
| GET | `/api/v1/branding/{instanceId}/package` | Composite package + ETag |
| POST | `/internal/notify/instance-deployed` | Evict cache |

## Config keys

| Key | Default | Purpose |
|-----|---------|---------|
| `spring.cache.type` | `redis` (inherited) | Shared cache across service instances |
| `spring.cache.redis.time-to-live` | 1h (inherited) | Default TTL for `branding-package` |

## Log
- 2026-04-14 — Initial rules (Wave 3 Sub-PR 3.4, ADR-009)
