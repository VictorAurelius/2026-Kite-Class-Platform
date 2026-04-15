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

## Log
- 2026-04-14 — Initial rules (Wave 3 Sub-PR 3.6)
