# Storage — Business Rules

**Domain:** KiteClass Core
**Version:** 1.0
**Updated:** 2026-03-24

---

## 1. Rules

| ID | Rule | Detail |
|----|------|--------|
| BR-STR-001 | Presigned URL upload flow | Client requests presigned PUT URL, uploads directly to S3/MinIO, then confirms via API. Server never proxies file bytes. |
| BR-STR-002 | Upload URL TTL | Presigned upload URL expires after 30 minutes. File record created as PENDING with `expires_at`. |
| BR-STR-003 | Download URL TTL | Presigned download URL expires after 5 minutes. Only CONFIRMED files can be downloaded. |
| BR-STR-004 | Max file size | 100 MB per file. Validated server-side before generating presigned URL. |
| BR-STR-005 | MIME type whitelist | Only allowed types: images (jpeg, png, gif, webp, svg+xml), documents (pdf, doc/docx, xls/xlsx, ppt/pptx, txt, csv), video (mp4, mpeg, webm), audio (mpeg, wav, ogg). |
| BR-STR-006 | File status lifecycle | PENDING -> CONFIRMED (on confirm) or EXPIRED (after 30min TTL). CONFIRMED -> DELETED (on soft delete). |
| BR-STR-007 | Soft delete with grace period | Delete marks file as `deleted=true`. S3 object removed after 30-day grace period by cleanup scheduler. |
| BR-STR-008 | Quota per tenant per tier | FREE: 1 GB, BASIC: 10 GB, PREMIUM: 50 GB, ENTERPRISE: 100 GB. |
| BR-STR-009 | Quota enforcement | Quota checked before generating upload URL. PENDING files count towards quota immediately to prevent abuse. |
| BR-STR-010 | Multi-tenant isolation | All files scoped by `instance_id` (tenant UUID). Storage path: `{instanceId}/uploads/{year}/{month}/{uuid}.ext`. |
| BR-STR-011 | Access level — PUBLIC | Anyone with presigned URL can download. No ownership check. |
| BR-STR-012 | Access level — PRIVATE | Only the original uploader can generate download URL. |
| BR-STR-013 | Access level — TENANT | Any user within the same tenant can download. |
| BR-STR-014 | Default access level | PRIVATE if not specified in upload request. |
| BR-STR-015 | Confirm requires S3 verification | Confirm endpoint calls S3 HeadObject to verify file actually exists before marking CONFIRMED. |
| BR-STR-016 | Quota recalculation | `recalculateQuotaUsage()` sums actual file sizes from DB to correct drift. |

**File statuses:** PENDING, CONFIRMED, EXPIRED, DELETED
**Access levels:** PUBLIC, PRIVATE, TENANT
**File types:** IMAGE, DOCUMENT, VIDEO, AUDIO, OTHER
**Storage tiers:** FREE, BASIC, PREMIUM, ENTERPRISE

---

## 2. Cleanup Scheduler

| Job | Schedule | Behavior |
|-----|----------|----------|
| markExpiredPendingUploads | Every 10 minutes (`0 */10 * * * *`) | PENDING files past `expires_at` -> EXPIRED. Quota freed. |
| cleanupDeletedFiles | Daily 2:00 AM (`0 0 2 * * *`) | Files with `deleted=true` older than 30 days: delete from S3 + hard delete from DB. |

---

## 3. Storage Path Convention

```
{instanceId}/uploads/{year}/{month}/{uuid}.{ext}
```

- Bucket: `kiteclass-files` (config: `storage.s3.bucket-name`)
- S3-compatible (MinIO in dev, AWS S3 in production)
- Path-style access enabled for MinIO compatibility

---

## 4. Config Keys

| Key | Default | Description |
|-----|---------|-------------|
| `storage.s3.endpoint` | `http://localhost:9000` | S3/MinIO endpoint |
| `storage.s3.region` | `us-east-1` | AWS region |
| `storage.s3.bucket-name` | `kiteclass-files` | Bucket name |
| `storage.s3.path-style-access-enabled` | `true` | MinIO compatibility |

## Five-attribute review per `business-logic-review.md`

Per-rule attributes (Source / Rationale / Reviewer / Compliance check / Review cadence) backfilled at file-level placeholder per Phase 1 of GAP-433. Per-rule granularity tracked via GAP-156 Phase 2 stakeholder sign-offs.

- **Source:** Existing rules in this file derive from a mix of: feature gaps cited inline (where present), ADRs, persona reviews, and informed-gut estimates from Wave 1-30 work. Rules without inline citation default to `informed gut` per `business-logic-review.md` §2.1 and inherit quarterly re-review obligation below.
- **Rationale:** Rule values reflect product judgment + (where applicable) competitor benchmarks + VN regulatory minimums. Detailed per-rule rationale to be backfilled during GAP-156 Phase 2 stakeholder review; until then, treat values as `informed gut` subject to next quarterly review.
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-08). Formal stakeholder + legal counsel sign-off queued via GAP-156. Solo-dev exemption per `business-logic-review.md` §2.3 — the Reviewer line documents which hat is being worn AND obligation is attached for team-growth or pre-launch trigger.
- **Compliance check:** **Considered** — Decree 53/2022/NĐ-CP Art 26 (VN data localization for Vietnamese-tenant data); PDPL 2023 cross-border transfer rules.
- **Review cadence:** Quarterly (default per `business-logic-review.md` §2.5). **Next review:** 2026-08-08. Event triggers: Storage region change (e.g., add Singapore mirror), VN localization regulation update.

## Log

- **2026-05-08** Backfill 5-attribute review section per GAP-433 Phase 1 (`business-logic-review.md` §2 standard). Placeholder Reviewer + Quarterly cadence + domain-specific Compliance check. GAP-156 Phase 2 will replace placeholders with stakeholder sign-offs.
