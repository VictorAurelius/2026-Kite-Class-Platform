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

## 7b. Cascade hard-purge đã triển khai (GAP-954 — PDPL Art 23)

§1–§7 mô tả thiết kế *dự kiến* `OffBoardingService` / `PurgeScheduler` (GAP-201, GAP-073 — chưa build). GAP-954 ship **cascade DELETE thực tế** dựa trên `InstancePurgeService` có sẵn (hard purge sau grace 30 ngày). Section này là source of truth cho những gì chạy production hiện tại.

### 7b.1 State machine off-boarding (2 layer)

| Layer | Enum | States thêm (GAP-954) | Transitions |
|---|---|---|---|
| Control-plane (kitehub-platform) | `InstanceStatus` | `SUSPENDED`, `DELETED`, `PURGED` (đã có sẵn) | `ACTIVE/TRIAL → SUSPENDED → DELETED → PURGED` |
| KiteClass FE (kiteclass-core) | `FrontendInstanceStatus` | `SUSPENDED` + `DELETED` (mới) | `DEPLOYED → SUSPENDED ⇄ DEPLOYED`; `SUSPENDED → DELETED` (terminal) |

`DELETED` là terminal trong FE FSM — một chiều. Regression / re-onboard phải file instance mới. Grace 30 ngày track qua `FrontendInstance.deletedAt` (FE) + `Instance.updatedAt` tại `DELETED` (control-plane, drive `InstancePurgeService.findPurgeEligible`).

### 7b.2 Cascade hard-purge (chạy tại PURGE, ≥30 ngày sau soft-DELETE)

`InstancePurgeService.executePurge` — gated bởi safety check có sẵn (cần ≥1 backup COMPLETED, nếu không → `SKIPPED_NO_BACKUP`). Các bước cascade:

| # | Resource | Action | Code |
|---|---|---|---|
| 1 | DB Postgres tenant | Drop database | `DatabaseProvisioningService.deleteDatabase` |
| 2 | File backup (S3) | Xoá theo `BackupRecord.s3Key` + mark DELETED | `BackupStorageService.deleteBackup` |
| 3 | Email logs | Xoá theo instance | `EmailSentLogRepository.deleteByInstanceId` |
| 4 | **MinIO/S3 logo + branding assets** (GAP-954) | Xoá TẤT CẢ object dưới prefix `instances/{instanceId}/` (paginated) | `BackupStorageService.deleteByPrefix` |
| 5 | **DNS / custom-domain record** (GAP-954) | Clear custom-domain fields (subdomain chính dùng wildcard DNS — không có record per-tenant để xoá) | `DomainService.removeCustomDomain` |
| 6 | Cleanup cross-service | Outbox + best-effort RabbitMQ purge event | `SubscriptionEventEmitter` + `PurgeQueueConfig.PURGE_*` |
| 7 | Instance status | Set `PURGED` | `InstanceRepository.save` |
| 8 | **`TENANT_DELETED` audit row** (GAP-954) | Ghi `AdminAuditLog` (action `TENANT_DELETED`, REQUIRES_NEW) kèm snapshot cascade | `TenantAuditService.recordTenantDeleted` |

Prefix S3 logo/branding `instances/{instanceId}/` mirror `kitehub-branding S3StorageService.generateAssetPath`. Financial records (invoice, payment log) KHÔNG bị đụng ở đây — chúng được pseudonymize/retain 7 năm per OFF-08 + retention matrix §6.

### 7b.3 PDPL Art 23 retention

