# ADR-013: Data Retention Classification (GDPR + VN Compliance)

**Status:** ACCEPTED
**Date:** 2026-04-14
**Deciders:** Tech Lead + Legal + Architect
**Related Gap:** GAP-073 (Wave 4 Sub-PR 4.4)

## Context

When a tenant deletes their account we must reconcile two conflicting legal regimes:
- **GDPR Art. 17 (right to erasure)** — purge personal data promptly on request
- **VN tax law / audit requirements** — invoices retained 10 years; audit logs 2+ years

A single "delete everything" policy breaks accounting; a single "retain everything" policy violates GDPR. We need **per-category classification**.

## Decision

**Four retention buckets**, applied via `RetentionClassifier`:

| Bucket | What | Example | On account delete |
|--------|------|---------|--------------------|
| **PURGE_ON_REQUEST** | PII with no legal retention requirement | Profile photo, logo uploads, AI-generated assets, branding history | 7-day grace → hard delete |
| **PURGE_DELAYED** | Non-PII useful for short-term ops | Session data, caches, queue messages | Already-TTL'd; no action |
| **RETAIN_WITH_PSEUDO** | PII under legal retention | Invoices (10y tax), audit logs (2y), moderation decisions (5y legal evidence) | Pseudonymize PII (name→hash, email→hash) but keep row |
| **RETAIN_LEGAL_HOLD** | Data under active legal proceeding | DMCA dispute records, active incident evidence | Retained as-is until hold lifts; deletion request queues post-lift |

Classification lives in `RetentionClassifier` service (Sub-PR 4.4), fed by:
- Entity `@Retention` annotation (class-level)
- Override rules in `retention-policy.yml` for edge cases

### Deletion flow

```
User clicks "Delete account"
   ▼
DeletionRequest(PENDING) → email confirmation
   ▼ (7-day grace, reversible)
DeletionRequest(PROCESSING)
   ├── Disable login
   ├── For each domain entity: apply RetentionClassifier
   │     ├── PURGE_ON_REQUEST → soft-delete, mark for purge
   │     ├── RETAIN_WITH_PSEUDO → pseudonymize in-place
   │     └── RETAIN_LEGAL_HOLD → add to hold queue
   └── Notify AI providers to purge prompts (OpenAI data-deletion API)
   ▼ (Day +30 from PROCESSING)
Hard-delete soft-deleted rows from DB + MinIO
   ▼
DeletionRequest(COMPLETED) + tombstone record (future signup restore ban)
```

Plus **GDPR Art. 20 data export** delivered as a signed ZIP link before PROCESSING starts.

## Consequences

### Positive
- ✅ Compliant with both regimes simultaneously
- ✅ Explicit classification prevents accidental deletion of legal-hold data
- ✅ 7-day grace protects against accidental deletion
- ✅ Pseudonymization preserves aggregate analytics without PII

### Negative
- ❌ Classification requires annotating every entity
- ❌ Pseudonymization function must be cryptographically sound (HMAC with platform key)
- ❌ 30-day soft-delete window has operational cost (storage)
- ❌ Cannot restore account after hard-delete — tenant must re-sign up (design, not defect)

## Alternatives

- **A. Full purge, ignore tax retention** — rejected: illegal in VN
- **B. Full retention, pseudonymize all PII immediately on delete** — rejected: GDPR says "without undue delay"; pseudonymization ≠ erasure for GDPR purposes on most PII categories
- **C. Outsource to a GDPR vendor (OneTrust)** — deferred: over-engineered for current scale

## Implementation Notes

Sub-PR 4.4 delivers:
- `DeletionRequest` entity + state machine
- `RetentionClassifier` service
- `DataExportService` (ZIP export via ArchiveOutputStream)
- V38 migration + retention-policy.yml
- Settings → DangerZone wire-up
- Jobs: scheduled pseudonymization + hard-purge

### Annotations

```java
@Retention(RetentionClassifier.Bucket.PURGE_ON_REQUEST)
public class BrandingResource extends BaseEntity { ... }

@Retention(value = RetentionClassifier.Bucket.RETAIN_WITH_PSEUDO,
           pseudonymize = {"email", "phone"})
public class Invoice extends BaseEntity { ... }
```

## References
- GAP-073
- GDPR Art. 17 (erasure), Art. 20 (portability)
- VN Decree 53/2022 (data residency) — ensures we don't export EU PII outside VN boundaries without consent
- Our `ai-branding-guidelines.md` §9 (Security & Privacy)

## Log
- 2026-04-14 — Accepted
