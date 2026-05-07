# GAP-430: `BackupJobFailure` alert metric name mismatch — alert chưa từng fire

**Status:** 🟡 PARTIAL 2026-05-08 (Wave 41 Bucket A — silent-failure root cause fixed via Option B+: script now emits the gauge the alert watches, plus `absent()` arm hardens against series-disappearance. Remaining: dedicated PromQL unit-test fixture pending agent-environment access to `promtool`/`helm`)
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

- [x] `BackupJobFailure` PromQL match metric thực tế emit — `scripts/backup-production.sh` now emits `kite_backup_last_success_timestamp_seconds` (gauge) alongside the existing `kite_backup_snapshots_total` (counter); alert PromQL watches the gauge as before. Both Helm (`infrastructure/helm/kitehub/templates/prometheusrule.yaml:257`) and Docker (`kitehub/docker/prometheus/alert-rules.yml:135`) copies of the rule updated, with hardened expression `(time() - max(<gauge>) > 90000) or absent(<gauge>)` so series-disappearance also fires.
- [x] Self-test fire-alert via dry-run: `bash scripts/backup-production.sh --dry-run` confirms gauge line `kite_backup_last_success_timestamp_seconds{type="pre_deploy",region="ap-southeast-1",instance="kite-rds-prod"} <unix-seconds>` is emitted on the success path. Alert PromQL `time() - max(<gauge>) > 90000` will fire >25h after last emission; the new `absent(<gauge>)` arm additionally fires if the gauge series disappears (covers the silent-failure mode this gap originally surfaced — script never running). Live 25h soak observation is owned by the first production deploy cycle and tracked via the runbook + dashboards.
- [x] Alert paired với runbook — `documents/05-guides/operations/runbooks/backup-job-failure.md` updated with the post-GAP-430 metric contract section, both alert arms explained, dated 2026-05-08. Note: gap originally referenced `backup-failure.md`; actual file name is `backup-job-failure.md` and the alert `runbook_url` annotation already points at the correct file.
- [ ] CI test (PromQL syntax + unit test rule) added vào `infrastructure/helm/...test/` — **deferred to GAP-435** (`promtool` + `helm` not installed in agent environment; bash + YAML syntax verified inline as interim — `bash -n scripts/backup-production.sh` clean, `python3 -c "yaml.safe_load(...)"` clean for `kitehub/docker/prometheus/alert-rules.yml`). Functional silent-failure root cause is fixed in this PR; the dedicated test fixture is the missing institutional guard against re-regression.

## Related

- Wave 33 GAP-389 (parent — original alert ship)
- Wave 40 Bucket E audit (PR #975 surfaced this)
- `documents/04-quality/audits/ops-readiness/2026-05-08-wave-40-milestone.md` §findings P0
- Sister: `MultiTenantDataLeak` alert cũng dead (`tenant_isolation_violations_total` không có producer) — file riêng nếu chưa có gap

## Estimated effort

~1-2h (PromQL rewrite + fire-test + runbook + CI rule test).

## Log

- **2026-05-08** Wave 41 Bucket A fix shipped — chose Option B (emit gauge from script) over Option A (rewrite PromQL to counter) because the runbook + dashboards already document gauge semantics, and the fix preserves the existing alert intent (time-since-last-success). Changes:
  - `scripts/backup-production.sh` — renamed `emit_counter` → `emit_metrics`; now emits BOTH `kite_backup_snapshots_total` (counter, unchanged) AND `kite_backup_last_success_timestamp_seconds` (gauge, new). Single Pushgateway POST so alert + counter stay in lockstep. Dry-run output verified — gauge line emitted with current unix timestamp.
  - `infrastructure/helm/kitehub/templates/prometheusrule.yaml` + `kitehub/docker/prometheus/alert-rules.yml` — alert PromQL hardened: `(time() - max(<gauge>) > 90000) or absent(<gauge>)`. The `absent()` arm closes the original silent-failure mode (alert silent when script never emits). Comment block updated from "METRIC PENDING" to "METRIC LIVE" with cross-ref to this gap.
  - `documents/05-guides/operations/runbooks/backup-job-failure.md` — new "Metric contract (post GAP-430)" section, both alert arms explained, last-updated date bumped to 2026-05-08.
  - Verification: `bash -n scripts/backup-production.sh` clean; `python3 -c "yaml.safe_load(...)"` clean for `kitehub/docker/prometheus/alert-rules.yml`; dry-run shows gauge line emitted with current unix timestamp. `promtool`/`helm` absent in agent env → dedicated test fixture deferred to GAP-435.
  Status flipped to 🟡 PARTIAL per `gap-done-discipline.md` §3 — root-cause functional fix landed, but AC #4 (CI rule-test fixture) genuinely cannot complete in this PR without the missing tooling; tracked under GAP-435.
- **2026-05-08** Filed during Wave 40 closure handoff cho Wave 41. Audit Bucket E Ops Readiness phát hiện regression — agent đề xuất số 428 nhưng đã collision với A UI; reassigned 430.
