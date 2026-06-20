# Data Retention — Business Rules

**Last verified:** 2026-04-16
**Config prefix:** `kitehub.data-retention`

## Rules

| ID | Rule | Value | Config Key |
|----|------|-------|-----------|
| RET-01 | TRIAL tier retention | 7 ngày sau suspend | `kitehub.data-retention.trial` |
| RET-02 | FREE tier retention | 7 ngày sau suspend | `kitehub.data-retention.free` |
| RET-03 | BASIC tier retention | 30 ngày sau suspend | `kitehub.data-retention.basic` |
| RET-04 | PREMIUM tier retention | 60 ngày sau suspend | `kitehub.data-retention.premium` |
| RET-05 | ENTERPRISE tier retention | 90 ngày sau suspend | `kitehub.data-retention.enterprise` |
| RET-06 | Warning count | 2 lần trước khi xóa | `kitehub.data-retention.warning-count` |
| RET-07 | Warning 1 | 50% retention period | shouldSendWarning() |
| RET-08 | Warning 2 | 80% retention period | shouldSendWarning() |
| RET-09 | Final warning | Range-based trong lead window `final-warning-lead-days` (default 1) trước khi xóa — KHÔNG còn exact ==1 (tránh cron-downtime skip, GAP-1026) | `kitehub.data-retention.final-warning-lead-days` |
| RET-10 | Deletion method | Soft delete (status=DELETED, deleted=true) | instance.softDelete() |
| RET-11 | Scheduler time | Daily 3:00 AM | `0 0 3 * * *` |
| RET-12 | Retention start | Instance `suspended_at` (stamped khi suspend, SUB-25/GAP-1264); fallback `updated_at` chỉ cho legacy row null (pre-V73) | `DataRetentionService.retentionClockStart()` |
| RET-13 | Default fallback tier | FREE (7 ngày) | getRetentionDays() default |
| RET-14 | TRIAL/FREE xử lý như nhau | Cùng map tới trial config | getRetentionDays() switch |
| RET-15 | Hard purge safety gate | Ít nhất 1 backup COMPLETED trước khi purge | `backupRecordRepository.existsByInstanceIdAndStatus()` |
| RET-16 | PURGED = permanently deleted | Tất cả tài nguyên (DB, S3, email logs, branding) bị xóa vĩnh viễn | `InstancePurgeService.executePurge()` |
| RET-17 | Automatic purge schedule | Weekly (Sunday 3:00 AM), instances DELETED > 30 ngày | `0 0 3 * * SUN` |
| RET-18 | Purge publishes RabbitMQ event | Fanout exchange cho cross-service cleanup | `instance.purge.exchange` |
| RET-19 | Backup retention count | Số lượng backups giữ lại mỗi instance | `backup.retention-count` (default: 7) |
| RET-20 | S3 mock mode | Tắt S3 thật cho môi trường dev | `storage.s3.mock-mode` (default: true) |

## Warning Schedule Examples

**FREE/TRIAL (7 ngày):**
- Ngày 0: Suspended
- Ngày 3 (50%): Warning 1
- Ngày 5 (80%): Warning 2
- Ngày 6: Final warning
- Ngày 7: Data deleted

**BASIC (30 ngày):**
- Ngày 0: Suspended
- Ngày 15 (50%): Warning 1
- Ngày 24 (80%): Warning 2
- Ngày 29: Final warning
- Ngày 30: Data deleted

## Purge Lifecycle

**Sau khi DELETED:**
- Ngày 0: Instance status = DELETED (soft delete)
- Ngày 30+: Eligible cho hard purge (weekly scheduler kiểm tra)
- Purge: status → PURGED (permanent, không thể recover)

**Purge xóa:**
- PostgreSQL database (drop)
- S3 backup files (delete objects, mark records DELETED)
- Email sent logs (delete)
- Branding assets (via RabbitMQ event tới kitehub-branding)

## Config

```yaml
kitehub:
  data-retention:
    trial: 7
    free: 7
    basic: 30
    premium: 60
    enterprise: 90
    warning-count: 2
    final-warning-lead-days: 1   # RET-09 (GAP-1026) — range-based final-warning lead window

backup:
  retention-count: 7    # Số lượng backups giữ lại mỗi instance

storage:
  s3:
    mock-mode: true     # Tắt S3 thật cho dev (set false cho production)
```

## Five-attribute review per `business-logic-review.md`

Per-rule attributes (Source / Rationale / Reviewer / Compliance check / Review cadence) backfilled at file-level placeholder per Phase 1 of GAP-433. Per-rule granularity tracked via GAP-156 Phase 2 stakeholder sign-offs.

- **Source:** Existing rules in this file derive from a mix of: feature gaps cited inline (where present), ADRs, persona reviews, and informed-gut estimates from Wave 1-30 work. Rules without inline citation default to `informed gut` per `business-logic-review.md` §2.1 and inherit quarterly re-review obligation below.
- **Rationale:** Rule values reflect product judgment + (where applicable) competitor benchmarks + VN regulatory minimums. Detailed per-rule rationale to be backfilled during GAP-156 Phase 2 stakeholder review; until then, treat values as `informed gut` subject to next quarterly review.
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-08). Formal stakeholder + legal counsel sign-off queued via GAP-156. Solo-dev exemption per `business-logic-review.md` §2.3 — the Reviewer line documents which hat is being worn AND obligation is attached for team-growth or pre-launch trigger.
- **Compliance check:** **Considered (self-assessed, counsel pending GAP-156 AC-D)** — per `documents/00-brd/compliance-checklist.md` L1/L2/L6: **Nghị định 13/2023/NĐ-CP (PDPL)** Điều 23 (retention ≥24 tháng + delete when purpose served; soft-delete → hard purge RET-10/16 honors right-to-erasure); **Luật Quản lý Thuế 2019** (billing/financial records 10-year retention); **Luật An ninh mạng 2018 + Nghị định 53/2022/NĐ-CP** (VN-user data localization). ⚠️ **Retention-conflict note:** tier retention windows (7–90 ngày sau suspend, RET-01..05) cover instance/operational data; billing-financial records carry a separate 10-year tax obligation that must survive instance purge — this split (operational purge vs financial-record retention) is self-assessed, not counsel-confirmed. No counsel verification yet.
- **Review cadence:** Quarterly (default per `business-logic-review.md` §2.5). **Next review:** 2026-08-08. Event triggers: PDPL implementing-decree, retention-tier change.

## Log

- **2026-05-08** Backfill 5-attribute review section per GAP-433 Phase 1 (`business-logic-review.md` §2 standard). Placeholder Reviewer + Quarterly cadence + domain-specific Compliance check. GAP-156 Phase 2 will replace placeholders with stakeholder sign-offs.