- **Value:** grace soft-delete 30 ngày trước hard purge (`InstancePurgeService.PURGE_RETENTION_DAYS = 30`).
- **Source:** PDPL 2023 Art 23 (mức tối thiểu retention dữ liệu cá nhân) — xem `business-logic-review.md` §2.4 + `documents/00-brd/data-retention-deletion-policy.md`.
- **Rationale:** 30 ngày cho phép tenant recover khỏi deletion nhầm/đang tranh chấp (undo per OFF-07) đồng thời bound thời gian lưu personal data đang chờ xoá. Sau grace, cascade phía trên destroy toàn bộ personal data non-financial; financial data được pseudonymize chứ không xoá (override tax-law 7 năm, OFF-08).
- **Reviewer:** @nguyenvankiet (acting Product Owner + Legal scout, solo-dev, 2026-06-06). Formal legal counsel review queued — GAP-156.
- **Compliance check:** **Compliant** — PDPL 2023 Art 16 (quyền xoá) + Art 23 (retention tối thiểu); override financial-retention per Luật Quản lý Thuế 2019 (7 năm).
- **Review cadence:** Annual + event-driven khi có PDPL implementing-decree. **Next review:** 2027-06-06.
- **Audit evidence:** mỗi lần purge ghi 1 `TENANT_DELETED` `AdminAuditLog` row (PDPL Art 11 audit trail tamper-evident) — trả lời được câu "có giữ dữ liệu tenant sau khi xoá account không?".

### 7b.4 Code + test references

- `kiteclass-core`: `FrontendInstanceStatus` (SUSPENDED/DELETED) + `FrontendInstance` (suspendedAt/deletedAt) + `InstanceLifecycleService` (suspend/reactivate/softDelete) + Flyway `V91__frontend_instance_offboarding_states.sql`. Tests: `FrontendInstanceStatusTest`, `InstanceLifecycleServiceTest`.
- `kitehub-subscription`: cascade trong `InstancePurgeService` + `BackupStorageService.deleteByPrefix` + `TenantAuditService` (REQUIRES_NEW per `audit-service-isolation.md`). Tests: `InstancePurgeServiceTest` (nested `Pdpl23Cascade` verify MinIO purge + DNS clear + audit write + safety-gate skip).

## 8. Log

- 2026-06-06 — GAP-954 (Wave provisioning-1 Bucket G): thêm §7b cascade hard-purge đã triển khai. Ship MinIO/S3 logo prefix purge + DNS/custom-domain clear + `TENANT_DELETED` audit trên `InstancePurgeService` có sẵn; thêm SUSPENDED/DELETED vào kiteclass-core `FrontendInstanceStatus` FSM + V91 migration. PDPL Art 23 retention 30 ngày documented + tested.
- 2026-04-20 — Drafted under GAP-201 Phase 1. State-check: no existing off-boarding service exists; `StorageCleanupScheduler.SOFT_DELETE_GRACE_PERIOD_DAYS = 30` is the only churn-adjacent constant. Phase 2 (implementation) will add `OffBoardingService`, `OffBoardingController`, `PurgeScheduler`, `offboarding_request` table migration.

## Five-attribute review per `business-logic-review.md`

Per-rule attributes (Source / Rationale / Reviewer / Compliance check / Review cadence) backfilled at file-level placeholder per Phase 1 of GAP-433. Per-rule granularity tracked via GAP-156 Phase 2 stakeholder sign-offs.

- **Source:** Existing rules in this file derive from a mix of: feature gaps cited inline (where present), ADRs, persona reviews, and informed-gut estimates from Wave 1-30 work. Rules without inline citation default to `informed gut` per `business-logic-review.md` §2.1 and inherit quarterly re-review obligation below.
- **Rationale:** Rule values reflect product judgment + (where applicable) competitor benchmarks + VN regulatory minimums. Detailed per-rule rationale to be backfilled during GAP-156 Phase 2 stakeholder review; until then, treat values as `informed gut` subject to next quarterly review.
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-08). Formal stakeholder + legal counsel sign-off queued via GAP-156. Solo-dev exemption per `business-logic-review.md` §2.3 — the Reviewer line documents which hat is being worn AND obligation is attached for team-growth or pre-launch trigger.
- **Compliance check:** **Compliant** — PDPL 2023 Art 16 (right to deletion / data portability); Decree 53/2022 (data export obligations).
- **Review cadence:** Quarterly (default per `business-logic-review.md` §2.5). **Next review:** 2026-08-08. Event triggers: PDPL implementing-decree on deletion-right, off-boarding SLA shift.

## Log

- **2026-05-08** Backfill 5-attribute review section per GAP-433 Phase 1 (`business-logic-review.md` §2 standard). Placeholder Reviewer + Quarterly cadence + domain-specific Compliance check. GAP-156 Phase 2 will replace placeholders with stakeholder sign-offs.
