# Pre-deploy Production Backup Runbook

**Status:** Active
**Created:** 2026-05-07 (GAP-389-A, Wave 36 Bucket C)
**Owner:** Coordinator-on-call (per `release-deploy-standard.md` §4.2)
**Standards:** AWS Well-Architected Reliability pillar (REL-09); `release-deploy-standard.md` §3.1 (Pre-release subset) and §3.4 (MAJOR / first PRODUCTION)

---

## 1. Purpose

Every production deploy MUST be preceded by an RDS snapshot serving as the rollback point if migrations fail or post-deploy smoke tests reveal a regression. This runbook documents the automated path (CI gate) and the manual fallback (coordinator-invoked from a workstation).

The script `scripts/backup-production.sh` encapsulates the operation; the workflow `.github/workflows/deploy-production.yml` invokes it as the first step before Helm upgrade.

---

## 2. Automated path (CI gate — primary)

`deploy-production.yml` runs `scripts/backup-production.sh` immediately after kubeconfig is configured, before the `helm upgrade` step. The snapshot identifier is exported as `PRE_DEPLOY_SNAPSHOT` so subsequent steps (and rollback) can reference it.

Required GitHub secrets:

| Secret | Purpose | Default if unset |
|--------|---------|------------------|
| `AWS_ROLE_ARN` | OIDC role assumed for `aws rds create-db-snapshot` | (workflow fails — required) |
| `RDS_INSTANCE_ID` | RDS DB instance identifier | `kite-rds-prod` |
| `PUSHGATEWAY_URL` | Optional — Prometheus pushgateway for counter | unset → metric logged only |

The role MUST have permissions:
- `rds:CreateDBSnapshot` on `arn:aws:rds:ap-southeast-1:<account>:db:kite-rds-prod`
- `rds:DescribeDBSnapshots`
- `rds:AddTagsToResource`

Failure of the backup step aborts the deploy (no Helm upgrade, no rollout). This is the intended behavior — never deploy without a rollback point.

---

## 3. Manual fallback (coordinator workstation)

If CI is unavailable (GitHub Actions outage, role-assume failure) and the deploy is time-sensitive:

```bash
# 1) Authenticate to AWS production account
aws sso login --profile kite-prod

# 2) Run the script (defaults match production)
AWS_PROFILE=kite-prod ./scripts/backup-production.sh

# 3) Capture snapshot id from stdout for rollback record
#    Example output: kite-prod-pre-deploy-20260507-143012
```

Override variables for non-default environments (e.g., staging dry-run):

```bash
AWS_REGION=ap-southeast-1 \
RDS_INSTANCE_ID=kite-rds-staging \
SNAPSHOT_PREFIX=kite-staging-pre-deploy \
./scripts/backup-production.sh
```

---

## 4. Smoke / verification

Before relying on the script in a deploy, validate behavior with `--dry-run`:

```bash
./scripts/backup-production.sh --dry-run
# Expected: pre-flight skipped, simulated AWS invocation logged, snapshot id printed.
# Exit code: 0
```

shellcheck passes cleanly (only SC2059 advisory which is mitigated by using `printf '%s' "$VAR"` form). Re-run after edits.

---

## 5. Restore from snapshot (rollback path)

If post-deploy smoke fails and the deploy must roll back to the pre-deploy snapshot, follow `documents/05-guides/operations/restore-runbook.md` (per GAP-117) using the snapshot identifier captured in step 2/3 above. Summary:

1. Halt all writes (scale subscription/branding deployments to 0 replicas).
2. Restore RDS snapshot to a new DB instance: `aws rds restore-db-instance-from-db-snapshot`.
3. Re-point Helm `global.database.host` to the restored instance and `helm rollback` to the previous Helm revision.
4. Bring services back up; smoke-test before opening to traffic.

---

## 6. Observability

Every successful invocation emits a Prometheus-format counter line:

```
kite_backup_snapshots_total{type="pre_deploy",region="ap-southeast-1",instance="kite-rds-prod"} 1
```

If `PUSHGATEWAY_URL` is set, the line is pushed to `${PUSHGATEWAY_URL}/metrics/job/backup-production/instance/${RDS_INSTANCE_ID}`. Otherwise the line is logged to stderr — operators can scrape from the workflow log or set up `kite_backup_last_success_timestamp_seconds` derivation in Loki/CloudWatch.

Alert rule (already declared in `infrastructure/k8s/prometheus/alerts/`):

```yaml
- alert: KiteBackupMissing
  expr: time() - kite_backup_last_success_timestamp_seconds > 86400
  for: 10m
  annotations:
    summary: "No production backup in last 24h"
    runbook_url: "https://github.com/.../documents/05-guides/deploy/backup-runbook.md"
```

---

## 7. Retention

Snapshots created by this runbook follow the existing RDS automated-retention policy (default 7 days). For long-term retention (compliance, disaster recovery), promote selected snapshots to manual snapshots with `aws rds copy-db-snapshot --copy-tags`. Snapshot tags `Source=pre-deploy-ci` and `DeployTimestamp=<ts>` are set automatically to aid lifecycle management.

---

## 8. Related

- Script: `scripts/backup-production.sh`
- Workflow: `.github/workflows/deploy-production.yml`
- Gap: `documents/04-quality/gaps/GAP-389-wave-33-ops-p1-cluster.md`
- Standards: `.claude/rules/release-deploy-standard.md` §3.1 + §3.4
- Restore runbook (separate): `documents/05-guides/operations/restore-runbook.md` (GAP-117)
- Alert rule: `infrastructure/k8s/prometheus/alerts/backup.yaml`
