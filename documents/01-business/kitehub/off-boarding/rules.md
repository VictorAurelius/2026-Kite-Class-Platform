# Tenant Off-boarding — Business Rules

**Last verified:** 2026-04-20 (drafted via GAP-201 Phase 1)
**Config prefix:** `kitehub.off-boarding`
**Related domains:** subscription-billing (SB-*), data-retention (DR-*), ai-branding (AI-BR-*), trial-to-paid-migration (T2P-*)

## 1. Scope

Rules governing the transition of an instance from `PAID_ACTIVE` (or TRIAL) to fully purged, once the tenant requests cancellation or the subscription ends without renewal.

In scope:
- Cancellation state machine + grace periods
- Data-export bundle guarantees (GDPR Art. 20 portability)
- Right-to-be-forgotten (GDPR Art. 17) endpoint contract
- Retention conflict resolution (GAP-185 tax 7y vs GDPR purge)
- Outbox events for downstream consumers

Out of scope (owned elsewhere):
- Billing refund math → `subscription-billing/rules.md`
- Retention category classification → ADR-013 + `data-retention/rules.md`
- Trial expiry path → `trial-lifecycle/rules.md` (cancellation here applies to PAID_ACTIVE only; trial lapse is a separate flow)
- Payment reversal post-upgrade rollback → `trial-to-paid-migration/rules.md` (UC-T2P-02)

## 2. Rules

| ID | Rule | Value | Config Key | Code Location |
|----|------|-------|------------|---------------|
| OFF-01 | Cancel self-service always allowed | Enabled for all tiers (no retention gate) | `kitehub.off-boarding.self-service-cancel: true` | OffBoardingController (new) |
| OFF-02 | Grace period — read/write active | 30 days from `CANCEL_REQUESTED` | `kitehub.off-boarding.grace.active-days: 30` | OffBoardingService |
| OFF-03 | Grace period — read-only cold | 60 days (cumulative 90d from cancel) | `kitehub.off-boarding.grace.readonly-days: 60` | OffBoardingService |
| OFF-04 | Final purge | Day 90 from `CANCEL_REQUESTED` | `kitehub.off-boarding.purge-days: 90` | PurgeScheduler (GAP-073 deferred `@Scheduled`) |
| OFF-05 | Export bundle SLA | Signed URL ready ≤ 24h after request | `kitehub.off-boarding.export.sla-hours: 24` | DataExportService (GAP-073) |
| OFF-06 | Export bundle link TTL | 7 days from delivery | `kitehub.off-boarding.export.link-ttl-days: 7` | MinIO presigned URL |
| OFF-07 | Undo cancellation window | 30 days (matches OFF-02 active grace) | `kitehub.off-boarding.undo-window-days: 30` | OffBoardingService |
| OFF-08 | Financial-retention override | Invoices + payment logs retained 7y (VN tax — GAP-185) regardless of purge | `kitehub.off-boarding.financial-retention-years: 7` | RetentionClassifier bucket `RETAIN_WITH_PSEUDO` |
| OFF-09 | Right-to-be-forgotten purge | Same-day purge on verified request; financial data pseudonymized not deleted | `kitehub.off-boarding.rtbf.same-day: true` | OffBoardingController |
| OFF-10 | Purge confirmation token | Emailed token, 6-digit, 15-min TTL | `kitehub.off-boarding.purge-confirmation.ttl-minutes: 15` | OffBoardingController |
| OFF-11 | Final backup snapshot | Atomic snapshot to cold storage (MinIO `backup/` bucket) before purge; retained 30d post-purge for dispute | `kitehub.off-boarding.backup.retention-days: 30` | PurgeScheduler |
| OFF-12 | Staff SLA — cancel ticket response | 1 business day for self-service; 4h for Enterprise manual | — | Support runbook |
| OFF-13 | Subdomain release | Released at `ARCHIVED` transition; 180-day quarantine before reuse (prevent squatting) | `kitehub.off-boarding.subdomain.quarantine-days: 180` | DomainRegistryService |
| OFF-14 | Outbox required | Yes — every phase transition publishes via outbox (per design-patterns rule) | — | OutboxPublisher |
| OFF-15 | Tombstone record | Persist tenantId + purgedAt hash; used to prevent re-signup with same identifiers (fraud) | `kitehub.off-boarding.tombstone.retention-years: 7` | TombstoneRepository |
| OFF-16 | Audit log retention | 7 years (tax + cybersecurity law) | `kitehub.off-boarding.audit-retention-years: 7` | OffBoardingAuditEntity |

## 3. State Machine

Introduces new column `off_boarding_phase ENUM` on `instance` table. Works alongside existing `InstanceStatus`; `status` only flips at phase transitions, not on every sub-step.

```
OffBoardingPhase:
  NONE                  — default; instance ACTIVE
  CANCEL_REQUESTED      — user clicked "Cancel subscription"; export bundle being prepared
  EXPORT_READY          — export bundle signed URL available; user notified
  CANCEL_GRACE_ACTIVE   — 30d countdown; full read/write; undo allowed
  CANCEL_GRACE_READONLY — days 31-90; read-only; undo disabled; AI features off
  ARCHIVED              — day 90; no login; subdomain released; backup snapshot taken
  PURGED                — purge complete; tombstone record stored
  RTBF_FAST_TRACK       — right-to-be-forgotten fast path; skips grace to ARCHIVED then PURGED within 24h
```

