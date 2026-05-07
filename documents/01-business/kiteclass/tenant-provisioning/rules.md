# Tenant Provisioning — Business Rules

**Domain:** tenant-provisioning
**Source:** GAP-015, Wave 3 Sub-PR 3.6, ADR-006 + ADR-004

## Rules

### Saga lifecycle
| ID | Rule |
|----|------|
| BR-PROV-001 | TenantProvisioningSaga is the ONE entrypoint that wires initiate → infrastructure-ready → plan-exec |
| BR-PROV-002 | Each lifecycle transition runs in its own transaction (saga is NOT @Transactional) |
| BR-PROV-003 | Any failure between initiate and plan completion triggers compensation: markFailed(reason) |
| BR-PROV-004 | initiate failure does NOT trigger compensation (no instance row to mark) |
| BR-PROV-005 | Compensation failure is logged but never rethrown (best-effort) |

### Event-driven entry
| ID | Rule |
|----|------|
| BR-PROV-010 | TenantCreatedEvent carries {tenantId, slug, audience, tone} — minimum viable input |
| BR-PROV-011 | Saga invoked via direct method call in this Sub-PR; Spring @EventListener / RabbitMQ consumer wiring lands alongside the outbox RabbitMQ dispatcher in follow-up |

### Infrastructure provisioning
| ID | Rule |
|----|------|
| BR-PROV-020 | provisionInfrastructure is a placeholder in this sub-PR (logs only) |
| BR-PROV-021 | Future implementation: DB schema create, MinIO bucket, DNS record, TLS cert — separate service |

## Config keys

| Key | Default | Purpose |
|-----|---------|---------|
| `provisioning.infrastructure.timeout-seconds` | 120 | Future: cap on infra prep before saga gives up |
| `provisioning.saga.auto-retry-on-failure` | false | Future: wire to scheduled retry (uses lifecycle.retry up to MAX_RETRIES) |

## Five-attribute review per `business-logic-review.md`

Per-rule attributes (Source / Rationale / Reviewer / Compliance check / Review cadence) backfilled at file-level placeholder per Phase 1 of GAP-433. Per-rule granularity tracked via GAP-156 Phase 2 stakeholder sign-offs.

- **Source:** Existing rules in this file derive from a mix of: feature gaps cited inline (where present), ADRs, persona reviews, and informed-gut estimates from Wave 1-30 work. Rules without inline citation default to `informed gut` per `business-logic-review.md` §2.1 and inherit quarterly re-review obligation below.
- **Rationale:** Rule values reflect product judgment + (where applicable) competitor benchmarks + VN regulatory minimums. Detailed per-rule rationale to be backfilled during GAP-156 Phase 2 stakeholder review; until then, treat values as `informed gut` subject to next quarterly review.
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-08). Formal stakeholder + legal counsel sign-off queued via GAP-156. Solo-dev exemption per `business-logic-review.md` §2.3 — the Reviewer line documents which hat is being worn AND obligation is attached for team-growth or pre-launch trigger.
- **Compliance check:** **Considered** — PDPL 2023 (tenant admin PII collected at signup); Consumer Protection (signup terms display).
- **Review cadence:** Quarterly (default per `business-logic-review.md` §2.5). **Next review:** 2026-08-08. Event triggers: Signup flow legal-disclaimer change, PDPL implementing-decree.

## Log
- 2026-04-14 — Initial rules (Wave 3 Sub-PR 3.6)
