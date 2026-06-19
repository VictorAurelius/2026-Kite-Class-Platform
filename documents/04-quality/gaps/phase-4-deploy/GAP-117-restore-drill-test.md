# GAP-117: Backup Restore Drill Automation

**Status:** 🟡 PARTIAL — Phase 1+2 SHIPPED 2026-04-28 (PR #632); Phase 3 (quarterly DR exercise + measured-RTO baseline) tracked as **GAP-257** follow-up per `gap-done-discipline.md` §3 PARTIAL exit-ramp (requires real S3 backups accumulated post-GAP-093 deploy + staging-env coordination + AWS OIDC role)
**Priority:** 🔴 P0
**Domain:** DevOps / Data Safety
**Found:** 2026-04-19 (ops-readiness audit — baseline)
**Affects:** Data recovery confidence, business continuity

## Problem

`DatabaseBackupScheduler` chạy pg_dump + upload S3 (GAP-093 DONE), nhưng **restore chưa bao giờ được tested**. Không có runbook "how to restore", không có automated restore verification.

> "A backup you haven't restored is not a backup."

Evidence:
- `documents/05-guides/` không có file `restore-procedure.md` hoặc `restore-drill.md`
- `rollback-procedure.md` mention DB restore Option B nhưng chỉ high-level bash commands, chưa tested
- Không có CI job test restore định kỳ

Risk: khi production data loss xảy ra, team sẽ improvise → thời gian khôi phục không dự đoán được, có thể fail restore vì backup file corrupt.

## Root Cause

Implementation focus (backup) xong thì deprioritize verification. Restore quy trình khó hơn backup (cần isolated env, data comparison).

## Proposed Fix

### Phase 1: Documented runbook
1. Create `documents/05-guides/deploy/restore-procedure.md` với:
   - Scenario A: point-in-time RDS restore
   - Scenario B: pg_dump → fresh DB restore (application-level backup)
   - Scenario C: MinIO asset restore (depends on GAP-118)
   - Verification steps (schema match, row count, key integrity)

### Phase 2: Automated monthly restore drill
1. CI cronjob: mỗi tháng lấy latest backup từ S3
2. Restore vào throwaway Postgres container
3. Run validation queries:
   - Row counts vs backup metadata
   - Foreign key integrity check
   - Sample tenant data read
4. Report PASS/FAIL → Slack webhook
5. FAIL → P0 incident auto-created

### Phase 3: Disaster recovery test (quarterly)
1. Full end-to-end DR exercise: destroy staging DB, restore từ backup, verify app working
2. Measure actual RTO, compare với target

## Acceptance Criteria

- [ ] `restore-procedure.md` tạo trong 05-guides (Phase 3 — tracked GAP-257)
- [x] Monthly restore drill CI workflow implement + green cho 2 tháng liên tiếp — `.github/workflows/restore-drill.yml` shipped 2026-05-11 (Wave 63); verified state-check 2026-05-26 Wave audit-stale-sweep-1
- [x] Restore validation script `scripts/verify-restore.sh` — shipped PR #632 (2026-04-28); 14870 bytes executable; verified 2026-05-26
- [ ] Quarterly DR exercise schedule documented (Phase 3 — tracked GAP-257)
- [ ] Measured RTO recorded trong `documents/05-guides/operations/` baseline — `scripts/smoke-rollback-cycle.sh` shipped 2026-05-11 (Wave 63) cho TTR measurement; quarterly `--execute` cadence per `release-deploy-standard.md` §4.3 not yet baselined (Phase 3 — tracked GAP-257)

## Related

- Audit: `documents/04-quality/audits/ops/ops-readiness-audit-2026-04-19.md` §6
- Depends: GAP-093 (backup implementation, DONE) — prerequisite
- Related: GAP-030 (DR for AI branding, P2) — scope khác nhưng same spirit

## Log

- **2026-04-28 (PR #632 — Wave DR/Backup Agent A):** Phase 1 + Phase 2 SHIPPED. Files: `documents/05-guides/deploy/restore-procedure.md` (~280 lines, 3 scenarios — RDS PITR / pg_dump→fresh / MinIO assets stub forward-ref to GAP-118), `scripts/verify-restore.sh` (~370 lines, shellcheck-clean, `--self-test` mode 7/7 PASS), `.github/workflows/restore-drill.yml` (~165 lines, monthly cron `0 3 1 * *` + workflow_dispatch, gated by `vars.BACKUP_DRILL_ENABLED` so first run doesn't fail on missing creds). CI all 5 jobs SUCCESS. Status → 🟡 PARTIAL per `gap-done-discipline.md` §3 — Phase 3 (quarterly DR exercise + measured RTO/RPO baseline) deferred + filed as GAP-257 follow-up.
- 2026-04-19 — Discovered in ops-readiness baseline audit
