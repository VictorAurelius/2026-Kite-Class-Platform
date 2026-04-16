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

## Acceptance Criteria

- [ ] pg_dump runs and uploads to MinIO/S3
- [ ] Backup integrity verified (checksum)
- [ ] Admin can view email history per instance
- [ ] Admin can toggle email types
- [ ] Hard purge removes DB + files + messaging resources
- [ ] PURGED status added to InstanceStatus
- [ ] All 3 gaps marked DONE