**Valid transitions:**

```
NONE ──(user cancel)──► CANCEL_REQUESTED ──(bundle ready)──► EXPORT_READY
                                                                 │
                                                                 ▼
                                                     CANCEL_GRACE_ACTIVE
                                                     │            │
                                                     │ (undo)     │ (30d)
                                                     ▼            ▼
                                                    NONE  CANCEL_GRACE_READONLY
                                                               │
                                                               ▼ (60d)
                                                           ARCHIVED
                                                               │
                                                               ▼ (purge scheduled)
                                                            PURGED

NONE ──(RTBF request + confirm)──► RTBF_FAST_TRACK ──► ARCHIVED ──► PURGED
```

**Invariants:**
- `InstanceStatus` transitions: `PAID_ACTIVE → SUSPENDED` on entering `CANCEL_GRACE_READONLY`; `SUSPENDED → DELETED` on `ARCHIVED`; `DELETED → PURGED` on `PURGED`.
- Undo only valid when `off_boarding_phase ∈ {CANCEL_REQUESTED, EXPORT_READY, CANCEL_GRACE_ACTIVE}`.
- Financial records (invoices, payment logs) never deleted — pseudonymized at `PURGED` per OFF-08 + ADR-013.
- RTBF fast-track bypasses grace but still pseudonymizes retained financial records — it does not override tax law.

## 4. Config

```yaml
kitehub:
  off-boarding:
    self-service-cancel: true
    grace:
      active-days: 30
      readonly-days: 60
    purge-days: 90
    undo-window-days: 30
    export:
      sla-hours: 24
      link-ttl-days: 7
    rtbf:
      same-day: true
    purge-confirmation:
      ttl-minutes: 15
    backup:
      retention-days: 30
    subdomain:
      quarantine-days: 180
    tombstone:
      retention-years: 7
    audit-retention-years: 7
    financial-retention-years: 7
```

## 5. Outbox Events

| Event | Topic | Payload | Consumers |
|-------|-------|---------|-----------|
| `offboarding.cancel.requested` | `kitehub.offboarding` | `instanceId, tier, requestedAt, exportRequested` | BillingService, EmailService, AnalyticsService |
| `offboarding.export.ready` | `kitehub.offboarding` | `instanceId, bundleUrl, expiresAt` | EmailService |
| `offboarding.grace.readonly` | `kitehub.offboarding` | `instanceId, transitionedAt` | KiteClass cache invalidator, AIBrandingService (disable), EmailService |
| `offboarding.archived` | `kitehub.offboarding` | `instanceId, archivedAt, subdomain` | DomainRegistryService, KiteClass cache invalidator, EmailService |
| `offboarding.purged` | `kitehub.offboarding` | `instanceId, purgedAt, retentionHash` | AuditService, AnalyticsService |
| `offboarding.rtbf.requested` | `kitehub.offboarding` | `instanceId, subjectEmailHash, requestedAt, legalBasis` | LegalComplianceService, AuditService |
| `offboarding.undo` | `kitehub.offboarding` | `instanceId, undoneAt` | BillingService, EmailService |

## 6. Retention Conflict Matrix

| Data | GDPR Art. 17 purge | VN Tax Law 7y retain | Resolution |
|------|:------------------:|:--------------------:|------------|
| Invoices + payment logs | Requested | ✅ required | Pseudonymize (hash email/phone/name) — keep row |
| AI-generated assets | Requested | — | Hard delete at PURGED |
| Uploaded logos | Requested | — | Hard delete at PURGED |
| Student grades | Requested | MOET 5y | Pseudonymize per ADR-013 `RETAIN_WITH_PSEUDO` |
| Audit logs | — | ✅ Cybersecurity Law | Pseudonymize subject references |
| Moderation decisions | — | 5y legal evidence | Pseudonymize |
| Session + cache data | — | — | Already TTL'd; no action |

Classification sourced from `RetentionClassifier` (GAP-073 DONE, ADR-013). Off-boarding service invokes classifier per entity at PURGED; does not reimplement.

## 7. Related Rules + Dependencies

- **Reads from:** `subscription-billing/rules.md` (SB-* for refund math on mid-cycle cancel)
- **Writes to:** `data-retention/rules.md` (DR-* for retention classification hook)
- **Dependency (Phase 2):** GAP-073 deferred items — MinIO streaming export implementation, `@Scheduled` expiry job, pseudonymization executor. GAP-201 Phase 1 designs the rules; Phase 2 executes.
- **Dependency:** GAP-184 retention policy must be accepted before OFF-08 values are authoritative
- **Dependency:** GAP-185 (VN VAT + tax compliance) — 7y value must be confirmed by legal
- **Cross-ref:** GAP-034 branding export pack — becomes a subcomponent of export bundle
- **Cross-ref:** GAP-174 legal review hook — RTBF endpoint must be legal-reviewed before production

## 8. Log

- 2026-04-20 — Drafted under GAP-201 Phase 1. State-check: no existing off-boarding service exists; `StorageCleanupScheduler.SOFT_DELETE_GRACE_PERIOD_DAYS = 30` is the only churn-adjacent constant. Phase 2 (implementation) will add `OffBoardingService`, `OffBoardingController`, `PurgeScheduler`, `offboarding_request` table migration.
