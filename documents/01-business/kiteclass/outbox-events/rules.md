# Outbox Events — Business Rules

**Domain:** outbox-events
**Source:** Wave 3 Sub-PR 3.1, ADR-007

## Rules

### OutboxEvent lifecycle

| ID | Rule |
|----|------|
| BR-OBX-001 | Domain write + outbox write happen in the same JPA transaction (OutboxEventWriter uses `Propagation.MANDATORY`) |
| BR-OBX-002 | Status transitions: PENDING → PUBLISHED on dispatch success; PENDING → FAILED after `MAX_RETRIES` failures |
| BR-OBX-003 | `PUBLISHED` and `FAILED` are terminal — publisher skips |
| BR-OBX-004 | Consumers MUST be idempotent — at-least-once delivery may cause duplicates |
| BR-OBX-005 | `retry_count` auto-increments on each failed dispatch; never decrements |
| BR-OBX-006 | Exponential backoff: `nextAttemptAt = now + BACKOFF_SECONDS * 2^min(retry, 6)` |
| BR-OBX-007 | Published rows older than 30 days may be pruned (ops hygiene; implemented in later PR) |

### Event schema conventions

| ID | Rule |
|----|------|
| BR-OBX-EVT-001 | `event_type` format: `{aggregate_lower}.{verb_past}` (e.g. `instance.deployed`) |
| BR-OBX-EVT-002 | `aggregate_type` = entity class simple name (e.g. `FrontendInstance`) |
| BR-OBX-EVT-003 | `aggregate_id` = string form of entity primary key |
| BR-OBX-EVT-004 | `payload` is JSONB with at least `{aggregateType, aggregateId, timestamp, ...domain fields}` |

## Config keys

| Key | Default | Purpose |
|-----|---------|---------|
| `outbox.publisher.interval-ms` | 5000 | Scheduler polling cadence |
| `outbox.publisher.batch-size` | 50 | Max rows processed per tick |
| `outbox.publisher.max-retries` | 10 | Max attempts before FAILED |
| `outbox.publisher.backoff-seconds` | 5 | Base backoff (doubled per attempt up to 2^6) |

## Event catalogue (current)

Emitted by `InstanceLifecycleService`:

| Event type | Trigger | Aggregate |
|-----------|---------|-----------|
| `instance.initializing` | `initiate()` or `retry()` | FrontendInstance |
| `instance.generating` | `markInfrastructureReady()` | FrontendInstance |
| `instance.deployed` | `markBrandingCompleted()` | FrontendInstance |
| `instance.regenerating` | `rebrand()` | FrontendInstance |
| `instance.failed` | `markFailed()` | FrontendInstance |

Future producers (Wave 3 Sub-PRs 3.5, 3.6, 3.7) will extend this catalogue.

## Five-attribute review per `business-logic-review.md`

Per-rule attributes (Source / Rationale / Reviewer / Compliance check / Review cadence) backfilled at file-level placeholder per Phase 1 of GAP-433. Per-rule granularity tracked via GAP-156 Phase 2 stakeholder sign-offs.

- **Source:** Existing rules in this file derive from a mix of: feature gaps cited inline (where present), ADRs, persona reviews, and informed-gut estimates from Wave 1-30 work. Rules without inline citation default to `informed gut` per `business-logic-review.md` §2.1 and inherit quarterly re-review obligation below.
- **Rationale:** Rule values reflect product judgment + (where applicable) competitor benchmarks + VN regulatory minimums. Detailed per-rule rationale to be backfilled during GAP-156 Phase 2 stakeholder review; until then, treat values as `informed gut` subject to next quarterly review.
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-08). Formal stakeholder + legal counsel sign-off queued via GAP-156. Solo-dev exemption per `business-logic-review.md` §2.3 — the Reviewer line documents which hat is being worn AND obligation is attached for team-growth or pre-launch trigger.
- **Compliance check:** **N/A** — internal event-publishing pattern; payload PII handling delegated to producing module.
- **Review cadence:** Quarterly (default per `business-logic-review.md` §2.5). **Next review:** 2026-08-08. Event triggers: Event payload schema change requiring re-review.

## Log
- 2026-04-14 — Initial rules (ADR-007, Wave 3 Sub-PR 3.1)
