# Data Retention — Business Rules

**Domain:** data-retention
**Source:** GAP-073, Wave 4 Sub-PR 4.4, ADR-013

## Rules

### Deletion request lifecycle (State Machine)
| ID | Rule |
|----|------|
| BR-RET-001 | 7-day grace window after `requestDeletion()`; reversible via `cancelDeletion()` |
| BR-RET-002 | Status transitions enforced by `DeletionStatus` machine; COMPLETED / CANCELLED terminal |
| BR-RET-003 | Every state transition writes an `AuditLog` row in the same `@Transactional` block |
| BR-RET-004 | Exactly one non-terminal `DeletionRequest` per (userId, tenantId) |
| BR-RET-005 | `dataExportUrl` is populated by `DataExportService.exportForUser()` before PROCESSING |
| BR-RET-006 | `expirePastGrace()` promotes PENDING rows past `graceEndsAt` to PROCESSING (scheduler) |

### Retention classification (ADR-013)
| ID | Rule |
|----|------|
| BR-RET-010 | Every domain entity MUST be annotated with `@Retention(RetentionBucket)` or the classifier falls back to `PURGE_ON_REQUEST` (safe default) |
| BR-RET-011 | `@Retention` is RUNTIME-retained so reflection-based lookup works |
| BR-RET-012 | `pseudonymizeFields` only meaningful on `RETAIN_WITH_PSEUDO`; ignored otherwise |

### Retention buckets
| Bucket | Meaning | Example entity |
|--------|---------|----------------|
| `PURGE_ON_REQUEST` | PII with no legal retention — 7d grace → hard delete | `BrandingResource` |
| `PURGE_DELAYED` | Non-PII, short-term ops, already TTL'd | `OutboxEvent` |
| `RETAIN_WITH_PSEUDO` | PII under legal retention (tax 10y, audit 2y) — pseudonymize in place | `AuditLog` |
| `RETAIN_LEGAL_HOLD` | Under active legal proceeding — retain until hold lifts | (future: DMCA dispute records) |

### GDPR Art. 20 export
| ID | Rule |
|----|------|
| BR-RET-020 | `DataExportService.exportForUser(userId, tenantId)` returns a ZIP byte stream |
| BR-RET-021 | ZIP contains at minimum: `profile.json`, `audit-trail.csv`, `README.txt` |
| BR-RET-022 | Generation is audited via `deletion.export_generated` AuditLog row |
| BR-RET-023 | Audit trail export limited to `AUDIT_EXPORT_LIMIT` (500) rows per export |

## Config keys

| Key | Default | Purpose |
|-----|---------|---------|
| `retention.deletion.grace-period-days` | 7 | Grace window before PENDING → PROCESSING |
| `retention.deletion.pseudonymize-secret` | `CHANGE_ME_32_BYTE_MIN` (dev) | HMAC key for PII hashing — MUST override in prod |

## Audit events

| Event (`actionType`) | Trigger |
|----------------------|---------|
| `deletion.requested` | `requestDeletion()` persists PENDING row |
| `deletion.cancelled` | `cancelDeletion()` transitions to CANCELLED |
| `deletion.processing_started` | `startProcessing()` or `expirePastGrace()` promotes to PROCESSING |
| `deletion.completed` | `markCompleted()` transitions to COMPLETED |
| `deletion.export_generated` | `DataExportService.exportForUser()` builds ZIP |

## Deferred (non-goals for Sub-PR 4.4)

- Real streaming-to-MinIO + signed URL generation
- Full user profile query in export (currently stubbed)
- Email notifications for grace period expiry + export-ready
- Automated `@Scheduled` wiring of `expirePastGrace()` (can be wired in Sub-PR 4.6 integration or 4.5)
- Pseudonymization executor (applies `pseudonymizeFields` in PROCESSING phase)
- KiteHub DangerZone FE hookup

## Log
- 2026-04-15 — Initial rules (Wave 4 Sub-PR 4.4, GAP-073, ADR-013)
