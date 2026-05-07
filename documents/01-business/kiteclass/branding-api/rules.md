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

## Five-attribute review per `business-logic-review.md`

Per-rule attributes (Source / Rationale / Reviewer / Compliance check / Review cadence) backfilled at file-level placeholder per Phase 1 of GAP-433. Per-rule granularity tracked via GAP-156 Phase 2 stakeholder sign-offs.

- **Source:** Existing rules in this file derive from a mix of: feature gaps cited inline (where present), ADRs, persona reviews, and informed-gut estimates from Wave 1-30 work. Rules without inline citation default to `informed gut` per `business-logic-review.md` §2.1 and inherit quarterly re-review obligation below.
- **Rationale:** Rule values reflect product judgment + (where applicable) competitor benchmarks + VN regulatory minimums. Detailed per-rule rationale to be backfilled during GAP-156 Phase 2 stakeholder review; until then, treat values as `informed gut` subject to next quarterly review.
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-08). Formal stakeholder + legal counsel sign-off queued via GAP-156. Solo-dev exemption per `business-logic-review.md` §2.3 — the Reviewer line documents which hat is being worn AND obligation is attached for team-growth or pre-launch trigger.
- **Compliance check:** **N/A** — internal API contract for branding payload; no PII surface; cross-reference `kitehub/ai-branding/rules.md` for AI-asset compliance.
- **Review cadence:** Quarterly (default per `business-logic-review.md` §2.5). **Next review:** 2026-08-08. Event triggers: Branding contract version bump, FE consumer change.

## Log
- 2026-04-14 — Initial rules (Wave 3 Sub-PR 3.4, ADR-009)
