# GAP-117: Backup Restore Drill Automation

**Status:** 🔵 OPEN
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
1. Create `documents/05-guides/restore-procedure.md` với:
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

- [ ] `restore-procedure.md` tạo trong 05-guides
- [ ] Monthly restore drill CI workflow implement + green cho 2 tháng liên tiếp
- [ ] Restore validation script `scripts/verify-restore.sh`
- [ ] Quarterly DR exercise schedule documented
- [ ] Measured RTO recorded trong `documents/05-guides/operations/` (baseline)

## Related

- Audit: `documents/04-quality/audits/ops/ops-readiness-audit-2026-04-19.md` §6
- Depends: GAP-093 (backup implementation, DONE) — prerequisite
- Related: GAP-030 (DR for AI branding, P2) — scope khác nhưng same spirit

## Log

- 2026-04-19 — Discovered in ops-readiness baseline audit
