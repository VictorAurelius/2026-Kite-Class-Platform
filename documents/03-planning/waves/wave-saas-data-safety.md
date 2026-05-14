<!-- wave-plan-completeness-exempt: Pre-Wave-76 legacy plan — predates current _TEMPLATE.md structure -->

# Wave: SaaS Data Safety

**Gaps:** GAP-093, GAP-094, GAP-096
**Epic:** E11 — SaaS Lifecycle Hardening
**Duration estimate:** L + L + M = ~20 person-days
**Created:** 2026-04-16

## Sub-PRs

1. **Sub-PR 1: GAP-093** — Database backup functional (pg_dump + MinIO)
   - Branch: `feat/kite-database-backup`
   - Mode: parallel agent (worktree)

2. **Sub-PR 2: GAP-096** — Email admin controls + monitoring
   - Branch: `feat/email-admin-controls`
   - Mode: parallel agent (worktree)

3. **Sub-PR 3: GAP-094** — Hard delete (data purge)
   - Branch: `feat/hard-delete-purge`
   - Mode: sequential (depends on Sub-PR 1)

## Integration Plan

1. Sub-PR 1 + 2 parallel → cherry-pick into wave branch
2. Sub-PR 3 after Sub-PR 1 merged → cherry-pick
3. Integration test: full lifecycle trial→expire→backup→retain→purge
4. Wave completion check

## Status

All 3 sub-PRs implemented and cherry-picked into wave branch (2026-04-16):
- GAP-093: `7627834c` — BackupRecord entity, DatabaseBackupService (pg_dump + S3), BackupStorageService, S3Config, V16 migration
- GAP-096: `0b7b79dc` — AdminEmailController, EmailAdminService, EmailConfigProperties, 4 DTOs, toggle system
- GAP-094: `1210fbdc` — InstancePurgeService, PurgeQueueConfig (fanout), PURGED status, V17 migration, scheduler update

## Acceptance Criteria

- [x] pg_dump runs and uploads to MinIO/S3
- [x] Backup integrity verified (checksum SHA-256)
- [x] Admin can view email history per instance
- [x] Admin can toggle email types
- [x] Hard purge removes DB + files + messaging resources
- [x] PURGED status added to InstanceStatus
- [x] All 3 gaps marked DONE
