# GAP-435: promtool alert-rule unit test fixture

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** DevOps / Monitoring
**Found:** 2026-05-08 (Wave 41 Bucket A AC #4 deferral)
**Affects:** Prometheus alert rules — currently no automated regression coverage on alert PromQL changes.

## Problem

Wave 41 Bucket A (PR #983) closed GAP-430 với fix metric mismatch trên `BackupJobFailure` alert. AC #4 ("alert unit test fire-test") deferred vì:
- `promtool` không có sẵn trong solo-dev WSL env
- CI workflow chưa có job chạy `promtool check rules` + alert unit tests

Hệ quả: alert PromQL có thể drift trong tương lai mà không có guard.

## Root Cause

`infrastructure/helm/**/prometheusrule.yaml` + `kitehub/docker/prometheus/alert-rules.yml` chỉ được validate qua `yaml.safe_load` parse — không có semantic check trên PromQL syntax hoặc test fire/no-fire trên metric inputs.

## Proposed Fix

1. Add CI job (e.g. trong `.github/workflows/core-ci.yml` hoặc workflow mới `prometheus-rules.yml`):
   ```yaml
   - uses: actions/setup-go@v5
   - run: go install github.com/prometheus/prometheus/cmd/promtool@latest
   - run: promtool check rules infrastructure/helm/**/prometheusrule.yaml
   - run: promtool check rules kitehub/docker/prometheus/alert-rules.yml
   - run: promtool test rules tests/prometheus/*-test.yml
   ```
2. Tạo `tests/prometheus/backup-alert-test.yml` fixture:
   - Input series mô phỏng cả 2 trạng thái (last_success_timestamp_seconds gần đây vs >86400s ago)
   - `absent()` arm test khi metric không có
   - Assert `BackupJobFailure` alert fire/no-fire đúng kỳ vọng
3. Document trong `documents/05-guides/operations/runbooks/backup-job-failure.md` cách thêm test mới khi alert mới được add.

## Acceptance Criteria

- [ ] CI job `prometheus-rules` chạy clean trên main
- [ ] `tests/prometheus/backup-alert-test.yml` cover ≥3 cases (fire khi stale, no-fire khi fresh, fire khi absent)
- [ ] CI fails khi PR break alert PromQL syntax hoặc semantic test
- [ ] Runbook document test-add-pattern

## Related

- Closes deferred AC #4 from GAP-430 (Wave 41 Bucket A, PR #983)
- Wave 41 plan §5 row A
- Reference: [Prometheus alert testing docs](https://prometheus.io/docs/prometheus/latest/configuration/unit_testing_rules/)

## Log

- **2026-05-08:** GAP filed as Wave 41 Bucket A follow-up per `gap-done-discipline.md` §3 PARTIAL exit ramp. Ship trong closure PR Wave 41.
