# Runbook: Backup Job Failure

**Alert:** `BackupJobFailure`
**Severity:** critical
**Last updated:** 2026-05-08 (GAP-430 Wave 41 Bucket A — alert/metric alignment)

## What does this alert mean?

The scheduled pre-deploy RDS snapshot (or equivalent backup job) for the production database has not reported a successful run within the expected window, OR the success-marker metric is missing entirely.

Detection uses the gauge `kite_backup_last_success_timestamp_seconds` emitted by `scripts/backup-production.sh` (via Pushgateway) at the end of every successful snapshot. Two arms fire this alert:

1. `time() - max(kite_backup_last_success_timestamp_seconds) > 90000` — last success was >25h ago (24h cadence + 1h grace).
2. `absent(kite_backup_last_success_timestamp_seconds)` — the gauge series itself disappeared (script never ran, Pushgateway lost the series, or scraper config drifted).

Both arms are intentional. The `absent()` arm exists because Wave 40 audit (Bucket E) found the previous PromQL only watched the gauge value, but the script never emitted that gauge — so the alert was silent for the entire Wave 33-40 window. GAP-430 fixed both sides: script now emits the gauge AND a counter (`kite_backup_snapshots_total`), and the alert checks for either staleness or disappearance.

**Every hour without a healthy backup increases the data-loss window** in the event of a Postgres disk corruption, accidental DROP, or region failure. Recovery RTO/RPO targets are documented in the platform DR runbook (GAP-119), and this alert is the canary that those targets are actually being met.

## Metric contract (post GAP-430)

| Metric | Type | Emitter | Watched by |
|---|---|---|---|
| `kite_backup_last_success_timestamp_seconds` | gauge (unix seconds) | `scripts/backup-production.sh` → Pushgateway | `BackupJobFailure` alert |
| `kite_backup_snapshots_total` | counter | `scripts/backup-production.sh` → Pushgateway | dashboards, retention reporting |

Labels on both: `type="pre_deploy"`, `region`, `instance`. Job label = `backup-production`.

If you are migrating an older CronJob (Helm `kitehub-backup` chart) to use the same metric contract, mirror the emit block in `scripts/backup-production.sh` (`emit_metrics` function) so all backup paths converge on the same series.

## Immediate checks (0-5 min)

1. **Locate the most recent backup attempt logs:**
   ```bash
   # K8s CronJob
   kubectl get cronjob -n kitehub kitehub-backup
   kubectl get jobs -n kitehub --selector=cronjob=kitehub-backup --sort-by=.metadata.creationTimestamp | tail -3
   kubectl logs -n kitehub job/<latest-job-name>

   # Docker dev (kitehub/scripts/backup.sh manually triggered)
   ls -la kitehub/backups/ | tail -5
   ```
2. **Check destination bucket** — was the file actually uploaded?
   ```bash
   # Production S3
   aws s3 ls s3://kite-platform-backups/postgres/ --recursive | sort | tail -10
   # Dev MinIO
   docker exec kite-minio mc ls local/kite-backups/postgres/ | tail -10
   ```
3. **Postgres health** — backup may be failing because the source DB is unhappy:
   ```bash
   docker exec kite-postgres pg_isready -U postgres
   docker exec kite-postgres psql -U postgres -c "SELECT pg_database_size('kitehub');"
   ```
4. **Disk space at backup destination** — S3 quota or local volume:
   ```bash
   docker exec kite-minio df -h /data
   ```

## Likely causes

- **Credentials expired/rotated** — backup script uses an AWS access key that was rotated without updating the K8s secret or `.env` file. **Fix:** rotate keys, update `kubectl create secret generic backup-credentials --from-literal=...`, re-run job.
- **MinIO/S3 bucket missing or permissions changed** — bucket was deleted or IAM policy revoked write. **Fix:** verify bucket exists (`aws s3 ls`), IAM role/user has `s3:PutObject` + `s3:PutObjectAcl`. See `documents/05-guides/operations/restore-procedure.md` (GAP-117) for credential layout.
- **Postgres connection refused / timeout** — backup pod cannot reach `kite-postgres:5433` because of network policy or service rename. **Fix:** verify NetworkPolicy, DNS, port — same as `service-down.md` checklist.
- **Disk full at source or destination** — `pg_dump` writes temp files in `/tmp` of the runner; if container `emptyDir` exhausts, dump aborts. **Fix:** scale ephemeral storage, or stream directly to S3 via `pg_dump | aws s3 cp - s3://...`.
- **Cron schedule misalignment** — `tz` mismatch between K8s scheduler and runtime; job ran at unexpected time and was killed by ttlSecondsAfterFinished before metric pushed.
- **Versioning/lifecycle policy mis-applied** — bucket has lifecycle that auto-deletes objects within 24h, so the file existed but expired before the next probe. **Fix:** review lifecycle config from GAP-118.

## Mitigation

```bash
# 1. Trigger backup immediately (do NOT wait for next scheduled run)
kubectl create job --from=cronjob/kitehub-backup kitehub-backup-manual-$(date +%s) -n kitehub
# OR Docker dev:
cd kitehub && ./scripts/backup.sh

# 2. Verify the artifact lands and is valid (gzip header sanity check)
aws s3 cp s3://kite-platform-backups/postgres/$(date +%Y%m%d)-latest.sql.gz /tmp/check.sql.gz
gunzip -t /tmp/check.sql.gz && echo "OK: gzip integrity passes"

# 3. If still failing, capture full debug trace and run a manual pg_dump from a privileged pod
kubectl run pgdump-debug --rm -it --image=postgres:15 --restart=Never -- \
  pg_dump -h kite-postgres -U postgres -d kitehub > /tmp/manual-dump.sql

# 4. After fix, manually push a success metric so the alert clears in next eval
echo "kite_backup_last_success_timestamp_seconds $(date +%s)" \
  | curl --data-binary @- http://pushgateway:9091/metrics/job/kitehub-backup
```

After mitigation, the next scheduled run must succeed. If two consecutive scheduled runs fail, re-page until backup pipeline is healthy AND verified by running the restore drill (`.github/workflows/monthly-restore-drill.yml` from GAP-117).

## When to escalate

- **>48h since last successful backup** → P0 escalation; engage platform lead AND prepare to invoke DR runbook
- **Restore verification fails after seemingly-successful backup** → backup pipeline is silently corrupting data; halt deploys until root-caused
- **Cross-region replication broken** (per GAP-118) → escalate to infra lead; treat as elevated severity even if primary backup works

## Related

- Alert rule: `kitehub/docker/prometheus/alert-rules.yml` (kitehub-platform-alerts group), `infrastructure/helm/kitehub/templates/prometheusrule.yaml`
- Procedure: [`../restore-procedure.md`](../restore-procedure.md) (GAP-117 restore runbook + verify script)
- Platform DR: `documents/05-guides/operations/dr-runbook.md` (GAP-119, RTO/RPO matrix)
- Memory: `feedback_post_merge_doc_sync.md` (gap closure discipline)
- Related runbooks: [`high-disk-usage.md`](./high-disk-usage.md), [`flyway-migration-failure.md`](./flyway-migration-failure.md)
