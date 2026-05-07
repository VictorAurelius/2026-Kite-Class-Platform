# GAP-430: `BackupJobFailure` alert metric name mismatch — alert chưa từng fire

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 BLOCKING (Phase 1 BETA — silent monitoring failure; backup chạy nhưng nếu fail không ai biết)
**Domain:** DevOps / Observability
**Found:** 2026-05-08 Wave 40 audit milestone (Bucket E Ops Readiness, PR #975)
**Affects:** `infrastructure/helm/.../prometheusrule.yaml` + `kitehub/scripts/backup-production.sh`

## Problem

`BackupJobFailure` alert (Wave 33 GAP-389 cluster) watches metric `kite_backup_last_success_timestamp_seconds`, nhưng `backup-production.sh` chỉ emit `kite_backup_snapshots_total` (counter, khác name + type). Result: **alert KHÔNG BAO GIỜ fire** dù backup fail. Silent monitoring failure.

State-check (Wave 40 audit):
```
kite_backup_last_success_timestamp_seconds  ← alert PromQL expects (gauge)
kite_backup_snapshots_total                  ← script actually emits (counter)
```

## Root Cause

GAP-389 Wave 36 ship alert + script độc lập, không cross-validate metric name. Test chỉ check syntax YAML hợp lệ, không fire-test alert.

## Proposed Fix

**Option A (recommended):** rewrite alert PromQL match counter name + delta-time logic:
```yaml
- alert: BackupJobFailure
  expr: time() - max(kite_backup_snapshots_total_timestamp{job="backup"}) > 90000  # 25h
```

**Option B:** add gauge emit vào `backup-production.sh` qua Pushgateway:
```bash
echo "kite_backup_last_success_timestamp_seconds $(date +%s)" \
  | curl --data-binary @- http://pushgateway:9091/metrics/job/backup
```

Recommend Option A (no new infrastructure).

## Acceptance Criteria

- [ ] `BackupJobFailure` PromQL match metric thực tế emit
- [ ] Self-test fire-alert: dừng cron 1 ngày → verify alert fire trong 25h
- [ ] Alert paired với runbook `documents/05-guides/operations/runbooks/backup-failure.md`
- [ ] CI test (PromQL syntax + unit test rule) added vào `infrastructure/helm/...test/`

## Related

- Wave 33 GAP-389 (parent — original alert ship)
- Wave 40 Bucket E audit (PR #975 surfaced this)
- `documents/04-quality/audits/ops-readiness/2026-05-08-wave-40-milestone.md` §findings P0
- Sister: `MultiTenantDataLeak` alert cũng dead (`tenant_isolation_violations_total` không có producer) — file riêng nếu chưa có gap

## Estimated effort

~1-2h (PromQL rewrite + fire-test + runbook + CI rule test).

## Log

- **2026-05-08** Filed during Wave 40 closure handoff cho Wave 41. Audit Bucket E Ops Readiness phát hiện regression — agent đề xuất số 428 nhưng đã collision với A UI; reassigned 430.
